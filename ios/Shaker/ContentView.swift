import SwiftUI

struct ContentView: View {

    @ObservedObject private var onboardingRepository = OnboardingRepository.shared

    var body: some View {
        if !onboardingRepository.isOnboardingCompleted {
            OnboardingScreen {
                onboardingRepository.isOnboardingCompleted = true
            }
        } else {
            TabView {
                NavigationStack {
                    HomeScreen()
                }
                .tabItem {
                    Label("Home", systemImage: "house")
                }

                NavigationStack {
                    FavoritesScreen()
                }
                .tabItem {
                    Label("Favorites", systemImage: "heart")
                }

                NavigationStack {
                    SettingsScreen()
                }
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
            }
            .tint(.orange)
        }
    }
}
