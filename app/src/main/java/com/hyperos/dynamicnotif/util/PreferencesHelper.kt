package com.hyperos.dynamicnotif.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hyper_island_prefs", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("is_enabled", true)
        set(value) = prefs.edit().putBoolean("is_enabled", value).apply()

    var isTriggerOnly: Boolean
        get() = prefs.getBoolean("is_trigger_only", true)
        set(value) = prefs.edit().putBoolean("is_trigger_only", value).apply()

    var isHideStatusBar: Boolean
        get() = prefs.getBoolean("is_hide_status_bar", false)
        set(value) = prefs.edit().putBoolean("is_hide_status_bar", value).apply()

    var offsetX: Int
        get() = prefs.getInt("offset_x", 0)
        set(value) = prefs.edit().putInt("offset_x", value).apply()

    var offsetY: Int
        get() = prefs.getInt("offset_y", 22)
        set(value) = prefs.edit().putInt("offset_y", value).apply()

    var notchWidth: Int
        get() = prefs.getInt("notch_width", 160)
        set(value) = prefs.edit().putInt("notch_width", value).apply()

    var notchHeight: Int
        get() = prefs.getInt("notch_height", 36)
        set(value) = prefs.edit().putInt("notch_height", value).apply()

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
        offsetX = 0
        offsetY = 22
        notchWidth = 160
        notchHeight = 36
    }
}
