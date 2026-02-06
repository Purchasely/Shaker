import SwiftUI
import UIKit

/// Resolves the nearest UIViewController from a SwiftUI view hierarchy.
/// Used to present UIKit-based Purchasely paywalls from SwiftUI.
struct ViewControllerResolver: UIViewControllerRepresentable {

    let onResolve: (UIViewController) -> Void

    func makeUIViewController(context: Context) -> ParentResolverViewController {
        ParentResolverViewController(onResolve: onResolve)
    }

    func updateUIViewController(_ uiViewController: ParentResolverViewController, context: Context) {}
}

class ParentResolverViewController: UIViewController {

    let onResolve: (UIViewController) -> Void

    init(onResolve: @escaping (UIViewController) -> Void) {
        self.onResolve = onResolve
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func didMove(toParent parent: UIViewController?) {
        super.didMove(toParent: parent)
        if let parent = parent {
            onResolve(parent)
        }
    }
}
