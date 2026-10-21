package com.ghostmesh.app.data.network

import android.util.Base64
import com.ghostmesh.app.data.model.ConnectionRequestIncoming
import com.ghostmesh.app.data.model.MutualConnection
import com.ghostmesh.app.data.model.NearbyPeerUiModel
import com.ghostmesh.app.data.model.UserProfile
import com.ghostmesh.app.proximity.ConfidenceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class InstagramVerificationResult(
    val exists: bool_val = false,
    val username: String = "",
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val error: String? = null
) {
    // Helper typealias alias for boolean
}
typealias bool_val = Boolean

object SyncApiConfig {
    // Production URL on Render — set this after deploying to Render
    // e.g. "https://sync-api.onrender.com"
    const val RENDER_URL: String = ""   // ← Paste your Render URL here after deploy

    // Local dev fallbacks
    // 127.0.0.1:8000 works with `adb reverse tcp:8000 tcp:8000`
    // 10.0.2.2:8000 works on the standard Android Emulator
    val candidateBaseUrls: List<String> = buildList {
        if (RENDER_URL.isNotBlank()) add(RENDER_URL)
        add("http://127.0.0.1:8000")
        add("http://10.0.2.2:8000")
        add("http://10.41.89.246:8000")
    }
    var activeBaseUrl: String = if (RENDER_URL.isNotBlank()) RENDER_URL else "http://127.0.0.1:8000"
}

class SyncApiClient {

    private suspend fun <T> executeRequest(
        path: String,
        method: String = "GET",
        jsonBody: String? = null,
        timeoutMs: Int = 5000,
        parser: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        // Try active URL first, then fallback candidates if connection refused
        val candidateUrls = listOf(SyncApiConfig.activeBaseUrl) + 
            SyncApiConfig.candidateBaseUrls.filter { it != SyncApiConfig.activeBaseUrl }

        var lastException: Exception? = null

        for (base in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                val fullUrl = "$base$path"
                val url = URL(fullUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                    setRequestProperty("Accept", "application/json")
                    if (jsonBody != null) {
                        setRequestProperty("Content-Type", "application/json")
                        doOutput = true
                    }
                }

                if (jsonBody != null) {
                    OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                        writer.write(jsonBody)
                        writer.flush()
                    }
                }

                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val responseText = BufferedReader(InputStreamReader(stream ?: conn.inputStream, "UTF-8")).use { reader ->
                    reader.readText()
                }

                if (responseCode in 200..299) {
                    SyncApiConfig.activeBaseUrl = base
                    return@withContext Result.success(parser(responseText))
                } else {
                    return@withContext Result.failure(Exception("HTTP $responseCode: $responseText"))
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                conn?.disconnect()
            }
        }

