package com.purchasely.shaker.ui.screen.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import com.purchasely.shaker.data.PremiumManager
import io.purchasely.ext.Purchasely
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val context: Context,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("shaker_settings", Context.MODE_PRIVATE)

    private val _userId = MutableStateFlow(prefs.getString(KEY_USER_ID, null))
    val userId: StateFlow<String?> = _userId.asStateFlow()

    val isPremium: StateFlow<Boolean> = premiumManager.isPremium

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME, "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun login(userId: String) {
        if (userId.isBlank()) return

        Purchasely.userLogin(userId) { refresh ->
            if (refresh) {
                premiumManager.refreshPremiumStatus()
            }
            Log.d(TAG, "[Shaker] Logged in as: $userId (refresh: $refresh)")
        }

        _userId.value = userId
        prefs.edit().putString(KEY_USER_ID, userId).apply()

        Purchasely.setUserAttribute("user_id", userId)
    }

    fun logout() {
        Purchasely.userLogout()
        _userId.value = null
        prefs.edit().remove(KEY_USER_ID).apply()
        premiumManager.refreshPremiumStatus()
        Log.d(TAG, "[Shaker] Logged out")
    }

    fun restorePurchases() {
        _restoreMessage.value = null
        Purchasely.restoreAllProducts(
            onSuccess = { plan ->
                premiumManager.refreshPremiumStatus()
                _restoreMessage.value = "Purchases restored successfully!"
                Log.d(TAG, "[Shaker] Restore success: ${plan?.name}")
            },
            onError = { error ->
                _restoreMessage.value = error?.message ?: "No purchases to restore"
                Log.e(TAG, "[Shaker] Restore error: ${error?.message}")
            }
        )
    }

    fun clearRestoreMessage() {
        _restoreMessage.value = null
    }

    fun onPurchaseCompleted() {
        premiumManager.refreshPremiumStatus()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode).apply()
        Purchasely.setUserAttribute("app_theme", mode)
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_THEME = "theme_mode"
    }
}
