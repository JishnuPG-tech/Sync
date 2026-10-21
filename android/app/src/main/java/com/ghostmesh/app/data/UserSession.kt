package com.ghostmesh.app.data

import android.content.Context
import android.content.SharedPreferences

object UserSession {
    private const val PREF_NAME = "sync_app_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_SYNC_USERNAME = "sync_username"
    private const val KEY_INSTAGRAM_USERNAME = "instagram_username"
    private const val KEY_BIO = "bio"
    private const val KEY_LOCATION = "location"
    private const val KEY_AVATAR = "avatar_uri_or_url"
    private const val KEY_IS_REGISTERED = "is_registered"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUser(
        context: Context,
        userId: String,
        displayName: String,
        syncUsername: String,
        instagramUsername: String,
        bio: String,
        location: String,
        avatar: String?
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_DISPLAY_NAME, displayName)
            putString(KEY_SYNC_USERNAME, syncUsername)
            putString(KEY_INSTAGRAM_USERNAME, instagramUsername)
            putString(KEY_BIO, bio)
            putString(KEY_LOCATION, location)
            putString(KEY_AVATAR, avatar ?: "")
            putBoolean(KEY_IS_REGISTERED, true)
            apply()
        }
    }

    fun getUserId(context: Context): String? =
        getPrefs(context).getString(KEY_USER_ID, null)

    fun getDisplayName(context: Context): String =
        getPrefs(context).getString(KEY_DISPLAY_NAME, "") ?: ""

    fun getSyncUsername(context: Context): String =
        getPrefs(context).getString(KEY_SYNC_USERNAME, "") ?: ""

    fun getInstagramUsername(context: Context): String =
        getPrefs(context).getString(KEY_INSTAGRAM_USERNAME, "") ?: ""

    fun getBio(context: Context): String =
        getPrefs(context).getString(KEY_BIO, "") ?: ""

    fun getLocation(context: Context): String =
        getPrefs(context).getString(KEY_LOCATION, "") ?: ""

    fun getAvatar(context: Context): String? =
        getPrefs(context).getString(KEY_AVATAR, null)?.takeIf { it.isNotEmpty() }

    fun isRegistered(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_IS_REGISTERED, false)

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
