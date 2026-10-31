package com.spott.app.proximity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.spott.app.data.UserSession

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Device boot / update completed. Checking if Sync Radar should resume.")
            if (UserSession.isRegistered(context)) {
                Log.i(TAG, "User registered. Auto-starting silent background SyncRadarService.")
                SyncRadarService.startService(context, isAppForeground = false)
            }
        }
    }
}
