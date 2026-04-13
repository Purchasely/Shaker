import UIKit
import Purchasely

/// Protocol abstracting PurchaselyWrapper for testability.
/// ViewModels depend on this protocol rather than the concrete wrapper.
protocol PurchaselyWrapping {

    // MARK: - Presentation Loading

    @MainActor
    func loadPresentation(
        placementId: String,
        contentId: String?,
        onResult: @escaping @MainActor (DisplayResult) -> Void
    ) async -> FetchResult

    // MARK: - Modal Display

    func display(presentation: PLYPresentation, from viewController: UIViewController?)

    // MARK: - Embedded View Controller

    func getController(presentation: PLYPresentation) -> PLYPresentationViewController?

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

    // MARK: - Restore

    func restoreAllProducts(
        success: @escaping () -> Void,
        failure: @escaping (Error) -> Void
    )

    // MARK: - Consent

    func revokeDataProcessingConsent(for purposes: Set<PLYDataProcessingPurpose>)

    // MARK: - Lifecycle

    func restart()
    func closeDisplayedPresentation()

    // MARK: - SDK Info

    var sdkVersion: String { get }
}

// MARK: - Default parameter for contentId

extension PurchaselyWrapping {
    @MainActor
    func loadPresentation(
        placementId: String,
        onResult: @escaping @MainActor (DisplayResult) -> Void
    ) async -> FetchResult {
        await loadPresentation(placementId: placementId, contentId: nil, onResult: onResult)
    }
}