        Result.failure(lastException ?: Exception("Network request failed to all candidate endpoints"))
    }

    suspend fun verifyInstagram(username: String): Result<InstagramVerificationResult> {
        val clean = username.trim().removePrefix("@").lowercase()
        return executeRequest("/api/v1/users/verify-instagram/$clean") { jsonStr ->
            val obj = JSONObject(jsonStr)
            InstagramVerificationResult(
                exists = obj.optBoolean("exists", false),
                username = obj.optString("username", clean),
                fullName = obj.optString("full_name").takeIf { it.isNotEmpty() },
                avatarUrl = obj.optString("avatar_url").takeIf { it.isNotEmpty() },
                bio = obj.optString("description").takeIf { it.isNotEmpty() },
                error = obj.optString("error").takeIf { it.isNotEmpty() }
            )
        }
    }

    suspend fun uploadProfilePhoto(imageBytes: ByteArray, filename: String = "profile.jpg"): Result<String> {
        val base64Str = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val payload = JSONObject().apply {
            put("image_base64", base64Str)
            put("filename", filename)
        }

        return executeRequest(
            path = "/api/v1/users/upload-photo",
            method = "POST",
            jsonBody = payload.toString()
        ) { jsonStr ->
            val obj = JSONObject(jsonStr)
            obj.optString("photo_url", "")
        }
    }

    suspend fun registerUser(
        displayName: String,
        syncUsername: String,
        instagramUsername: String,
        bio: String,
        location: String,
        avatarUrl: String?,
        interests: List<String> = listOf("Tech", "Design", "Music")
    ): Result<UserProfile> {
        val payload = JSONObject().apply {
            put("display_name", displayName.trim())
            put("sync_username", syncUsername.trim().lowercase())
            put("instagram_username", instagramUsername.trim().removePrefix("@").lowercase())
            put("bio", bio.trim())
            put("location", location.trim())
            if (avatarUrl != null) put("avatar_url", avatarUrl)
            val arr = JSONArray()
            interests.forEach { arr.put(it) }
            put("interests", arr)
        }

        return executeRequest(
            path = "/api/v1/users/register",
            method = "POST",
            jsonBody = payload.toString()
        ) { jsonStr ->
            val obj = JSONObject(jsonStr)
            UserProfile(
                id = obj.getString("id"),
                display_name = obj.getString("display_name"),
                instagram_username = obj.getString("instagram_username"),
                introduction = obj.optString("bio", ""),
                photo_uri = obj.optString("avatar_url").takeIf { it.isNotEmpty() },
                is_discovery_active = obj.optBoolean("is_discovery_active", true),
                is_instagram_verified = true
            )
        }
    }

    suspend fun pingLocation(userId: String, latitude: Double, longitude: Double): Result<Boolean> {
        val payload = JSONObject().apply {
            put("user_id", userId)
            put("latitude", latitude)
            put("longitude", longitude)
        }
        return executeRequest(
            path = "/api/v1/radar/ping",
            method = "POST",
            jsonBody = payload.toString()
        ) { true }
    }

    suspend fun getNearbyPeers(userId: String): Result<List<NearbyPeerUiModel>> {
        return executeRequest("/api/v1/radar/nearby/$userId") { jsonStr ->
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<NearbyPeerUiModel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val interestsArr = obj.optJSONArray("interests")
                val interestsList = mutableListOf<String>()
                if (interestsArr != null) {
                    for (j in 0 until interestsArr.length()) {
                        interestsList.add(interestsArr.getString(j))
                    }
                }
                val distMeters = obj.optDouble("approx_distance_meters", 5.0)
                val tier = when {
                    distMeters <= 5.0 -> ConfidenceCategory.VERY_CLOSE
                    distMeters <= 15.0 -> ConfidenceCategory.NEARBY
                    else -> ConfidenceCategory.EXTENDED
                }
                list.add(
                    NearbyPeerUiModel(
                        encounterId = obj.getString("user_id"),
                        displayName = obj.getString("display_name"),
                        introduction = obj.optString("bio").takeIf { it.isNotEmpty() },
                        interests = if (interestsList.isNotEmpty()) interestsList else listOf("Nearby"),
                        confidenceTier = tier,
                        isCurrentlyInRange = true,
                        distanceDisplay = "${distMeters.toInt()}m",
                        avatarAsset = obj.optString("avatar_url").takeIf { it.isNotEmpty() } ?: "",
                        connectionState = obj.optString("connection_status", "NONE")
                    )
                )
            }
            list
        }
    }

    suspend fun sendConnectionRequest(senderId: String, receiverId: String): Result<String> {
        val payload = JSONObject().apply {
            put("sender_id", senderId)
            put("receiver_id", receiverId)
        }
        return executeRequest(
            path = "/api/v1/connections/request",
            method = "POST",
            jsonBody = payload.toString()
        ) { jsonStr ->
            val obj = JSONObject(jsonStr)
            obj.optString("request_id", "")
        }
    }

    suspend fun getPendingRequests(userId: String): Result<List<ConnectionRequestIncoming>> {
        return executeRequest("/api/v1/connections/pending/$userId") { jsonStr ->
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<ConnectionRequestIncoming>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val interestsArr = obj.optJSONArray("sender_interests")
                val interestsList = mutableListOf<String>()
                if (interestsArr != null) {
                    for (j in 0 until interestsArr.length()) {
                        interestsList.add(interestsArr.getString(j))
                    }
                }
                list.add(
                    ConnectionRequestIncoming(
                        id = obj.getString("id"),
                        sender_id = obj.getString("sender_id"),
                        sender_display_name = obj.getString("sender_display_name"),
                        sender_introduction = obj.optString("sender_bio").takeIf { it.isNotEmpty() },
                        sender_interests = if (interestsList.isNotEmpty()) interestsList else listOf("Tech"),
                        sender_avatar_asset = obj.optString("sender_avatar_url").takeIf { it.isNotEmpty() } ?: "",
                        status = obj.optString("status", "PENDING"),
                        created_at = "Just now"
                    )
                )
            }
            list
        }
    }

    suspend fun respondToRequest(requestId: String, action: String, userId: String): Result<Boolean> {
        val payload = JSONObject().apply {
            put("action", action)
        }
        return executeRequest(
            path = "/api/v1/connections/$requestId/respond?user_id=$userId",
            method = "POST",
            jsonBody = payload.toString()
        ) { true }
    }

    suspend fun getMutualConnections(userId: String): Result<List<MutualConnection>> {
        return executeRequest("/api/v1/connections/mutual/$userId") { jsonStr ->
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<MutualConnection>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val interestsArr = obj.optJSONArray("interests")
                val interestsList = mutableListOf<String>()
                if (interestsArr != null) {
                    for (j in 0 until interestsArr.length()) {
                        interestsList.add(interestsArr.getString(j))
                    }
                }
                list.add(
                    MutualConnection(
                        connection_id = obj.getString("connection_id"),
                        connected_user_id = obj.getString("connected_user_id"),
                        display_name = obj.getString("display_name"),
                        instagram_username = obj.getString("instagram_username"),
                        introduction = obj.optString("bio").takeIf { it.isNotEmpty() },
                        interests = if (interestsList.isNotEmpty()) interestsList else listOf("Connected"),
                        connected_at = "Connected"
                    )
                )
            }
            list
        }
    }

    suspend fun toggleDiscovery(userId: String, isActive: Boolean): Result<Boolean> {
        return executeRequest(
            path = "/api/v1/users/$userId/discovery?is_active=$isActive",
            method = "PATCH"
        ) { true }
    }
}
