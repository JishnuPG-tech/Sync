package com.ghostmesh.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.ui.components.SyncAvatar
import com.ghostmesh.app.ui.components.SyncBrandLockup
import com.ghostmesh.app.ui.theme.AuraTheme
import com.ghostmesh.app.ui.theme.DmMonoFontFamily
import com.ghostmesh.app.ui.theme.DmSansFontFamily
import com.ghostmesh.app.ui.theme.PlayfairFontFamily

/**
 * Welcome / Onboarding Carousel
 * Pixel-perfect match to sync-app.html with authentic typography, real photo portraits,
 * concentric radar rings, and slide indicators.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSlide by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraTheme.colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HEADER: Brand Lockup + SKIP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SyncBrandLockup(iconSize = 24.dp, fontSize = 20.sp, withDot = false)

            Text(
                text = "SKIP",
                fontFamily = DmMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.08.sp,
                color = AuraTheme.colors.inkSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onGetStarted() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        // SLIDE CONTENT WITH ANIMATION
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = {
                    fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(280))
                },
                label = "welcomeSlide"
            ) { slideIndex ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (slideIndex) {
                        0 -> {
                            // Slide 1: Proximity radar rings with real human portraits
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .padding(bottom = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Concentric radar circles
                                Box(
                                    modifier = Modifier
                                        .size(210.dp)
                                        .border(1.dp, AuraTheme.colors.border, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(136.dp)
                                        .border(1.dp, AuraTheme.colors.border, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(66.dp)
                                        .background(AuraTheme.colors.accentSubtle, CircleShape)
                                        .border(1.dp, AuraTheme.colors.accent.copy(alpha = 0.4f), CircleShape)
                                )

                                // Center Target Bullseye
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(AuraTheme.colors.accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }

                                // 4 Orbit Avatars matching HTML prototype
                                SyncAvatar(
                                    assetName = "portrait_02.jpg",
                                    size = 42.dp,
                                    borderWidth = 2.dp,
                                    borderColor = AuraTheme.colors.accent,
                                    modifier = Modifier.offset(x = (-56).dp, y = (-58).dp)
                                )
                                SyncAvatar(
                                    assetName = "portrait_03.jpg",
                                    size = 42.dp,
                                    borderWidth = 2.dp,
                                    borderColor = AuraTheme.colors.accent,
                                    modifier = Modifier.offset(x = 58.dp, y = (-50).dp)
                                )
                                SyncAvatar(
                                    assetName = "portrait_04.jpg",
                                    size = 42.dp,
                                    borderWidth = 2.dp,
                                    borderColor = AuraTheme.colors.accent,
                                    modifier = Modifier.offset(x = (-50).dp, y = 54.dp)
                                )
                                SyncAvatar(
                                    assetName = "portrait_01.jpg",
                                    size = 42.dp,
                                    borderWidth = 2.dp,
                                    borderColor = AuraTheme.colors.accent,
                                    modifier = Modifier.offset(x = 54.dp, y = 50.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Title with editorial italic accent
                            val title1 = buildAnnotatedString {
                                append("Meet nearby.\nConnect by ")
                                withStyle(
                                    SpanStyle(
                                        color = AuraTheme.colors.accent,
                                        fontStyle = FontStyle.Italic
                                    )
                                ) {
                                    append("choice.")
                                }
                            }
                            Text(
                                text = title1,
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 28.sp,
                                lineHeight = 33.sp,
                                letterSpacing = (-0.035).sp,
                                textAlign = TextAlign.Center,
                                color = AuraTheme.colors.ink
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Discover opted-in people around you through Bluetooth radar. Zero cloud tracking, zero location broadcasting.",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Center,
                                color = AuraTheme.colors.inkSecondary,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }

                        1 -> {
                            // Slide 2: Instagram stays strictly private
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .padding(bottom = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(AuraTheme.colors.accentSubtle, CircleShape)
                                        .border(2.dp, AuraTheme.colors.accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = AuraTheme.colors.accent,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            val title2 = buildAnnotatedString {
                                append("Instagram stays\n")
                                withStyle(
                                    SpanStyle(
                                        color = AuraTheme.colors.accent,
                                        fontStyle = FontStyle.Italic
                                    )
                                ) {
                                    append("strictly private.")
                                }
                            }
                            Text(
                                text = title2,
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 28.sp,
                                lineHeight = 33.sp,
                                letterSpacing = (-0.035).sp,
                                textAlign = TextAlign.Center,
                                color = AuraTheme.colors.ink
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Your Instagram username remains locked and hidden until both people mutually accept a connection request.",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Center,
                                color = AuraTheme.colors.inkSecondary,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }

                        2 -> {
                            // Slide 3: Encounters stay saved in feed
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .padding(bottom = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(AuraTheme.colors.success.copy(alpha = 0.14f), CircleShape)
                                        .border(2.dp, AuraTheme.colors.success, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.VerifiedUser,
                                        contentDescription = null,
                                        tint = AuraTheme.colors.success,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            val title3 = buildAnnotatedString {
                                append("Encounters stay\n")
                                withStyle(
                                    SpanStyle(
                                        color = AuraTheme.colors.accent,
                                        fontStyle = FontStyle.Italic
                                    )
                                ) {
                                    append("saved in feed.")
                                }
                            }
                            Text(
                                text = title3,
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 28.sp,
                                lineHeight = 33.sp,
                                letterSpacing = (-0.035).sp,
                                textAlign = TextAlign.Center,
                                color = AuraTheme.colors.ink
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "When people walk out of radar range, they won't vanish. You can review profiles and connect whenever you choose.",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Center,
                                color = AuraTheme.colors.inkSecondary,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }
        }

        // CAROUSEL INDICATORS + ACTION BUTTONS
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pill Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                for (i in 0..2) {
                    val isActive = i == currentSlide
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isActive) 20.dp else 6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isActive) AuraTheme.colors.accent else AuraTheme.colors.border)
                            .clickable { currentSlide = i }
                    )
                }
            }

            // Primary Button: "Next" or "Get Started"
            Button(
                onClick = {
                    if (currentSlide < 2) {
                        currentSlide++
                    } else {
                        onGetStarted()
                    }
                },
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
                    text = if (currentSlide < 2) "Next" else "Get Started",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Button: "I already have an account"
            OutlinedButton(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AuraTheme.colors.inkSecondary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
            ) {
                Text(
                    text = "I already have an account",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp
                )
            }
        }
    }
}
