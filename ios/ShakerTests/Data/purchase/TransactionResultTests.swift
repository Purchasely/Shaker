import XCTest
@testable import Shaker

final class TransactionResultTests: XCTestCase {

    func testSuccess() {
        let result = TransactionResult.success
        if case .success = result {
            // OK
        } else {
            XCTFail("Expected .success")
        }
    }

    func testCancelled() {
        let result = TransactionResult.cancelled
        if case .cancelled = result {
            // OK
        } else {
            XCTFail("Expected .cancelled")
        }
    }

    func testErrorWithMessage() {
        let result = TransactionResult.error("Payment failed")
        if case .error(let message) = result {
            XCTAssertEqual(message, "Payment failed")
        } else {
            XCTFail("Expected .error")
        }
    }

    func testErrorWithNil() {
        let result = TransactionResult.error(nil)
        if case .error(let message) = result {
            XCTAssertNil(message)
        } else {
            XCTFail("Expected .error")
        }
    }

    func testIdle() {
        let result = TransactionResult.idle
        if case .idle = result {
            // OK
        } else {
            XCTFail("Expected .idle")
        }
    }

    func testExhaustiveSwitch() {
        let results: [TransactionResult] = [.success, .cancelled, .error("fail"), .idle]
        for result in results {
            switch result {
            case .success: break
            case .cancelled: break
            case .error(let msg): XCTAssertEqual(msg, "fail")
            case .idle: break
            }
        }
    }
}
