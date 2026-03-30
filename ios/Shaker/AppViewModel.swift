import Foundation
import UIKit
import StoreKit
import Purchasely

class AppViewModel: ObservableObject {

    @Published var isSDKReady = false
    @Published var sdkError: String?

    init() {
        initPurchasely()
    }

    /// Initialize (or re-initialize) the Purchasely SDK with the current running mode.
    /// Called on app launch and whenever the user toggles the running mode in Settings.
    func initPurchasely() {
        let mode = RunningModeRepository.shared.runningMode
        let modeName = mode == .paywallObserver ? "PaywallObserver" : "Full"
        print("[Shaker] Initializing Purchasely SDK in \(modeName) mode")

        Purchasely.start(
            withAPIKey: "6cda6b92-d63c-4444-bd55-5a164c989bd4",
            appUserId: nil,
            runningMode: mode,
            storekitSettings: .storeKit2,
            logLevel: .debug
        ) { [weak self] success, error in
            DispatchQueue.main.async {
                self?.isSDKReady = success
                if success {
                    print("[Shaker] Purchasely SDK configured successfully (\(modeName))")
                    PremiumManager.shared.refreshPremiumStatus()
                    // Track additional user attributes for demo
                    let dateFormatter = ISO8601DateFormatter()
                    Purchasely.setUserAttribute(withStringValue: dateFormatter.string(from: Date()), forKey: "last_open_date")
                    Purchasely.incrementUserAttribute(withKey: "session_count")
                } else {
                    self?.sdkError = error?.localizedDescription
                    print("[Shaker] Purchasely configuration error: \(error?.localizedDescription ?? "unknown")")
                }
            }
        }

        Purchasely.readyToOpenDeeplink(true)

        Purchasely.setEventDelegate(self)

        // Handle results from deeplink-triggered paywalls
        Purchasely.setDefaultPresentationResultHandler { result, plan in
            print("[Shaker] Default presentation result: \(result) | Plan: \(plan?.name ?? "none")")
            PremiumManager.shared.refreshPremiumStatus()
        }

        // Note: User attribute changes from in-paywall surveys are captured via PLYEventDelegate
        // (no separate setUserAttributeListener API exists in the iOS SDK)

        setupInterceptor()

        // Synchronize on launch when in Observer mode to catch external transactions
        if RunningModeRepository.shared.isObserverMode {
            Purchasely.synchronize(
                success: { print("[Shaker] Observer mode: synchronize() on launch succeeded") },
                failure: { error in print("[Shaker] Observer mode: synchronize() on launch failed: \(error.localizedDescription)") }
            )
        }
    }

    private func setupInterceptor() {
        Purchasely.setPaywallActionsInterceptor { action, parameters, info, proceed in
            let isObserver = RunningModeRepository.shared.isObserverMode

            switch action {
            case .login:
                print("[Shaker] Paywall login action intercepted")
                proceed(false)

            case .navigate:
                if let url = parameters?.url {
                    print("[Shaker] Paywall navigate action: \(url)")
                    DispatchQueue.main.async {
                        UIApplication.shared.open(url)
                    }
                }
                proceed(false)

            case .purchase:
                if isObserver {
                    // Observer mode: handle purchase natively via StoreKit 2
                    guard let plan = parameters?.plan,
                          let appleProductId = plan.appleProductId else {
                        print("[Shaker] Observer mode: missing plan or productId")
                        proceed(false)
                        return
                    }

                    // Check if there's a promotional offer
                    if let promoOffer = parameters?.promoOffer {
                        let storeOfferId = promoOffer.storeOfferId
                        print("[Shaker] Observer mode: purchasing with promo offer \(storeOfferId)")
                        if #available(iOS 15.0, *) {
                            Task {
                                do {
                                    _ = try await PurchaseManager.shared.purchaseWithPromoOffer(
                                        productId: appleProductId,
                                        storeOfferId: storeOfferId
                                    )
                                    PremiumManager.shared.refreshPremiumStatus()
                                } catch {
                                    print("[Shaker] Observer mode: promo purchase error: \(error.localizedDescription)")
                                }
                                proceed(false)
                            }
                        } else {
                            proceed(false)
                        }
                    } else {
                        // Standard purchase without promotional offer
                        print("[Shaker] Observer mode: launching native purchase for \(appleProductId)")
                        if #available(iOS 15.0, *) {
                            Task {
                                do {
                                    _ = try await PurchaseManager.shared.purchase(productId: appleProductId)
                                    PremiumManager.shared.refreshPremiumStatus()
                                } catch {
                                    print("[Shaker] Observer mode: purchase error: \(error.localizedDescription)")
                                }
                                proceed(false)
                            }
                        } else {
                            proceed(false)
                        }
                    }
                } else {
                    // Full mode: let Purchasely handle the purchase
                    proceed(true)
                }

            case .restore:
                if isObserver {
                    // Observer mode: restore via StoreKit 2
                    print("[Shaker] Observer mode: restoring purchases natively")
                    if #available(iOS 15.0, *) {
                        Task {
                            do {
                                try await PurchaseManager.shared.restoreAllTransactions()
                                PremiumManager.shared.refreshPremiumStatus()
                            } catch {
                                print("[Shaker] Observer mode: restore error: \(error.localizedDescription)")
                            }
                            proceed(false)
                        }
                    } else {
                        proceed(false)
                    }
                } else {
                    proceed(true)
                }

            case .promoCode:
                print("[Shaker] Promo code action intercepted")
                DispatchQueue.main.async {
                    if let windowScene = UIApplication.shared.connectedScenes
                        .compactMap({ $0 as? UIWindowScene })
                        .first {
                        SKPaymentQueue.default().presentCodeRedemptionSheet()
                    }
                }
                proceed(false)

            default:
                proceed(true)
            }
        }
    }
}

extension AppViewModel: PLYEventDelegate {
    func eventTriggered(_ event: PLYEvent, properties: [String: Any]?) {
        print("[Shaker] Event: \(event.name) | Properties: \(properties ?? [:])")
    }
}
