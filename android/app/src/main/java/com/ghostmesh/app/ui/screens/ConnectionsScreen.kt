package com.ghostmesh.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.data.model.MutualConnection
import com.ghostmesh.app.ui.components.AuraConfirmDialog
import com.ghostmesh.app.ui.components.AuraEmptyState
import com.ghostmesh.app.ui.components.AuraInitialsAvatar
import com.ghostmesh.app.ui.components.AuraTopBar
import com.ghostmesh.app.ui.theme.AuraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    connections: List<MutualConnection>,
    onOpenChat: (MutualConnection) -> Unit = {},
    onRemoveConnection: (MutualConnection) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedConnection by remember { mutableStateOf<MutualConnection?>(null) }
    var connectionToRemove by remember { mutableStateOf<MutualConnection?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraTheme.colors.canvas)
    ) {
        AuraTopBar(
            title = "Connections",
            statusPillText = if (connections.isNotEmpty()) "${connections.size} matched" else null,
            statusPillSuccess = true
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            if (connections.isEmpty()) {
                item {
                    AuraEmptyState(
                        icon = Icons.Outlined.Group,
                        title = "No connections yet",
                        description = "When you and someone nearby mutually accept each other's request, their verified Instagram handle and direct chat will be revealed here."
                    )
                }
            } else {
                items(connections, key = { it.connection_id }) { connection ->
                    ConnectionCard(
                        connection = connection,
                        onClick = { selectedConnection = connection },
                        onChatClick = { onOpenChat(connection) }
                    )
                }
            }
        }
    }

    // S12 / S20: Connection Detail / Instagram Reveal Bottom Sheet
    selectedConnection?.let { conn ->
        ModalBottomSheet(
            onDismissRequest = { selectedConnection = null },
            sheetState = sheetState,
            containerColor = AuraTheme.colors.surfaceElevated,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuraInitialsAvatar(name = conn.display_name, size = 72)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${conn.display_name}, ${conn.age}",
                    style = AuraTheme.typography.headlineMedium,
                    color = AuraTheme.colors.ink,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Mutual verified badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AuraTheme.colors.accentSubtle)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = AuraTheme.colors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Mutual Connection Verified",
                        style = AuraTheme.typography.labelSmall,
                        color = AuraTheme.colors.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!conn.introduction.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = conn.introduction,
                        style = AuraTheme.typography.bodyMedium,
                        color = AuraTheme.colors.inkSecondary
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Unlocked Instagram Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surfaceSubtle),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "INSTAGRAM USERNAME (UNLOCKED)",
                            style = AuraTheme.typography.labelSmall,
                            color = AuraTheme.colors.inkMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "@${conn.instagram_username.removePrefix("@")}",
                            style = AuraTheme.typography.titleLarge,
                            color = AuraTheme.colors.ink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1-Tap In-App Chat Button
                Button(
                    onClick = {
                        val c = conn
                        selectedConnection = null
                        onOpenChat(c)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraTheme.colors.accent,
                        contentColor = if (AuraTheme.colors.isDark) AuraTheme.colors.canvas else Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat with ${conn.display_name.split(" ").first()}", style = AuraTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Instagram Launch Buttons
                OutlinedButton(
                    onClick = {
                        launchInstagramApp(context, conn.instagram_username)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AuraTheme.colors.border)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraTheme.colors.ink)
                ) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open in Instagram App", style = AuraTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Instagram Handle", "@${conn.instagram_username.removePrefix("@")}")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied @${conn.instagram_username} to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = AuraTheme.colors.inkSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Username", color = AuraTheme.colors.inkSecondary, style = AuraTheme.typography.labelSmall)
                    }

                    TextButton(
                        onClick = {
                            connectionToRemove = conn
                            selectedConnection = null
                        }
                    ) {
                        Text("Remove connection", color = AuraTheme.colors.danger, style = AuraTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Confirmation dialog before removing connection
    connectionToRemove?.let { conn ->
        AuraConfirmDialog(
            title = "Remove connection?",
            message = "Remove ${conn.display_name} from your connections? Their Instagram username will no longer be visible.",
            confirmText = "Remove",
            onConfirm = {
                onRemoveConnection(conn)
                connectionToRemove = null
            },
            onDismiss = { connectionToRemove = null },
            isDestructive = true
        )
    }
}

@Composable
private fun ConnectionCard(
    connection: MutualConnection,
    onClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
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
                AuraInitialsAvatar(name = connection.display_name, size = 48)
                Column {
                    Text(
                        text = "${connection.display_name}, ${connection.age}",
                        style = AuraTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AuraTheme.colors.ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "@${connection.instagram_username.removePrefix("@")}",
                        style = AuraTheme.typography.bodySmall,
                        color = AuraTheme.colors.accent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AuraTheme.colors.accentSubtle)
                        .clickable { onChatClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Chat,
                            contentDescription = "Chat",
                            tint = AuraTheme.colors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Chat",
                            style = AuraTheme.typography.labelSmall,
                            color = AuraTheme.colors.accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun launchInstagramApp(context: Context, username: String) {
    val clean = username.removePrefix("@")
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("instagram://user?username=$clean")).apply {
        `package` = "com.instagram.android"
    }
    try {
        context.startActivity(appIntent)
    } catch (e: Exception) {
        launchInstagramWeb(context, username)
    }
}

private fun launchInstagramWeb(context: Context, username: String) {
    val clean = username.removePrefix("@")
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$clean"))
    try {
        context.startActivity(webIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
    }
}
