package com.hyperos.dynamicnotif.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import com.hyperos.dynamicnotif.R
import com.hyperos.dynamicnotif.util.PreferencesHelper
import com.hyperos.dynamicnotif.view.EqualizerView

class DynamicIslandService : Service() {

    companion object {
        const val ACTION_SHOW_EVENT = "com.hyperos.dynamicnotif.ACTION_SHOW_EVENT"
        const val ACTION_UPDATE_SETTINGS = "com.hyperos.dynamicnotif.ACTION_UPDATE_SETTINGS"
        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_SUBTITLE = "EXTRA_SUBTITLE"
        const val EXTRA_BADGE = "EXTRA_BADGE"
        const val EXTRA_ICON_RES = "EXTRA_ICON_RES"
        const val EXTRA_DURATION = "EXTRA_DURATION"
        const val EXTRA_PENDING_INTENT = "EXTRA_PENDING_INTENT"
        const val EXTRA_IS_MUSIC = "EXTRA_IS_MUSIC"
        const val EXTRA_BATTERY_LEVEL = "EXTRA_BATTERY_LEVEL"
        private const val CHANNEL_ID = "hyper_island_foreground"
        private const val NOTIF_ID = 1001

        var isRunning = false
            private set

        fun sendEvent(
            context: Context,
            type: String,
            title: String,
            subtitle: String,
            badge: String? = null,
            iconRes: Int? = null,
            duration: Long = 3500L,
            pendingIntent: PendingIntent? = null,
            isMusic: Boolean = false,
            batteryLevel: Int = 85
        ) {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = ACTION_SHOW_EVENT
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SUBTITLE, subtitle)
                putExtra(EXTRA_BADGE, badge)
                iconRes?.let { putExtra(EXTRA_ICON_RES, it) }
                putExtra(EXTRA_DURATION, duration)
                pendingIntent?.let { putExtra(EXTRA_PENDING_INTENT, it) }
                putExtra(EXTRA_IS_MUSIC, isMusic)
                putExtra(EXTRA_BATTERY_LEVEL, batteryLevel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var prefsHelper: PreferencesHelper
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var statusBarMaskView: View? = null

    private var notchContainer: ConstraintLayout? = null
    private var layoutBatteryNotch: RelativeLayout? = null
    private var layoutDefaultNotch: RelativeLayout? = null

    private var tvChargingTitle: TextView? = null
    private var tvBatteryPercent: TextView? = null
    private var pbBattery: ProgressBar? = null

    private var ivNotchIcon: ImageView? = null
    private var tvNotchTitle: TextView? = null
    private var tvNotchSubtitle: TextView? = null
    private var tvNotchBadge: TextView? = null
    private var notchEqualizer: EqualizerView? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isExpanded = false
    private var currentPendingIntent: PendingIntent? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        prefsHelper = PreferencesHelper(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        startForegroundNotification()
        createIslandOverlay()
        updateStatusBarMask()
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hyper Island Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Layanan aktifitas Dynamic Island HyperOS"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hyper Island Berjalan")
            .setContentText("Kapsul notifikasi HyperOS siap siaga")
            .setSmallIcon(R.drawable.ic_hyper_bolt)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)
    }

    private fun createIslandOverlay() {
        if (!prefsHelper.isEnabled) return

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.layout_hyper_island, null)

        notchContainer = overlayView?.findViewById(R.id.container)
        layoutBatteryNotch = overlayView?.findViewById(R.id.layoutBatteryNotch)
        layoutDefaultNotch = overlayView?.findViewById(R.id.layoutDefaultNotch)

        tvChargingTitle = overlayView?.findViewById(R.id.tvChargingTitle)
        tvBatteryPercent = overlayView?.findViewById(R.id.tvBatteryPercent)
        pbBattery = overlayView?.findViewById(R.id.pbBattery)

        ivNotchIcon = overlayView?.findViewById(R.id.ivNotchIcon)
        tvNotchTitle = overlayView?.findViewById(R.id.tvNotchTitle)
        tvNotchSubtitle = overlayView?.findViewById(R.id.tvNotchSubtitle)
        tvNotchBadge = overlayView?.findViewById(R.id.tvNotchBadge)
        notchEqualizer = overlayView?.findViewById(R.id.notchEqualizer)

