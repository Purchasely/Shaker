import SwiftUI

struct SettingsScreen: View {

    @StateObject private var viewModel = SettingsViewModel()
    @EnvironmentObject private var premiumManager: PremiumManager
    @EnvironmentObject private var appViewModel: AppViewModel
    @Environment(\.shakerTokens) private var tokens
    @State private var loginInput = ""
    @State private var hostViewController: UIViewController?

    var body: some View {
        ZStack {
            tokens.bg.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Settings")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(tokens.text)
                        .padding(.horizontal, 20)
                        .padding(.top, 16)

                    sectionHeader("Account")
                    accountCard

                    sectionHeader("Purchases")
                    VStack(spacing: 10) {
                        outlineButton("Restore purchases") { viewModel.restorePurchases() }
                        outlineButton("Show onboarding") {
                            viewModel.displayOnboardingPaywall(from: hostViewController)
                        }
                    }
                    .padding(.horizontal, 20)

                    sectionHeader("Purchasely SDK")
                    segmented(
                        options: PurchaselySDKMode.allCases.map { $0.title },
                        active: viewModel.sdkMode.title,
                        onSelect: { t in
                            PurchaselySDKMode.allCases.first(where: { $0.title == t }).map { viewModel.setSdkMode($0) }
                        }
                    )
                    Text("Default mode is Paywall Observer — Shaker observes purchases but uses its own paywall UI.")
                        .font(.system(size: 12))
                        .foregroundStyle(tokens.textSec)
                        .padding(.horizontal, 20)

                    sectionHeader("Data privacy")
                    privacyCard
                    Text("Technical processing required for app operation cannot be disabled.")
                        .font(.system(size: 12))
                        .foregroundStyle(tokens.textSec)
                        .padding(.horizontal, 20)

                    sectionHeader("Appearance")
                    segmented(
                        options: ["Light", "Dark", "System"],
                        active: viewModel.themeMode.capitalized,
                        onSelect: { t in
                            viewModel.setThemeMode(t.lowercased())
                        }
                    )

                    sectionHeader("Screen display mode")
                    Text("How paywalls are presented on screen")
                        .font(.system(size: 12))
                        .foregroundStyle(tokens.textSec)
                        .padding(.horizontal, 20)
                    segmented(
                        options: ["Full", "Modal", "Drawer", "Popin"],
                        active: displayLabel(viewModel.displayMode),
                        onSelect: { t in viewModel.setDisplayMode(displayStorage(t)) }
                    )

                    sectionHeader("About")
                    aboutCard

                    Text("Powered by Purchasely")
                        .font(.system(size: 12))
                        .foregroundStyle(tokens.textTer)
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 60)
                }
            }
        }
        .background { ViewControllerResolver { vc in hostViewController = vc } }
        .navigationBarHidden(true)
        .onAppear { viewModel.prefetchOnboardingPresentation() }
        .alert("Restore", isPresented: Binding(
            get: { viewModel.restoreMessage != nil },
            set: { if !$0 { viewModel.clearRestoreMessage() } }
        )) {
            Button("OK") { viewModel.clearRestoreMessage() }
        } message: { Text(viewModel.restoreMessage ?? "") }
        .alert("SDK Restart Required", isPresented: Binding(
            get: { viewModel.sdkModeRestartMessage != nil },
            set: { if !$0 { viewModel.clearSdkModeRestartMessage() } }
        )) {
            Button("OK") { viewModel.clearSdkModeRestartMessage() }
        } message: { Text(viewModel.sdkModeRestartMessage ?? "") }
    }

    // MARK: - Sections

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(tokens.indigoText)
            .padding(.horizontal, 20)
    }

    private var accountCard: some View {
        VStack(spacing: 0) {
            if let userId = viewModel.userId {
                HStack(spacing: 12) {
                    ZStack {
                        Circle().fill(tokens.indigo)
                        Text(String(userId.prefix(2)).uppercased())
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(tokens.onIndigo)
                    }
                    .frame(width: 44, height: 44)

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Logged in as")
                            .font(.system(size: 12))
                            .foregroundStyle(tokens.textSec)
                        Text(userId)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(tokens.text)
                    }
                    Spacer()
                    Button { viewModel.logout() } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                                .font(.system(size: 13))
                            Text("Logout").font(.system(size: 14, weight: .semibold))
                        }
                        .foregroundStyle(tokens.danger)
                    }
                }
                .padding(16)
            } else {
                HStack(spacing: 10) {
                    TextField("User ID", text: $loginInput)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.system(size: 15))
                        .padding(.horizontal, 14).frame(height: 44)
                        .background(RoundedRectangle(cornerRadius: 12).fill(tokens.bgSubtle))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(tokens.hair, lineWidth: 1))

                    Button {
                        viewModel.login(userId: loginInput)
                        loginInput = ""
                    } label: {
                        Text("Login")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(loginInput.isEmpty ? tokens.textSec : .white)
                            .frame(height: 44).padding(.horizontal, 18)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(loginInput.isEmpty ? tokens.bgSubtle : tokens.accent)
                            )
                    }
                    .disabled(loginInput.trimmingCharacters(in: .whitespaces).isEmpty)
                }
                .padding(12)
            }
            divider
            HStack {
                Text("Premium status").font(.system(size: 15)).foregroundStyle(tokens.text)
                Spacer()
                Text(premiumManager.isPremium ? "PRO" : "FREE")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(tokens.indigoText)
                    .padding(.horizontal, 10).padding(.vertical, 3)
                    .background(Capsule().fill(premiumManager.isPremium ? tokens.goldSoft : tokens.indigoSoft))
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            divider
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Anonymous ID").font(.system(size: 13)).foregroundStyle(tokens.textSec)
                    Text(viewModel.anonymousId).font(.system(size: 11, design: .monospaced)).foregroundStyle(tokens.text).lineLimit(1)
                }
                Spacer()
                Button {
                    UIPasteboard.general.string = viewModel.anonymousId
                } label: {
                    Image(systemName: "doc.on.doc")
                        .font(.system(size: 13))
                        .foregroundStyle(tokens.textSec)
                        .frame(width: 32, height: 32)
                        .background(RoundedRectangle(cornerRadius: 8).fill(tokens.inputBg))
                }
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
        }
        .background(RoundedRectangle(cornerRadius: 16).fill(tokens.bgCard))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(tokens.hair, lineWidth: 1))
        .padding(.horizontal, 20)
    }

    private var privacyCard: some View {
        VStack(spacing: 0) {
            toggleRow("Analytics", sub: "Anonymous audience measurement",
                      on: Binding(get: { viewModel.analyticsConsent }, set: { viewModel.setAnalyticsConsent($0) }))
            divider
            toggleRow("Identified analytics", sub: "User-identified analytics",
                      on: Binding(get: { viewModel.identifiedAnalyticsConsent }, set: { viewModel.setIdentifiedAnalyticsConsent($0) }))
            divider
            toggleRow("Personalization", sub: "Personalized content & offers",
                      on: Binding(get: { viewModel.personalizationConsent }, set: { viewModel.setPersonalizationConsent($0) }))
            divider
            toggleRow("Campaigns", sub: "Promotional campaigns",
                      on: Binding(get: { viewModel.campaignsConsent }, set: { viewModel.setCampaignsConsent($0) }))
            divider
            toggleRow("Third-party integrations", sub: "External analytics & integrations",
                      on: Binding(get: { viewModel.thirdPartyConsent }, set: { viewModel.setThirdPartyConsent($0) }))
        }
        .background(RoundedRectangle(cornerRadius: 16).fill(tokens.bgCard))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(tokens.hair, lineWidth: 1))
        .padding(.horizontal, 20)
    }

    private var aboutCard: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Version").font(.system(size: 15)).foregroundStyle(tokens.text)
                Spacer()
                Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
                    .font(.system(size: 15))
                    .foregroundStyle(tokens.textSec)
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            divider
            HStack {
                Text("Purchasely SDK").font(.system(size: 15)).foregroundStyle(tokens.text)
                Spacer()
                Text(viewModel.sdkVersion).font(.system(size: 15)).foregroundStyle(tokens.textSec)
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
        }
        .background(RoundedRectangle(cornerRadius: 16).fill(tokens.bgCard))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(tokens.hair, lineWidth: 1))
        .padding(.horizontal, 20)
    }

    private func toggleRow(_ title: String, sub: String, on: Binding<Bool>) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.system(size: 15, weight: .medium)).foregroundStyle(tokens.text)
                Text(sub).font(.system(size: 13)).foregroundStyle(tokens.textSec)
            }
            Spacer()
            Toggle("", isOn: on).labelsHidden().tint(tokens.indigo)
        }
        .padding(.horizontal, 16).padding(.vertical, 12)
    }

    private var divider: some View {
        Rectangle().fill(tokens.hair).frame(height: 1)
    }

    private func outlineButton(_ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(tokens.indigoText)
                .frame(maxWidth: .infinity, minHeight: 52)
                .overlay(Capsule().stroke(tokens.indigoText, lineWidth: 1.5))
        }
    }

    private func segmented(options: [String], active: String, onSelect: @escaping (String) -> Void) -> some View {
        HStack(spacing: 4) {
            ForEach(options, id: \.self) { o in
                let selected = o == active
                Button {
                    onSelect(o)
                } label: {
                    HStack(spacing: 6) {
                        if selected {
                            Image(systemName: "checkmark")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(tokens.indigoText)
                        }
                        Text(o)
                            .font(.system(size: 13, weight: selected ? .semibold : .medium))
                            .foregroundStyle(selected ? tokens.indigoText : tokens.textSec)
                    }
                    .frame(maxWidth: .infinity, minHeight: 36)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .fill(selected ? tokens.indigoSoft : Color.clear)
                    )
                }
            }
        }
        .padding(4)
        .background(RoundedRectangle(cornerRadius: 12).fill(tokens.inputBg))
        .padding(.horizontal, 20)
    }

    private func displayLabel(_ v: String) -> String {
        switch v {
        case "fullscreen": return "Full"
        case "modal": return "Modal"
        case "drawer": return "Drawer"
        case "popin": return "Popin"
        default: return "Full"
        }
    }

    private func displayStorage(_ l: String) -> String {
        switch l {
        case "Full": return "fullscreen"
        case "Modal": return "modal"
        case "Drawer": return "drawer"
        case "Popin": return "popin"
        default: return "fullscreen"
        }
    }
}
