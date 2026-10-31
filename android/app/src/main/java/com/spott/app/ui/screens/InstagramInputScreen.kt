package com.spott.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.spott.app.data.network.SyncApiClient
import com.spott.app.ui.components.SyncBrandLockup
import com.spott.app.ui.theme.AuraTheme
import com.spott.app.ui.theme.DmMonoFontFamily
import com.spott.app.ui.theme.DmSansFontFamily
import com.spott.app.ui.theme.PlayfairFontFamily
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Clean Instagram Input Screen
 * - Normal username input only (no authentication, no OAuth, no logins, no passwords).
 * - Fast, seamless profile transition.
 * - Auto-detects clipboard handle.
 * - Optional instant profile extraction in background without blocking.
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
    var isSubmitting by remember { mutableStateOf(false) }
    var detectedClipboardHandle by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { SyncApiClient() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    // Clipboard auto-detection helper
    fun checkClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0)?.text?.toString()?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        val extracted = when {
                            text.contains("instagram.com/") -> {
                                text.substringAfter("instagram.com/")
                                    .substringBefore("?")
                                    .substringBefore("/")
                                    .removePrefix("@")
                                    .trim()
                            }
                            text.startsWith("@") -> text.removePrefix("@").trim()
                            else -> text.trim()
                        }
                        if (extracted.matches(Regex("^[a-zA-Z0-9._]{1,30}$")) && extracted != instagramInput) {
                            detectedClipboardHandle = extracted
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    val lifecycleOwner = context as? LifecycleOwner
    if (lifecycleOwner != null) {
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    checkClipboard()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    fun submitUsername() {
        val clean = instagramInput.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) {
            errorMessage = "Please enter your Instagram username (or tap Skip)"
            return
        }

        keyboardController?.hide()
        isSubmitting = true
        errorMessage = null

        coroutineScope.launch {
            try {
                // Quick gentle lookup for profile avatar/bio with 2-second timeout
                val res = withTimeoutOrNull(10000) {
                    apiClient.verifyInstagram(clean)
                }
                val profile = res?.getOrNull()
                if (profile != null && profile.exists) {
                    onContinue(
                        clean,
                        profile.fullName ?: clean,
                        profile.avatarUrl,
                        profile.bio
                    )
                } else {
                    // Seamless fallback: proceed directly with handle
                    onContinue(clean, clean, null, null)
                }
            } catch (_: Exception) {
                onContinue(clean, clean, null, null)
            } finally {
                isSubmitting = false
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

        // CENTER SECTION: Card & Inputs
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Instagram badge
            Box(
                modifier = Modifier
                    .size(72.dp)
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
                    text = "@",
                    fontFamily = DmMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = AuraTheme.colors.accent
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Heading
            Text(
                text = "Enter your Instagram",
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp,
                letterSpacing = (-0.02).sp,
                color = AuraTheme.colors.ink,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Connect your Instagram handle so nearby peers on radar can view and follow your profile.",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = AuraTheme.colors.inkSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Clipboard Quick-Fill Chip (if detected)
            AnimatedVisibility(visible = detectedClipboardHandle != null && instagramInput.isEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(AuraTheme.colors.surface)
                        .border(1.dp, AuraTheme.colors.accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable {
                            instagramInput = detectedClipboardHandle ?: ""
                            detectedClipboardHandle = null
                            errorMessage = null
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = "Paste",
                        tint = AuraTheme.colors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Paste @ from clipboard",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = AuraTheme.colors.accent
                    )
                }
            }

            // USERNAME INPUT FIELD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
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
                        fontSize = 20.sp,
                        color = if (instagramInput.isNotEmpty()) AuraTheme.colors.accent else AuraTheme.colors.inkSecondary
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        if (instagramInput.isEmpty()) {
                            Text(
                                text = "username",
                                fontFamily = DmSansFontFamily,
                                fontSize = 16.sp,
                                color = AuraTheme.colors.inkMuted
                            )
                        }

                        BasicTextField(
                            value = instagramInput,
                            onValueChange = {
                                val sanitized = it.trim().removePrefix("@").lowercase()
                                    .filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' }
                                instagramInput = sanitized
                                errorMessage = null
                            },
                            textStyle = TextStyle(
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.5.sp,
                                color = AuraTheme.colors.ink
                            ),
                            cursorBrush = SolidColor(AuraTheme.colors.accent),
                            singleLine = true,
                            enabled = !isSubmitting,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitUsername() }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (instagramInput.isNotEmpty() && !isSubmitting) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear",
                            tint = AuraTheme.colors.inkMuted,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable {
                                    instagramInput = ""
                                    errorMessage = null
                                }
                        )
                    }
                }
            }

            // Error Message Box
            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.5.sp,
                    color = Color(0xFFEF4444),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PRIMARY ACTION: CONTINUE BUTTON
            Button(
                onClick = { submitUsername() },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuraTheme.colors.accent,
                    disabledContainerColor = AuraTheme.colors.accent.copy(alpha = 0.5f)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues()
            ) {
                if (isSubmitting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Extracting @" + instagramInput.trim().removePrefix("@") + "...",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Continue",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Jump to Instagram app (if user wants to check handle)
            OutlinedButton(
                onClick = {
                    val clean = instagramInput.trim().removePrefix("@")
                    val uri = if (clean.isNotEmpty()) {
                        Uri.parse("instagram://user?username=")
                    } else {
                        Uri.parse("instagram://app")
                    }
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.instagram.android")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/")))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Could not open Instagram", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AuraTheme.colors.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AuraTheme.colors.inkSecondary
                )
            ) {
                Text(
                    text = "Open Instagram App ↗",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = AuraTheme.colors.inkSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FOOTER: Privacy & No Auth Guarantee
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
                text = "Zero passwords · Offline BLE proximity · Direct profile link",
                fontFamily = DmMonoFontFamily,
                fontSize = 10.sp,
                color = AuraTheme.colors.inkMuted
            )
        }
    }
}
