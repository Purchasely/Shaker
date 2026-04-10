import XCTest
@testable import Shaker

final class OnboardingRepositoryTests: XCTestCase {

    private var defaults: UserDefaults!
    private var repo: OnboardingRepository!

    override func setUp() {
        super.setUp()
        defaults = UserDefaults(suiteName: "OnboardingRepositoryTests")!
        defaults.removePersistentDomain(forName: "OnboardingRepositoryTests")
        repo = OnboardingRepository(defaults: defaults)
    }

    override func tearDown() {
        defaults.removePersistentDomain(forName: "OnboardingRepositoryTests")
        super.tearDown()
    }

    func testInitialStateIsFalse() {
        XCTAssertFalse(repo.isOnboardingCompleted)
    }

    func testSetToTrue() {
        repo.isOnboardingCompleted = true
        XCTAssertTrue(repo.isOnboardingCompleted)
    }

    func testSetToFalse() {
        repo.isOnboardingCompleted = true
        repo.isOnboardingCompleted = false
        XCTAssertFalse(repo.isOnboardingCompleted)
    }

    func testPersistence() {
        repo.isOnboardingCompleted = true

        // Create a new repo with the same UserDefaults
        let newRepo = OnboardingRepository(defaults: defaults)
        XCTAssertTrue(newRepo.isOnboardingCompleted)
    }

    func testPersistsFalse() {
        repo.isOnboardingCompleted = true
        repo.isOnboardingCompleted = false

        let newRepo = OnboardingRepository(defaults: defaults)
        XCTAssertFalse(newRepo.isOnboardingCompleted)
    }
}
