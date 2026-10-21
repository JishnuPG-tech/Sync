package com.ghostmesh.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.ui.components.SyncHelixLogo
import com.ghostmesh.app.ui.theme.AuraTheme
import com.ghostmesh.app.ui.theme.DmSansFontFamily
import com.ghostmesh.app.ui.theme.PlayfairFontFamily
import kotlinx.coroutines.delay

/**
 * Splash Screen
 * Features the Synchronous Helix logo with warm glow, Playfair Display title,
 * and animated progress bar that transitions to Welcome screen.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1800, easing = LinearEasing),
        label = "splashProgress"
    )

    LaunchedEffect(Unit) {
        progress = 1f
        delay(2000)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuraTheme.colors.canvas)
            .clickable { onSplashFinished() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Synchronous Helix S-Monogram Logo (110x110dp container)
            Box(
                modifier = Modifier.padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                SyncHelixLogo(size = 96.dp, animated = true)
            }

            // Brand Title: Sync. in Playfair Display
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Sync",
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 42.sp,
                    color = AuraTheme.colors.ink,
                    letterSpacing = (-0.04).sp
                )
                Text(
                    text = ".",
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 42.sp,
                    color = AuraTheme.colors.accent
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Tagline in DM Sans
            Text(
                text = "Meet nearby. Connect by choice.",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = AuraTheme.colors.inkSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Subtle loader bar
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AuraTheme.colors.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AuraTheme.colors.accent)
                )
            }
        }
    }
}
