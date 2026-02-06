import SwiftUI

struct ContentView: View {

    var body: some View {
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
