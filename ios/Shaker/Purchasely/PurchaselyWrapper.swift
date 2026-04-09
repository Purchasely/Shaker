import UIKit
import Purchasely

final class PurchaselyWrapper {

    static let shared = PurchaselyWrapper()

    private init() {}

    // MARK: - SDK Initialization

    func start(
        apiKey: String,
        appUserId: String? = nil,
        runningMode: PLYRunningMode = .full,
        storekitSettings: StorekitSettings = .storeKit2,
        logLevel: PLYLogger.PLYLogLevel = .debug,
        onStarted: @escaping (Bool, Error?) -> Void
    ) {
        Purchasely.start(
            withAPIKey: apiKey,
            appUserId: appUserId,
            runningMode: runningMode,
            storekitSettings: storekitSettings,
            logLevel: logLevel
        ) { success, error in
            onStarted(success, error)
        }
    }

    func readyToOpenDeeplink(_ ready: Bool) {
        Purchasely.readyToOpenDeeplink(ready)
    }

    func setEventDelegate(_ delegate: PLYEventDelegate) {
        Purchasely.setEventDelegate(delegate)
    }

    func setPaywallActionsInterceptor(
        _ interceptor: @escaping (PLYPresentationAction, PLYPresentationActionParameters?, PLYPresentationInfo?, @escaping (Bool) -> Void) -> Void
    ) {
        Purchasely.setPaywallActionsInterceptor(interceptor)
    }

    func closeDisplayedPresentation() {
        Purchasely.closeDisplayedPresentation()
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
