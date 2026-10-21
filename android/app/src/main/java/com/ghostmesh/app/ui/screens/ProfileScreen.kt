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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.data.model.AppearanceMode
import com.ghostmesh.app.data.model.ThemePreset
import com.ghostmesh.app.proximity.RadarPowerMode
import com.ghostmesh.app.ui.components.SyncAvatar
import com.ghostmesh.app.ui.components.SyncTopBar
import com.ghostmesh.app.ui.theme.AuraTheme
import com.ghostmesh.app.ui.theme.DmMonoFontFamily
import com.ghostmesh.app.ui.theme.DmSansFontFamily
import com.ghostmesh.app.ui.theme.PlayfairFontFamily

/**
 * Profile & Settings Screen
 * Tab 3 of Sync: profile hero card, stats readout, radar preferences, theme toggles, and reset action.
 */
@Composable
fun ProfileScreen(
    displayName: String,
    syncUsername: String = "",
    instagramHandle: String,
    introduction: String = "",
    location: String = "",
    avatarAsset: String? = null,
    encountersCount: Int = 0,
    connectedCount: Int = 0,
    powerMode: RadarPowerMode,
    onPowerModeChanged: (RadarPowerMode) -> Unit,
    themePreset: ThemePreset = ThemePreset.CLAUDE_WARM,
    onThemePresetChanged: (ThemePreset) -> Unit = {},
    appearanceMode: AppearanceMode,
    onAppearanceChanged: (AppearanceMode) -> Unit,
    onEditProfileClick: () -> Unit,
    onRerunOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var smartScanning by remember { mutableStateOf(true) }
    var ghostMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraTheme.colors.canvas)
    ) {
        SyncTopBar()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 60.dp)
        ) {
            // 1. PROFILE HERO CARD
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
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar Large
                        SyncAvatar(
                            assetName = avatarAsset,
                            size = 76.dp,
                            borderWidth = 2.5.dp,
                            borderColor = AuraTheme.colors.accent
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Display Name in Playfair Display
                        Text(
                            text = displayName,
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.02).sp,
                            color = AuraTheme.colors.ink
                        )

                        // Handle in DM Mono
                        Text(
                            text = "@$syncUsername",
                            fontFamily = DmMonoFontFamily,
                            fontSize = 11.5.sp,
                            color = AuraTheme.colors.accent
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bio
                        Text(
                            text = introduction,
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            color = AuraTheme.colors.inkSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Edit Details Button
                        OutlinedButton(
                            onClick = onEditProfileClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AuraTheme.colors.inkSecondary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Edit Profile Details",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AuraTheme.colors.border.copy(alpha = 0.5f))
                                .height(1.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox(value = "$encountersCount", label = "Encounters", valueColor = AuraTheme.colors.ink)
                            StatBox(value = "$connectedCount", label = "Connected", valueColor = AuraTheme.colors.accent)
                            StatBox(
                                value = if (powerMode != RadarPowerMode.OFF) "Active" else "Off",
                                label = "Radar",
                                valueColor = if (powerMode != RadarPowerMode.OFF) AuraTheme.colors.success else AuraTheme.colors.inkMuted
                            )
                        }
                    }
                }
            }

            // 2. SETTINGS: RADAR & DISCOVERY
            item {
                SettingsGroupHeader(title = "Radar & Discovery")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsToggleRow(
                            title = "Smart Proximity Scanning",
                            subtitle = "Auto-adjust scan cycles based on phone motion",
                            checked = smartScanning,
                            onCheckedChange = { smartScanning = it }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .background(AuraTheme.colors.border)
                                .height(1.dp)
                        )

                        SettingsToggleRow(
                            title = "Ghost Mode (Incognito)",
                            subtitle = "Scan for others without broadcasting yourself",
                            checked = ghostMode,
                            onCheckedChange = { ghostMode = it }
                        )
                    }
                }
            }

            // 3. SETTINGS: APPEARANCE & THEME
            item {
                SettingsGroupHeader(title = "Appearance & Theme")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val isDark = appearanceMode == AppearanceMode.DARK
                        SettingsToggleRow(
                            title = "Dark Mode",
                            subtitle = "Obsidian dark theme with warm terracotta accent",
                            checked = isDark,
                            onCheckedChange = { checked ->
                                onAppearanceChanged(if (checked) AppearanceMode.DARK else AppearanceMode.LIGHT)
                            }
                        )
                    }
                }
            }

            // 4. SETTINGS: SAFETY & RESET
            item {
                SettingsGroupHeader(title = "Safety & Reset")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRerunOnboarding() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Reset Demo to Start",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = AuraTheme.colors.accent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Restart welcome onboarding flow",
                                fontFamily = DmSansFontFamily,
                                fontSize = 11.5.sp,
                                color = AuraTheme.colors.inkSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = AuraTheme.colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 5. FOOTER
            item {
                Text(
                    text = "Sync APK v1.0.0 • Universal Android & iOS",
                    fontFamily = DmMonoFontFamily,
                    fontSize = 10.5.sp,
                    color = AuraTheme.colors.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StatBox(
    value: String,
    label: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = PlayfairFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = valueColor
        )
        Text(
            text = label,
            fontFamily = DmMonoFontFamily,
            fontSize = 10.sp,
            color = AuraTheme.colors.inkSecondary
        )
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        fontFamily = DmMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.06.sp,
        color = AuraTheme.colors.inkSecondary,
        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = AuraTheme.colors.ink
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = DmSansFontFamily,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = AuraTheme.colors.inkSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AuraTheme.colors.accent,
                uncheckedThumbColor = AuraTheme.colors.inkMuted,
                uncheckedTrackColor = AuraTheme.colors.surfaceSubtle
            )
        )
    }
}
