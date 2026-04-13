import Foundation
import Purchasely

enum PurchaselySDKMode: String, CaseIterable, Identifiable {
    case paywallObserver = "paywallObserver"
    case full = "full"

    static let storageKey = "purchasely_sdk_mode"
    static let defaultMode: PurchaselySDKMode = .paywallObserver

    var id: String { rawValue }

    var title: String {
        switch self {
        case .paywallObserver:
            return "Paywall Observer"
        case .full:
            return "Full"
        }
    }

    var runningMode: PLYRunningMode {
        switch self {
        case .paywallObserver:
            return .paywallObserver
        case .full:
            return .full
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
