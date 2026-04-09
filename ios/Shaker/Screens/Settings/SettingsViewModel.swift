import Foundation
import Purchasely

class SettingsViewModel: ObservableObject {

    @Published var userId: String?
    @Published var restoreMessage: String?
    @Published var themeMode: String
    @Published var sdkMode: PurchaselySDKMode
    @Published var sdkModeRestartMessage: String?

    // Data privacy consent (default: true = consent given)
    @Published var analyticsConsent: Bool
    @Published var identifiedAnalyticsConsent: Bool
    @Published var personalizationConsent: Bool
    @Published var campaignsConsent: Bool
    @Published var thirdPartyConsent: Bool
    @Published var runningMode: String
    @Published var anonymousId: String = ""
    @Published var displayMode: String

    // Prefetched onboarding presentation
    @Published var onboardingFetchResult: FetchResult?

    private let wrapper = PurchaselyWrapper.shared

    private let userIdKey = "user_id"
    private let themeKey = "theme_mode"
    private let displayModeKey = "display_mode"
    private let consentAnalyticsKey = "consent_analytics"
    private let consentIdentifiedAnalyticsKey = "consent_identified_analytics"
    private let consentPersonalizationKey = "consent_personalization"
    private let consentCampaignsKey = "consent_campaigns"
    private let consentThirdPartyKey = "consent_third_party"

    var sdkVersion: String { wrapper.sdkVersion }

    init() {
        userId = UserDefaults.standard.string(forKey: userIdKey)
        themeMode = UserDefaults.standard.string(forKey: themeKey) ?? "system"
        sdkMode = PurchaselySDKMode.current()
        sdkModeRestartMessage = nil

        let defaults = UserDefaults.standard
        analyticsConsent = defaults.object(forKey: consentAnalyticsKey) == nil ? true : defaults.bool(forKey: consentAnalyticsKey)
        identifiedAnalyticsConsent = defaults.object(forKey: consentIdentifiedAnalyticsKey) == nil ? true : defaults.bool(forKey: consentIdentifiedAnalyticsKey)
        personalizationConsent = defaults.object(forKey: consentPersonalizationKey) == nil ? true : defaults.bool(forKey: consentPersonalizationKey)
        campaignsConsent = defaults.object(forKey: consentCampaignsKey) == nil ? true : defaults.bool(forKey: consentCampaignsKey)
        thirdPartyConsent = defaults.object(forKey: consentThirdPartyKey) == nil ? true : defaults.bool(forKey: consentThirdPartyKey)
        runningMode = RunningModeRepository.shared.isObserverMode ? "observer" : "full"
        displayMode = UserDefaults.standard.string(forKey: displayModeKey) ?? "fullscreen"

        applyConsentPreferences()
        anonymousId = wrapper.anonymousUserId
    }

    func refreshAnonymousId() {
        anonymousId = wrapper.anonymousUserId
    }

    func prefetchOnboardingPresentation() {
        Task {
            onboardingFetchResult = await wrapper.loadPresentation(placementId: "onboarding") { result in
                if case .purchased = result { PremiumManager.shared.refreshPremiumStatus() }
                if case .restored = result { PremiumManager.shared.refreshPremiumStatus() }
            }
        }
    }

    func displayOnboardingPaywall(from viewController: UIViewController?) {
        guard case .success(let presentation) = onboardingFetchResult else { return }
        wrapper.display(presentation: presentation, from: viewController)
    }

    func login(userId: String) {
        guard !userId.isEmpty else { return }

        wrapper.userLogin(userId: userId) { refresh in
            if refresh {
                PremiumManager.shared.refreshPremiumStatus()
            }
            print("[Shaker] Logged in as: \(userId) (refresh: \(refresh))")
        }

        self.userId = userId
        UserDefaults.standard.set(userId, forKey: userIdKey)
        wrapper.setUserAttribute(userId, forKey: "user_id")
    }

    func logout() {
        wrapper.userLogout()
        userId = nil
        UserDefaults.standard.removeObject(forKey: userIdKey)
        PremiumManager.shared.refreshPremiumStatus()
        print("[Shaker] Logged out")
    }

    func restorePurchases() {
        restoreMessage = nil
        wrapper.restoreAllProducts(
            success: { [weak self] in
                PremiumManager.shared.refreshPremiumStatus()
                DispatchQueue.main.async {
                    self?.restoreMessage = "Purchases restored successfully!"
                }
                print("[Shaker] Restore success")
            },
            failure: { [weak self] error in
                DispatchQueue.main.async {
                    self?.restoreMessage = error.localizedDescription
                }
                print("[Shaker] Restore error: \(error.localizedDescription)")
            }
        )
    }

    func clearRestoreMessage() {
        restoreMessage = nil
    }

    func setThemeMode(_ mode: String) {
        themeMode = mode
        UserDefaults.standard.set(mode, forKey: themeKey)
        wrapper.setUserAttribute(mode, forKey: "app_theme")
    }

    func setSdkMode(_ mode: PurchaselySDKMode) {
        guard sdkMode != mode else { return }

        sdkMode = mode
        mode.persist()
        NotificationCenter.default.post(name: .purchaselySdkModeDidChange, object: nil)
        sdkModeRestartMessage =
            "Purchasely SDK switched to \(mode.title). Please kill and relaunch the app."
        print("[Shaker] SDK mode updated to \(mode.rawValue)")
    }

    func clearSdkModeRestartMessage() {
        sdkModeRestartMessage = nil
    }

    func setAnalyticsConsent(_ enabled: Bool) {
        analyticsConsent = enabled
        UserDefaults.standard.set(enabled, forKey: consentAnalyticsKey)
        applyConsentPreferences()
    }

    func setIdentifiedAnalyticsConsent(_ enabled: Bool) {
        identifiedAnalyticsConsent = enabled
        UserDefaults.standard.set(enabled, forKey: consentIdentifiedAnalyticsKey)
        applyConsentPreferences()
    }

    func setPersonalizationConsent(_ enabled: Bool) {
        personalizationConsent = enabled
        UserDefaults.standard.set(enabled, forKey: consentPersonalizationKey)
        applyConsentPreferences()
    }

    func setCampaignsConsent(_ enabled: Bool) {
        campaignsConsent = enabled
        UserDefaults.standard.set(enabled, forKey: consentCampaignsKey)
        applyConsentPreferences()
    }

    func setThirdPartyConsent(_ enabled: Bool) {
        thirdPartyConsent = enabled
        UserDefaults.standard.set(enabled, forKey: consentThirdPartyKey)
        applyConsentPreferences()
    }

    func setRunningMode(_ mode: String) {
        runningMode = mode
        RunningModeRepository.shared.runningMode = mode == "observer" ? .paywallObserver : .full
        print("[Shaker] Running mode changed to: \(mode)")
    }

    func setDisplayMode(_ mode: String) {
        displayMode = mode
        UserDefaults.standard.set(mode, forKey: displayModeKey)
        print("[Shaker] Display mode changed to: \(mode)")
    }

    private func applyConsentPreferences() {
        var revoked = Set<PLYDataProcessingPurpose>()
        if !analyticsConsent { revoked.insert(.analytics) }
        if !identifiedAnalyticsConsent { revoked.insert(.identifiedAnalytics) }
        if !personalizationConsent { revoked.insert(.personalization) }
        if !campaignsConsent { revoked.insert(.campaigns) }
        if !thirdPartyConsent { revoked.insert(.thirdPartyIntegrations) }
        wrapper.revokeDataProcessingConsent(for: revoked)
        print("[Shaker] Consent updated — revoked: \(revoked)")
    }
}
