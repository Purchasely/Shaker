import XCTest
@testable import Shaker

@MainActor
final class PresentationDisplayFinisherTests: XCTestCase {

    func testFinishRunsOnlyOnceUntilReset() {
        let finisher = PresentationDisplayFinisher()
        var finishCount = 0

        finisher.finish { finishCount += 1 }
        finisher.finish { finishCount += 1 }

        XCTAssertEqual(finishCount, 1)

        finisher.reset()
        finisher.finish { finishCount += 1 }

        XCTAssertEqual(finishCount, 2)
    }
}
