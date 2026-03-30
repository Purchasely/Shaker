import Foundation
import UIKit
import Purchasely

class AppViewModel: ObservableObject {

    @Published var isSDKReady = false
    @Published var sdkError: String?

    init() {
        initPurchasely()
    }

    func initPurchasely() {
        // PURCHASELY: Initialize the SDK with your API key and configuration
        // Must be called once at app launch before any other SDK call
        // Docs: https://docs.purchasely.com/quick-start/sdk-configuration
        Purchasely.start(
            withAPIKey: "6cda6b92-d63c-4444-bd55-5a164c989bd4",
            appUserId: nil,
            runningMode: .full,
            storekitSettings: .storeKit2,
            logLevel: .debug
        ) { [weak self] success, error in
            DispatchQueue.main.async {
                self?.isSDKReady = success
                if success {
                    print("[Shaker] Purchasely SDK configured successfully")
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
}

extension AppViewModel: PLYEventDelegate {
    func eventTriggered(_ event: PLYEvent, properties: [String: Any]?) {
        print("[Shaker] Event: \(event.name) | Properties: \(properties ?? [:])")
    }
}
