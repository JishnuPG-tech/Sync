package com.ghostmesh.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.data.model.ConnectionRequestIncoming
import com.ghostmesh.app.data.model.NearbyPeerUiModel
import com.ghostmesh.app.proximity.ConfidenceCategory
import com.ghostmesh.app.ui.components.SyncAvatar
import com.ghostmesh.app.ui.components.SyncConnectionCelebrationDialog
import com.ghostmesh.app.ui.components.SyncTopBar
import com.ghostmesh.app.ui.theme.AuraTheme
import com.ghostmesh.app.ui.theme.DmMonoFontFamily
import com.ghostmesh.app.ui.theme.DmSansFontFamily
import com.ghostmesh.app.ui.theme.PlayfairFontFamily

enum class DiscoveredFilter {
    ALL,
    IN_RADAR,
    OUT_OF_RADAR,
    REQUESTS
}

/**
 * Discovered Screen
 * Tab 2 of Sync: shows search input, filter pills, incoming request hero card, and persistent encounters feed.
 */
@Composable
fun DiscoveredScreen(
    nearbyPeers: List<NearbyPeerUiModel>,
    incomingRequests: List<ConnectionRequestIncoming>,
    requestedPeerIds: Set<String>,
    onAcceptRequest: (ConnectionRequestIncoming) -> Unit,
    onDeclineRequest: (ConnectionRequestIncoming) -> Unit,
    onSendConnect: (NearbyPeerUiModel) -> Unit,
    onSkipPeer: (NearbyPeerUiModel) -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(DiscoveredFilter.ALL) }
    var celebrationPeer by remember { mutableStateOf<ConnectionRequestIncoming?>(null) }

    val inRadarCount = nearbyPeers.count { it.isCurrentlyInRange }
    val outOfRadarCount = nearbyPeers.count { !it.isCurrentlyInRange }

    val filteredPeers = remember(searchQuery, activeFilter, nearbyPeers) {
        val base = when (activeFilter) {
            DiscoveredFilter.ALL -> nearbyPeers
            DiscoveredFilter.IN_RADAR -> nearbyPeers.filter { it.isCurrentlyInRange }
            DiscoveredFilter.OUT_OF_RADAR -> nearbyPeers.filter { !it.isCurrentlyInRange }
            DiscoveredFilter.REQUESTS -> emptyList()
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                (it.introduction?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraTheme.colors.canvas)
    ) {
        SyncTopBar(onAddClick = onRefresh)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. SEARCH INPUT BOX
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraTheme.colors.surface)
                        .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = AuraTheme.colors.inkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search people by name or bio...",
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 13.sp,
                                    color = AuraTheme.colors.inkMuted
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 13.sp,
                                    color = AuraTheme.colors.ink
                                ),
                                cursorBrush = SolidColor(AuraTheme.colors.accent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 2. FILTER PILLS ROW (Horizontally scrollable with zero text clipping)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        FilterPill(
                            label = "All (${nearbyPeers.size})",
                            isSelected = activeFilter == DiscoveredFilter.ALL,
                            onClick = { activeFilter = DiscoveredFilter.ALL }
                        )
                    }
                    item {
                        FilterPill(
                            label = "In Radar ($inRadarCount)",
                            isSelected = activeFilter == DiscoveredFilter.IN_RADAR,
                            onClick = { activeFilter = DiscoveredFilter.IN_RADAR }
                        )
                    }
                    item {
                        FilterPill(
                            label = "Out of Radar ($outOfRadarCount)",
                            isSelected = activeFilter == DiscoveredFilter.OUT_OF_RADAR,
                            onClick = { activeFilter = DiscoveredFilter.OUT_OF_RADAR }
                        )
                    }
                    item {
                        FilterPill(
                            label = "Requests (${incomingRequests.size})",
                            isSelected = activeFilter == DiscoveredFilter.REQUESTS,
                            onClick = { activeFilter = DiscoveredFilter.REQUESTS }
                        )
                    }
                }
            }

            // 3. INCOMING CONNECTION REQUEST HERO CARD
            if (incomingRequests.isNotEmpty() && (activeFilter == DiscoveredFilter.ALL || activeFilter == DiscoveredFilter.REQUESTS)) {
                items(incomingRequests, key = { it.id }) { req ->
                    IncomingRequestHeroCard(
                        request = req,
                        onAccept = {
                            celebrationPeer = req
                            onAcceptRequest(req)
                        },
                        onDecline = { onDeclineRequest(req) }
                    )
                }
            }

            // 4. DISCOVERED FEED CARDS
            if (activeFilter != DiscoveredFilter.REQUESTS) {
                if (filteredPeers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No encounters match this filter",
                                    fontFamily = PlayfairFontFamily,
                                    fontSize = 16.sp,
                                    color = AuraTheme.colors.ink
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try switching filters or scanning for nearby peers",
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 12.sp,
                                    color = AuraTheme.colors.inkSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(filteredPeers, key = { it.encounterId }) { peer ->
                        SyncPeerCard(
                            peer = peer,
                            isRequested = requestedPeerIds.contains(peer.encounterId) || peer.connectionState == "PENDING_SENT",
                            onConnect = { onSendConnect(peer) },
                            onSkip = { onSkipPeer(peer) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // CELEBRATION MODAL (Strictly NO heart icons!)
    celebrationPeer?.let { peer ->
        SyncConnectionCelebrationDialog(
            matchedName = peer.sender_display_name,
            instagramHandle = "${peer.sender_display_name.lowercase().replace(" ", "")}.dev",
            myAvatarAsset = "portrait_05.jpg",
            peerAvatarAsset = peer.sender_avatar_asset,
            onDismiss = { celebrationPeer = null }
        )
    }
}

/**
 * Filter Pill matching .filter-pill
 */
@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSelected) AuraTheme.colors.accent else AuraTheme.colors.surface)
            .border(
                width = 1.dp,
                color = if (isSelected) AuraTheme.colors.accentHover else AuraTheme.colors.border,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            softWrap = false,
            maxLines = 1,
            color = if (isSelected) Color.White else AuraTheme.colors.inkSecondary
        )
    }
}

/**
 * Incoming Request Hero Card matching .incoming-request-card
 */
@Composable
private fun IncomingRequestHeroCard(
    request: ConnectionRequestIncoming,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, AuraTheme.colors.accent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = AuraTheme.colors.accent,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "INCOMING CONNECTION REQUEST",
                    fontFamily = DmMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.08.sp,
                    color = AuraTheme.colors.accent
                )
            }

            // Peer info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SyncAvatar(
                    assetName = request.sender_avatar_asset,
                    size = 44.dp,
                    borderWidth = 1.dp,
                    borderColor = AuraTheme.colors.border,
                    isOnline = true
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${request.sender_display_name}, ${request.sender_age}",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                            color = AuraTheme.colors.ink
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(AuraTheme.colors.success.copy(alpha = 0.14f))
                                .border(1.dp, AuraTheme.colors.success.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Very Close • 3m",
                                fontFamily = DmMonoFontFamily,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                softWrap = false,
                                maxLines = 1,
                                color = AuraTheme.colors.success
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = AuraTheme.colors.inkSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Instagram hidden",
                            fontFamily = DmMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = AuraTheme.colors.inkSecondary
                        )
                    }

                    if (!request.sender_introduction.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = request.sender_introduction,
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            color = AuraTheme.colors.inkSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: Decline & Accept
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AuraTheme.colors.surface)
                        .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(8.dp))
                        .clickable { onDecline() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Decline",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = AuraTheme.colors.inkSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AuraTheme.colors.accent)
                        .clickable { onAccept() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Accept & Reveal",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
