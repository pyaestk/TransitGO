package com.bangkoktransit.app.data.local

import android.content.Context

class AndroidStringPreferences(context: Context, name: String) : StringPreferences {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun setString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}