        val density = resources.displayMetrics.density
        val topMargin = (prefsHelper.offsetY * density).toInt()
        val xMargin = (prefsHelper.offsetX * density).toInt()

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = xMargin
            y = topMargin
        }

        overlayView?.visibility = View.GONE

        notchContainer?.setOnClickListener {
            currentPendingIntent?.let { intent ->
                try {
                    intent.send()
                    collapseIsland()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        var startY = 0f
        notchContainer?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    false
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = event.rawY - startY
                    if (deltaY < -30f) {
                        collapseIsland()
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateStatusBarMask() {
        if (!prefsHelper.isEnabled || !prefsHelper.isHideStatusBar) {
            removeStatusBarMask()
            return
        }

        if (statusBarMaskView != null) return

        val statusBarHeight = getStatusBarHeight()
        val mask = View(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            statusBarHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
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

    private fun removeStatusBarMask() {
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY

        when (intent.action) {
            ACTION_UPDATE_SETTINGS -> {
                updatePositionSettings()
                updateStatusBarMask()
                MyAccessibilityService.instance?.updateStatusBarMask()
            }
            ACTION_SHOW_EVENT -> {
                val type = intent.getStringExtra(EXTRA_TYPE) ?: "NOTIFICATION"
                val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
                val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: ""
                val badge = intent.getStringExtra(EXTRA_BADGE)
                val iconRes = intent.getIntExtra(EXTRA_ICON_RES, R.drawable.ic_hyper_bolt)
                val duration = intent.getLongExtra(EXTRA_DURATION, 3500L)
                val isMusic = intent.getBooleanExtra(EXTRA_IS_MUSIC, false)
                val batteryLevel = intent.getIntExtra(EXTRA_BATTERY_LEVEL, 85)
                val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_PENDING_INTENT, PendingIntent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_PENDING_INTENT)
                }

                displayIsland(
                    type = type,
                    title = title,
                    subtitle = subtitle,
                    badge = badge,
                    iconRes = iconRes,
                    duration = duration,
                    pendingIntent = pendingIntent,
                    isMusic = isMusic,
                    batteryLevel = batteryLevel
                )
            }
        }

        return START_STICKY
    }

    private fun displayIsland(
        type: String,
        title: String,
        subtitle: String,
        badge: String?,
        iconRes: Int,
        duration: Long,
        pendingIntent: PendingIntent?,
        isMusic: Boolean,
        batteryLevel: Int
    ) {
        if (!prefsHelper.isEnabled || overlayView == null) return

        mainHandler.removeCallbacksAndMessages(null)
        currentPendingIntent = pendingIntent

        if (type == "CHARGING") {
            layoutDefaultNotch?.visibility = View.GONE
            layoutBatteryNotch?.visibility = View.VISIBLE

            tvChargingTitle?.text = if (title.isNotBlank()) title else "Mi Turbo Charge"
            tvBatteryPercent?.text = "$batteryLevel%"
            pbBattery?.progress = batteryLevel
            notchEqualizer?.stop()
        } else {
            layoutBatteryNotch?.visibility = View.GONE
            layoutDefaultNotch?.visibility = View.VISIBLE

            tvNotchTitle?.text = title
            tvNotchSubtitle?.text = subtitle
            ivNotchIcon?.setImageResource(iconRes)

            if (!badge.isNullOrEmpty()) {
                tvNotchBadge?.visibility = View.VISIBLE
                tvNotchBadge?.text = badge
            } else {
                tvNotchBadge?.visibility = View.GONE
            }

            if (isMusic) {
                notchEqualizer?.visibility = View.VISIBLE
                notchEqualizer?.start()
            } else {
                notchEqualizer?.stop()
                notchEqualizer?.visibility = View.GONE
            }
        }

        expandIsland()

        if (duration > 0) {
            mainHandler.postDelayed({
                collapseIsland()
            }, duration)
        }
    }

    private fun expandIsland() {
        val container = notchContainer ?: return
        val overlay = overlayView ?: return

        overlay.visibility = View.VISIBLE
        isExpanded = true

        container.scaleX = 0.35f
        container.scaleY = 0.35f
        container.alpha = 0.1f

        container.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(320)
            .setInterpolator(OvershootInterpolator(1.25f))
            .setListener(null)
            .start()
    }

    private fun collapseIsland() {
        val container = notchContainer ?: return
        val overlay = overlayView ?: return
        if (!isExpanded) return

        isExpanded = false
        notchEqualizer?.stop()

        container.animate()
            .scaleX(0.35f)
            .scaleY(0.35f)
            .alpha(0f)
            .setDuration(220)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    overlay.visibility = View.GONE
                }
            })
            .start()
    }

    fun updatePositionSettings() {
        if (overlayView == null || layoutParams == null) return
        val density = resources.displayMetrics.density
        layoutParams?.y = (prefsHelper.offsetY * density).toInt()
        layoutParams?.x = (prefsHelper.offsetX * density).toInt()
        try {
            windowManager.updateViewLayout(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        mainHandler.removeCallbacksAndMessages(null)
        removeStatusBarMask()
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
