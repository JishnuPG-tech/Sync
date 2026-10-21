package com.ghostmesh.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ghostmesh.app.ui.theme.AuraTheme
import com.ghostmesh.app.ui.theme.DmMonoFontFamily
import com.ghostmesh.app.ui.theme.DmSansFontFamily
import com.ghostmesh.app.ui.theme.PlayfairFontFamily
import kotlin.math.roundToInt

/**
 * Clean Top Bar for Sync
 * Brand lockup on left, optional action on right.
 * STRICTLY NO "Radar active" text, NO proximity symbol in top bar.
 */
@Composable
fun SyncTopBar(
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null
) {
    Surface(
        color = AuraTheme.colors.canvas,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SyncBrandLockup(iconSize = 24.dp, fontSize = 20.sp, withDot = false)

            if (onAddClick != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AuraTheme.colors.surface)
                        .border(1.dp, AuraTheme.colors.border, CircleShape)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontFamily = DmSansFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = AuraTheme.colors.inkSecondary
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(34.dp))
            }
        }
    }
}

/**
 * Mutual Connection Modal
 * STRICTLY NO heart/love icon!
 * Displays two circular photo avatars with central Synchronous Helix badge.
 */
@Composable
fun SyncConnectionCelebrationDialog(
    matchedName: String,
    instagramHandle: String,
    myAvatarAsset: String = "portrait_05.jpg",
    peerAvatarAsset: String = "portrait_02.jpg",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top handle pill
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AuraTheme.colors.border)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Overlapping Avatars with Central Synchronous Helix Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Left Avatar (You)
                    SyncAvatar(
                        assetName = myAvatarAsset,
                        size = 72.dp,
                        borderWidth = 3.dp,
                        borderColor = AuraTheme.colors.surface,
                        modifier = Modifier.offset(x = (-32).dp)
                    )

                    // Right Avatar (Peer)
                    SyncAvatar(
                        assetName = peerAvatarAsset,
                        size = 72.dp,
                        borderWidth = 3.dp,
                        borderColor = AuraTheme.colors.surface,
                        modifier = Modifier.offset(x = 32.dp)
                    )

                    // Central Synchronous Helix Badge (NO HEARTS!)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(AuraTheme.colors.surface)
                            .border(2.dp, AuraTheme.colors.accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        SyncHelixLogo(size = 24.dp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Headline in Playfair Display
                Text(
                    text = "It's a Sync!",
                    fontFamily = PlayfairFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = AuraTheme.colors.ink,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "You and $matchedName mutually connected. Instagram handle is now unlocked.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = AuraTheme.colors.inkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Unlocked Instagram Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraTheme.colors.surfaceSubtle)
                        .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AuraTheme.colors.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "IG",
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column {
                                Text(
                                    text = "@$instagramHandle",
                                    fontFamily = DmMonoFontFamily,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraTheme.colors.ink
                                )
                                Text(
                                    text = "Unlocked Instagram profile",
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 11.sp,
                                    color = AuraTheme.colors.success
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val cleanHandle = instagramHandle.removePrefix("@")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$cleanHandle"))
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuraTheme.colors.accent,
                                contentColor = Color.White
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Open IG",
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Full clearance dismiss button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraTheme.colors.accent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Great! Back to Radar",
                        fontFamily = DmSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Interactive Vector Map Picker Modal
 * Renders roads, campus lawns, tech zones, waterways and draggable/tappable pin marker.
 */
@Composable
fun SyncLocationMapDialog(
    currentLocation: String = "Kochi Tech Campus",
    onLocationConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pinXRatio by remember { mutableFloatStateOf(0.52f) }
    var pinYRatio by remember { mutableFloatStateOf(0.48f) }
    var selectedPlaceName by remember { mutableStateOf(if (currentLocation.isNotBlank()) currentLocation else "Kochi Tech Campus") }
    var coordinatesText by remember { mutableStateOf("9.9312° N, 76.2673° E • Kochi") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Precise Location",
                        fontFamily = PlayfairFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        color = AuraTheme.colors.ink
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = AuraTheme.colors.inkSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Vector Map Canvas
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEFECE6))
                        .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val rx = (offset.x / size.width).coerceIn(0.1f, 0.9f)
                                val ry = (offset.y / size.height).coerceIn(0.1f, 0.9f)
                                pinXRatio = rx
                                pinYRatio = ry

                                val lat = 9.9200 + (1f - ry) * 0.0200
                                val lng = 76.2500 + rx * 0.0300
                                coordinatesText = String.format(java.util.Locale.US, "%.4f° N, %.4f° E • Kochi", lat, lng)
                            }
                        }
                ) {
                    val w = maxWidth
                    val h = maxHeight

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height

                        // Background land
                        drawRect(color = Color(0xFFF1EEE6))

                        // Waterway curve
                        val waterPath = Path().apply {
                            moveTo(canvasW * 0.82f, 0f)
                            cubicTo(
                                canvasW * 0.75f, canvasH * 0.4f,
                                canvasW * 0.9f, canvasH * 0.65f,
                                canvasW * 0.78f, canvasH
                            )
                            lineTo(canvasW, canvasH)
                            lineTo(canvasW, 0f)
                            close()
                        }
                        drawPath(waterPath, color = Color(0xFFD6E3E6))

                        // Green lawn park
                        drawRoundRect(
                            color = Color(0xFFE2EAD8),
                            topLeft = Offset(canvasW * 0.12f, canvasH * 0.18f),
                            size = Size(canvasW * 0.32f, canvasH * 0.28f),
                            cornerRadius = CornerRadius(12f, 12f)
                        )

                        // Roads
                        val roadColor = Color(0xFFFFFFFF)
                        drawLine(
                            color = roadColor,
                            start = Offset(0f, canvasH * 0.45f),
                            end = Offset(canvasW, canvasH * 0.45f),
                            strokeWidth = 14f
                        )
                        drawLine(
                            color = roadColor,
                            start = Offset(canvasW * 0.48f, 0f),
                            end = Offset(canvasW * 0.48f, canvasH),
                            strokeWidth = 12f
                        )
                        drawLine(
                            color = roadColor,
                            start = Offset(0f, canvasH * 0.78f),
                            end = Offset(canvasW * 0.78f, canvasH * 0.78f),
                            strokeWidth = 10f
                        )
                    }

                    // Interactive Pin Marker
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = ((w.toPx() * pinXRatio) - 14.dp.toPx()).roundToInt(),
                                    y = ((h.toPx() * pinYRatio) - 34.dp.toPx()).roundToInt()
                                )
                            }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = AuraTheme.colors.accent,
                                modifier = Modifier.size(32.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(AuraTheme.colors.accent.copy(alpha = 0.35f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location Details Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraTheme.colors.surfaceSubtle)
                        .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = selectedPlaceName,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = AuraTheme.colors.ink
                        )
                        Text(
                            text = coordinatesText,
                            fontFamily = DmMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = AuraTheme.colors.inkSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Preset Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Infopark Campus, Kochi",
                        "Maker Village, Kalamassery",
                        "Central Library Hub"
                    ).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedPlaceName == preset) AuraTheme.colors.accentSubtle else AuraTheme.colors.surfaceSubtle)
                                .border(
                                    width = 1.dp,
                                    color = if (selectedPlaceName == preset) AuraTheme.colors.accent else AuraTheme.colors.border,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedPlaceName = preset
                                    if (preset.startsWith("Infopark")) {
                                        pinXRatio = 0.52f; pinYRatio = 0.48f
                                        coordinatesText = "9.9312° N, 76.2673° E • Kochi"
                                    } else if (preset.startsWith("Maker")) {
                                        pinXRatio = 0.35f; pinYRatio = 0.32f
                                        coordinatesText = "10.054° N, 76.353° E • Kalamassery"
                                    } else {
                                        pinXRatio = 0.68f; pinYRatio = 0.62f
                                        coordinatesText = "9.9816° N, 76.2999° E • Marine Drive"
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.split(",")[0].take(12),
                                fontFamily = DmSansFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedPlaceName == preset) AuraTheme.colors.accent else AuraTheme.colors.inkSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Confirm Button
                Button(
                    onClick = { onLocationConfirmed(selectedPlaceName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraTheme.colors.accent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Confirm Location",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}
