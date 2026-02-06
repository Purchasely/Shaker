import Foundation
import Purchasely

class SettingsViewModel: ObservableObject {

    @Published var userId: String?
    @Published var restoreMessage: String?
    @Published var themeMode: String

    private let userIdKey = "user_id"
    private let themeKey = "theme_mode"

    init() {
        userId = UserDefaults.standard.string(forKey: userIdKey)
        themeMode = UserDefaults.standard.string(forKey: themeKey) ?? "system"
    }

    func login(userId: String) {
        guard !userId.isEmpty else { return }

        Purchasely.userLogin(with: userId) { refresh in
            if refresh {
                PremiumManager.shared.refreshPremiumStatus()
            }
            print("[Shaker] Logged in as: \(userId) (refresh: \(refresh))")
        }

        self.userId = userId
        UserDefaults.standard.set(userId, forKey: userIdKey)

        Purchasely.setUserAttribute(withStringValue: userId, forKey: "user_id")
    }

    func logout() {
        Purchasely.userLogout()
        userId = nil
        UserDefaults.standard.removeObject(forKey: userIdKey)
        PremiumManager.shared.refreshPremiumStatus()
        print("[Shaker] Logged out")
    }

    func restorePurchases() {
        restoreMessage = nil
        Purchasely.restoreAllProducts(
            success: { [weak self] in
                PremiumManager.shared.refreshPremiumStatus()
                DispatchQueue.main.async {
                    self?.restoreMessage = "Purchases restored successfully!"
                }
                print("[Shaker] Restore success")
            },
            failure: { [weak self] error in
                DispatchQueue.main.async {
                    self?.restoreMessage = error.localizedDescription
                }
                print("[Shaker] Restore error: \(error.localizedDescription)")
            }
        )
    }

    func clearRestoreMessage() {
        restoreMessage = nil
    }

    func setThemeMode(_ mode: String) {
        themeMode = mode
        UserDefaults.standard.set(mode, forKey: themeKey)
        Purchasely.setUserAttribute(withStringValue: mode, forKey: "app_theme")
    }
}
