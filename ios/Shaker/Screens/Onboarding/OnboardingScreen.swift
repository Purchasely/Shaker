import SwiftUI

struct OnboardingScreen: View {

    let showOnboarding: Bool
    let onComplete: () -> Void

    @State private var hostViewController: UIViewController?

    private let wrapper = PurchaselyWrapper.shared

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
        Task {
            let result = await wrapper.loadPresentation(placementId: "onboarding") { displayResult in
                switch displayResult {
                case .purchased, .restored:
                    PremiumManager.shared.refreshPremiumStatus()
                case .cancelled:
                    break
                }
                onComplete()
            }

            guard showOnboarding else {
                onComplete()
                return
            }

            guard case .success(let presentation) = result else {
                onComplete()
                return
            }

            wrapper.display(presentation: presentation, from: hostViewController)
        }
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
