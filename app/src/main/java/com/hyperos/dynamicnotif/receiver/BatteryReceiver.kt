package com.hyperos.dynamicnotif.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.hyperos.dynamicnotif.R
import com.hyperos.dynamicnotif.service.DynamicIslandService
import com.hyperos.dynamicnotif.util.PreferencesHelper

class BatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferencesHelper(context)
        if (!prefs.isEnabled || !prefs.isChargingEnabled) return

        val action = intent.action ?: return
        if (action == Intent.ACTION_POWER_CONNECTED) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val badgeText = "$level%"

            DynamicIslandService.sendEvent(
                context = context,
                type = "CHARGING",
                title = "Mi Turbo Charge",
                subtitle = "Mengisi daya • $badgeText",
                badge = badgeText,
                iconRes = R.drawable.ic_hyper_bolt,
                duration = 3800L,
                isMusic = false,
                batteryLevel = level
            )
        }
    }
}
