import Foundation
import Purchasely

class RunningModeRepository {

    static let shared = RunningModeRepository()

    private let key = "running_mode"

    private init() {}

    var runningMode: PLYRunningMode {
        get {
            let stored = UserDefaults.standard.string(forKey: key) ?? "full"
            return stored == "observer" ? .paywallObserver : .full
        }
        set {
            let str = newValue == .paywallObserver ? "observer" : "full"
            UserDefaults.standard.set(str, forKey: key)
        }
    }

    var isObserverMode: Bool {
        runningMode == .paywallObserver
    }
}
