import SwiftUI
import Purchasely

struct OnboardingScreen: View {

    let showOnboarding: Bool
    let onComplete: () -> Void

    @State private var hostViewController: UIViewController?

    var body: some View {
        SplashContent()
            .background(
                ViewControllerResolver { vc in
                    hostViewController = vc
                }
                .frame(width: 0, height: 0)
            )
            .onAppear {
                fetchPresentation()
            }
    }

    private func fetchPresentation() {
        Purchasely.fetchPresentation(
            for: "onboarding",
            fetchCompletion: { presentation, error in
                guard showOnboarding,
                      let presentation = presentation,
                      presentation.type != .deactivated else {
                    if let error = error {
                        print("[Shaker] Error fetching onboarding: \(error.localizedDescription)")
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

private struct SplashContent: View {

    var body: some View {
        ZStack {
            Color(.systemBackground)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                Text("🍸")
                    .font(.system(size: 72))

                Text("Shaker")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("Discover cocktails")
                    .font(.body)
                    .foregroundStyle(.secondary)

                ProgressView()
                    .padding(.top, 16)
            }
        }
    }
}
