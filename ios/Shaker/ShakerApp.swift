import SwiftUI
import Purchasely

@main
struct ShakerApp: App {

    @StateObject private var appViewModel = AppViewModel()
    @StateObject private var premiumManager = PremiumManager.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appViewModel)
                .environmentObject(premiumManager)
                .onOpenURL { url in
                    _ = Purchasely.isDeeplinkHandled(deeplink: url)
                }
        }
    }
}
