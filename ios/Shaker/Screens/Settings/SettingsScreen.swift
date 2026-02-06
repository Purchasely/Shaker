import SwiftUI

struct SettingsScreen: View {

    var body: some View {
        // TODO: Implement settings UI in Phase 4
        List {
            Section("About") {
                HStack {
                    Text("Version")
                    Spacer()
                    Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .navigationTitle("Settings")
    }
}
