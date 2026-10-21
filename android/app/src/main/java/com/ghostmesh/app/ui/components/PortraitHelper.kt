package com.ghostmesh.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.data.network.SyncApiConfig
import com.ghostmesh.app.ui.theme.AuraTheme
import com.ghostmesh.app.ui.theme.DmSansFontFamily
import java.io.File
import java.net.URL

@Composable
fun rememberPortraitBitmap(assetName: String?): ImageBitmap? {
    if (assetName.isNullOrEmpty()) return null
    val context = LocalContext.current
    return remember(assetName) {
        try {
            when {
                // Content URI from system photo picker
                assetName.startsWith("content://") || assetName.startsWith("file://") -> {
                    context.contentResolver.openInputStream(Uri.parse(assetName))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }
                // Direct absolute file path
                File(assetName).exists() -> {
                    BitmapFactory.decodeFile(assetName)?.asImageBitmap()
                }
                // Server static photo path
                assetName.startsWith("/static/") || assetName.startsWith("http://") || assetName.startsWith("https://") -> {
                    val fullUrl = if (assetName.startsWith("/static/")) "${SyncApiConfig.activeBaseUrl}$assetName" else assetName
                    URL(fullUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }
                // Local asset name
                else -> {
                    val cleanName = if (assetName.startsWith("portraits/")) assetName else "portraits/$assetName"
                    context.assets.open(cleanName).use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Universal Sync Avatar component matching HTML prototype .peer-avatar-wrap
 */
@Composable
fun SyncAvatar(
    assetName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    borderWidth: Dp = 1.5.dp,
    borderColor: Color = AuraTheme.colors.border,
    isOnline: Boolean? = null,
    fallbackInitial: String = "S"
) {
    val bitmap = rememberPortraitBitmap(assetName)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(AuraTheme.colors.surfaceSubtle)
                .border(borderWidth, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = fallbackInitial.take(1).uppercase(),
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.4).sp,
                    color = AuraTheme.colors.inkSecondary
                )
            }
        }

        if (isOnline != null) {
            val dotColor = if (isOnline) AuraTheme.colors.success else AuraTheme.colors.inkMuted
            val dotSize = (size.value * 0.25f).coerceIn(8f, 13f).dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(2.dp, AuraTheme.colors.surface, CircleShape)
            )
        }
    }
}
