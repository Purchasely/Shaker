import Foundation
import Purchasely

class AppViewModel: ObservableObject {

    @Published var isSDKReady = false
    @Published var sdkError: String?

    private let wrapper = PurchaselyWrapper.shared

    init() {
        let apiKey = (Bundle.main.object(forInfoDictionaryKey: "PURCHASELY_API_KEY") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let resolvedApiKey = apiKey.isEmpty ? "6cda6b92-d63c-4444-bd55-5a164c989bd4" : apiKey
        let storedUserId = UserDefaults.standard.string(forKey: "user_id")

        #if DEBUG
        let sdkLogLevel: PLYLogger.PLYLogLevel = .debug
        #else
        let sdkLogLevel: PLYLogger.PLYLogLevel = .warn
        #endif

        wrapper.initialize(
            apiKey: resolvedApiKey,
            appUserId: storedUserId,
            logLevel: sdkLogLevel
        ) { [weak self] success, error in
            DispatchQueue.main.async {
                self?.isSDKReady = success
                self?.sdkError = success ? nil : error?.localizedDescription
            }
        }
    }
}
