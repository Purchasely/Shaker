package com.purchasely.shaker.domain.model

enum class DisplayMode(val storageValue: String, val label: String) {
    FULLSCREEN("fullscreen", "Full"),
    MODAL("modal", "Modal"),
    DRAWER("drawer", "Drawer"),
    POPIN("popin", "Popin");

    companion object {
        fun fromStorage(value: String?): DisplayMode =
            entries.firstOrNull { it.storageValue == value } ?: FULLSCREEN
    }
}
