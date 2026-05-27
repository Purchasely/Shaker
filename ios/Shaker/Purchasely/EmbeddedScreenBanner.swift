import SwiftUI
@preconcurrency import Purchasely

/// Displays a prefetched Purchasely presentation inline as an embedded view.
/// SDK v6 exposes the embedded UI as a `PLYPresentationViewController`, so Shaker
/// wraps that controller in SwiftUI instead of using the removed `PresentationView` property.
struct EmbeddedScreenBanner: View {

    let controller: PLYPresentationViewController

    var body: some View {
        EmbeddedPresentationController(controller: controller)
    }
}

private struct EmbeddedPresentationController: UIViewControllerRepresentable {
    let controller: PLYPresentationViewController

    func makeUIViewController(context: Context) -> PLYPresentationViewController {
        controller
    }

    func updateUIViewController(_ uiViewController: PLYPresentationViewController, context: Context) {}
}
