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
import android.widget.Toast
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

        val isValidHandle = cleanHandle.isNotBlank() &&
                cleanHandle != "user" &&
                cleanHandle != "someone" &&
                cleanHandle != "spott.user" &&
                cleanHandle != "null"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // 1. Action: Open native Instagram profile or web fallback
        val igIntent = createInstagramIntent(context, if (isValidHandle) cleanHandle else "")
        val igPendingIntent = PendingIntent.getActivity(
            context,
            encounterId.hashCode() + 1,
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

        val isRealName = displayName.isNotBlank() && 
                !displayName.equals(cleanHandle, ignoreCase = true) && 
                !displayName.startsWith("Nearby Spott User") &&
                !BleDiscoveryManager.isDeviceHardwareName(displayName)

        val title = if (isValidHandle && isRealName) {
            "👋 $displayName (@$cleanHandle) is nearby!"
        } else if (isValidHandle) {
            "👋 @$cleanHandle is nearby!"
        } else if (isRealName) {
            "👋 $displayName is nearby!"
        } else {
            "👋 New person spotted nearby!"
        }

        val text = "Detected ~$distanceDisplay away · Tap to connect."

        val bigText = if (isValidHandle) {
            "$text\nInstagram Profile: @$cleanHandle"
        } else {
            text
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ENCOUNTERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(syncPendingIntent)
            .addAction(0, if (isValidHandle) "Open Instagram ↗" else "Open Spott", if (isValidHandle) igPendingIntent else syncPendingIntent)
            .addAction(0, "View in Spott", syncPendingIntent)
            .build()

        val notifId = PEER_NOTIFICATION_BASE_ID + (encounterId.hashCode() and 0x7FFF)
        try {
            manager.notify(notifId, notification)
        } catch (e: Exception) {
            android.util.Log.w("NotificationHelper", "Cannot post notification: " + e.message)
        }
    }

    /**
     * Creates direct deep link into official Instagram app using native uri,
     * or web fallback if app is not installed.
     * Prevents deprecated _u/ links and non-existent handles from creating blank black screens!
     */
    fun createInstagramIntent(context: Context, rawHandle: String): Intent {
        val cleanHandle = rawHandle.trim().removePrefix("@").filter {
            it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' || it == '.'
        }.lowercase()

        val isValidHandle = cleanHandle.isNotBlank() &&
                cleanHandle != "user" &&
                cleanHandle != "spott.user" &&
                cleanHandle != "someone" &&
                cleanHandle != "null"

        if (!isValidHandle) {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage("com.instagram.android")
            return launchIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        val isAppInstalled = try {
            val pm = context.packageManager
            pm.getPackageInfo("com.instagram.android", PackageManager.GET_ACTIVITIES)
            true
        } catch (_: Exception) {
            false
        }

        val webUri = Uri.parse("https://www.instagram.com/$cleanHandle/")

        return if (isAppInstalled) {
            // Using https web URI targeted to package handles deep link routing smoothly without black screen
            Intent(Intent.ACTION_VIEW, webUri).apply {
                setPackage("com.instagram.android")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    /**
     * Safely launches Instagram native profile without freezing on blank black screens.
     */
    fun launchInstagram(context: Context, rawHandle: String) {
        val cleanHandle = rawHandle.trim().removePrefix("@").filter {
            it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' || it == '.'
        }.lowercase()

        val isValidHandle = cleanHandle.isNotBlank() &&
                cleanHandle != "user" &&
                cleanHandle != "spott.user" &&
                cleanHandle != "someone" &&
                cleanHandle != "null"

        if (!isValidHandle) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    return
                }
            } catch (_: Exception) {}
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {
                Toast.makeText(context, "Instagram profile is resolving...", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val webUri = Uri.parse("https://www.instagram.com/$cleanHandle/")
        val nativeUri = Uri.parse("instagram://user?username=$cleanHandle")

        val isAppInstalled = try {
            val pm = context.packageManager
            pm.getPackageInfo("com.instagram.android", PackageManager.GET_ACTIVITIES)
            true
        } catch (_: Exception) {
            false
        }

        if (isAppInstalled) {
            // 1. Preferred modern route: HTTPS deep-link targeted directly to Instagram app
            // This prevents blank black screens caused by deprecated internal URI parameters
            try {
                val appIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    setPackage("com.instagram.android")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(appIntent)
                return
            } catch (e: Exception) {
                android.util.Log.w("NotificationHelper", "Web link to app failed, trying native uri: ${e.message}")
            }

            // 2. Secondary fallback: Native scheme targeted to Instagram app
            try {
                val nativeIntent = Intent(Intent.ACTION_VIEW, nativeUri).apply {
                    setPackage("com.instagram.android")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(nativeIntent)
                return
            } catch (e2: Exception) {
                android.util.Log.w("NotificationHelper", "Native scheme to app failed: ${e2.message}")
            }
        }

        // 3. Fallback to Web Browser
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        } catch (e3: Exception) {
            Toast.makeText(context, "Could not open Instagram profile: @$cleanHandle", Toast.LENGTH_SHORT).show()
        }
    }
}
