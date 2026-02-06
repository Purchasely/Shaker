import SwiftUI
import Purchasely

struct OnboardingPage {
    let emoji: String
    let title: String
    let description: String
}

private let onboardingPages = [
    OnboardingPage(
        emoji: "\u{1F378}",
        title: "Discover Cocktails",
        description: "Browse 25 handcrafted cocktail recipes from classic to modern."
    ),
    OnboardingPage(
        emoji: "\u{1F4DA}",
        title: "Learn Recipes",
        description: "Step-by-step instructions with precise ingredient amounts."
    ),
    OnboardingPage(
        emoji: "\u{2B50}",
        title: "Go Premium",
        description: "Unlock all recipes, filters, and favorites with a premium subscription."
    )
]

struct OnboardingScreen: View {

    let onComplete: () -> Void

    @State private var currentPage = 0
    @State private var hostViewController: UIViewController?

    var body: some View {
        VStack(spacing: 0) {
            // Skip button
            HStack {
                Spacer()
                Button("Skip") {
                    onComplete()
                }
                .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 24)
            .padding(.top, 16)

            // Pager
            TabView(selection: $currentPage) {
                ForEach(Array(onboardingPages.enumerated()), id: \.offset) { index, page in
                    VStack(spacing: 32) {
                        Spacer()

                        Text(page.emoji)
                            .font(.system(size: 72))

                        Text(page.title)
                            .font(.title)
                            .fontWeight(.bold)
                            .multilineTextAlignment(.center)

                        Text(page.description)
                            .font(.body)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)

                        Spacer()
                    }
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .always))

            // Action button
            if currentPage == onboardingPages.count - 1 {
                Button {
                    showOnboardingPaywall()
                } label: {
                    Text("Get Started")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .buttonStyle(.borderedProminent)
                .tint(.orange)
                .padding(.horizontal, 24)
            } else {
                Button {
                    withAnimation {
                        currentPage += 1
                    }
                } label: {
                    Text("Next")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .buttonStyle(.borderedProminent)
                .tint(.orange)
                .padding(.horizontal, 24)
            }

            Spacer().frame(height: 32)
        }
        .background(
            ViewControllerResolver { vc in
                hostViewController = vc
            }
            .frame(width: 0, height: 0)
        )
    }

    private func showOnboardingPaywall() {
        guard let vc = hostViewController else {
            onComplete()
            return
        }

        let paywallCtrl = Purchasely.presentationController(
            for: "onboarding",
            completion: { result, plan in
                switch result {
                case .purchased, .restored:
                    print("[Shaker] Purchased/Restored from onboarding: \(plan?.name ?? "unknown")")
                    PremiumManager.shared.refreshPremiumStatus()
                    onComplete()
                case .cancelled:
                    print("[Shaker] Onboarding paywall cancelled")
                    onComplete()
                @unknown default:
                    onComplete()
                }
            }
        )

        if let paywallCtrl = paywallCtrl {
            vc.present(paywallCtrl, animated: true)
        } else {
            onComplete()
        }
    }
}
