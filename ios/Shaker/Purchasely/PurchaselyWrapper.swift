import UIKit
import Purchasely

final class PurchaselyWrapper {

    static let shared = PurchaselyWrapper()

    private init() {}

    // MARK: - Presentation Loading

    /// Fetches a presentation for a given placement. The `onResult` callback fires
    /// when the presentation is dismissed (after display or embedded interaction).
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

    /// Displays a previously loaded presentation modally.
    func display(presentation: PLYPresentation, from viewController: UIViewController?) {
        DispatchQueue.main.async {
            presentation.display(from: viewController)
        }
    }

    // MARK: - Embedded View Controller

    /// Returns the PLYPresentationViewController for inline/embedded display.
    func getController(presentation: PLYPresentation) -> PLYPresentationViewController? {
        presentation.controller
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
}
