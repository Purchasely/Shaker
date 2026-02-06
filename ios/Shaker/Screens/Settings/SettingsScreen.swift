import SwiftUI

struct SettingsScreen: View {

    @StateObject private var viewModel = SettingsViewModel()
    @EnvironmentObject private var premiumManager: PremiumManager
    @State private var loginInput = ""

    var body: some View {
        List {
            // Account section
            Section("Account") {
                if let userId = viewModel.userId {
                    HStack {
                        Image(systemName: "person.fill")
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Logged in as")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(userId)
                                .font(.body)
                        }
                    }

                    Button("Logout") {
                        viewModel.logout()
                    }
                    .foregroundStyle(.red)
                } else {
                    HStack {
                        TextField("User ID", text: $loginInput)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()

                        Button("Login") {
                            viewModel.login(userId: loginInput)
                            loginInput = ""
                        }
                        .disabled(loginInput.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                }

                HStack {
                    Text("Premium Status")
                    Spacer()
                    Text(premiumManager.isPremium ? "Active" : "Free")
                        .foregroundStyle(premiumManager.isPremium ? .orange : .secondary)
                }
            }

            // Purchases section
            Section("Purchases") {
                Button("Restore Purchases") {
                    viewModel.restorePurchases()
                }
            }

            // Appearance section
            Section("Appearance") {
                Picker("Theme", selection: $viewModel.themeMode) {
                    Text("Light").tag("light")
                    Text("Dark").tag("dark")
                    Text("System").tag("system")
                }
                .pickerStyle(.segmented)
                .onChange(of: viewModel.themeMode) { newValue in
                    viewModel.setThemeMode(newValue)
                }
            }

            // About section
            Section("About") {
                HStack {
                    Text("Version")
                    Spacer()
                    Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
                        .foregroundStyle(.secondary)
                }

                Text("Powered by Purchasely")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle("Settings")
        .alert("Restore", isPresented: .init(
            get: { viewModel.restoreMessage != nil },
            set: { if !$0 { viewModel.clearRestoreMessage() } }
        )) {
            Button("OK") { viewModel.clearRestoreMessage() }
        } message: {
            Text(viewModel.restoreMessage ?? "")
        }
    }
}
