import Foundation

/// App-level SDK mode selection. Deliberately SDK-free: only `PurchaselyWrapper`
/// maps this to the Purchasely `PLYRunningMode` — the rest of the app never
/// imports `Purchasely`. Mirrors the Android `PurchaselySdkMode` enum.
enum PurchaselySDKMode: String, CaseIterable, Identifiable {
    // rawValue "paywallObserver" is kept for storage compatibility with
    // pre-v6 installs; the user-facing label is "Observer" (same as Android).
    case paywallObserver = "paywallObserver"
    case full = "full"

    static let storageKey = "purchasely_sdk_mode"
    static let defaultMode: PurchaselySDKMode = .paywallObserver

    var id: String { rawValue }

    var title: String {
        switch self {
        case .paywallObserver:
            return "Observer"
        case .full:
            return "Full"
        }
    }

    static func current() -> PurchaselySDKMode {
        let defaults = UserDefaults.standard
        guard let rawValue = defaults.string(forKey: storageKey),
              let mode = PurchaselySDKMode(rawValue: rawValue) else {
            defaults.set(defaultMode.rawValue, forKey: storageKey)
            return defaultMode
        }
        return mode
    }

    func persist() {
        UserDefaults.standard.set(rawValue, forKey: Self.storageKey)
    }
}

extension Notification.Name {
    static let purchaselySdkModeDidChange = Notification.Name("purchaselySdkModeDidChange")
}
