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

        Purchasely.setPaywallActionsInterceptor { [weak self] action, parameters, info, proceed in
            self?.handlePaywallAction(action: action, parameters: parameters, info: info, processAction: proceed)
        }
    }

    func restart() {
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
        await withCheckedContinuation { continuation in
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
        Purchasely.synchronize(success: {}, failure: { _ in })
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
