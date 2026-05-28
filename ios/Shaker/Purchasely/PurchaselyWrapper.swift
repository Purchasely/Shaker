import UIKit
@preconcurrency import Purchasely

@MainActor
final class PurchaselyWrapper: PurchaselyWrapping {

    static let shared = PurchaselyWrapper()

    private var apiKey: String = ""
    private var logLevel: PLYLogger.PLYLogLevel = .debug
    private var isObserverActionRunning = false

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
        onReady: @escaping @Sendable (Bool, Error?) -> Void
    ) {
        self.apiKey = apiKey
        self.logLevel = logLevel

        let selectedMode = PurchaselySDKMode.current()
        // SDK v6 develop currently performs StoreKit 2 transaction scans before
        // calling the initialization callback. In the sample app we keep native
        // Observer-mode purchases on StoreKit 2 through PurchaseManager, and start
        // Purchasely itself with StoreKit 1 so the demo UI can become ready reliably.
        let storekitSettings: StorekitSettings = .storeKit1

        Purchasely.apiKey(apiKey)
            .appUserId(appUserId)
            .runningMode(selectedMode.runningMode)
            .storekitSettings(storekitSettings)
            .logLevel(logLevel)
            .start { error in
                if let error {
                    print("[Shaker] Purchasely configuration error: \(error.localizedDescription)")
                    onReady(false, error)
                } else {
                    print("[Shaker] Purchasely SDK configured successfully (mode: \(selectedMode.title))")
                    Task { @MainActor in
                        PremiumManager.shared.refreshPremiumStatus()
                    }
                    onReady(true, nil)
                }
            }

        Purchasely.allowDeeplink(true)

        Purchasely.setEventDelegate(self)

        // PURCHASELY: Observe user attribute changes to invalidate cached presentations
        // Audience targeting depends on attributes, so cached paywalls become stale
        // when any attribute changes. Docs: https://docs.purchasely.com/docs/listener-delegate
        Purchasely.setUserAttributeDelegate(self)

        registerActionInterceptors()
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

    private func registerActionInterceptors() {
        Purchasely.removeAllActionInterceptors()

        Purchasely.interceptAction(.login) { _, _ in
            print("[Shaker] Paywall login action intercepted")
            return .success
        }

        Purchasely.interceptAction(.navigate) { _, parameters in
            if let url = parameters?.url {
                print("[Shaker] Paywall navigate action: \(url)")
                await MainActor.run { UIApplication.shared.open(url) }
            }
            return .success
        }

        Purchasely.interceptAction(.purchase) { [weak self] _, parameters in
            guard let self else { return .notHandled }
            return await self.handlePurchase(parameters: parameters)
        }

        Purchasely.interceptAction(.restore) { [weak self] _, _ in
            guard let self else { return .notHandled }
            return await self.handleRestore()
        }
    }

    @MainActor
    internal func handlePurchase(parameters: PLYPresentationActionParameters?) async -> PLYInterceptResult {
        await handleObserverAction {
            guard let productId = parameters?.plan?.appleProductId else {
                print("[Shaker] Observer mode purchase: missing product ID")
                return .error("Missing product ID")
            }
            if let promoOffer = parameters?.promoOffer {
                return await PurchaseManager.shared.purchaseWithPromoOffer(
                    productId: productId,
                    storeOfferId: promoOffer.storeOfferId
                )
            }
            return await PurchaseManager.shared.purchase(productId: productId)
        }
    }

    @MainActor
    internal func handleRestore() async -> PLYInterceptResult {
        await handleObserverAction {
            await PurchaseManager.shared.restore()
        }
    }

    /// Routes purchase/restore through the native PurchaseManager in Observer mode,
    /// or returns `.notHandled` so the SDK owns the action in Full mode.
    @MainActor
    private func handleObserverAction(
        run: @escaping () async -> TransactionResult
    ) async -> PLYInterceptResult {
        guard PurchaselySDKMode.current() == .paywallObserver else {
            return .notHandled
        }
        guard #available(iOS 15.0, *) else {
            return .notHandled
        }
        guard !isObserverActionRunning else {
            print("[Shaker] Observer mode action ignored: another transaction is already running")
            // Block the SDK's default flow (Observer mode owns purchases). .notHandled would
            // let the SDK launch its own purchase for this duplicate/ignored action.
            return .success
        }

        isObserverActionRunning = true
        defer { isObserverActionRunning = false }

        let result = await run()
        return await handleTransactionResult(result)
    }

    // MARK: - Transaction Result Handling

    @MainActor
    private func handleTransactionResult(_ result: TransactionResult) async -> PLYInterceptResult {
        switch result {
        case .success:
            // PURCHASELY: Observer mode flow — we await synchronize() not because closing
            // is blocked otherwise (closeAllScreens is safe to call anytime), but because
            // we chain a success_payment placement next; that placement's audience targeting
            // depends on the just-activated subscription, so we want the SDK's state fresh
            // before the success screen fetches it. Premium refresh is deferred to the
            // success_payment chain via pendingSuccessfulPurchase.
            pendingSuccessfulPurchase = true
            do {
                try await synchronizeReceipt()
                PresentationCache.shared.invalidateAll()
                Purchasely.closeAllScreens()
                print("[Shaker] Transaction success — synchronized; presentation closed, awaiting success_payment")
                return .success
            } catch {
                print("[Shaker] Synchronize failed after transaction: \(error.localizedDescription)")
                Purchasely.closeAllScreens()
                return .failed
            }

        case .cancelled:
            print("[Shaker] Transaction cancelled")
            // Observer mode owns the transaction: block the SDK's default purchase/restore
            // flow on cancellation (v5 processAction(false)). .notHandled maps to
            // processAction(true) and would let the SDK run its own flow.
            return .success

        case .error(let message):
            print("[Shaker] Transaction error: \(message ?? "unknown")")
            return .failed

        case .idle:
            return .notHandled
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
        Purchasely.handleDeeplink(deeplink)
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
            let builder = PLYPresentationBuilder.from(placementId: placementId)
            if let contentId {
                builder.contentId(contentId)
            }
            var didFinishPresentation = false
            let finishPresentation: (DisplayResult) -> Void = { [weak self] displayResult in
                // PURCHASELY: After the paywall closes, if a purchase succeeded —
                // either reported directly by the SDK (Full mode) or signaled via
                // pendingSuccessfulPurchase (Observer mode) — chain a "success_payment"
                // placement. Skip if this IS the success_payment to avoid recursion.
                Task { @MainActor [weak self] in
                    guard !didFinishPresentation else { return }
                    didFinishPresentation = true
                    let pending = self?.pendingSuccessfulPurchase ?? false
                    print("[Shaker] loadPresentation finished — placement=\(placementId) displayResult=\(displayResult) pendingSuccessfulPurchase=\(pending)")
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
            builder.onClose {
                finishPresentation(.cancelled)
            }
            builder.onDismissed { outcome in
                finishPresentation(PurchaselyWrapper.displayResult(from: outcome))
            }

            builder.build().preload { presentation, error in
                guard let presentation else {
                    continuation.resume(returning: .error(error))
                    return
                }
                // SDK v6 exposes lifecycle callbacks on the loaded presentation.
                // Re-assign them after preload as a defensive measure because the
                // builder-seeded callbacks are not fired by all develop snapshots.
                presentation.onClose = {
                    finishPresentation(.cancelled)
                }
                presentation.onDismissed = { outcome in
                    finishPresentation(PurchaselyWrapper.displayResult(from: outcome))
                }
                switch presentation.type {
                case .deactivated:
                    continuation.resume(returning: .deactivated)
                case .client:
                    continuation.resume(returning: .client(presentation: presentation))
                default:
                    continuation.resume(returning: .success(presentation: presentation))
                }
            }
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
        // dismissal callback below is wired up freshly each time. The cache binds onResult
        // at first fetch and reuses it across callers, which we want to avoid for the chain.
        PLYPresentationBuilder.from(placementId: PurchaselyWrapper.successPaymentPlacement)
            .onDismissed { [weak self] _ in
                Task { @MainActor [weak self] in
                    self?.refreshAfterSuccessPayment()
                }
            }
            .build()
            .preload { [weak self] presentation, error in
                Task { @MainActor [weak self] in
                    guard let self else { return }
                    let presentationId = presentation?.id ?? "nil"
                    let presentationType = presentation.map { "\($0.type)" } ?? "nil"
                    print("[Shaker] success_payment preload — id=\(presentationId) type=\(presentationType) error=\(error?.localizedDescription ?? "none")")
                    if let presentation, presentation.type != .deactivated {
                        presentation.display(from: nil)
                    } else {
                        // No success_payment placement (deactivated, error) — still refresh
                        print("[Shaker] success_payment placement unavailable: \(error?.localizedDescription ?? "deactivated")")
                        self.refreshAfterSuccessPayment()
                    }
                }
            }
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

    private static func displayResult(from outcome: PLYPresentationOutcome) -> DisplayResult {
        switch outcome.purchaseResult {
        case .purchased:
            return .purchased(planName: outcome.plan?.name)
        case .restored:
            return .restored(planName: outcome.plan?.name)
        default:
            return .cancelled
        }
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
