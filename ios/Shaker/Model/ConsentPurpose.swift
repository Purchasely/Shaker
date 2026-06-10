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
}
