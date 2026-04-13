import Foundation

class OnboardingRepository: ObservableObject {

    static let shared = OnboardingRepository()

    @Published var isOnboardingCompleted: Bool {
        didSet {
            defaults.set(isOnboardingCompleted, forKey: key)
        }
    }

    private let defaults: UserDefaults
    private let key: String

    private init() {
        self.defaults = .standard
        self.key = "onboarding_completed"
        isOnboardingCompleted = defaults.bool(forKey: key)
    }

    /// For testing: create a repository with a custom UserDefaults suite
    init(defaults: UserDefaults, key: String = "onboarding_completed") {
        self.defaults = defaults
        self.key = key
        isOnboardingCompleted = defaults.bool(forKey: key)
    }
}
