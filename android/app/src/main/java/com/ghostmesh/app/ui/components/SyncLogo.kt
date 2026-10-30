package com.ghostmesh.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.R
import com.ghostmesh.app.ui.theme.AuraTheme

/**
 * The Sync Logo:
 * Geometric interlocking angular ribbon emblem styled with the signature warm terracotta & copper gradient.
 */
@Composable
fun SyncHelixLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    animated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "syncPulse")
    val pulseScale by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        rememberInfiniteTransition(label = "static").animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000)),
            label = "static"
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.sync_emblem),
            contentDescription = "Sync Logo",
            modifier = Modifier
                .size(size)
                .scale(if (animated) pulseScale else 1f)
        )
    }
}

/**
 * Sync Brand Lockup: Sync Logo Icon + "Sync" in Playfair Editorial Serif
 */
@Composable
fun SyncBrandLockup(
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    textColor: Color = AuraTheme.colors.ink,
    withDot: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SyncHelixLogo(size = iconSize)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Sync",
            fontFamily = com.ghostmesh.app.ui.theme.PlayfairFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            color = textColor
        )
        if (withDot) {
            Text(
                text = ".",
                fontFamily = com.ghostmesh.app.ui.theme.PlayfairFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize,
                color = AuraTheme.colors.accent
            )
        }
    }
}
