import XCTest
@testable import Shaker

final class PurchaseRequestTests: XCTestCase {

    func testCreation() {
        let request = PurchaseRequest(productId: "com.test.premium")
        XCTAssertEqual(request.productId, "com.test.premium")
    }

    func testDifferentProductIds() {
        let r1 = PurchaseRequest(productId: "product.monthly")
        let r2 = PurchaseRequest(productId: "product.yearly")
        XCTAssertNotEqual(r1.productId, r2.productId)
    }
}
