import SwiftUI

struct ContentView: View {

    @ObservedObject private var onboardingRepository = OnboardingRepository.shared

    enum AppPhase {
        case splash
        case main
    }

    @State private var phase: AppPhase = .splash

    var body: some View {
        switch phase {
        case .splash:
            OnboardingScreen(
                showOnboarding: !onboardingRepository.isOnboardingCompleted,
                onComplete: {
                    onboardingRepository.isOnboardingCompleted = true
                    phase = .main
                }
            )
        case .main:
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
