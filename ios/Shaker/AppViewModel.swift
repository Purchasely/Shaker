import Foundation
import UIKit
import Purchasely

class AppViewModel: ObservableObject {

    @Published var isSDKReady = false
    @Published var sdkError: String?

    init() {
        initPurchasely()
    }

    private func initPurchasely() {
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

        Purchasely.readyToOpenDeeplink(true)

        Purchasely.setEventDelegate(self)

        Purchasely.setPaywallActionsInterceptor { action, parameters, info, proceed in
            switch action {
            case .login:
                print("[Shaker] Paywall login action intercepted")
                proceed(false)
            case .navigate:
                if let url = parameters?.url {
                    print("[Shaker] Paywall navigate action: \(url)")
                    DispatchQueue.main.async {
                        UIApplication.shared.open(url)
                    }
                }
                proceed(false)
            default:
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
