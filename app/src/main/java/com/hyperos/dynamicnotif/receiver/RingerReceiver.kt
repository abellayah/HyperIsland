package com.hyperos.dynamicnotif.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.hyperos.dynamicnotif.R
import com.hyperos.dynamicnotif.service.DynamicIslandService
import com.hyperos.dynamicnotif.util.PreferencesHelper

class RingerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferencesHelper(context)
        if (!prefs.isEnabled || !prefs.isRingerEnabled) return

        val action = intent.action ?: return
        if (action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val mode = audioManager.ringerMode

            val (title, iconRes) = when (mode) {
                AudioManager.RINGER_MODE_SILENT -> {
                    Pair("Silent", R.drawable.ic_hyper_ringer_silent)
                }
                AudioManager.RINGER_MODE_VIBRATE -> {
                    Pair("Vibrate", R.drawable.ic_hyper_ringer_vibrate)
                }
                else -> {
                    Pair("Regular", R.drawable.ic_hyper_ringer_normal)
                }
            }

            DynamicIslandService.sendEvent(
                context = context,
                type = "RINGER",
                title = title,
                subtitle = "Mode Suara Berubah",
                iconRes = iconRes,
                duration = 2500L,
                isMusic = false
            )
        }
    }
}
