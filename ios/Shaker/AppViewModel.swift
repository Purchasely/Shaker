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
        let apiKey = Bundle.main.infoDictionary?["PURCHASELY_API_KEY"] as? String ?? ""
        guard !apiKey.isEmpty, apiKey != "your_api_key_here" else {
            print("[Shaker] Purchasely API key not set. Copy Config.xcconfig.example to Config.xcconfig and add your key.")
            return
        }

        Purchasely.start(
            withAPIKey: apiKey,
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
