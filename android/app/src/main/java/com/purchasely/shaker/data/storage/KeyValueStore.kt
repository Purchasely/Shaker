package com.purchasely.shaker.data.storage

interface KeyValueStore {
    fun getString(key: String, default: String? = null): String?
    fun putString(key: String, value: String?)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String>
    fun putStringSet(key: String, value: Set<String>)
    fun contains(key: String): Boolean
    fun remove(key: String)
}
