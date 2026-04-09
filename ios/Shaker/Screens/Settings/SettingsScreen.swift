import SwiftUI

struct SettingsScreen: View {

    @StateObject private var viewModel = SettingsViewModel()
    @EnvironmentObject private var premiumManager: PremiumManager
    @EnvironmentObject private var appViewModel: AppViewModel
    @State private var loginInput = ""
    @State private var hostViewController: UIViewController?

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
                        Spacer()
                        Button("Logout") {
                            viewModel.logout()
                        }
                        .font(.caption)
                        .foregroundStyle(.red)
                    }
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

                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Anonymous ID")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text(viewModel.anonymousId)
                            .font(.caption2)
                            .lineLimit(1)
                    }
                    Spacer()
                    Button {
                        UIPasteboard.general.string = viewModel.anonymousId
                    } label: {
                        Image(systemName: "doc.on.doc")
                            .font(.caption)
                    }
                }
            }

            // Purchases section
            Section("Purchases") {
                Button("Restore Purchases") {
                    viewModel.restorePurchases()
                }
                Button("Show Onboarding") {
                    viewModel.displayOnboardingPaywall(from: hostViewController)
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

                Text("Default mode is Paywall Observer. Changing mode requires an app restart.")
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

            // Screen Display Mode section
            Section {
                Picker("Display Mode", selection: Binding(
                    get: { viewModel.displayMode },
                    set: { viewModel.setDisplayMode($0) }
                )) {
                    Text("Full").tag("fullscreen")
                    Text("Modal").tag("modal")
                    Text("Drawer").tag("drawer")
                    Text("Popin").tag("popin")
                }
                .pickerStyle(.segmented)
            } header: {
                Text("Screen Display Mode")
            } footer: {
                Text("How paywalls are presented on screen")
            }

            // About section
            Section("About") {
                HStack {
                    Text("Version")
                    Spacer()
                    Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
                        .foregroundStyle(.secondary)
                }

                HStack {
                    Text("Purchasely SDK")
                    Spacer()
                    Text(viewModel.sdkVersion)
                        .foregroundStyle(.secondary)
                }

                Text("Powered by Purchasely")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .background {
            ViewControllerResolver { vc in
                hostViewController = vc
            }
        }
        .navigationTitle("Settings")
        .onAppear {
            viewModel.prefetchOnboardingPresentation()
        }
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
}
