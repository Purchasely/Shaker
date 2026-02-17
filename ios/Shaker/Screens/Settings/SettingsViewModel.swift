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

    private let userIdKey = "user_id"
    private let themeKey = "theme_mode"
    private let consentAnalyticsKey = "consent_analytics"
    private let consentIdentifiedAnalyticsKey = "consent_identified_analytics"
    private let consentPersonalizationKey = "consent_personalization"
    private let consentCampaignsKey = "consent_campaigns"
    private let consentThirdPartyKey = "consent_third_party"

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

        applyConsentPreferences()
        anonymousId = Purchasely.anonymousUserId ?? ""
    }

    func refreshAnonymousId() {
        anonymousId = Purchasely.anonymousUserId ?? ""
    }

    func login(userId: String) {
        guard !userId.isEmpty else { return }

        // PURCHASELY: Associate the current device session with a user ID
        // The callback indicates whether entitlements need to be refreshed for the new user
        // Docs: https://docs.purchasely.com/quick-start/sdk-configuration/user-login
        Purchasely.userLogin(with: userId) { refresh in
            if refresh {
                PremiumManager.shared.refreshPremiumStatus()
            }
            print("[Shaker] Logged in as: \(userId) (refresh: \(refresh))")
        }

        self.userId = userId
        UserDefaults.standard.set(userId, forKey: userIdKey)

        // PURCHASELY: Store the user ID as a custom attribute for targeting and personalization
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        Purchasely.setUserAttribute(withStringValue: userId, forKey: "user_id")
    }

    func logout() {
        // PURCHASELY: Clear the current user session from the SDK
        // Resets entitlements to anonymous state; always refresh premium status afterwards
        // Docs: https://docs.purchasely.com/quick-start/sdk-configuration/user-login
        Purchasely.userLogout()
        userId = nil
        UserDefaults.standard.removeObject(forKey: userIdKey)
        PremiumManager.shared.refreshPremiumStatus()
        print("[Shaker] Logged out")
    }

    func restorePurchases() {
        restoreMessage = nil
        // PURCHASELY: Restore previously completed purchases for the current App Store account
        // Required by App Store guidelines; must be accessible via a visible UI button
        // Docs: https://docs.purchasely.com/quick-start/sdk-implementation/restore-purchases
        Purchasely.restoreAllProducts(
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
        // PURCHASELY: Track user's preferred theme as a custom attribute for audience segmentation
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        Purchasely.setUserAttribute(withStringValue: mode, forKey: "app_theme")
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

    private func applyConsentPreferences() {
        var revoked = Set<PLYDataProcessingPurpose>()
        if !analyticsConsent { revoked.insert(.analytics) }
        if !identifiedAnalyticsConsent { revoked.insert(.identifiedAnalytics) }
        if !personalizationConsent { revoked.insert(.personalization) }
        if !campaignsConsent { revoked.insert(.campaigns) }
        if !thirdPartyConsent { revoked.insert(.thirdPartyIntegrations) }
        // PURCHASELY: Apply GDPR consent — revoked purposes are excluded from SDK data processing
        // Call whenever the user changes any consent toggle to keep the SDK in sync
        // Docs: https://docs.purchasely.com/advanced-features/gdpr
        Purchasely.revokeDataProcessingConsent(for: revoked)
        print("[Shaker] Consent updated — revoked: \(revoked)")
    }
}
