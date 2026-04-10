import XCTest
@testable import Shaker

final class PurchaselyWrapperTests: XCTestCase {

    func testSharedInstanceExists() {
        let wrapper = PurchaselyWrapper.shared
        XCTAssertNotNil(wrapper)
    }

    func testConformsToProtocol() {
        let wrapper: PurchaselyWrapping = PurchaselyWrapper.shared
        XCTAssertNotNil(wrapper)
    }

    func testSdkVersionReturnsString() {
        let version = PurchaselyWrapper.shared.sdkVersion
        XCTAssertFalse(version.isEmpty)
    }

    func testAnonymousUserIdReturnsString() {
        // May be empty if SDK is not initialized, but should not crash
        let _ = PurchaselyWrapper.shared.anonymousUserId
    }

    // MARK: - Mock protocol conformance

    func testMockImplementsAllMethods() {
        let mock = MockPurchaselyWrapper()

        // User management
        mock.userLogin(userId: "test") { _ in }
        mock.userLogout()
        _ = mock.anonymousUserId

        // Attributes
        mock.setUserAttribute("value", forKey: "key")
        mock.setUserAttribute(true, forKey: "key")
        mock.setUserAttribute(42, forKey: "key")
        mock.setUserAttribute(3.14, forKey: "key")
        mock.incrementUserAttribute(forKey: "key")

        // Restore
        mock.restoreAllProducts(success: {}, failure: { _ in })

        // Consent
        mock.revokeDataProcessingConsent(for: [])

        // Info
        _ = mock.sdkVersion

        // Verify tracking
        XCTAssertEqual(mock.userLoginCalls.count, 1)
        XCTAssertEqual(mock.userLogoutCallCount, 1)
        XCTAssertEqual(mock.setStringAttributeCalls.count, 1)
        XCTAssertEqual(mock.setBoolAttributeCalls.count, 1)
        XCTAssertEqual(mock.setIntAttributeCalls.count, 1)
        XCTAssertEqual(mock.setDoubleAttributeCalls.count, 1)
        XCTAssertEqual(mock.incrementAttributeCalls.count, 1)
        XCTAssertEqual(mock.restoreCallCount, 1)
        XCTAssertEqual(mock.revokeConsentCalls.count, 1)
    }

    func testMockConfigurableReturnValues() {
        let mock = MockPurchaselyWrapper()
        mock.anonymousUserIdValue = "custom-id"
        mock.sdkVersionValue = "1.0.0"

        XCTAssertEqual(mock.anonymousUserId, "custom-id")
        XCTAssertEqual(mock.sdkVersion, "1.0.0")
    }
}
