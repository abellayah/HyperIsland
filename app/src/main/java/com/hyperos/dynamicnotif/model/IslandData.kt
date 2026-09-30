package com.hyperos.dynamicnotif.model

import android.app.PendingIntent
import android.graphics.drawable.Drawable

enum class IslandType {
    CHARGING,
    RINGER,
    MUSIC,
    NOTIFICATION
}

data class IslandData(
    val type: IslandType,
    val title: String,
    val subtitle: String,
    val iconRes: Int? = null,
    val customIcon: Drawable? = null,
    val badgeText: String? = null,
    val durationMs: Long = 3500L,
    val pendingIntent: PendingIntent? = null,
    val isMusicPlaying: Boolean = false
)
