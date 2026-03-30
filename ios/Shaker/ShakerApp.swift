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
                    // PURCHASELY: Forward incoming URLs to the SDK to handle Purchasely deeplinks
                    // Returns true if the SDK handled the URL (e.g., open a specific paywall)
                    // Docs: https://docs.purchasely.com/advanced-features/deeplinks
                    _ = Purchasely.isDeeplinkHandled(deeplink: url)
                }
        }
    }
}
