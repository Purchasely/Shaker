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

            // SDK mode section
            Section("Purchasely SDK") {
                Picker("Mode", selection: Binding(
                    get: { viewModel.sdkMode },
                    set: { viewModel.setSdkMode($0) }
                )) {
                    ForEach(PurchaselySDKMode.allCases) { mode in
                        Text(mode.title).tag(mode)
                    }
                }
                .pickerStyle(.segmented)

                Text("Default mode is Paywall Observer. Changing mode restarts the SDK.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            // Data Privacy section
            Section {
                Toggle(isOn: Binding(
                    get: { viewModel.analyticsConsent },
                    set: { viewModel.setAnalyticsConsent($0) }
                )) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Analytics")
                        Text("Anonymous audience measurement")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Toggle(isOn: Binding(
                    get: { viewModel.identifiedAnalyticsConsent },
                    set: { viewModel.setIdentifiedAnalyticsConsent($0) }
                )) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Identified Analytics")
                        Text("User-identified analytics")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Toggle(isOn: Binding(
                    get: { viewModel.personalizationConsent },
                    set: { viewModel.setPersonalizationConsent($0) }
                )) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Personalization")
                        Text("Personalized content & offers")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Toggle(isOn: Binding(
                    get: { viewModel.campaignsConsent },
                    set: { viewModel.setCampaignsConsent($0) }
                )) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Campaigns")
                        Text("Promotional campaigns")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Toggle(isOn: Binding(
                    get: { viewModel.thirdPartyConsent },
                    set: { viewModel.setThirdPartyConsent($0) }
                )) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Third-party Integrations")
                        Text("External analytics & integrations")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            } header: {
                Text("Data Privacy")
            } footer: {
                Text("Technical processing required for app operation cannot be disabled.")
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
        .alert("SDK Restart Required", isPresented: .init(
            get: { viewModel.sdkModeRestartMessage != nil },
            set: { if !$0 { viewModel.clearSdkModeRestartMessage() } }
        )) {
            Button("OK") { viewModel.clearSdkModeRestartMessage() }
        } message: {
            Text(viewModel.sdkModeRestartMessage ?? "")
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
