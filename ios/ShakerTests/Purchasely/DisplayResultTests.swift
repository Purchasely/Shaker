import XCTest
@testable import Shaker

final class DisplayResultTests: XCTestCase {

    func testPurchasedWithPlanName() {
        let result = DisplayResult.purchased(planName: "Premium Monthly")
        if case .purchased(let planName) = result {
            XCTAssertEqual(planName, "Premium Monthly")
        } else {
            XCTFail("Expected .purchased")
        }
    }

    func testPurchasedWithNilPlanName() {
        let result = DisplayResult.purchased(planName: nil)
        if case .purchased(let planName) = result {
            XCTAssertNil(planName)
        } else {
            XCTFail("Expected .purchased")
        }
    }

    func testRestoredWithPlanName() {
        let result = DisplayResult.restored(planName: "Premium Yearly")
        if case .restored(let planName) = result {
            XCTAssertEqual(planName, "Premium Yearly")
        } else {
            XCTFail("Expected .restored")
        }
    }

    func testRestoredWithNilPlanName() {
        let result = DisplayResult.restored(planName: nil)
        if case .restored(let planName) = result {
            XCTAssertNil(planName)
        } else {
            XCTFail("Expected .restored")
        }
    }

    func testCancelled() {
        let result = DisplayResult.cancelled
        if case .cancelled = result {
            // OK
        } else {
            XCTFail("Expected .cancelled")
        }
    }

    func testExhaustiveSwitch() {
        let results: [DisplayResult] = [
            .purchased(planName: "A"),
            .restored(planName: "B"),
            .cancelled
        ]

        for result in results {
            switch result {
            case .purchased(let name):
                XCTAssertEqual(name, "A")
            case .restored(let name):
                XCTAssertEqual(name, "B")
            case .cancelled:
                break
            }
        }
    }
}
