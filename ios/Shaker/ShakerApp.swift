import SwiftUI

@main
struct ShakerApp: App {

    @StateObject private var appViewModel = AppViewModel()
    @StateObject private var premiumManager = PremiumManager.shared
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appViewModel)
                .environmentObject(premiumManager)
                .onOpenURL { url in
                    // PURCHASELY: Forward incoming URLs to the SDK to handle Purchasely deeplinks
                    // Returns true if the SDK handled the URL (e.g., open a specific paywall)
                    // Docs: https://docs.purchasely.com/advanced-features/deeplinks
                    _ = PurchaselyWrapper.shared.isDeeplinkHandled(deeplink: url)
                }
                .onChange(of: scenePhase) { newValue in
                    // PURCHASELY: Refresh entitlements when app returns to foreground
                    // Catches subscription lapses or renewals that occurred while backgrounded
                    if newValue == .active {
                        premiumManager.refreshPremiumStatus()
                    }
                }
        }
    }
}
