package com.ghostmesh.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.data.network.SyncApiClient
import com.ghostmesh.app.ui.components.SyncBrandLockup
import com.ghostmesh.app.ui.theme.AuraTheme
import com.ghostmesh.app.ui.theme.DmMonoFontFamily
import com.ghostmesh.app.ui.theme.DmSansFontFamily
import com.ghostmesh.app.ui.theme.PlayfairFontFamily
import kotlinx.coroutines.launch

/**
 * Primary Instagram Input Screen
 * - Centered, minimalist design matching the APK's dark amber/cyber theme.
 * - Single-purpose input for Instagram handle.
 * - Analyzes & validates the Instagram account in real-time.
 * - Extracts profile picture, full name, bio, and handle, then forwards them to ProfileSetupScreen.
 */
@Composable
fun InstagramInputScreen(
    initialHandle: String = "",
    onContinue: (
        instagramHandle: String,
        extractedFullName: String?,
        extractedAvatarUrl: String?,
        extractedBio: String?
    ) -> Unit,
    onSkip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var instagramInput by remember { mutableStateOf(initialHandle.removePrefix("@")) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisStatusText by remember { mutableStateOf("Analyzing Instagram profile...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { SyncApiClient() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    fun performAnalysis() {
        val clean = instagramInput.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) {
            errorMessage = "Please enter your Instagram username"
            return
        }

        keyboardController?.hide()
        isAnalyzing = true
        errorMessage = null
        analysisStatusText = "Connecting to Instagram..."

        coroutineScope.launch {
            try {
                analysisStatusText = "Extracting profile data for @$clean..."
                val result = apiClient.verifyInstagram(clean)

                if (result.isSuccess && result.getOrNull()?.exists == true) {
                    val profile = result.getOrNull()!!
                    isSuccess = true
                    analysisStatusText = "Profile verified!"
                    kotlinx.coroutines.delay(400)
                    onContinue(
                        clean,
                        profile.fullName,
                        profile.avatarUrl,
                        profile.bio
                    )
                } else {
                    val errMsg = result.getOrNull()?.error ?: "Could not fetch Instagram profile for @$clean"
                    errorMessage = errMsg
                }
            } catch (e: Exception) {
                errorMessage = "Network error while reaching Instagram service: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isAnalyzing = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraTheme.colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP BAR: Brand lockup + Skip
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
                    .clickable { onSkip() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CENTER SECTION: Glowing icon, Heading, Input, and Action Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Instagram / Aura badge icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AuraTheme.colors.accent.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(1.5.dp, AuraTheme.colors.accent.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "IG",
                    fontFamily = DmMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = AuraTheme.colors.accent
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Heading
            Text(
                text = "Enter your Instagram",
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 30.sp,
                letterSpacing = (-0.03).sp,
                color = AuraTheme.colors.ink,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle explanation
            Text(
                text = "Sync will automatically extract your profile photo, display name, and bio so you don't have to enter them manually.",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = AuraTheme.colors.inkSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // CENTERED INPUT BOX
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AuraTheme.colors.surface)
                    .border(
                        width = if (instagramInput.isNotEmpty()) 1.5.dp else 1.dp,
                        color = if (instagramInput.isNotEmpty()) AuraTheme.colors.accent else AuraTheme.colors.border,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "@",
                        fontFamily = DmMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (instagramInput.isNotEmpty()) AuraTheme.colors.accent else AuraTheme.colors.inkSecondary
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        if (instagramInput.isEmpty()) {
                            Text(
                                text = "your_instagram_handle",
                                fontFamily = DmSansFontFamily,
                                fontSize = 16.sp,
                                color = AuraTheme.colors.inkMuted
                            )
                        }

                        BasicTextField(
                            value = instagramInput,
                            onValueChange = {
                                instagramInput = it.trim().removePrefix("@").lowercase()
                                errorMessage = null
                            },
                            textStyle = TextStyle(
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = AuraTheme.colors.ink
                            ),
                            cursorBrush = SolidColor(AuraTheme.colors.accent),
                            singleLine = true,
                            enabled = !isAnalyzing,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { performAnalysis() }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (instagramInput.isNotEmpty() && !isAnalyzing) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear",
                            tint = AuraTheme.colors.inkMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable {
                                    instagramInput = ""
                                    errorMessage = null
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy guarantee pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = AuraTheme.colors.inkMuted,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Encrypted. Shared only after a mutual connection match.",
                    fontFamily = DmMonoFontFamily,
                    fontSize = 10.5.sp,
                    color = AuraTheme.colors.inkMuted,
                    textAlign = TextAlign.Center
                )
            }

            // Error Message Box
            AnimatedVisibility(visible = errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.2f))
                        .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage ?: "",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.5.sp,
                        color = Color(0xFFFCA5A5),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Continue manually anyway →",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = AuraTheme.colors.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val clean = instagramInput.trim().removePrefix("@").lowercase()
                                onContinue(clean, null, null, null)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ACTION BUTTON: Analyze & Continue
            Button(
                onClick = { performAnalysis() },
                enabled = instagramInput.isNotBlank() && !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuraTheme.colors.accent,
                    contentColor = Color.White,
                    disabledContainerColor = AuraTheme.colors.surfaceSubtle,
                    disabledContentColor = AuraTheme.colors.inkMuted
                )
            ) {
                if (isAnalyzing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = analysisStatusText,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                } else if (isSuccess) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Profile Extracted!",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Analyze & Continue",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // FOOTER: Trust & privacy tagline
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = AuraTheme.colors.success,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Zero tracking · Offline BLE Proximity · Ephemeral IDs",
                fontFamily = DmMonoFontFamily,
                fontSize = 10.sp,
                color = AuraTheme.colors.inkMuted
            )
        }
    }
}
