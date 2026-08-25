package com.gamechest.ui.platform

import java.util.prefs.Preferences

object PreferenceStore {
    private val prefs: Preferences? = try {
        Preferences.userRoot().node("com.gamechest.prefs")
    } catch (e: Throwable) {
        null
    }

    private val inMemoryFallback = mutableMapOf<String, Any>()

    fun getBoolean(key: String, def: Boolean): Boolean {
        return try {
            prefs?.getBoolean(key, def) ?: (inMemoryFallback[key] as? Boolean ?: def)
        } catch (e: Throwable) {
            inMemoryFallback[key] as? Boolean ?: def
        }
    }

    fun setBoolean(key: String, value: Boolean) {
        inMemoryFallback[key] = value
        try {
            prefs?.putBoolean(key, value)
            prefs?.flush()
        } catch (e: Throwable) {
            // Ignored in restricted environments
        }
    }
}
