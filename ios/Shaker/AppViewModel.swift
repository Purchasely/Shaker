import Foundation
import UIKit
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

class AppViewModel: ObservableObject {

    @Published var isSDKReady = false
    @Published var sdkError: String?

    init() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleSdkModeDidChange),
            name: .purchaselySdkModeDidChange,
            object: nil
        )
        initPurchasely()
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    @objc private func handleSdkModeDidChange() {
        DispatchQueue.main.async { [weak self] in
            self?.restartPurchaselySdk()
        }
    }

    private func initPurchasely() {
        // PURCHASELY: Initialize the SDK with your API key and configuration
        // Must be called once at app launch before any other SDK call
        // Docs: https://docs.purchasely.com/quick-start/sdk-configuration
        let apiKey = (Bundle.main.object(forInfoDictionaryKey: "PURCHASELY_API_KEY") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let resolvedApiKey = apiKey.isEmpty ? "6cda6b92-d63c-4444-bd55-5a164c989bd4" : apiKey
        let selectedMode = PurchaselySDKMode.current()
        sdkError = nil

        Purchasely.start(
            withAPIKey: resolvedApiKey,
            appUserId: nil,
            runningMode: selectedMode.runningMode,
            storekitSettings: .storeKit2,
            logLevel: .debug
        ) { [weak self] success, error in
            DispatchQueue.main.async {
                self?.isSDKReady = success
                if success {
                    self?.sdkError = nil
                    print("[Shaker] Purchasely SDK configured successfully (mode: \(selectedMode.title))")
                    PremiumManager.shared.refreshPremiumStatus()
                } else {
                    self?.sdkError = error?.localizedDescription
                    print("[Shaker] Purchasely configuration error: \(error?.localizedDescription ?? "unknown")")
                }
            }
        }

        // PURCHASELY: Signal that the app is ready to process deeplinks
        // Call after SDK init so pending deeplinks queued at launch are not dropped
        // Docs: https://docs.purchasely.com/advanced-features/deeplinks
        Purchasely.readyToOpenDeeplink(true)

        // PURCHASELY: Register a delegate to receive SDK analytics events
        // Useful for forwarding Purchasely events to your own analytics pipeline
        // Docs: https://docs.purchasely.com/advanced-features/events
        Purchasely.setEventDelegate(self)

        // PURCHASELY: Intercept paywall actions before the SDK handles them
        // Use to implement custom login flow or handle navigation links from paywalls
        // Docs: https://docs.purchasely.com/advanced-features/customize-screens/paywall-action-interceptor
        Purchasely.setPaywallActionsInterceptor { action, parameters, info, proceed in
            switch action {
            case .login:
                // PURCHASELY: User tapped "Sign In" on a paywall — handle login yourself, then call proceed(true) if refresh is needed
                print("[Shaker] Paywall login action intercepted")
                proceed(false)
            case .navigate:
                // PURCHASELY: Paywall contains a URL link — open it natively and suppress default SDK handling
                if let url = parameters?.url {
                    print("[Shaker] Paywall navigate action: \(url)")
                    DispatchQueue.main.async {
                        UIApplication.shared.open(url)
                    }
                }
                proceed(false)
            default:
                // PURCHASELY: All other paywall actions (purchase, close, etc.) — let the SDK handle them normally
                proceed(true)
            }
        }
    }

    private func restartPurchaselySdk() {
        Purchasely.closeDisplayedPresentation()
        initPurchasely()
    }
}

extension AppViewModel: PLYEventDelegate {
    func eventTriggered(_ event: PLYEvent, properties: [String: Any]?) {
        print("[Shaker] Event: \(event.name) | Properties: \(properties ?? [:])")
    }
}
