import UIKit
import Purchasely
@testable import Shaker

/// Mock implementation of PurchaselyWrapping for unit tests.
/// Records all method calls and allows configuring return values.
final class MockPurchaselyWrapper: PurchaselyWrapping {

    // MARK: - Call tracking

    var loadPresentationCalls: [(placementId: String, contentId: String?)] = []
    var displayCalls: Int = 0
    var getControllerCalls: Int = 0
    var userLoginCalls: [String] = []
    var userLogoutCallCount = 0
    var setStringAttributeCalls: [(value: String, key: String)] = []
    var setBoolAttributeCalls: [(value: Bool, key: String)] = []
    var setIntAttributeCalls: [(value: Int, key: String)] = []
    var setDoubleAttributeCalls: [(value: Double, key: String)] = []
    var incrementAttributeCalls: [String] = []
    var restoreCallCount = 0
    var restartCallCount = 0
    var closeDisplayedPresentationCallCount = 0
    var revokeConsentCalls: [Set<PLYDataProcessingPurpose>] = []

    // MARK: - Configurable return values

    var loadPresentationResult: FetchResult = .deactivated
    var anonymousUserIdValue = "mock-anon-123"
    var sdkVersionValue = "5.7.3-mock"

    // MARK: - PurchaselyWrapping

    @MainActor
    func loadPresentation(
        placementId: String,
        contentId: String?,
        onResult: @escaping @MainActor (DisplayResult) -> Void
    ) async -> FetchResult {
        loadPresentationCalls.append((placementId, contentId))
        return loadPresentationResult
    }

    func display(presentation: PLYPresentation, from viewController: UIViewController?) {
        displayCalls += 1
    }

    func getController(presentation: PLYPresentation) -> PLYPresentationViewController? {
        getControllerCalls += 1
        return nil
    }

    func userLogin(userId: String, onRefresh: @escaping (Bool) -> Void) {
        userLoginCalls.append(userId)
        onRefresh(false)
    }

    func userLogout() {
        userLogoutCallCount += 1
    }

    var anonymousUserId: String {
        anonymousUserIdValue
    }

    func setUserAttribute(_ value: String, forKey key: String) {
        setStringAttributeCalls.append((value, key))
    }

    func setUserAttribute(_ value: Bool, forKey key: String) {
        setBoolAttributeCalls.append((value, key))
    }

    func setUserAttribute(_ value: Int, forKey key: String) {
        setIntAttributeCalls.append((value, key))
    }

    func setUserAttribute(_ value: Double, forKey key: String) {
        setDoubleAttributeCalls.append((value, key))
    }

    func incrementUserAttribute(forKey key: String) {
        incrementAttributeCalls.append(key)
    }

    func restoreAllProducts(
        success: @escaping () -> Void,
        failure: @escaping (Error) -> Void
    ) {
        restoreCallCount += 1
        success()
    }

    func revokeDataProcessingConsent(for purposes: Set<PLYDataProcessingPurpose>) {
        revokeConsentCalls.append(purposes)
    }

    func restart() {
        restartCallCount += 1
    }

    func closeDisplayedPresentation() {
        closeDisplayedPresentationCallCount += 1
    }

    var sdkVersion: String {
        sdkVersionValue
    }
}
