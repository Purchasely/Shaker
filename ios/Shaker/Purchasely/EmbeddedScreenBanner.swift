import SwiftUI
@preconcurrency import Purchasely

/// Displays a prefetched Purchasely presentation inline as an embedded view.
/// Accepts the SDK-free `FetchResult` (mirroring the Android EmbeddedScreenBanner)
/// and resolves the SDK's `PLYPresentationViewController` internally — callers
/// never import Purchasely. SDK v6 exposes the embedded UI as a controller, so
/// Shaker wraps it in SwiftUI instead of using the removed `PresentationView`.
struct EmbeddedScreenBanner: View {

    let fetchResult: FetchResult

    var body: some View {
        if case .success(let handle) = fetchResult,
           let controller = handle.presentation.controller {
            EmbeddedPresentationController(controller: controller)
        }
    }
}

private struct EmbeddedPresentationController: UIViewControllerRepresentable {
    let controller: PLYPresentationViewController

    func makeUIViewController(context: Context) -> PLYPresentationViewController {
        controller
    }

    func updateUIViewController(_ uiViewController: PLYPresentationViewController, context: Context) {}
}
