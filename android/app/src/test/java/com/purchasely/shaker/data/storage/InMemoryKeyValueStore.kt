package com.purchasely.shaker.data.storage

class InMemoryKeyValueStore : KeyValueStore {
    private val store = mutableMapOf<String, Any?>()

    override fun getString(key: String, default: String?): String? = store[key] as? String ?: default
    override fun putString(key: String, value: String?) { store[key] = value }
    override fun getBoolean(key: String, default: Boolean): Boolean = store[key] as? Boolean ?: default
    override fun putBoolean(key: String, value: Boolean) { store[key] = value }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, default: Set<String>): Set<String> =
        (store[key] as? Set<String>) ?: default

    override fun putStringSet(key: String, value: Set<String>) { store[key] = value }
    override fun contains(key: String): Boolean = store.containsKey(key)
    override fun remove(key: String) { store.remove(key) }
}
