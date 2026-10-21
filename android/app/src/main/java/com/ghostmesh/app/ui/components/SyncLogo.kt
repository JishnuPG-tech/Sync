package com.ghostmesh.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.ui.theme.AuraTheme

/**
 * The Synchronous Helix
 * Bespoke geometric S-monogram logo featuring two harmonic resonant loops
 * forming an iconic "S" with a central luminous nexus node and warm terracotta lighting gradients.
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
            initialValue = 0.96f,
            targetValue = 1.04f,
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
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 100f
            val centerPt = Offset(this.size.width / 2f, this.size.height / 2f)

            scale(if (animated) pulseScale else 1f, pivot = centerPt) {
                // Gradient colors matching portfolio terracotta/copper
                val helixGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF9A7F),
                        Color(0xFFCF7052),
                        Color(0xFF91331C)
                    ),
                    start = Offset(15f * scaleFactor, 15f * scaleFactor),
                    end = Offset(85f * scaleFactor, 85f * scaleFactor)
                )

                // Background resonance ring
                drawCircle(
                    brush = helixGradient,
                    radius = 42f * scaleFactor,
                    center = centerPt,
                    alpha = 0.25f,
                    style = Stroke(
                        width = 1.5f * scaleFactor,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * scaleFactor, 6f * scaleFactor), 0f)
                    )
                )

                // Upper S-loop Path
                val upperLoop = Path().apply {
                    moveTo(50f * scaleFactor, 50f * scaleFactor)
                    cubicTo(
                        35f * scaleFactor, 50f * scaleFactor,
                        22f * scaleFactor, 38f * scaleFactor,
                        22f * scaleFactor, 25f * scaleFactor
                    )
                    cubicTo(
                        22f * scaleFactor, 12f * scaleFactor,
                        35f * scaleFactor, 4f * scaleFactor,
                        50f * scaleFactor, 4f * scaleFactor
                    )
                    cubicTo(
                        65f * scaleFactor, 4f * scaleFactor,
                        78f * scaleFactor, 12f * scaleFactor,
                        78f * scaleFactor, 25f * scaleFactor
                    )
                    cubicTo(
                        78f * scaleFactor, 35f * scaleFactor,
                        68f * scaleFactor, 45f * scaleFactor,
                        50f * scaleFactor, 50f * scaleFactor
                    )
                    close()
                }

                drawPath(
                    path = upperLoop,
                    brush = helixGradient,
                    style = Stroke(
                        width = 6.5f * scaleFactor,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Lower S-loop Path
                val lowerLoop = Path().apply {
                    moveTo(50f * scaleFactor, 50f * scaleFactor)
                    cubicTo(
                        65f * scaleFactor, 50f * scaleFactor,
                        78f * scaleFactor, 62f * scaleFactor,
                        78f * scaleFactor, 75f * scaleFactor
                    )
                    cubicTo(
                        78f * scaleFactor, 88f * scaleFactor,
                        65f * scaleFactor, 96f * scaleFactor,
                        50f * scaleFactor, 96f * scaleFactor
                    )
                    cubicTo(
                        35f * scaleFactor, 96f * scaleFactor,
                        22f * scaleFactor, 88f * scaleFactor,
                        22f * scaleFactor, 75f * scaleFactor
                    )
                    cubicTo(
                        22f * scaleFactor, 65f * scaleFactor,
                        32f * scaleFactor, 55f * scaleFactor,
                        50f * scaleFactor, 50f * scaleFactor
                    )
                    close()
                }

                drawPath(
                    path = lowerLoop,
                    brush = helixGradient,
                    style = Stroke(
                        width = 6.5f * scaleFactor,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Center Luminous Nexus
                drawCircle(
                    color = Color(0xFFFF9A7F).copy(alpha = 0.5f),
                    radius = 9f * scaleFactor,
                    center = centerPt
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.5f * scaleFactor,
                    center = centerPt
                )
                drawCircle(
                    color = Color(0xFFCF7052),
                    radius = 2.5f * scaleFactor,
                    center = centerPt
                )
            }
        }
    }
}

/**
 * Sync Brand Lockup: Synchronous Helix Icon + "Sync" in Playfair Editorial Serif
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
