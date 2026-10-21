package com.ghostmesh.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ghostmesh.app.data.model.ThemePreset
import com.ghostmesh.app.proximity.ConfidenceCategory
import com.ghostmesh.app.proximity.RadarPowerMode
import com.ghostmesh.app.ui.theme.AuraTheme

// Palette of elegant avatar gradients matching high-end modern mobile apps
val AvatarGradients = listOf(
    listOf(Color(0xFFE06B43), Color(0xFFD97757)),
    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
    listOf(Color(0xFF0D9488), Color(0xFF14B8A6)),
    listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
    listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)),
    listOf(Color(0xFF8B5CF6), Color(0xFFA855F7))
)

@Composable
fun PlexTopBar(
    title: String = "PLEX",
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    statusPillText: String? = null,
    statusPillSuccess: Boolean = true,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null
) {
    Surface(
        color = AuraTheme.colors.canvas,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (navigationIcon != null) {
                    navigationIcon()
                }
                Column {
                    Text(
                        text = title,
                        style = AuraTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                        color = AuraTheme.colors.ink,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = AuraTheme.typography.bodySmall,
                            color = AuraTheme.colors.inkSecondary
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (statusPillText != null) {
                    PlexDiscoveryStatusPill(
                        text = statusPillText,
                        isActive = statusPillSuccess
                    )
                }
                if (actions != null) {
                    actions()
                }
            }
        }
    }
}

@Composable
fun AuraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    statusPillText: String? = null,
    statusPillSuccess: Boolean = true,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null
) {
    PlexTopBar(
        title = title,
        modifier = modifier,
        statusPillText = statusPillText,
        statusPillSuccess = statusPillSuccess,
        navigationIcon = navigationIcon,
        actions = actions
    )
}

@Composable
fun PlexDiscoveryStatusPill(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val pillBg = if (isActive) AuraTheme.colors.surfaceSubtle else AuraTheme.colors.surfaceSubtle
    val dotColor = if (isActive) AuraTheme.colors.success else AuraTheme.colors.inkMuted
    val textColor = if (isActive) AuraTheme.colors.ink else AuraTheme.colors.inkSecondary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(pillBg)
            .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            style = AuraTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AuraDiscoveryStatusPill(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    PlexDiscoveryStatusPill(text = text, isActive = isActive, modifier = modifier)
}

@Composable
fun AuraInitialsAvatar(
    name: String,
    size: Int = 54,
    colorIndex: Int = 0,
    isOnline: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val initials = name.trim().split("\\s+".toRegex())
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "P" }

    val gradient = AvatarGradients[colorIndex.coerceIn(0, AvatarGradients.size - 1)]

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(size.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = AuraTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.38).sp
            )
        }

        // Online or Active in-range badge dot
        if (isOnline != null) {
            Box(
                modifier = Modifier
                    .size((size * 0.28).coerceAtLeast(10.0).dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (isOnline) AuraTheme.colors.success else AuraTheme.colors.inkMuted)
                    .border(2.dp, AuraTheme.colors.surface, CircleShape)
            )
        }
    }
}

@Composable
fun PlexInterestChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AuraTheme.colors.surfaceSubtle)
            .border(0.5.dp, AuraTheme.colors.border, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = AuraTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = AuraTheme.colors.inkSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PlexFilterTabs(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    categories: List<String> = listOf("All", "In Radar", "Out of Radar"),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = selectedCategory == category
            val bg = if (isSelected) AuraTheme.colors.accent else AuraTheme.colors.surfaceSubtle
            val fg = if (isSelected) {
                if (AuraTheme.colors.isDark) AuraTheme.colors.canvas else Color.White
            } else {
                AuraTheme.colors.inkSecondary
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    style = AuraTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = fg
                )
            }
        }
    }
}

/**
 * High-End Redesigned AuraPersonCard with persistent radar presence:
 * - Real Name, Photo/Avatar, Description/Bio.
 * - Instagram handle is strictly hidden.
 * - Out of radar badge if they moved away (profile stays in feed!).
 * - Two explicit action buttons: [Skip / Remove] and [Connect / Send Request]!
 */
