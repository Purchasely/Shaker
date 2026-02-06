import SwiftUI
import Purchasely

struct OnboardingScreen: View {

    let onComplete: () -> Void

    @State private var hostViewController: UIViewController?

    var body: some View {
        Color.clear
            .background(
                ViewControllerResolver { vc in
                    hostViewController = vc
                }
                .frame(width: 0, height: 0)
            )
            .onAppear {
                fetchAndDisplayOnboarding()
            }
    }

    private func fetchAndDisplayOnboarding() {
        Purchasely.fetchPresentation(
            for: "onboarding",
            fetchCompletion: { presentation, error in
                guard let presentation = presentation,
                      presentation.type != .deactivated else {
                    if let error = error {
                        print("[Shaker] Error fetching onboarding: \(error.localizedDescription)")
                    } else {
                        print("[Shaker] Onboarding presentation not available, skipping")
                    }
                    DispatchQueue.main.async { onComplete() }
                    return
                }

                DispatchQueue.main.async {
                    presentation.display(from: hostViewController)
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
                DispatchQueue.main.async { onComplete() }
            }
        )
    }
}
