import SwiftUI

struct ContentView: View {

    @ObservedObject private var onboardingRepository = OnboardingRepository.shared
    @AppStorage("theme_mode") private var themeMode: String = "system"

    enum AppPhase {
        case splash
        case main
    }

    @State private var phase: AppPhase = .splash

    var body: some View {
        Group {
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
                MainTabs()
            }
        }
        .shakerTheme(mode: themeMode)
    }
}

struct MainTabs: View {
    @Environment(\.shakerTokens) private var tokens

    init() {
        // Customize UITabBar appearance to match tokens
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor.secondarySystemBackground
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        TabView {
            NavigationStack { HomeScreen() }
                .tabItem { Label("Home", systemImage: "house.fill") }

            NavigationStack { FavoritesScreen() }
                .tabItem { Label("Favorites", systemImage: "heart.fill") }

            NavigationStack { SettingsScreen() }
                .tabItem { Label("Settings", systemImage: "gearshape.fill") }
        }
        .tint(tokens.indigoText)
    }
}
