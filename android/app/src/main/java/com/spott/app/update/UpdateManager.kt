package com.spott.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val GITHUB_REPO = "JishnuPG-tech/Sync"
    private const val GITHUB_API_URL = "https://api.github.com/repos/JishnuPG-tech/Sync/releases/latest"

    /**
     * Checks GitHub Releases API for a newer version than currentVersionName
     */
    suspend fun checkForUpdate(currentVersionName: String): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Sync-Android-App")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GitHub API returned status: " + responseCode)
                return@withContext Result.failure(Exception("GitHub API error: " + responseCode))
            }

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val releaseObj = JSONObject(jsonString)

            val tagName = releaseObj.optString("tag_name", "")
            val title = releaseObj.optString("name", tagName)
            val body = releaseObj.optString("body", "Bug fixes and performance improvements.")

            // Look for APK asset in release
            val assetsArr = releaseObj.optJSONArray("assets")
            var apkDownloadUrl = ""
            var apkSize = 0L

            if (assetsArr != null) {
                for (i in 0 until assetsArr.length()) {
                    val asset = assetsArr.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkDownloadUrl.isBlank()) {
                Log.i(TAG, "No APK asset found in release: " + tagName)
                return@withContext Result.success(null)
            }

            val cleanRemoteVersion = tagName.removePrefix("v").substringBefore("-")
            val cleanLocalVersion = currentVersionName.removePrefix("v").substringBefore("-")

            val isNewer = isVersionNewer(cleanRemoteVersion, cleanLocalVersion)

            if (isNewer) {
                val info = AppUpdateInfo(
                    tagName = tagName,
                    versionName = cleanRemoteVersion,
                    releaseTitle = title,
                    releaseNotes = body,
                    downloadUrl = apkDownloadUrl,
                    sizeBytes = apkSize
                )
                Log.i(TAG, "Update available: " + tagName + " (" + apkDownloadUrl + ")")
                Result.success(info)
            } else {
                Log.i(TAG, "App is up to date (Local: " + currentVersionName + ", Remote: " + tagName + ")")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates: " + e.message, e)
            Result.failure(e)
        }
    }

    private fun isVersionNewer(remote: String, local: String): Boolean {
        try {
            val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
            val lParts = local.split(".").mapNotNull { it.toIntOrNull() }
            if (rParts.isEmpty() || lParts.isEmpty()) return false
            val maxLen = maxOf(rParts.size, lParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val l = lParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        } catch (_: Exception) {}
        return false
    }

    /**
     * Downloads APK from GitHub Releases with live progress reporting (0.0 to 1.0)
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var input: InputStream? = null
        var output: FileOutputStream? = null
        var connection: HttpURLConnection? = null
        try {
            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()
            val outputFile = File(updateDir, "Sync-update.apk")
            if (outputFile.exists()) outputFile.delete()

            var targetUrl = downloadUrl
            var redirects = 0
            while (redirects < 5) {
                val url = URL(targetUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Sync-Android-App")
                }
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_TEMP || 
                    code == HttpURLConnection.HTTP_MOVED_PERM || 
                    code == 307 || code == 308) {
                    val newLoc = connection.getHeaderField("Location")
                    if (!newLoc.isNullOrBlank()) {
                        targetUrl = newLoc
                        redirects++
                        continue
                    }
                }
                break
            }

            val fileLength = connection?.contentLengthLong ?: -1L
            input = connection?.inputStream
            output = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int

            while (input!!.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (fileLength > 0) {
                    val progress = totalBytesRead.toFloat() / fileLength.toFloat()
                    onProgress(progress.coerceIn(0f, 1f))
                }
            }
            output.flush()
            onProgress(1.0f)
            Log.i(TAG, "Downloaded update successfully: " + outputFile.length() + " bytes")
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: " + e.message, e)
            Result.failure(e)
        } finally {
            try { output?.close() } catch (_: Exception) {}
            try { input?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }

    /**
     * Prompts the system Package Installer to install the downloaded APK
     */
    fun launchInstall(context: Context, apkFile: File) {
        try {
            // Also ensure copy exists in public Downloads for fallback
            try {
                val pubDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (pubDir.exists()) {
                    val pubFile = File(pubDir, "Spott-debug.apk")
                    apkFile.copyTo(pubFile, overwrite = true)
                }
            } catch (_: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    android.widget.Toast.makeText(
                        context,
                        "Please enable 'Allow from this source' to install the update",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:" + context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    return
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            val resolveList = context.packageManager.queryIntentActivities(installIntent, 0)
            for (resolveInfo in resolveList) {
                val pkgName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkgName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            android.widget.Toast.makeText(context, "Opening installer...", android.widget.Toast.LENGTH_SHORT).show()
            context.startActivity(installIntent)
            Log.i(TAG, "Launched Package Installer with URI: " + uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer: " + e.message, e)
            android.widget.Toast.makeText(context, "Could not open installer: " + e.localizedMessage, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
