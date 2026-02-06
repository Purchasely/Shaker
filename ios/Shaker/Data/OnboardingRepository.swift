import Foundation

class OnboardingRepository: ObservableObject {

    static let shared = OnboardingRepository()

    @Published var isOnboardingCompleted: Bool {
        didSet {
            UserDefaults.standard.set(isOnboardingCompleted, forKey: key)
        }
    }

    private let key = "onboarding_completed"

    private init() {
        isOnboardingCompleted = UserDefaults.standard.bool(forKey: key)
    }
}
