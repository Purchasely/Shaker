import SwiftUI
import Purchasely

/// Displays a prefetched Purchasely presentation inline as an embedded view.
/// Uses the SDK's native PLYPresentationView (UIViewControllerRepresentable)
/// for correct rendering and trait collection propagation.
struct EmbeddedScreenBanner: View {

    let controller: PLYPresentationViewController

    var body: some View {
        controller.PresentationView
    }
}
