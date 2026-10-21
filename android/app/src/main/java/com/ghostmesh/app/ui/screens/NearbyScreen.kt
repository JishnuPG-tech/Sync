package com.ghostmesh.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.data.model.NearbyPeerUiModel
import com.ghostmesh.app.proximity.ConfidenceCategory
import com.ghostmesh.app.proximity.RadarPowerMode
import com.ghostmesh.app.ui.components.AuraEmptyState
import com.ghostmesh.app.ui.components.AuraPersonCard
import com.ghostmesh.app.ui.components.PlexFilterTabs
import com.ghostmesh.app.ui.components.PlexPersonPreviewDialog
import com.ghostmesh.app.ui.components.PlexTopBar
import com.ghostmesh.app.ui.theme.AuraTheme

@Composable
fun NearbyScreen(
    nearbyPeers: List<NearbyPeerUiModel>,
    requestedPeerIds: Set<String>,
    powerMode: RadarPowerMode,
    onSendConnect: (NearbyPeerUiModel) -> Unit,
    onSkipPeer: (NearbyPeerUiModel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var previewPeer by remember { mutableStateOf<NearbyPeerUiModel?>(null) }

    val activeCount = nearbyPeers.count { it.isCurrentlyInRange }
    val outCount = nearbyPeers.count { !it.isCurrentlyInRange }

    val categories = listOf(
        "All (${nearbyPeers.size})",
        "In Radar ($activeCount)",
        "Out of Radar ($outCount)"
    )

    val filteredPeers = nearbyPeers.filter { peer ->
        val matchesSearch = searchQuery.isBlank() ||
                peer.displayName.contains(searchQuery, ignoreCase = true) ||
                (peer.introduction?.contains(searchQuery, ignoreCase = true) == true) ||
                peer.interests.any { it.contains(searchQuery, ignoreCase = true) }

        val matchesCategory = when {
            selectedCategory.startsWith("In Radar") -> peer.isCurrentlyInRange
            selectedCategory.startsWith("Out of Radar") -> !peer.isCurrentlyInRange
            else -> true
        }

        matchesSearch && matchesCategory
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AuraTheme.colors.canvas
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlexTopBar(
                title = "Discovered",
                subtitle = "${nearbyPeers.size} people saved in your list",
                statusPillText = when (powerMode) {
                    RadarPowerMode.SMART_DISCOVERY -> "Smart on"
                    RadarPowerMode.EXTENDED_RANGE -> "Extended"
                    RadarPowerMode.OFF -> "Radar paused"
                },
                statusPillSuccess = powerMode != RadarPowerMode.OFF
            )

            // Search and Category filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by name or bio description...",
                            style = AuraTheme.typography.bodyMedium,
                            color = AuraTheme.colors.inkMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = AuraTheme.colors.inkMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AuraTheme.colors.surface,
                        unfocusedContainerColor = AuraTheme.colors.surface,
                        focusedBorderColor = AuraTheme.colors.accent,
                        unfocusedBorderColor = AuraTheme.colors.border,
                        focusedTextColor = AuraTheme.colors.ink,
                        unfocusedTextColor = AuraTheme.colors.ink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                PlexFilterTabs(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    categories = categories
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredPeers.isEmpty()) {
                AuraEmptyState(
                    icon = Icons.Outlined.Explore,
                    title = "No People Discovered",
                    description = if (searchQuery.isNotBlank()) "No one matches \"$searchQuery\"."
                    else "No users in this category. People automatically appear here when detected nearby and remain saved."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredPeers, key = { it.encounterId }) { peer ->
                        val hasRequested = requestedPeerIds.contains(peer.encounterId)
                        AuraPersonCard(
                            displayName = peer.displayName,
                            age = peer.age,
                            introduction = peer.introduction,
                            interests = peer.interests,
                            confidence = peer.confidenceTier,
                            isCurrentlyInRange = peer.isCurrentlyInRange,
                            discoveredTimeAgo = peer.discoveredTimeAgo,
                            colorIndex = peer.avatarColorIndex,
                            hasRequested = hasRequested,
                            onConnectClick = { onSendConnect(peer) },
                            onSkipClick = { onSkipPeer(peer) },
                            onClick = { previewPeer = peer }
                        )
                    }
                }
            }
        }
    }

    // Modal Preview Dialog
    previewPeer?.let { peer ->
        val hasRequested = requestedPeerIds.contains(peer.encounterId)
        PlexPersonPreviewDialog(
            displayName = peer.displayName,
            age = peer.age,
            introduction = peer.introduction,
            interests = peer.interests,
            confidence = peer.confidenceTier,
            isCurrentlyInRange = peer.isCurrentlyInRange,
            colorIndex = peer.avatarColorIndex,
            hasRequested = hasRequested,
            onConnectClick = { onSendConnect(peer) },
            onSkipClick = { onSkipPeer(peer) },
            onDismiss = { previewPeer = null }
        )
    }
}
