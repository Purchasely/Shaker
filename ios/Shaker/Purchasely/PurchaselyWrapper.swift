import UIKit
@preconcurrency import Purchasely

@MainActor
final class PurchaselyWrapper: PurchaselyWrapping {

    static let shared = PurchaselyWrapper()

    private var apiKey: String = ""
    private var logLevel: PLYLogger.PLYLogLevel = .debug
    private var observerActionTask: Task<Void, Never>?

    // PURCHASELY: Flag set when a successful purchase is reported by PurchaseManager
    // (Observer mode). In Full mode, the SDK reports `.purchased` directly via the
    // presentation completion. In both cases, the loadPresentation completion consumes
    // this signal to chain a "success_payment" placement once the original paywall
    // is dismissed.
    private var pendingSuccessfulPurchase: Bool = false

    private static let successPaymentPlacement = "success_payment"

    private init() {
        // Wire up providers so PurchaseManager doesn't need to import Purchasely
        if #available(iOS 15.0, *) {
            PurchaseManager.shared.anonymousUserIdProvider = { [weak self] in
                self?.anonymousUserId ?? ""
            }
            PurchaseManager.shared.signPromotionalOfferProvider = { [weak self] productId, offerId, success, failure in
                self?.signPromotionalOffer(
                    storeProductId: productId,
                    storeOfferId: offerId,
                    success: { signature in
                        success(PLYOfferSignatureData(
                            identifier: signature.identifier,
                            keyIdentifier: signature.keyIdentifier,
                            nonce: signature.nonce,
                            signature: signature.signature,
                            timestamp: Int(signature.timestamp)
                        ))
                    },
                    failure: failure
                )
            }
        }

