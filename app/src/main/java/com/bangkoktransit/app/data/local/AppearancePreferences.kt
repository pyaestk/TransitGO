package com.bangkoktransit.app.data.local

import android.content.Context

class AppearancePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun isDarkModeEnabled(defaultValue: Boolean): Boolean {
        return preferences.getBoolean(KEY_DARK_MODE, defaultValue)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "transit_appearance"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