@Composable
fun AuraPersonCard(
    displayName: String,
    age: Int = 21,
    introduction: String?,
    interests: List<String> = emptyList(),
    confidence: ConfidenceCategory,
    isCurrentlyInRange: Boolean = true,
    discoveredTimeAgo: String = "Just now",
    colorIndex: Int = 0,
    hasRequested: Boolean = false,
    onConnectClick: () -> Unit,
    onSkipClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Avatar, Name, Range Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AuraInitialsAvatar(
                        name = displayName,
                        size = 52,
                        colorIndex = colorIndex,
                        isOnline = isCurrentlyInRange
                    )
                    Column {
                        Text(
                            text = "$displayName, $age",
                            style = AuraTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            color = AuraTheme.colors.ink,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = AuraTheme.colors.inkMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Instagram hidden",
                                style = AuraTheme.typography.labelSmall,
                                color = AuraTheme.colors.inkMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Range Status badge
                if (isCurrentlyInRange) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AuraTheme.colors.accentSubtle)
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = confidence.label,
                            style = AuraTheme.typography.labelSmall,
                            color = AuraTheme.colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AuraTheme.colors.surfaceSubtle)
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = AuraTheme.colors.inkMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Saved in list",
                                style = AuraTheme.typography.labelSmall,
                                color = AuraTheme.colors.inkMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Description / Bio
            if (!introduction.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = introduction,
                    style = AuraTheme.typography.bodyMedium,
                    color = AuraTheme.colors.inkSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            // Interest Chips
            if (interests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    interests.forEach { interest ->
                        PlexInterestChip(text = interest)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row: [Skip / Remove] and [Connect / Send Request]
            if (hasRequested) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraTheme.colors.surfaceSubtle),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = AuraTheme.colors.success,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Request sent ✓ (Waiting for review)",
                        style = AuraTheme.typography.labelMedium,
                        color = AuraTheme.colors.inkSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSkipClick != null) {
                        OutlinedButton(
                            onClick = onSkipClick,
                            modifier = Modifier
                                .weight(0.38f)
                                .height(46.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraTheme.colors.inkSecondary)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Skip",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Skip",
                                style = AuraTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Button(
                        onClick = onConnectClick,
                        modifier = Modifier
                            .weight(if (onSkipClick != null) 0.62f else 1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuraTheme.colors.accent,
                            contentColor = if (AuraTheme.colors.isDark) AuraTheme.colors.canvas else Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Connect",
                            style = AuraTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Full Person Preview Dialog (S14 / S18)
@Composable
fun PlexPersonPreviewDialog(
    displayName: String,
    age: Int,
    introduction: String?,
    interests: List<String>,
    confidence: ConfidenceCategory,
    isCurrentlyInRange: Boolean = true,
    colorIndex: Int = 0,
    hasRequested: Boolean,
    onConnectClick: () -> Unit,
    onSkipClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrentlyInRange) AuraTheme.colors.accentSubtle else AuraTheme.colors.surfaceSubtle)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isCurrentlyInRange) confidence.label else "Saved in list",
                            style = AuraTheme.typography.labelSmall,
                            color = if (isCurrentlyInRange) AuraTheme.colors.accent else AuraTheme.colors.inkMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = AuraTheme.colors.inkSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                AuraInitialsAvatar(
                    name = displayName,
                    size = 80,
                    colorIndex = colorIndex,
                    isOnline = isCurrentlyInRange
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "$displayName, $age",
                    style = AuraTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                    color = AuraTheme.colors.ink,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Privacy Invariant banner
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraTheme.colors.surfaceSubtle)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = AuraTheme.colors.inkMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Instagram Hidden until mutual match",
                        style = AuraTheme.typography.labelSmall,
                        color = AuraTheme.colors.inkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!introduction.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = introduction,
                        style = AuraTheme.typography.bodyMedium,
                        color = AuraTheme.colors.inkSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }

                if (interests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        interests.forEach { interest ->
                            PlexInterestChip(text = interest)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (hasRequested) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AuraTheme.colors.surfaceSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Connection Request Sent ✓",
                            style = AuraTheme.typography.labelLarge,
                            color = AuraTheme.colors.inkSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onSkipClick != null) {
                            OutlinedButton(
                                onClick = {
                                    onSkipClick()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(0.38f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
                            ) {
                                Text("Skip", color = AuraTheme.colors.inkSecondary)
                            }
                        }

                        Button(
                            onClick = {
                                onConnectClick()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(if (onSkipClick != null) 0.62f else 1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuraTheme.colors.accent,
                                contentColor = if (AuraTheme.colors.isDark) AuraTheme.colors.canvas else Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Send Request",
                                style = AuraTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// S17: "It's a Match!" celebration modal
@Composable
fun PlexMatchCelebrationDialog(
    matchedName: String,
    onViewProfile: () -> Unit,
    onSendMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(AuraTheme.colors.accentSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Favorite,
                        contentDescription = null,
                        tint = AuraTheme.colors.accent,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "It's a Match!",
                    style = AuraTheme.typography.headlineMedium,
                    color = AuraTheme.colors.ink,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You and $matchedName liked each other! Instagram handle is now unlocked.",
                    style = AuraTheme.typography.bodyMedium,
                    color = AuraTheme.colors.inkSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                AuraPrimaryButton(
                    text = "Send a Message",
                    onClick = onSendMessage,
                    leadingIcon = Icons.AutoMirrored.Outlined.Send
                )

                Spacer(modifier = Modifier.height(10.dp))

                AuraSecondaryButton(
                    text = "View Profile & Instagram",
                    onClick = onViewProfile
                )
            }
        }
    }
}

// Visual Theme Selector Card (S26)
@Composable
fun PlexThemeSelectorCard(
    preset: ThemePreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (primaryColor, surfaceColor, canvasColor) = when (preset) {
        ThemePreset.CLAUDE_WARM -> Triple(Color(0xFFD97757), Color(0xFFFFFFFF), Color(0xFFFAF8F5))
        ThemePreset.MIDNIGHT_INDIGO -> Triple(Color(0xFF6366F1), Color(0xFF151C2C), Color(0xFF0B0F19))
        ThemePreset.CYBER_CYAN -> Triple(Color(0xFF06B6D4), Color(0xFF111827), Color(0xFF0A0F1D))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AuraTheme.colors.surfaceElevated else AuraTheme.colors.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AuraTheme.colors.accent else AuraTheme.colors.border
        )
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
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Color dots preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(primaryColor))
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(canvasColor).border(1.dp, Color.Gray, CircleShape))
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(surfaceColor).border(1.dp, Color.Gray, CircleShape))
                }

                Column {
                    Text(
                        text = preset.displayName,
                        style = AuraTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = AuraTheme.colors.ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = preset.subtitle,
                        style = AuraTheme.typography.bodySmall,
                        color = AuraTheme.colors.inkSecondary
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(AuraTheme.colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected",
                        tint = if (AuraTheme.colors.isDark) AuraTheme.colors.canvas else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AuraPowerModeSelector(
    selectedMode: RadarPowerMode,
    onModeSelected: (RadarPowerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AuraTheme.colors.surfaceSubtle)
                .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf(
                RadarPowerMode.SMART_DISCOVERY to "Smart",
                RadarPowerMode.EXTENDED_RANGE to "Extended",
                RadarPowerMode.OFF to "Off"
            )

            modes.forEach { (mode, label) ->
                val isSelected = selectedMode == mode
                val bg = if (isSelected) AuraTheme.colors.surface else Color.Transparent
                val fg = if (isSelected) AuraTheme.colors.ink else AuraTheme.colors.inkSecondary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .then(
                            if (isSelected) Modifier.border(1.dp, AuraTheme.colors.border, RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .clickable { onModeSelected(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = AuraTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = fg
                    )
                }
            }
        }

        if (selectedMode == RadarPowerMode.EXTENDED_RANGE) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = AuraTheme.colors.warning,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Extended range increases transmit power & battery use",
                    style = AuraTheme.typography.bodySmall,
                    color = AuraTheme.colors.warning
                )
            }
        }
    }
}

@Composable
fun AuraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    isDestructive: Boolean = false
) {
    val bgColor = if (isDestructive) AuraTheme.colors.danger else AuraTheme.colors.accent
    val contentColor = if (AuraTheme.colors.isDark) AuraTheme.colors.canvas else Color.White

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = contentColor,
            disabledContainerColor = AuraTheme.colors.surfaceSubtle,
            disabledContentColor = AuraTheme.colors.inkMuted
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = AuraTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AuraSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AuraTheme.colors.surface,
            contentColor = AuraTheme.colors.ink,
            disabledContainerColor = AuraTheme.colors.surfaceSubtle,
            disabledContentColor = AuraTheme.colors.inkMuted
        )
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = AuraTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AuraEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(AuraTheme.colors.surfaceSubtle)
                .border(1.dp, AuraTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AuraTheme.colors.inkMuted,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            style = AuraTheme.typography.headlineMedium.copy(fontSize = 20.sp),
            color = AuraTheme.colors.ink,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = AuraTheme.typography.bodyMedium,
            color = AuraTheme.colors.inkSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            AuraSecondaryButton(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Composable
fun AuraConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = AuraTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.ink
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = AuraTheme.typography.bodyMedium,
                    color = AuraTheme.colors.inkSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraTheme.colors.ink)
                    ) {
                        Text("Cancel", style = AuraTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDestructive) AuraTheme.colors.danger else AuraTheme.colors.accent,
                            contentColor = if (AuraTheme.colors.isDark) AuraTheme.colors.canvas else Color.White
                        )
                    ) {
                        Text(confirmText, style = AuraTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
