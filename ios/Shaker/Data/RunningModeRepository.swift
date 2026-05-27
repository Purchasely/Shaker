import Foundation
@preconcurrency import Purchasely

final class RunningModeRepository: @unchecked Sendable {

    static let shared = RunningModeRepository()

    private let key = "running_mode"

    private init() {}

    var runningMode: PLYRunningMode {
        get {
            let stored = UserDefaults.standard.string(forKey: key) ?? "full"
            return stored == "observer" ? .observer : .full
        }
        set {
            let str = newValue == .observer ? "observer" : "full"
            UserDefaults.standard.set(str, forKey: key)
        }
    }

    var isObserverMode: Bool {
        runningMode == .observer
    }
}
