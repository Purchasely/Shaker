import Foundation
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
                } else {
                    self?.sdkError = error?.localizedDescription
                    print("[Shaker] Purchasely configuration error: \(error?.localizedDescription ?? "unknown")")
                }
            }
        }

        Purchasely.readyToOpenDeeplink(true)

        Purchasely.setEventDelegate(self)
    }
}

extension AppViewModel: PLYEventDelegate {
    func eventTriggered(_ event: PLYEvent, properties: [String: Any]) {
        print("[Shaker] Event: \(event.name) | Properties: \(properties)")
    }
}
