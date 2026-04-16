package com.purchasely.shaker.data.storage

import android.content.SharedPreferences

class SharedPreferencesKeyValueStore(
    private val prefs: SharedPreferences
) : KeyValueStore {
    override fun getString(key: String, default: String?): String? = prefs.getString(key, default)
    override fun putString(key: String, value: String?) { prefs.edit().putString(key, value).apply() }
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    override fun getStringSet(key: String, default: Set<String>): Set<String> = prefs.getStringSet(key, default) ?: default
    override fun putStringSet(key: String, value: Set<String>) { prefs.edit().putStringSet(key, value).apply() }
    override fun contains(key: String): Boolean = prefs.contains(key)
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
}
