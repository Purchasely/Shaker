import XCTest
import Purchasely
@testable import Shaker

final class SettingsViewModelTests: XCTestCase {

    private var mockWrapper: MockPurchaselyWrapper!
    private var defaults: UserDefaults!

    override func setUp() {
        super.setUp()
        mockWrapper = MockPurchaselyWrapper()
        defaults = UserDefaults(suiteName: "SettingsViewModelTests")!
        defaults.removePersistentDomain(forName: "SettingsViewModelTests")
    }

    override func tearDown() {
        defaults.removePersistentDomain(forName: "SettingsViewModelTests")
        super.tearDown()
    }

    private func createViewModel() -> SettingsViewModel {
        SettingsViewModel(wrapper: mockWrapper, defaults: defaults)
    }

    // MARK: - Initial state

    func testInitialUserIdIsNil() {
        let vm = createViewModel()
        XCTAssertNil(vm.userId)
    }

    func testInitialUserIdReadsFromDefaults() {
        defaults.set("kevin", forKey: "user_id")
        let vm = createViewModel()
        XCTAssertEqual(vm.userId, "kevin")
    }

    func testInitialThemeModeIsSystem() {
        let vm = createViewModel()
        XCTAssertEqual(vm.themeMode, "system")
    }

    func testInitialAnonymousIdFromWrapper() {
        let vm = createViewModel()
        XCTAssertEqual(vm.anonymousId, "mock-anon-123")
    }

    func testSdkVersionFromWrapper() {
        let vm = createViewModel()
        XCTAssertEqual(vm.sdkVersion, "5.7.3-mock")
    }

    func testInitialConsentsAreTrue() {
        let vm = createViewModel()
        XCTAssertTrue(vm.analyticsConsent)
        XCTAssertTrue(vm.identifiedAnalyticsConsent)
        XCTAssertTrue(vm.personalizationConsent)
        XCTAssertTrue(vm.campaignsConsent)
        XCTAssertTrue(vm.thirdPartyConsent)
    }

    func testInitialDisplayModeIsFullscreen() {
        let vm = createViewModel()
        XCTAssertEqual(vm.displayMode, "fullscreen")
    }

    // MARK: - Login

    func testLoginSetsUserIdAndCallsWrapper() {
        let vm = createViewModel()
        vm.login(userId: "kevin")
        XCTAssertEqual(vm.userId, "kevin")
        XCTAssertEqual(mockWrapper.userLoginCalls.count, 1)
        XCTAssertEqual(mockWrapper.userLoginCalls.first, "kevin")
    }

    func testLoginSetsUserAttribute() {
        let vm = createViewModel()
        vm.login(userId: "kevin")
        XCTAssertTrue(mockWrapper.setStringAttributeCalls.contains(where: {
            $0.key == "user_id" && $0.value == "kevin"
        }))
    }

    func testLoginPersistsToDefaults() {
        let vm = createViewModel()
        vm.login(userId: "kevin")
        XCTAssertEqual(defaults.string(forKey: "user_id"), "kevin")
    }

    func testLoginWithEmptyStringDoesNothing() {
        let vm = createViewModel()
        vm.login(userId: "")
        XCTAssertNil(vm.userId)
        XCTAssertTrue(mockWrapper.userLoginCalls.isEmpty)
    }

    // MARK: - Logout

    func testLogoutClearsUserId() {
        let vm = createViewModel()
        vm.login(userId: "kevin")
        vm.logout()
        XCTAssertNil(vm.userId)
        XCTAssertEqual(mockWrapper.userLogoutCallCount, 1)
    }

    func testLogoutRemovesFromDefaults() {
        defaults.set("kevin", forKey: "user_id")
        let vm = createViewModel()
        vm.logout()
        XCTAssertNil(defaults.string(forKey: "user_id"))
    }

    // MARK: - Restore

    func testRestorePurchasesCallsWrapper() {
        let vm = createViewModel()
        vm.restorePurchases()
        XCTAssertEqual(mockWrapper.restoreCallCount, 1)
    }

