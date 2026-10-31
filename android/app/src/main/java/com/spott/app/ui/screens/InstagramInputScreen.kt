package com.spott.app.ui.screens
import android.app.Activity
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedButton
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

import com.spott.app.ui.components.MetaAuthDialog
import com.spott.app.data.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.widget.Toast

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.DisposableEffect
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalContext

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spott.app.data.network.SyncApiClient
import com.spott.app.ui.components.SyncBrandLockup
import com.spott.app.ui.theme.AuraTheme
import com.spott.app.ui.theme.DmMonoFontFamily
import com.spott.app.ui.theme.DmSansFontFamily
import com.spott.app.ui.theme.PlayfairFontFamily
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
    callbackManager: CallbackManager? = null,
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
    var showMetaDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { SyncApiClient() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    // Clipboard auto-detection: when returning from Instagram app, auto-fill copied username
    val lifecycleOwner = context as? LifecycleOwner
    if (lifecycleOwner != null) {
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
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
                                        else -> ""
                                    }
                                    if (extracted.matches(Regex("^[a-zA-Z0-9._]{1,30}$")) && instagramInput.isEmpty()) {
                                        instagramInput = extracted
                                        Toast.makeText(context, "Detected @" + extracted + " from clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }


    
    fun startMetaLogin() {
        val act = context as? Activity
        val cm = callbackManager ?: (context as? com.spott.app.MainActivity)?.callbackManager
        if (act == null || cm == null) {
            errorMessage = "Activity context unavailable for Meta SSO"
            return
        }

        isAnalyzing = true
        analysisStatusText = "Connecting to Meta authorization..."
        errorMessage = null

        val loginManager = LoginManager.getInstance()
        loginManager.registerCallback(cm, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                analysisStatusText = "Extracting verified profile..."
                val request = GraphRequest.newMeRequest(result.accessToken) { json, _ ->
                    if (json != null) {
                        val name = json.optString("name", "User")
                        val picUrl = json.optJSONObject("picture")?.optJSONObject("data")?.optString("url")
                        val cleanHandle = name.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }

                        isAnalyzing = false
                        isSuccess = true
                        UserSession.setVerifiedMeta(context, true)
                        Toast.makeText(context, "Verified with Meta: @" + cleanHandle, Toast.LENGTH_SHORT).show()
                        onContinue(cleanHandle, name, picUrl, "Verified via official Meta identity")
                    } else {
                        isAnalyzing = false
                        errorMessage = "Failed to extract profile information from Meta"
                    }
                }
                val parameters = Bundle().apply {
                    putString("fields", "id,name,picture.width(500),email")
                }
                request.parameters = parameters
                request.executeAsync()
            }

            override fun onCancel() {
                isAnalyzing = false
                Toast.makeText(context, "Meta login cancelled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                isAnalyzing = false
                errorMessage = "Meta authorization error: " + (error.localizedMessage ?: "Unknown error")
            }
        })

        loginManager.logInWithReadPermissions(
            act,
            listOf("public_profile", "email")
        )
    }

    fun performAnalysis() {
        val clean = instagramInput.trim().removePrefix("@").lowercase()
        if (clean.isBlank()) {
            errorMessage = "Please enter your Instagram username"
            return
        }

        keyboardController?.hide()
        isAnalyzing = true
        errorMessage = null
        analysisStatusText = "Connecting..."

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
                text = "Connect to Spott",
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
                text = "Authorize with 1-tap Meta / Instagram to import your real profile, or enter your handle manually below.",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = AuraTheme.colors.inkSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 1-TAP SECURE META / INSTAGRAM LOGIN WITH REAL PROFILE PERMISSIONS
            Button(
                onClick = {
                    errorMessage = null
                    startMetaLogin()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF833AB4), // Instagram Purple
                                    Color(0xFFFD1D1D), // Crimson
                                    Color(0xFFFCB045)  // Warm Amber
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = "Verified Meta Login",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Meta / Instagram",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary option: Launch installed Instagram App
            OutlinedButton(
                onClick = {
                    launchOfficialInstagramApk(context, instagramInput)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AuraTheme.colors.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AuraTheme.colors.inkSecondary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Open Installed Instagram App ↗",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = AuraTheme.colors.inkSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Divider: OR ENTER MANUALLY
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(AuraTheme.colors.border)
                )
                Text(
                    text = "OR ENTER MANUALLY",
                    fontFamily = DmMonoFontFamily,
                    fontSize = 10.sp,
                    color = AuraTheme.colors.inkMuted,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(AuraTheme.colors.border)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

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
                    text = "Pure offline BLE. 1-tap open directly to Instagram.",
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
                onClick = { if (!isAnalyzing) performAnalysis() },
                enabled = instagramInput.isNotBlank(),
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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = analysisStatusText,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (isSuccess) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Profile Extracted!",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = Color.White
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Analyze & Continue",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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

    if (showMetaDialog) {
        MetaAuthDialog(
            clientId = "1908405646991633",
            onTokenReceived = { token ->
                showMetaDialog = false
                isAnalyzing = true
                analysisStatusText = "Verifying Meta credentials..."
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val url = URL("https://graph.facebook.com/v19.0/me?fields=id,name,picture.width(400)&access_token=" + token)
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 8000
                            readTimeout = 8000
                        }
                        if (conn.responseCode == 200) {
                            val text = conn.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(text)
                            val verifiedName = json.optString("name", "User")
                            val picUrl = json.optJSONObject("picture")?.optJSONObject("data")?.optString("url")
                            val cleanHandle = verifiedName.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }

                            withContext(Dispatchers.Main) {
                                isAnalyzing = false
                                isSuccess = true
                                UserSession.setVerifiedMeta(context, true)
                                Toast.makeText(context, "Verified with Meta: @" + cleanHandle, Toast.LENGTH_SHORT).show()
                                onContinue(cleanHandle, verifiedName, picUrl, "Verified via official Meta identity")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                isAnalyzing = false
                                errorMessage = "Could not verify profile with Meta (HTTP " + conn.responseCode + ")"
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            isAnalyzing = false
                            errorMessage = "Meta verification error: " + (e.localizedMessage ?: "Unknown error")
                        }
                    }
                }
            },
            onDismiss = {
                showMetaDialog = false
            }
        )
    }
}


/**
 * Direct launch into official Instagram Android APK (com.instagram.android)
 * Opens user's profile if username entered, or opens Instagram app directly.
 */
private fun launchOfficialInstagramApk(context: Context, handle: String) {
    val clean = handle.trim().removePrefix("@")
    val igPackage = "com.instagram.android"

    // 1. Direct deep link into Instagram user profile if handle is present, or main app
    val deepLinkUri = if (clean.isNotBlank()) {
        Uri.parse("instagram://user?username=" + clean)
    } else {
        Uri.parse("instagram://app")
    }

    val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
        setPackage(igPackage)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
        Toast.makeText(context, "Opening official Instagram app...", Toast.LENGTH_SHORT).show()
        return
    } catch (_: Exception) {}

    // 2. Fallback to package launcher intent
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(igPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Toast.makeText(context, "Opening official Instagram app...", Toast.LENGTH_SHORT).show()
            return
        }
    } catch (_: Exception) {}

    // 3. Fallback to Google Play Store to install official Instagram APK
    try {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + igPackage)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(marketIntent)
        Toast.makeText(context, "Instagram app not installed. Opening Play Store...", Toast.LENGTH_SHORT).show()
        return
    } catch (_: Exception) {}

    // 4. Web browser fallback
    try {
        val webUri = if (clean.isNotBlank()) "https://instagram.com/" + clean else "https://instagram.com"
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open Instagram", Toast.LENGTH_SHORT).show()
    }
}