        // Observe SDK mode changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleSdkModeDidChange),
            name: .purchaselySdkModeDidChange,
            object: nil
        )
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    // MARK: - SDK Initialization

    func initialize(
        apiKey: String,
        appUserId: String? = nil,
        logLevel: PLYLogger.PLYLogLevel = .debug,
        onReady: @escaping (Bool, Error?) -> Void
    ) {
        self.apiKey = apiKey
        self.logLevel = logLevel

        let selectedMode = PurchaselySDKMode.current()
        let storekitSettings: StorekitSettings = .storeKit2

        Purchasely.start(
            withAPIKey: apiKey,
            appUserId: appUserId,
            runningMode: selectedMode.runningMode,
            storekitSettings: storekitSettings,
            logLevel: logLevel
        ) { success, error in
            if success {
                print("[Shaker] Purchasely SDK configured successfully (mode: \(selectedMode.title))")
                Task { @MainActor in
                    PremiumManager.shared.refreshPremiumStatus()
                }
            } else {
                print("[Shaker] Purchasely configuration error: \(error?.localizedDescription ?? "unknown")")
            }
            onReady(success, error)
        }

        Purchasely.readyToOpenDeeplink(true)

        Purchasely.setEventDelegate(self)

        // PURCHASELY: Observe user attribute changes to invalidate cached presentations
        // Audience targeting depends on attributes, so cached paywalls become stale
        // when any attribute changes. Docs: https://docs.purchasely.com/docs/listener-delegate
        Purchasely.setUserAttributeDelegate(self)

        Purchasely.setPaywallActionsInterceptor { [weak self] action, parameters, info, proceed in
            self?.handlePaywallAction(action: action, parameters: parameters, info: info, processAction: proceed)
        }
    }

    @MainActor
    func restart() {
        // SDK mode change invalidates any cached presentations (different session)
        PresentationCache.shared.invalidateAll()
        closeDisplayedPresentation()
        let storedUserId = UserDefaults.standard.string(forKey: "user_id")
        initialize(apiKey: apiKey, appUserId: storedUserId, logLevel: logLevel) { _, _ in }
    }

    @MainActor
    func closeDisplayedPresentation() {
        Purchasely.closeAllScreens()
    }

    @objc private func handleSdkModeDidChange() {
        Task { @MainActor [weak self] in
            self?.restart()
        }
    }

    // MARK: - Interceptor Logic

    internal func handlePaywallAction(
        action: PLYPresentationAction,
        parameters: PLYPresentationActionParameters?,
        info: PLYPresentationInfo?,
        processAction: @escaping (Bool) -> Void
    ) {
        switch action {
        case .login:
            print("[Shaker] Paywall login action intercepted")
            processAction(false)

        case .navigate:
            if let url = parameters?.url {
                print("[Shaker] Paywall navigate action: \(url)")
                Task { @MainActor in UIApplication.shared.open(url) }
            }
            processAction(false)

        case .purchase:
            handleObserverAction(processAction: processAction, fallback: true) {
                guard let productId = parameters?.plan?.appleProductId else {
                    print("[Shaker] Observer mode purchase: missing product ID")
                    return .error("Missing product ID")
                }
                return await PurchaseManager.shared.purchase(productId: productId)
            }

        case .restore:
            handleObserverAction(processAction: processAction, fallback: true) {
                await PurchaseManager.shared.restore()
            }

        default:
            processAction(true)
        }
    }

    /// Routes purchase/restore through the native PurchaseManager in Observer mode,
    /// or hands control back to the SDK in Full mode.
    private func handleObserverAction(
        processAction: @escaping (Bool) -> Void,
        fallback: Bool,
        run: @escaping () async -> TransactionResult
    ) {
        guard PurchaselySDKMode.current() == .paywallObserver else {
            processAction(fallback)
            return
        }
        guard #available(iOS 15.0, *) else {
            processAction(false)
            return
        }
        Task { @MainActor [weak self] in
            guard let self else {
                processAction(false)
                return
            }
            guard self.observerActionTask == nil else {
                print("[Shaker] Observer mode action ignored: another transaction is already running")
                processAction(false)
                return
            }
            let task = Task { @MainActor [weak self] in
                defer { self?.observerActionTask = nil }
                guard let self else {
                    processAction(false)
                    return
                }
                let result = await run()
                await self.handleTransactionResult(result, proceed: processAction)
            }
            self.observerActionTask = task
        }
    }

    // MARK: - Transaction Result Handling

    @MainActor
    private func handleTransactionResult(
        _ result: TransactionResult,
        proceed: @escaping (Bool) -> Void
    ) async {
        switch result {
        case .success:
            // PURCHASELY: Observer mode flow — we await synchronize() not because closing
            // is blocked otherwise (closeAllScreens is safe to call anytime), but because
            // we chain a success_payment placement next; that placement's audience targeting
            // depends on the just-activated subscription, so we want the SDK's state fresh
            // before loadPresentation's completion fetches it. Order matters: proceed(false)
            // BEFORE closeAllScreens(). Premium refresh is deferred to the success_payment
            // chain via pendingSuccessfulPurchase.
            pendingSuccessfulPurchase = true
            do {
                try await synchronizeReceipt()
                PresentationCache.shared.invalidateAll()
                proceed(false)
                Purchasely.closeAllScreens()
                print("[Shaker] Transaction success — synchronized; presentation closed, awaiting success_payment")
            } catch {
                print("[Shaker] Synchronize failed after transaction: \(error.localizedDescription)")
                proceed(false)
                Purchasely.closeAllScreens()
            }

        case .cancelled:
            proceed(false)
            print("[Shaker] Transaction cancelled")

        case .error(let message):
            proceed(false)
            print("[Shaker] Transaction error: \(message ?? "unknown")")

        case .idle:
            break
        }
    }

    private func synchronizeReceipt() async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            Purchasely.synchronize(
                success: { continuation.resume() },
                failure: { error in continuation.resume(throwing: error) }
            )
        }
    }

    // MARK: - Deeplinks

    @discardableResult
    func isDeeplinkHandled(deeplink: URL) -> Bool {
        Purchasely.isDeeplinkHandled(deeplink: deeplink)
    }

    // MARK: - Presentation Loading

    @MainActor
    func loadPresentation(
        placementId: String,
        contentId: String? = nil,
        onResult: @escaping @MainActor (DisplayResult) -> Void
    ) async -> FetchResult {
        // Cache hit — skip the network fetch. The `onResult` closure is bound
        // at first fetch time (SDK-internal); subsequent callers share that
        // binding. For Shaker, all `onResult` closures are equivalent (refresh
        // premium on purchased/restored), so this is safe.
        if let cached = PresentationCache.shared.get(placementId: placementId, contentId: contentId) {
            return cached
        }

        let result: FetchResult = await withCheckedContinuation { continuation in
            Purchasely.fetchPresentation(
                for: placementId,
                contentId: contentId,
                fetchCompletion: { presentation, error in
                    guard let presentation = presentation else {
                        continuation.resume(returning: .error(error))
                        return
                    }
                    switch presentation.type {
                    case .deactivated:
                        continuation.resume(returning: .deactivated)
                    case .client:
                        continuation.resume(returning: .client(presentation: presentation))
                    default:
                        continuation.resume(returning: .success(presentation: presentation))
                    }
                },
                completion: { [weak self] result, plan in
                    let displayResult: DisplayResult
                    switch result {
                    case .purchased:
                        displayResult = .purchased(planName: plan?.name)
                    case .restored:
                        displayResult = .restored(planName: plan?.name)
                    default:
                        displayResult = .cancelled
                    }
                    // PURCHASELY: After the paywall closes, if a purchase succeeded —
                    // either reported directly by the SDK (Full mode) or signaled via
                    // pendingSuccessfulPurchase (Observer mode) — chain a "success_payment"
                    // placement. Skip if this IS the success_payment to avoid recursion.
                    Task { @MainActor [weak self] in
                        let pending = self?.pendingSuccessfulPurchase ?? false
                        print("[Shaker] loadPresentation completion — placement=\(placementId) displayResult=\(displayResult) pendingSuccessfulPurchase=\(pending)")
                        onResult(displayResult)
                        let purchaseHappened: Bool = {
                            switch displayResult {
                            case .purchased, .restored: return true
                            case .cancelled: return pending
                            }
                        }()
                        if purchaseHappened && placementId != PurchaselyWrapper.successPaymentPlacement {
                            self?.pendingSuccessfulPurchase = false
                            print("[Shaker] Chaining success_payment after \(placementId)")
                            self?.showSuccessPaymentScreen()
                        }
                    }
                }
            )
        }

        // Cache everything except errors (errors should be retried on next call)
        if case .error = result { /* skip */ } else {
            PresentationCache.shared.set(result, placementId: placementId, contentId: contentId)
        }
        return result
    }

    // MARK: - Success Payment Chain

    @MainActor
    private func showSuccessPaymentScreen() {
        // PURCHASELY: Fetch directly via the SDK (bypassing PresentationCache) so the
        // completion below is wired up freshly each time. The cache binds onResult at
        // first fetch and reuses it across callers, which we want to avoid for the chain.
        Purchasely.fetchPresentation(
            for: PurchaselyWrapper.successPaymentPlacement,
            contentId: nil,
            fetchCompletion: { [weak self] presentation, error in
                Task { @MainActor [weak self] in
                    guard let self else { return }
                    let presentationId = presentation?.id ?? "nil"
                    let presentationType = presentation.map { "\($0.type)" } ?? "nil"
                    print("[Shaker] success_payment fetchCompletion — id=\(presentationId) type=\(presentationType) error=\(error?.localizedDescription ?? "none")")
                    if let presentation, presentation.type != .deactivated {
                        presentation.display(from: nil)
                    } else {
                        // No success_payment placement (deactivated, error) — still refresh
                        print("[Shaker] success_payment placement unavailable: \(error?.localizedDescription ?? "deactivated")")
                        self.refreshAfterSuccessPayment()
                    }
                }
            },
            completion: { [weak self] _, _ in
                Task { @MainActor [weak self] in
                    self?.refreshAfterSuccessPayment()
                }
            }
        )
    }

    private func refreshAfterSuccessPayment() {
        // PURCHASELY: Refresh subscriptions via the wrapper without forcing the cache
        // (default invalidateCache: false). The SDK has had time during the success_payment
        // screen to update its cache after the recent synchronize() call.
        userSubscriptions(
            success: { subscriptions in
                Task { @MainActor in
                    PremiumManager.shared.updatePremium(from: subscriptions)
                }
            },
            failure: { error in
                print("[Shaker] Error refreshing after success_payment: \(error.localizedDescription)")
            }
        )
    }

    // MARK: - Modal Display

    @MainActor
    func display(presentation: PLYPresentation, from viewController: UIViewController?) {
        presentation.display(from: viewController)
    }

    // MARK: - Embedded View Controller

    func getController(presentation: PLYPresentation) -> PLYPresentationViewController? {
        presentation.controller
    }

    // MARK: - User Management

    func userLogin(userId: String, onRefresh: @escaping (Bool) -> Void) {
        Purchasely.userLogin(with: userId, shouldRefresh: onRefresh)
    }

    func userLogout() {
        Purchasely.userLogout()
    }

    var anonymousUserId: String {
        Purchasely.anonymousUserId
    }

    // MARK: - User Attributes

    func setUserAttribute(_ value: String, forKey key: String) {
        Purchasely.setUserAttribute(withStringValue: value, forKey: key)
    }

    func setUserAttribute(_ value: Bool, forKey key: String) {
        Purchasely.setUserAttribute(withBoolValue: value, forKey: key)
    }

    func setUserAttribute(_ value: Int, forKey key: String) {
        Purchasely.setUserAttribute(withIntValue: value, forKey: key)
    }

    func setUserAttribute(_ value: Double, forKey key: String) {
        Purchasely.setUserAttribute(withDoubleValue: value, forKey: key)
    }

    func incrementUserAttribute(forKey key: String) {
        Purchasely.incrementUserAttribute(withKey: key)
    }

    // MARK: - Subscriptions

    func userSubscriptions(
        success: @escaping ([PLYSubscription]?) -> Void,
        failure: @escaping (Error) -> Void
    ) {
        // PURCHASELY: Default invalidateCache=false so the SDK returns its cached
        // subscriptions list. Pass `true` only when you must hit the network.
        Purchasely.userSubscriptions(success: success, failure: failure)
    }

    // MARK: - Restore

    func restoreAllProducts(
        success: @escaping () -> Void,
        failure: @escaping (Error) -> Void
    ) {
        Purchasely.restoreAllProducts(success: success, failure: failure)
    }

    // MARK: - Observer Mode

    func synchronize() {
        // Server-side sync can change subscription state & therefore targeting.
        // Invalidate cached presentations on success so the next fetch reflects
        // the fresh state.
        Task { @MainActor in
            do {
                try await synchronizeReceipt()
                PresentationCache.shared.invalidateAll()
            } catch {
                print("[Shaker] Synchronize failed: \(error.localizedDescription)")
            }
        }
    }

    func signPromotionalOffer(
        storeProductId: String,
        storeOfferId: String,
        success: @escaping (PLYOfferSignature) -> Void,
        failure: @escaping (Error) -> Void
    ) {
        Purchasely.signPromotionalOffer(
            storeProductId: storeProductId,
            storeOfferId: storeOfferId,
            success: success,
            failure: failure
        )
    }

    // MARK: - GDPR Consent

    func revokeDataProcessingConsent(for purposes: Set<PLYDataProcessingPurpose>) {
        Purchasely.revokeDataProcessingConsent(for: purposes)
    }

    // MARK: - SDK Info

    var sdkVersion: String {
        Purchasely.getSDKVersion() ?? ""
    }

}

