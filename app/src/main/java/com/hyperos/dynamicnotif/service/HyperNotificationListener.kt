package com.hyperos.dynamicnotif.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.hyperos.dynamicnotif.R
import com.hyperos.dynamicnotif.util.PreferencesHelper

class HyperNotificationListener : NotificationListenerService() {

    private lateinit var prefsHelper: PreferencesHelper

    override fun onCreate() {
        super.onCreate()
        prefsHelper = PreferencesHelper(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || !prefsHelper.isEnabled || !prefsHelper.isNotificationEnabled) return

        val packageName = sbn.packageName ?: return
        if (packageName == applicationContext.packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val isMusicApp = packageName.contains("spotify") ||
                packageName.contains("music") ||
                packageName.contains("youtube") ||
                packageName.contains("soundcloud") ||
                notification.category == Notification.CATEGORY_TRANSPORT

        if (isMusicApp) {
            if (!prefsHelper.isMusicEnabled) return
            DynamicIslandService.sendEvent(
                context = this,
                type = "MUSIC",
                title = if (title.isNotBlank()) title else "Musik",
                subtitle = if (text.isNotBlank()) text else "Sedang diputar",
                iconRes = R.drawable.ic_hyper_music,
                duration = 4500L,
                pendingIntent = notification.contentIntent,
                isMusic = true
            )
        } else {
            if (sbn.isOngoing) return
            DynamicIslandService.sendEvent(
                context = this,
                type = "NOTIFICATION",
                title = title,
                subtitle = text,
                iconRes = R.drawable.ic_hyper_notification,
                duration = 3800L,
                pendingIntent = notification.contentIntent,
                isMusic = false
            )
        }
    }
}
