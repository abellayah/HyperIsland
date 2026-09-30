package com.hyperos.dynamicnotif.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.hyperos.dynamicnotif.R
import com.hyperos.dynamicnotif.util.PreferencesHelper

class MyAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MyAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }

    private lateinit var windowManager: WindowManager
    private lateinit var prefsHelper: PreferencesHelper
    private var statusBarMaskView: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        prefsHelper = PreferencesHelper(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        updateStatusBarMask()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !prefsHelper.isEnabled || !prefsHelper.isNotificationEnabled) return

        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            if (packageName == this.packageName) return

            val texts = event.text
            if (texts.isEmpty()) return

            val message = texts.joinToString(" ")
            if (message.isBlank()) return

            val appName = try {
                val pm = packageManager
                val info = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                packageName
            }

            DynamicIslandService.sendEvent(
                context = this,
                type = "NOTIFICATION",
                title = appName,
                subtitle = message,
                iconRes = R.drawable.ic_hyper_notification,
                duration = 3800L,
                isMusic = false
            )
        }
    }

    fun updateStatusBarMask() {
        if (!::prefsHelper.isInitialized || !::windowManager.isInitialized) return

        if (prefsHelper.isEnabled && prefsHelper.isHideStatusBar) {
            showStatusBarMask()
        } else {
            hideStatusBarMask()
        }
    }

    private fun showStatusBarMask() {
        if (statusBarMaskView != null) return

        val statusBarHeight = getStatusBarHeight()
        val mask = View(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            statusBarHeight,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        try {
            windowManager.addView(mask, params)
            statusBarMaskView = mask
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideStatusBarMask() {
        statusBarMaskView?.let { mask ->
            try {
                windowManager.removeView(mask)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            statusBarMaskView = null
        }
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        if (result <= 0) {
            val density = resources.displayMetrics.density
            result = (26 * density).toInt()
        }
        return result
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        hideStatusBarMask()
        instance = null
    }
}
