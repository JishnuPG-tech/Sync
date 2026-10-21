package com.ghostmesh.app.proximity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log

class ThermalAndBatteryController(
    private val context: Context,
    private val onThrottleRequired: (reason: String, shouldPauseCompletely: Boolean) -> Unit
) {
    companion object {
        private const val TAG = "ThermalBatteryCtrl"
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    // Thermal listener for Android 10+ (API 29+)
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    // Battery status broadcast receiver
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_LOW -> {
                    Log.w(TAG, "Battery Low detected (<15%). Downthrottling radar.")
                    onThrottleRequired("Battery Low (<15%)", false)
                }
            }
        }
    }

    fun startMonitoring() {
        // 1. Register Thermal Status Listener (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
                when (status) {
                    PowerManager.THERMAL_STATUS_EMERGENCY -> {
                        Log.e(TAG, "Hardware Emergency Thermal Limit reached (>60°C). Pausing radar safely.")
                        onThrottleRequired("Hardware Emergency Heating", true)
                    }
                    PowerManager.THERMAL_STATUS_MODERATE,
                    PowerManager.THERMAL_STATUS_SEVERE,
                    PowerManager.THERMAL_STATUS_CRITICAL -> {
                        Log.i(TAG, "Device warm (thermal status: $status). Maintaining active radar.")
                    }
                    PowerManager.THERMAL_STATUS_NONE,
                    PowerManager.THERMAL_STATUS_LIGHT -> {
                        // Normal thermal conditions
                    }
                }
            }
            powerManager.addThermalStatusListener(thermalListener!!)
        }

        // 2. Register Battery Receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_LOW)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListener != null && powerManager != null) {
            powerManager.removeThermalStatusListener(thermalListener!!)
            thermalListener = null
        }
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }

    fun isBatteryCurrentlyLow(): Boolean {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) {
            val batteryPct = (level / scale.toFloat()) * 100
            return batteryPct <= 15f
        }
        return false
    }
}
