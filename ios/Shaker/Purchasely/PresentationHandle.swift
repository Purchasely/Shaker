@preconcurrency import Purchasely

/// Opaque handle around a loaded `PLYPresentation`, mirroring the Android
/// `PresentationHandle` value class. ViewModels and Screens hold and pass this
/// handle exclusively — only the `Purchasely/` package unwraps it, so the rest
/// of the app never imports the SDK.
@MainActor
struct PresentationHandle {
    /// SDK-internal — only files in `Purchasely/` may touch this.
    let presentation: any PLYPresentation
}
