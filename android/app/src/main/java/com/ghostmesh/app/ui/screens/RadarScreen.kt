package com.ghostmesh.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.data.model.NearbyPeerUiModel
import com.ghostmesh.app.proximity.ConfidenceCategory
import com.ghostmesh.app.proximity.RadarPowerMode
import com.ghostmesh.app.ui.components.SyncAvatar
import com.ghostmesh.app.ui.components.SyncTopBar
import com.ghostmesh.app.ui.theme.AuraTheme
import com.ghostmesh.app.ui.theme.DmMonoFontFamily
import com.ghostmesh.app.ui.theme.DmSansFontFamily
import com.ghostmesh.app.ui.theme.PlayfairFontFamily
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radar Screen
 * Features clean SyncTopBar, power mode controls, interactive concentric radar canvas with sweep line,
 * dynamic peer pins (rendered ONLY when devices are detected nearby), and the "People nearby" feed.
 */
@Composable
fun RadarScreen(
    currentUserName: String,
    currentUserAvatar: String? = null,
    powerMode: RadarPowerMode,
    onPowerModeChanged: (RadarPowerMode) -> Unit,
    nearbyPeers: List<NearbyPeerUiModel>,
    onSendConnect: (NearbyPeerUiModel) -> Unit,
    onSkipPeer: (NearbyPeerUiModel) -> Unit = {},
    requestedPeerIds: Set<String> = emptySet(),
    onRefreshPeers: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDiscoveryActive = powerMode != RadarPowerMode.OFF
    val activeInRadarPeers = nearbyPeers.filter { it.isCurrentlyInRange }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraTheme.colors.canvas)
    ) {
        // Clean Sync Top Bar: NO "Radar active" text, NO proximity beacon icon
        SyncTopBar(onAddClick = onRefreshPeers)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. POWER MODE SELECTOR (Off / Smart / Extended)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraTheme.colors.surfaceSubtle)
                        .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PowerModeChip(
                        title = "Off",
                        icon = Icons.Outlined.PowerSettingsNew,
                        isSelected = powerMode == RadarPowerMode.OFF,
                        onClick = { onPowerModeChanged(RadarPowerMode.OFF) },
                        modifier = Modifier.weight(1f)
                    )
                    PowerModeChip(
                        title = "Smart",
                        icon = Icons.Outlined.Sensors,
                        isSelected = powerMode == RadarPowerMode.SMART_DISCOVERY,
                        onClick = { onPowerModeChanged(RadarPowerMode.SMART_DISCOVERY) },
                        modifier = Modifier.weight(1.1f)
                    )
                    PowerModeChip(
                        title = "Extended (70m)",
                        icon = Icons.Outlined.WifiTethering,
                        isSelected = powerMode == RadarPowerMode.EXTENDED_RANGE,
                        onClick = { onPowerModeChanged(RadarPowerMode.EXTENDED_RANGE) },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }

            // 2. RADAR VIEWPORT CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Radar Circular Screen
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Animated Radar Canvas
                            RadarScanCanvas(
                                isScanning = isDiscoveryActive,
                                sweepColor = AuraTheme.colors.accent
                            )

                            // Center User Avatar (You)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(AuraTheme.colors.surface)
                                    .border(2.dp, AuraTheme.colors.accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                SyncAvatar(
                                    assetName = currentUserAvatar,
                                    size = 42.dp,
                                    borderWidth = 0.dp,
                                    borderColor = Color.Transparent
                                )
                            }

                            // Dynamic Peer Pins on Rings (Rendered ONLY when devices detected nearby!)
                            if (isDiscoveryActive && activeInRadarPeers.isNotEmpty()) {
                                val fixedAngles = listOf(45.0, 275.0, 140.0, 330.0, 200.0)
                                val fixedRadii = listOf(76.dp, 88.dp, 64.dp, 94.dp, 80.dp)

                                activeInRadarPeers.take(5).forEachIndexed { index, peer ->
                                    val angleDeg = fixedAngles[index % fixedAngles.size]
                                    val rad = Math.toRadians(angleDeg)
                                    val radius = fixedRadii[index % fixedRadii.size]
                                    val offsetX = (radius.value * cos(rad)).dp
                                    val offsetY = (radius.value * sin(rad)).dp

                                    Box(
                                        modifier = Modifier
                                            .offset(x = offsetX, y = offsetY)
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(AuraTheme.colors.surface)
                                            .border(1.5.dp, AuraTheme.colors.accent, CircleShape)
                                            .clickable { onSendConnect(peer) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SyncAvatar(
                                            assetName = peer.avatarAsset,
                                            size = 30.dp,
                                            borderWidth = 0.dp,
                                            borderColor = Color.Transparent
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Radar Summary
                        val summaryTitle = if (!isDiscoveryActive) {
                            "Radar paused"
                        } else if (activeInRadarPeers.isEmpty()) {
                            "Scanning nearby frequencies..."
                        } else {
                            "${activeInRadarPeers.size} ${if (activeInRadarPeers.size == 1) "person" else "people"} in radar"
                        }

                        Text(
                            text = summaryTitle,
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = AuraTheme.colors.ink,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        val summarySub = if (activeInRadarPeers.isEmpty()) {
                            "Profiles appear only when a device is detected nearby"
                        } else {
                            "Profiles persist in your feed even when out of range"
                        }

                        Text(
                            text = summarySub,
                            fontFamily = DmMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = AuraTheme.colors.inkMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 3. PEOPLE NEARBY SECTION TITLE ROW
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "People nearby",
                        fontFamily = PlayfairFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        letterSpacing = (-0.02).sp,
                        color = AuraTheme.colors.ink
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(AuraTheme.colors.surfaceSubtle)
                            .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${activeInRadarPeers.size} active",
                            fontFamily = DmMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = AuraTheme.colors.inkSecondary
                        )
                    }
                }
            }

            // 4. EMPTY SCAN CARD OR ACTIVE PEER CARDS
            if (activeInRadarPeers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(AuraTheme.colors.accentSubtle)
                                    .border(1.dp, AuraTheme.colors.accent.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(AuraTheme.colors.accent)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Looking for nearby Sync peers",
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.5.sp,
                                color = AuraTheme.colors.ink,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Profiles only appear when another Sync user enters your physical proximity. Bluetooth LE scanning active.",
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = AuraTheme.colors.inkSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = onRefreshPeers,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AuraTheme.colors.accent,
                                    contentColor = Color.White
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        text = "Scan & Refresh Radar",
                                        fontFamily = DmSansFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(activeInRadarPeers, key = { it.encounterId }) { peer ->
                    SyncPeerCard(
                        peer = peer,
                        isRequested = requestedPeerIds.contains(peer.encounterId) || peer.connectionState == "PENDING_SENT",
                        onConnect = { onSendConnect(peer) },
                        onSkip = { onSkipPeer(peer) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

/**
 * Concentric radar circles with sweeping beam
 */
@Composable
private fun RadarScanCanvas(
    isScanning: Boolean,
    sweepColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f - 4.dp.toPx()

        // 3 concentric radar rings
        val r1 = maxRadius * 0.32f
        val r2 = maxRadius * 0.65f
        val r3 = maxRadius

        val ringColor = Color(0x33CF7052)
        val stroke = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )

        drawCircle(color = ringColor, radius = r1, center = center, style = stroke)
        drawCircle(color = ringColor, radius = r2, center = center, style = stroke)
        drawCircle(color = ringColor, radius = r3, center = center, style = stroke)

        // Crosshairs
        drawLine(
            color = ringColor.copy(alpha = 0.15f),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 1f
        )
        drawLine(
            color = ringColor.copy(alpha = 0.15f),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 1f
        )

        // Sweeping beam
        if (isScanning) {
            val sweepPath = Path().apply {
                moveTo(center.x, center.y)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        center.x - maxRadius,
                        center.y - maxRadius,
                        center.x + maxRadius,
                        center.y + maxRadius
                    ),
                    startAngleDegrees = sweepAngle - 45f,
                    sweepAngleDegrees = 45f,
                    forceMoveTo = false
                )
                close()
            }

            drawPath(
                path = sweepPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        sweepColor.copy(alpha = 0.28f),
                        sweepColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                )
            )

            // Sweep leading line
            val rad = Math.toRadians(sweepAngle.toDouble())
            val endPt = Offset(
                x = (center.x + maxRadius * cos(rad)).toFloat(),
                y = (center.y + maxRadius * sin(rad)).toFloat()
            )
            drawLine(
                color = sweepColor.copy(alpha = 0.65f),
                start = center,
                end = endPt,
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}

/**
 * Peer Card matching HTML prototype .peer-card
 */
@Composable
fun SyncPeerCard(
    peer: NearbyPeerUiModel,
    isRequested: Boolean,
    onConnect: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // TOP ROW: Avatar + Info + Distance Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Real Avatar with Online Dot
                SyncAvatar(
                    assetName = peer.avatarAsset,
                    size = 44.dp,
                    borderWidth = 1.dp,
                    borderColor = AuraTheme.colors.border,
                    isOnline = peer.isCurrentlyInRange
                )

                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${peer.displayName}, ${peer.age}",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                            color = AuraTheme.colors.ink
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Distance / Proximity Badge matching HTML prototype
                        val isVeryClose = peer.isCurrentlyInRange && (peer.confidenceTier == ConfidenceCategory.VERY_CLOSE || peer.distanceDisplay.contains("3m"))
                        val badgeBg = if (!peer.isCurrentlyInRange) {
                            AuraTheme.colors.surfaceSubtle
                        } else if (isVeryClose) {
                            AuraTheme.colors.success.copy(alpha = 0.14f)
                        } else {
                            AuraTheme.colors.accentSubtle
                        }
                        val badgeColor = if (!peer.isCurrentlyInRange) {
                            AuraTheme.colors.inkSecondary
                        } else if (isVeryClose) {
                            AuraTheme.colors.success
                        } else {
                            AuraTheme.colors.accent
                        }
                        val badgeLabel = if (!peer.isCurrentlyInRange) {
                            "Out of radar"
                        } else if (isVeryClose) {
                            "Very Close • 3m"
                        } else {
                            "Nearby • ${peer.distanceDisplay.takeWhile { it != '•' }.trim()}"
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(badgeBg)
                                .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeLabel,
                                fontFamily = DmMonoFontFamily,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                softWrap = false,
                                maxLines = 1,
                                color = badgeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Privacy Row
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

                    // Bio
                    if (!peer.introduction.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = peer.introduction,
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            color = AuraTheme.colors.inkSecondary,
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ACTION ROW: Skip & Connect Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Skip Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AuraTheme.colors.surfaceSubtle)
                        .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(8.dp))
                        .clickable { onSkip() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = AuraTheme.colors.inkSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Skip",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = AuraTheme.colors.inkSecondary
                        )
                    }
                }

                // Connect Button
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRequested) AuraTheme.colors.success else AuraTheme.colors.accent)
                        .clickable(enabled = !isRequested) { onConnect() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isRequested) Icons.Outlined.Check else Icons.Outlined.NearMe,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isRequested) "Request Sent" else "Connect",
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
}

@Composable
private fun PowerModeChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) AuraTheme.colors.accent else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else AuraTheme.colors.inkSecondary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = title,
                fontFamily = DmSansFontFamily,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.5.sp,
                color = if (isSelected) Color.White else AuraTheme.colors.inkSecondary
            )
        }
    }
}
