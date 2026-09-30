package com.hyperos.dynamicnotif.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hyper_island_prefs", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("is_enabled", true)
        set(value) = prefs.edit().putBoolean("is_enabled", value).apply()

    var offsetY: Int
        get() = prefs.getInt("offset_y", 22)
        set(value) = prefs.edit().putInt("offset_y", value).apply()

    var pillWidth: Int
        get() = prefs.getInt("pill_width", 160)
        set(value) = prefs.edit().putInt("pill_width", value).apply()

    var pillHeight: Int
        get() = prefs.getInt("pill_height", 36)
        set(value) = prefs.edit().putInt("pill_height", value).apply()

    var isChargingEnabled: Boolean
        get() = prefs.getBoolean("is_charging_enabled", true)
        set(value) = prefs.edit().putBoolean("is_charging_enabled", value).apply()

    var isRingerEnabled: Boolean
        get() = prefs.getBoolean("is_ringer_enabled", true)
        set(value) = prefs.edit().putBoolean("is_ringer_enabled", value).apply()

    var isMusicEnabled: Boolean
        get() = prefs.getBoolean("is_music_enabled", true)
        set(value) = prefs.edit().putBoolean("is_music_enabled", value).apply()

    var isNotificationEnabled: Boolean
        get() = prefs.getBoolean("is_notif_enabled", true)
        set(value) = prefs.edit().putBoolean("is_notif_enabled", value).apply()

    fun resetToInfinixSmart9Preset() {
        offsetY = 22
        pillWidth = 160
        pillHeight = 36
    }
}
