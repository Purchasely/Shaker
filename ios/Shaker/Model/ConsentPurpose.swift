import Foundation

/// App-level GDPR data-processing purposes. SDK-free on purpose — only
/// `PurchaselyWrapper` maps these to the SDK's `PLYDataProcessingPurpose`.
/// Mirrors the Android `ConsentPurpose` enum.
enum ConsentPurpose: CaseIterable {
    case analytics
    case identifiedAnalytics
    case personalization
    case campaigns
    case thirdPartyIntegrations

    /// UserDefaults key written by Settings; absent means consent given.
    var defaultsKey: String {
        switch self {
        case .analytics: return "consent_analytics"
        case .identifiedAnalytics: return "consent_identified_analytics"
        case .personalization: return "consent_personalization"
        case .campaigns: return "consent_campaigns"
        case .thirdPartyIntegrations: return "consent_third_party"
        }
    }

    /// Purposes the user revoked in Settings, read from persisted flags.
    static func revoked(in defaults: UserDefaults) -> Set<ConsentPurpose> {
        Set(allCases.filter { defaults.object(forKey: $0.defaultsKey) != nil && !defaults.bool(forKey: $0.defaultsKey) })
    }

    /// The SDK keeps consent in memory only: call after every SDK start.
    @MainActor
    static func applyStored(to wrapper: PurchaselyWrapping, defaults: UserDefaults = .standard) {
        wrapper.revokeDataProcessingConsent(for: revoked(in: defaults))
    }
}
