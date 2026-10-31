package com.spott.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.spott.app.ui.theme.AuraTheme
import com.spott.app.ui.theme.DmMonoFontFamily
import com.spott.app.ui.theme.DmSansFontFamily
import com.spott.app.ui.theme.PlayfairFontFamily
import com.spott.app.update.AppUpdateInfo

fun formatReleaseNotes(raw: String): String {
    if (raw.isBlank()) return "Bug fixes and performance improvements."
    return raw
        .lines()
        .map { line ->
            var l = line.trim()
            while (l.startsWith("#")) {
                l = l.removePrefix("#").trim()
            }
            if (l.startsWith("- ") || l.startsWith("* ")) {
                l = "• " + l.substring(2).trim()
            }
            l = l.replace("**", "").replace("*", "").replace("__", "").replace("`", "")
            l.trim()
        }
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
}

fun formatReleaseTitle(title: String): String {
    var t = title.trim()
    while (t.startsWith("#")) {
        t = t.removePrefix("#").trim()
    }
    t = t.replace("**", "").replace("*", "").replace("`", "").trim()
    return t.ifBlank { "What is new in this update" }
}
@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    isDownloading: Boolean,
    downloadProgress: Float,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isDownloading) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AuraTheme.colors.canvas,
            border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AuraTheme.colors.accentSubtle)
                        .border(1.5.dp, AuraTheme.colors.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        tint = AuraTheme.colors.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Update Available",
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 23.sp,
                    color = AuraTheme.colors.ink,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Version Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AuraTheme.colors.surfaceSubtle)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = updateInfo.tagName,
                        fontFamily = DmMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = AuraTheme.colors.accent
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Formatted Release Notes Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraTheme.colors.surface)
                        .border(1.dp, AuraTheme.colors.border, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = formatReleaseTitle(updateInfo.releaseTitle),
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AuraTheme.colors.ink
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatReleaseNotes(updateInfo.releaseNotes),
                            fontFamily = DmSansFontFamily,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = AuraTheme.colors.inkSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Progress Indicator if downloading
                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = AuraTheme.colors.accent,
                            trackColor = AuraTheme.colors.surfaceSubtle
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val pct = (downloadProgress * 100).toInt()
                        Text(
                            text = "Downloading update: " + pct + "%",
                            fontFamily = DmMonoFontFamily,
                            fontSize = 11.5.sp,
                            color = AuraTheme.colors.inkSecondary
                        )
                    }
                } else {
                    // Actions: Update Now & Later
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onUpdateClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuraTheme.colors.accent,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Download & Install Now",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
                        ) {
                            Text(
                                text = "Later",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = AuraTheme.colors.inkSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