    func testRestoreSuccessUpdatesMessage() {
        let vm = createViewModel()
        vm.restorePurchases()

        let expectation = expectation(description: "Restore message updated")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            XCTAssertEqual(vm.restoreMessage, "Purchases restored successfully!")
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 1.0)
    }

    func testClearRestoreMessage() {
        let vm = createViewModel()
        vm.restorePurchases()
        vm.clearRestoreMessage()
        XCTAssertNil(vm.restoreMessage)
    }

    // MARK: - Theme

    func testSetThemeMode() {
        let vm = createViewModel()
        vm.setThemeMode("dark")
        XCTAssertEqual(vm.themeMode, "dark")
        XCTAssertEqual(defaults.string(forKey: "theme_mode"), "dark")
    }

    func testSetThemeSetsUserAttribute() {
        let vm = createViewModel()
        vm.setThemeMode("dark")
        XCTAssertTrue(mockWrapper.setStringAttributeCalls.contains(where: {
            $0.key == "app_theme" && $0.value == "dark"
        }))
    }

    // MARK: - Display mode

    func testSetDisplayMode() {
        let vm = createViewModel()
        vm.setDisplayMode("embedded")
        XCTAssertEqual(vm.displayMode, "embedded")
        XCTAssertEqual(defaults.string(forKey: "display_mode"), "embedded")
    }

    // MARK: - Anonymous ID

    func testRefreshAnonymousId() {
        let vm = createViewModel()
        mockWrapper.anonymousUserIdValue = "new-anon-456"
        vm.refreshAnonymousId()
        XCTAssertEqual(vm.anonymousId, "new-anon-456")
    }

    // MARK: - Consent

    func testSetAnalyticsConsentFalse() {
        let vm = createViewModel()
        vm.setAnalyticsConsent(false)
        XCTAssertFalse(vm.analyticsConsent)
        XCTAssertTrue(mockWrapper.revokeConsentCalls.last?.contains(.analytics) ?? false)
    }

    func testSetIdentifiedAnalyticsConsentFalse() {
        let vm = createViewModel()
        vm.setIdentifiedAnalyticsConsent(false)
        XCTAssertFalse(vm.identifiedAnalyticsConsent)
        XCTAssertTrue(mockWrapper.revokeConsentCalls.last?.contains(.identifiedAnalytics) ?? false)
    }

    func testSetPersonalizationConsentFalse() {
        let vm = createViewModel()
        vm.setPersonalizationConsent(false)
        XCTAssertFalse(vm.personalizationConsent)
        XCTAssertTrue(mockWrapper.revokeConsentCalls.last?.contains(.personalization) ?? false)
    }

    func testSetCampaignsConsentFalse() {
        let vm = createViewModel()
        vm.setCampaignsConsent(false)
        XCTAssertFalse(vm.campaignsConsent)
        XCTAssertTrue(mockWrapper.revokeConsentCalls.last?.contains(.campaigns) ?? false)
    }

    func testSetThirdPartyConsentFalse() {
        let vm = createViewModel()
        vm.setThirdPartyConsent(false)
        XCTAssertFalse(vm.thirdPartyConsent)
        XCTAssertTrue(mockWrapper.revokeConsentCalls.last?.contains(.thirdPartyIntegrations) ?? false)
    }

    func testAllConsentsTrue_revokesEmptySet() {
        let vm = createViewModel()
        // Init calls applyConsentPreferences with all true
        XCTAssertTrue(mockWrapper.revokeConsentCalls.last?.isEmpty ?? false)
    }

    func testMultipleConsentsRevoked() {
        let vm = createViewModel()
        vm.setAnalyticsConsent(false)
        vm.setPersonalizationConsent(false)

        let lastRevoked = mockWrapper.revokeConsentCalls.last
        XCTAssertNotNil(lastRevoked)
        XCTAssertTrue(lastRevoked?.contains(.analytics) ?? false)
        XCTAssertTrue(lastRevoked?.contains(.personalization) ?? false)
    }

    // MARK: - Prefetch onboarding

    func testPrefetchOnboardingPresentation() {
        let vm = createViewModel()
        vm.prefetchOnboardingPresentation()

        let expectation = expectation(description: "Prefetch onboarding")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            XCTAssertTrue(self.mockWrapper.loadPresentationCalls.contains(where: {
                $0.placementId == "onboarding"
            }))
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 1.0)
    }
}
