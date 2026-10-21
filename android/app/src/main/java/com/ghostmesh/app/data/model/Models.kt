package com.ghostmesh.app.data.model

import com.ghostmesh.app.proximity.ConfidenceCategory

data class UserRegistrationRequest(
    val display_name: String,
    val instagram_username: String,
    val introduction: String = "",
    val photo_uri: String? = null,
    val interests: List<String> = emptyList()
)

data class UserProfile(
    val id: String,
    val display_name: String,
    val instagram_username: String,
    val introduction: String = "",
    val age: Int = 22,
    val gender: String = "Not specified",
    val photo_uri: String? = null,
    val interests: List<String> = emptyList(),
    val is_discovery_active: Boolean = true,
    val is_instagram_verified: Boolean = true
)

/**
 * Spec Section 9: NearbyPeerUiModel
 * CRITICAL PRIVACY: instagram_username is STRICTLY NEVER included in this model!
 *
 * Persistence rule: When peers leave radar range, isCurrentlyInRange becomes false,
 * but the profile STAYS in the discovered feed permanently until the user chooses to
 * either Send Request or Skip / Remove from list.
 */
data class NearbyPeerUiModel(
    val encounterId: String,
    val displayName: String,
    val age: Int = 21,
    val introduction: String? = null,
    val interests: List<String> = listOf("Tech", "Music", "Design"),
    val confidenceTier: ConfidenceCategory = ConfidenceCategory.NEARBY,
    val isCurrentlyInRange: Boolean = true,
    val discoveredTimeAgo: String = "Just now",
    val avatarColorIndex: Int = 0,
    val avatarAsset: String = "portrait_02.jpg",
    val distanceDisplay: String = "14m",
    val lastSeenElapsedMs: Long = 0L,
    val connectionState: String = "NONE" // "NONE", "PENDING_SENT", "CONNECTED"
)

// Legacy alias for proximity compatibility
typealias NearbyUser = NearbyPeerUiModel

data class ConnectionRequestIncoming(
    val id: String,
    val sender_id: String,
    val sender_display_name: String,
    val sender_age: Int = 22,
    val sender_introduction: String? = null,
    val sender_interests: List<String> = listOf("Design", "Photography"),
    val sender_avatar_index: Int = 0,
    val sender_avatar_asset: String = "portrait_02.jpg",
    val status: String = "PENDING",
    val created_at: String = "Just now"
)

data class ConnectionRequestOutgoing(
    val id: String,
    val receiver_id: String,
    val receiver_display_name: String,
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "DECLINED"
    val created_at: String = "Just now"
)

data class MutualConnection(
    val connection_id: String,
    val connected_user_id: String,
    val display_name: String,
    val age: Int = 22,
    val instagram_username: String, // UNLOCKED UPON MUTUAL ACCEPT ONLY!
    val introduction: String? = null,
    val interests: List<String> = listOf("Music", "Travel", "Coffee"),
    val avatar_index: Int = 0,
    val connected_at: String? = "Just now"
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean
)

enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ThemePreset(val displayName: String, val subtitle: String) {
    CLAUDE_WARM("Claude Warm Paper", "Organic warmth, soft paper & terracotta"),
    MIDNIGHT_INDIGO("Midnight Indigo", "Deep obsidian with electric violet"),
    CYBER_CYAN("Cyber Cyan", "Neon cyan radar grid & emerald abyss")
}
