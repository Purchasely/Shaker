import SwiftUI
import Purchasely

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
                Button("Show Onboarding") {
                    showOnboardingPaywall()
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

    private func showOnboardingPaywall() {
        let vc = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?
            .rootViewController

        Purchasely.fetchPresentation(
            for: "onboarding",
            fetchCompletion: { presentation, error in
                guard let presentation = presentation, presentation.type != .deactivated else {
                    print("[Shaker] Onboarding presentation not available: \(error?.localizedDescription ?? "deactivated")")
                    return
                }
                DispatchQueue.main.async {
                    presentation.display(from: vc)
                }
            },
            completion: { result, plan in
                switch result {
                case .purchased, .restored:
                    print("[Shaker] Purchased/Restored from onboarding: \(plan?.name ?? "unknown")")
                    PremiumManager.shared.refreshPremiumStatus()
                case .cancelled:
                    print("[Shaker] Onboarding paywall cancelled")
                @unknown default:
                    break
                }
            }
        )
    }
}
