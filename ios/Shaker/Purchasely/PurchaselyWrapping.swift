import UIKit

/// Protocol abstracting PurchaselyWrapper for testability.
/// ViewModels depend on this protocol rather than the concrete wrapper.
/// Deliberately SDK-free: every Purchasely type is mapped to an app-owned
/// type (`PresentationHandle`, `FetchResult`, `DisplayResult`,
/// `SubscriptionInfo`, `ConsentPurpose`) at the wrapper boundary.
@MainActor
protocol PurchaselyWrapping {

    // MARK: - Presentation Loading

    func loadPresentation(
        placementId: String,
        contentId: String?,
        onResult: @escaping @MainActor (DisplayResult) -> Void
    ) async -> FetchResult

    // MARK: - Modal Display

    func display(handle: PresentationHandle, from viewController: UIViewController?)

    // MARK: - User Management

    func userLogin(userId: String, onRefresh: @escaping (Bool) -> Void)
    func userLogout()
    var anonymousUserId: String { get }

    // MARK: - User Attributes

    func setUserAttribute(_ value: String, forKey key: String)
    func setUserAttribute(_ value: Bool, forKey key: String)
    func setUserAttribute(_ value: Int, forKey key: String)
    func setUserAttribute(_ value: Double, forKey key: String)
    func incrementUserAttribute(forKey key: String)

    // MARK: - Subscriptions

    func fetchSubscriptions(
        onSuccess: @escaping ([SubscriptionInfo]) -> Void,
        onError: @escaping (Error) -> Void
    )

    // MARK: - Restore

    func restoreAllProducts(
        success: @escaping () -> Void,
        failure: @escaping (Error) -> Void
    )

    // MARK: - Consent

    func revokeDataProcessingConsent(for purposes: Set<ConsentPurpose>)

    // MARK: - Lifecycle

    func restart()
    func closeDisplayedPresentation()

    // MARK: - SDK Info

    var sdkVersion: String { get }
}

// MARK: - Default parameter for contentId

extension PurchaselyWrapping {
    func loadPresentation(
        placementId: String,
        onResult: @escaping @MainActor (DisplayResult) -> Void
    ) async -> FetchResult {
        await loadPresentation(placementId: placementId, contentId: nil, onResult: onResult)
    }
}
