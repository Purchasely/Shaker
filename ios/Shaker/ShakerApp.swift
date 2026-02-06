import SwiftUI
import Purchasely

@main
struct ShakerApp: App {

    @StateObject private var appViewModel = AppViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appViewModel)
                .onOpenURL { url in
                    _ = Purchasely.isDeeplinkHandled(deeplink: url)
                }
        }
    }
}