// MARK: - Event Delegate

extension PurchaselyWrapper: PLYEventDelegate {
    nonisolated func eventTriggered(_ event: PLYEvent, properties: [String: Any]?) {
        print("[Shaker] Event: \(event.name) | Properties: \(properties ?? [:])")
    }
}

// MARK: - User Attribute Delegate

extension PurchaselyWrapper: PLYUserAttributeDelegate {

    // PURCHASELY: Invalidate the presentation cache whenever a user attribute
    // changes. Audience targeting depends on attributes, so any change can
    // alter which paywall a placement resolves to. We invalidate for every
    // source (app-driven AND SDK-internal) for simplicity — a future SDK 6.x
    // is expected to handle this natively at the placement level.
    // Docs: https://docs.purchasely.com/docs/listener-delegate#implementation-1

    nonisolated func onUserAttributeSet(key: String,
                                        type: PLYUserAttributeType,
                                        value: Any?,
                                        source: PLYUserAttributeSource) {
        print("[Shaker] User attribute set: \(key)=\(value ?? "nil") (source: \(source))")
        PresentationCache.shared.invalidateAll()
    }

    nonisolated func onUserAttributeRemoved(key: String, source: PLYUserAttributeSource) {
        print("[Shaker] User attribute removed: \(key) (source: \(source))")
        PresentationCache.shared.invalidateAll()
    }
}
