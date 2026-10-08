import Foundation

@MainActor
final class PresentationDisplayFinisher {
    private var didFinish = false

    func reset() {
        didFinish = false
    }

    func finish(_ action: () -> Void) {
        guard !didFinish else { return }
        didFinish = true
        action()
    }
}
