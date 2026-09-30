package com.hyperos.dynamicnotif

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.hyperos.dynamicnotif.databinding.ActivityMainBinding
import com.hyperos.dynamicnotif.service.DynamicIslandService
import com.hyperos.dynamicnotif.service.MyAccessibilityService
import com.hyperos.dynamicnotif.util.PreferencesHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsHelper: PreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsHelper = PreferencesHelper(this)

        initViews()
        initListeners()
        updatePermissionStates()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
    }

    private fun initViews() {
        binding.switchMaster.isChecked = prefsHelper.isEnabled
        binding.switchTriggerOnly.isChecked = prefsHelper.isTriggerOnly
        binding.switchHideBar.isChecked = prefsHelper.isHideStatusBar

        binding.switchCharging.isChecked = prefsHelper.isChargingEnabled
        binding.switchRinger.isChecked = prefsHelper.isRingerEnabled
        binding.switchMusic.isChecked = prefsHelper.isMusicEnabled
        binding.switchNotification.isChecked = prefsHelper.isNotificationEnabled

        val currentY = prefsHelper.offsetY.toFloat()
        val currentX = prefsHelper.offsetX.toFloat()

        binding.sliderOffsetY.value = currentY.coerceIn(0f, 60f)
        binding.tvOffsetYLabel.text = "Posisi Vertikal (Y): ${currentY.toInt()} dp"

        binding.sliderOffsetX.value = currentX.coerceIn(-50f, 50f)
        binding.tvOffsetXLabel.text = "Posisi Horizontal (X): ${currentX.toInt()} dp"
    }

    private fun initListeners() {
        binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            prefsHelper.isEnabled = isChecked
            if (isChecked) {
                if (hasOverlayPermission()) {
                    startIslandService()
                } else {
                    requestOverlayPermission()
                    binding.switchMaster.isChecked = false
                }
            } else {
                stopIslandService()
            }
        }

        binding.switchTriggerOnly.setOnCheckedChangeListener { _, isChecked ->
            prefsHelper.isTriggerOnly = isChecked
            notifyServiceSettingsChanged()
            val msg = if (isChecked) "Kapsul hanya muncul saat ada notifikasi" else "Kapsul selalu tampil"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        binding.switchHideBar.setOnCheckedChangeListener { _, isChecked ->
            prefsHelper.isHideStatusBar = isChecked
            notifyServiceSettingsChanged()
            val msg = if (isChecked) "Status bar (jam, sinyal, wifi) disembunyikan" else "Status bar ditampilkan normal"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        binding.switchCharging.setOnCheckedChangeListener { _, isChecked ->
            prefsHelper.isChargingEnabled = isChecked
        }

        binding.switchRinger.setOnCheckedChangeListener { _, isChecked ->
            prefsHelper.isRingerEnabled = isChecked
        }

        binding.switchMusic.setOnCheckedChangeListener { _, isChecked ->
            prefsHelper.isMusicEnabled = isChecked
        }

        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            prefsHelper.isNotificationEnabled = isChecked
        }

        binding.sliderOffsetY.addOnChangeListener { _, value, _ ->
            val yVal = value.toInt()
            prefsHelper.offsetY = yVal
            binding.tvOffsetYLabel.text = "Posisi Vertikal (Y): $yVal dp"
            notifyServiceSettingsChanged()
        }

        binding.btnOffsetYMinus.setOnClickListener {
            val nextVal = (prefsHelper.offsetY - 1).coerceAtLeast(0)
            prefsHelper.offsetY = nextVal
            binding.sliderOffsetY.value = nextVal.toFloat()
            binding.tvOffsetYLabel.text = "Posisi Vertikal (Y): $nextVal dp"
            notifyServiceSettingsChanged()
        }

        binding.btnOffsetYPlus.setOnClickListener {
            val nextVal = (prefsHelper.offsetY + 1).coerceAtMost(60)
            prefsHelper.offsetY = nextVal
            binding.sliderOffsetY.value = nextVal.toFloat()
            binding.tvOffsetYLabel.text = "Posisi Vertikal (Y): $nextVal dp"
            notifyServiceSettingsChanged()
        }

        binding.sliderOffsetX.addOnChangeListener { _, value, _ ->
            val xVal = value.toInt()
            prefsHelper.offsetX = xVal
            binding.tvOffsetXLabel.text = "Posisi Horizontal (X): $xVal dp"
            notifyServiceSettingsChanged()
        }

        binding.btnOffsetXMinus.setOnClickListener {
            val nextVal = (prefsHelper.offsetX - 1).coerceAtLeast(-50)
            prefsHelper.offsetX = nextVal
            binding.sliderOffsetX.value = nextVal.toFloat()
            binding.tvOffsetXLabel.text = "Posisi Horizontal (X): $nextVal dp"
            notifyServiceSettingsChanged()
        }

        binding.btnOffsetXPlus.setOnClickListener {
            val nextVal = (prefsHelper.offsetX + 1).coerceAtMost(50)
            prefsHelper.offsetX = nextVal
            binding.sliderOffsetX.value = nextVal.toFloat()
            binding.tvOffsetXLabel.text = "Posisi Horizontal (X): $nextVal dp"
            notifyServiceSettingsChanged()
        }

        binding.btnPresetInfinix.setOnClickListener {
            prefsHelper.resetToInfinixSmart9Preset()
            binding.sliderOffsetY.value = 22f
            binding.sliderOffsetX.value = 0f
            binding.tvOffsetYLabel.text = "Posisi Vertikal (Y): 22 dp"
            binding.tvOffsetXLabel.text = "Posisi Horizontal (X): 0 dp"
            notifyServiceSettingsChanged()
            Toast.makeText(this, "Preset Infinix Smart 9 diterapkan", Toast.LENGTH_SHORT).show()
        }

        binding.btnAccessibilityPermission.setOnClickListener {
            requestAccessibilityPermission()
        }

        binding.btnOverlayPermission.setOnClickListener {
            requestOverlayPermission()
        }

        binding.btnNotifPermission.setOnClickListener {
            requestNotificationListenerPermission()
        }

        binding.btnBatteryOptimization.setOnClickListener {
            requestIgnoreBatteryOptimization()
        }

        binding.btnTestWhatsapp.setOnClickListener {
            if (ensureServiceRunning()) {
                DynamicIslandService.sendEvent(
                    context = this,
                    type = "NOTIFICATION",
                    title = "WhatsApp",
                    subtitle = "David: Halo! Notif HyperOS real-time mantap!",
                    iconRes = R.drawable.ic_hyper_notification,
                    duration = 3800L,
                    isMusic = false
                )
            }
        }

        binding.btnTestCharging.setOnClickListener {
            if (ensureServiceRunning()) {
                DynamicIslandService.sendEvent(
                    context = this,
                    type = "CHARGING",
                    title = "Mi Turbo Charge",
                    subtitle = "Baterai 90% • Mengisi Daya",
                    badge = "90%",
                    iconRes = R.drawable.ic_hyper_bolt,
                    duration = 3500L,
                    isMusic = false
                )
            }
        }

        binding.btnTestMusic.setOnClickListener {
            if (ensureServiceRunning()) {
                DynamicIslandService.sendEvent(
                    context = this,
                    type = "MUSIC",
                    title = "Alan Walker - Faded",
                    subtitle = "Spotify Music",
                    iconRes = R.drawable.ic_hyper_music,
                    duration = 5000L,
                    isMusic = true
                )
            }
        }

        binding.btnTestRinger.setOnClickListener {
            if (ensureServiceRunning()) {
                DynamicIslandService.sendEvent(
                    context = this,
                    type = "RINGER",
                    title = "Silent",
                    subtitle = "Mode Hening Diaktifkan",
                    iconRes = R.drawable.ic_hyper_ringer_silent,
                    duration = 2500L,
                    isMusic = false
                )
            }
        }
    }

    private fun ensureServiceRunning(): Boolean {
        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Izinkan Tampil Di Atas Aplikasi terlebih dahulu", Toast.LENGTH_SHORT).show()
            requestOverlayPermission()
            return false
        }
        if (!DynamicIslandService.isRunning) {
            startIslandService()
        }
        return true
    }

    private fun startIslandService() {
        val serviceIntent = Intent(this, DynamicIslandService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopIslandService() {
        val serviceIntent = Intent(this, DynamicIslandService::class.java)
        stopService(serviceIntent)
    }

    private fun notifyServiceSettingsChanged() {
        val intent = Intent(this, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_UPDATE_SETTINGS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun hasNotificationPermission(): Boolean {
        val packages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return packages.contains(packageName)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return MyAccessibilityService.isServiceRunning
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun updatePermissionStates() {
        if (isAccessibilityServiceEnabled()) {
            binding.btnAccessibilityPermission.text = getString(R.string.granted)
            binding.btnAccessibilityPermission.isEnabled = false
            binding.btnAccessibilityPermission.setBackgroundColor(getColor(R.color.dnotch_card_stroke))
        } else {
            binding.btnAccessibilityPermission.text = getString(R.string.grant)
            binding.btnAccessibilityPermission.isEnabled = true
        }

        if (hasOverlayPermission()) {
            binding.btnOverlayPermission.text = getString(R.string.granted)
            binding.btnOverlayPermission.isEnabled = false
            binding.btnOverlayPermission.setBackgroundColor(getColor(R.color.dnotch_card_stroke))
        } else {
            binding.btnOverlayPermission.text = getString(R.string.grant)
            binding.btnOverlayPermission.isEnabled = true
        }

        if (hasNotificationPermission()) {
            binding.btnNotifPermission.text = getString(R.string.granted)
            binding.btnNotifPermission.isEnabled = false
            binding.btnNotifPermission.setBackgroundColor(getColor(R.color.dnotch_card_stroke))
        } else {
            binding.btnNotifPermission.text = getString(R.string.grant)
            binding.btnNotifPermission.isEnabled = true
        }

        if (isBatteryOptimizationIgnored()) {
            binding.btnBatteryOptimization.text = getString(R.string.granted)
            binding.btnBatteryOptimization.isEnabled = false
        } else {
            binding.btnBatteryOptimization.text = "Bebaskan"
            binding.btnBatteryOptimization.isEnabled = true
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestNotificationListenerPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun requestIgnoreBatteryOptimization() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(fallback)
        }
    }
}
