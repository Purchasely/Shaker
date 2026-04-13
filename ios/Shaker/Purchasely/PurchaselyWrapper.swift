import UIKit
import Combine
import Purchasely

final class PurchaselyWrapper: PurchaselyWrapping {

    static let shared = PurchaselyWrapper()

    private var apiKey: String = ""
    private var logLevel: PLYLogger.PLYLogLevel = .debug
    private var pendingProcessAction: ((Bool) -> Void)?
    private var cancellables = Set<AnyCancellable>()

    private init() {
        // Observe TransactionResult from PurchaseManager
        if #available(iOS 15.0, *) {
            PurchaseManager.shared.resultSubject
                .receive(on: DispatchQueue.main)
                .sink { [weak self] result in
                    self?.handleTransactionResult(result)
                }
                .store(in: &cancellables)

            // Wire up providers so PurchaseManager doesn't need to import Purchasely
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
                PremiumManager.shared.refreshPremiumStatus()
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

    func restart() {
        // SDK mode change invalidates any cached presentations (different session)
        PresentationCache.shared.invalidateAll()
        closeDisplayedPresentation()
        let storedUserId = UserDefaults.standard.string(forKey: "user_id")
        initialize(apiKey: apiKey, appUserId: storedUserId, logLevel: logLevel) { _, _ in }
    }

    func closeDisplayedPresentation() {
        Purchasely.closeDisplayedPresentation()
    }

    @objc private func handleSdkModeDidChange() {
        DispatchQueue.main.async { [weak self] in
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
                DispatchQueue.main.async {
                    UIApplication.shared.open(url)
                }
            }
            processAction(false)
        case .purchase:
            if PurchaselySDKMode.current() == .paywallObserver {
                if let productId = parameters?.plan?.appleProductId {
                    if #available(iOS 15.0, *) {
                        pendingProcessAction?(false)
                        pendingProcessAction = processAction
                        PurchaseManager.shared.purchaseSubject.send(PurchaseRequest(productId: productId))
                    } else {
                        processAction(false)
                    }
                } else {
                    print("[Shaker] Observer mode purchase: missing product ID")
                    processAction(false)
                }
            } else {
                processAction(true)
            }
        case .restore:
            if PurchaselySDKMode.current() == .paywallObserver {
                if #available(iOS 15.0, *) {
                    pendingProcessAction?(false)
                    pendingProcessAction = processAction
                    PurchaseManager.shared.restoreSubject.send()
                } else {
                    processAction(false)
                }
            } else {
                processAction(true)
            }
        default:
            processAction(true)
        }
    }

    // MARK: - Transaction Result Handling

    private func handleTransactionResult(_ result: TransactionResult) {
        switch result {
        case .success:
            synchronize()
            pendingProcessAction?(false)
            pendingProcessAction = nil
            PremiumManager.shared.refreshPremiumStatus()
            print("[Shaker] Transaction success — synchronized and refreshed")
        case .cancelled:
            pendingProcessAction?(false)
            pendingProcessAction = nil
            print("[Shaker] Transaction cancelled")
        case .error(let message):
            pendingProcessAction?(false)
            pendingProcessAction = nil
            print("[Shaker] Transaction error: \(message ?? "unknown")")
        case .idle:
            break
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
                completion: { result, plan in
                    let displayResult: DisplayResult
                    switch result {
                    case .purchased:
                        displayResult = .purchased(planName: plan?.name)
                    case .restored:
                        displayResult = .restored(planName: plan?.name)
                    default:
                        displayResult = .cancelled
                    }
                    DispatchQueue.main.async {
                        onResult(displayResult)
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

    // MARK: - Modal Display

    func display(presentation: PLYPresentation, from viewController: UIViewController?) {
        DispatchQueue.main.async {
            presentation.display(from: viewController)
        }
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
        Purchasely.anonymousUserId ?? ""
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
        Purchasely.synchronize(success: {
            PresentationCache.shared.invalidateAll()
        }, failure: { _ in })
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
    func eventTriggered(_ event: PLYEvent, properties: [String: Any]?) {
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

    func onUserAttributeSet(key: String,
                            type: PLYUserAttributeType,
                            value: Any?,
                            source: PLYUserAttributeSource) {
        print("[Shaker] User attribute set: \(key)=\(value ?? "nil") (source: \(source))")
        PresentationCache.shared.invalidateAll()
    }

    func onUserAttributeRemoved(key: String, source: PLYUserAttributeSource) {
        print("[Shaker] User attribute removed: \(key) (source: \(source))")
        PresentationCache.shared.invalidateAll()
    }
}
