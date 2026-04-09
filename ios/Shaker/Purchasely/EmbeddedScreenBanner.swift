import SwiftUI

/// Displays a prefetched Purchasely presentation inline as an embedded view.
/// The presentation must be loaded by the ViewModel before passing it here.
struct EmbeddedScreenBanner: UIViewControllerRepresentable {

    let controller: UIViewController
    let height: CGFloat

    func makeUIViewController(context: Context) -> UIViewController {
        controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
