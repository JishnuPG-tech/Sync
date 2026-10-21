package com.ghostmesh.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.data.model.ConnectionRequestIncoming
import com.ghostmesh.app.data.model.ConnectionRequestOutgoing
import com.ghostmesh.app.ui.components.AuraConfirmDialog
import com.ghostmesh.app.ui.components.AuraEmptyState
import com.ghostmesh.app.ui.components.AuraInitialsAvatar
import com.ghostmesh.app.ui.components.PlexInterestChip
import com.ghostmesh.app.ui.components.PlexMatchCelebrationDialog
import com.ghostmesh.app.ui.components.PlexTopBar
import com.ghostmesh.app.ui.theme.AuraTheme

enum class RequestTab { RECEIVED, SENT }

@Composable
fun RequestsScreen(
    incomingRequests: List<ConnectionRequestIncoming>,
    outgoingRequests: List<ConnectionRequestOutgoing>,
    onAcceptRequest: (ConnectionRequestIncoming) -> Unit,
    onDeclineRequest: (ConnectionRequestIncoming) -> Unit,
    onNavigateToChat: (senderName: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(RequestTab.RECEIVED) }
    var requestToDecline by remember { mutableStateOf<ConnectionRequestIncoming?>(null) }
    var matchCelebrationPeer by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraTheme.colors.canvas)
    ) {
        PlexTopBar(
            title = "Requests",
            statusPillText = if (incomingRequests.isNotEmpty()) "${incomingRequests.size} pending" else null,
            statusPillSuccess = true
        )

        // Segmented Control (Received / Sent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AuraTheme.colors.surfaceSubtle)
                .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                RequestTab.RECEIVED to "Received (${incomingRequests.size})",
                RequestTab.SENT to "Sent (${outgoingRequests.size})"
            )

            tabs.forEach { (tab, label) ->
                val isSelected = selectedTab == tab
                val bg = if (isSelected) AuraTheme.colors.surface else Color.Transparent
                val fg = if (isSelected) AuraTheme.colors.ink else AuraTheme.colors.inkSecondary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(bg)
                        .then(
                            if (isSelected) Modifier.border(1.dp, AuraTheme.colors.border, RoundedCornerShape(9.dp))
                            else Modifier
                        )
                        .clickable { selectedTab = tab },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = AuraTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = fg
                    )
                }
            }
        }

        // List Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
        ) {
            when (selectedTab) {
                RequestTab.RECEIVED -> {
                    if (incomingRequests.isEmpty()) {
                        item {
                            AuraEmptyState(
                                icon = Icons.Outlined.Handshake,
                                title = "No requests yet",
                                description = "When nearby people want to connect with you, their requests will appear here."
                            )
                        }
                    } else {
                        items(incomingRequests, key = { it.id }) { request ->
                            IncomingRequestCard(
                                request = request,
                                onAccept = {
                                    onAcceptRequest(request)
                                    matchCelebrationPeer = request.sender_display_name
                                },
                                onDecline = { requestToDecline = request }
                            )
                        }
                    }
                }
                RequestTab.SENT -> {
                    if (outgoingRequests.isEmpty()) {
                        item {
                            AuraEmptyState(
                                icon = Icons.Outlined.Schedule,
                                title = "No sent requests",
                                description = "Requests you send to nearby people will be tracked here until they accept."
                            )
                        }
                    } else {
                        items(outgoingRequests, key = { it.id }) { request ->
                            OutgoingRequestCard(request = request)
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before declining
    requestToDecline?.let { req ->
        AuraConfirmDialog(
            title = "Decline request?",
            message = "Decline connection request from ${req.sender_display_name}? Their Instagram username will remain hidden.",
            confirmText = "Decline",
            onConfirm = {
                onDeclineRequest(req)
                requestToDecline = null
            },
            onDismiss = { requestToDecline = null },
            isDestructive = true
        )
    }

    // Celebration modal when match happens (S17)
    matchCelebrationPeer?.let { peerName ->
        PlexMatchCelebrationDialog(
            matchedName = peerName,
            onViewProfile = { matchCelebrationPeer = null },
            onSendMessage = {
                val name = peerName
                matchCelebrationPeer = null
                onNavigateToChat(name)
            },
            onDismiss = { matchCelebrationPeer = null }
        )
    }
}

@Composable
private fun IncomingRequestCard(
    request: ConnectionRequestIncoming,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AuraInitialsAvatar(name = request.sender_display_name, size = 48)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${request.sender_display_name}, ${request.sender_age}",
                        style = AuraTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AuraTheme.colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = AuraTheme.colors.inkMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Handle unlocks upon mutual accept",
                            style = AuraTheme.typography.bodySmall,
                            color = AuraTheme.colors.inkMuted
                        )
                    }
                }
            }

            if (!request.sender_introduction.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = request.sender_introduction,
                    style = AuraTheme.typography.bodyMedium,
                    color = AuraTheme.colors.inkSecondary
                )
            }

            if (request.sender_interests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    request.sender_interests.forEach { interest ->
                        PlexInterestChip(text = interest)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AuraTheme.colors.border)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraTheme.colors.inkSecondary)
                ) {
                    Text("Decline", style = AuraTheme.typography.labelMedium)
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraTheme.colors.accent,
                        contentColor = if (AuraTheme.colors.isDark) AuraTheme.colors.canvas else Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("Accept", style = AuraTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun OutgoingRequestCard(
    request: ConnectionRequestOutgoing
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                AuraInitialsAvatar(name = request.receiver_display_name, size = 44)
                Column {
                    Text(
                        text = request.receiver_display_name,
                        style = AuraTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AuraTheme.colors.ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Awaiting response",
                        style = AuraTheme.typography.bodySmall,
                        color = AuraTheme.colors.inkMuted
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AuraTheme.colors.surfaceSubtle)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = request.status.lowercase().replaceFirstChar { it.uppercase() },
                    style = AuraTheme.typography.labelSmall,
                    color = AuraTheme.colors.inkSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
