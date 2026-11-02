package com.spott.app.proximity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.spott.app.MainActivity
import com.spott.app.R

object NotificationHelper {

    const val CHANNEL_STATUS_ID = "sync_radar_status"
    const val CHANNEL_ENCOUNTERS_ID = "sync_peer_encounters"

    const val SERVICE_NOTIFICATION_ID = 45100
    private const val PEER_NOTIFICATION_BASE_ID = 50000

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Silent Low-Priority Status Channel (for Foreground Service)
            val statusChannel = NotificationChannel(
                CHANNEL_STATUS_ID,
                "Sync Radar Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows silent background status while Radar is active"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }

            // 2. High-Priority Alert Channel for Detected Peers (Heads-Up Banner)
            val encounterChannel = NotificationChannel(
                CHANNEL_ENCOUNTERS_ID,
                "Spott Nearby Proximity Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate heads-up alert when a new person is spotted nearby"
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 250)
                enableLights(true)
                lightColor = 0xFFF59E0B.toInt() // Spott Amber
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            manager.createNotificationChannel(statusChannel)
            manager.createNotificationChannel(encounterChannel)
        }
    }

    /**
     * Silent persistent notification required for Android Foreground Service
     */
    fun buildServiceNotification(context: Context): Notification {
        initNotificationChannels(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingLaunch = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_STATUS_ID)
            .setSmallIcon(R.drawable.sync_emblem)
            .setContentTitle("Sync Radar Active")
            .setContentText("Silently scanning for nearby peers with zero battery drain")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingLaunch)
            .build()
    }

    /**
     * Posts high-priority alert when a nearby peer is detected
     */
    fun postPeerDetectedNotification(
        context: Context,
        instagramHandle: String,
        displayName: String,
        distanceDisplay: String,
        encounterId: String
    ) {
        initNotificationChannels(context)

        val cleanHandle = instagramHandle.trim().removePrefix("@").filter {
            it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' || it == '.'
        }.lowercase()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // 1. Action: Open native Instagram profile or web fallback
        val igIntent = createInstagramIntent(context, cleanHandle)
        val igPendingIntent = PendingIntent.getActivity(
            context,
            cleanHandle.hashCode(),
            igIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action: Open Sync App
        val syncIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_encounter_id", encounterId)
        }
        val syncPendingIntent = PendingIntent.getActivity(
            context,
            encounterId.hashCode(),
            syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (displayName.isNotBlank() && !displayName.equals("Nearby Sync User", ignoreCase = true) && !displayName.equals(cleanHandle, ignoreCase = true)) {
            "👋 $displayName (@$cleanHandle) is nearby!"
        } else {
            "👋 @$cleanHandle spotted nearby!"
        }

        val text = "Detected ~$distanceDisplay away · Tap to connect on Instagram."

        val notification = NotificationCompat.Builder(context, CHANNEL_ENCOUNTERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text + "\nInstagram Profile: @" + cleanHandle))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(syncPendingIntent)
            .addAction(0, "Open Instagram ↗", igPendingIntent)
            .addAction(0, "View in Spott", syncPendingIntent)
            .build()

        val notifId = PEER_NOTIFICATION_BASE_ID + (cleanHandle.hashCode() and 0x7FFF)
        try {
            manager.notify(notifId, notification)
        } catch (e: Exception) {
            android.util.Log.w("NotificationHelper", "Cannot post notification: " + e.message)
        }
    }

    /**
     * Creates direct deep link into Instagram app, or web fallback if app is not installed
     */
    fun createInstagramIntent(context: Context, rawHandle: String): Intent {
        val cleanHandle = rawHandle.trim().removePrefix("@").filter {
            it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' || it == '.'
        }.lowercase()

        val targetUrl = if (cleanHandle.isNotEmpty() && cleanHandle != "user" && cleanHandle != "spott.user") {
            "https://instagram.com/_u/$cleanHandle"
        } else {
            "https://instagram.com/"
        }
        val appUri = Uri.parse(targetUrl)
        val webUri = Uri.parse(if (cleanHandle.isNotEmpty() && cleanHandle != "user") "https://instagram.com/$cleanHandle" else "https://instagram.com/")

        val isAppInstalled = try {
            val pm = context.packageManager
            pm.getPackageInfo("com.instagram.android", PackageManager.GET_ACTIVITIES)
            true
        } catch (_: Exception) {
            false
        }

        return if (isAppInstalled) {
            Intent(Intent.ACTION_VIEW, appUri).apply {
                setPackage("com.instagram.android")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}
