package com.spott.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spott.app.ui.theme.AuraTheme
import com.spott.app.ui.theme.DmSansFontFamily

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MetaAuthDialog(
    clientId: String = "1908405646991633",
    onTokenReceived: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val redirectUri = "https://www.facebook.com/connect/login_success.html"
    val authUrl = "https://www.facebook.com/v19.0/dialog/oauth" +
            "?client_id=" + clientId +
            "&redirect_uri=" + Uri.encode(redirectUri) +
            "&response_type=token,granted_scopes" +
            "&scope=public_profile"

    var isLoading by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = AuraTheme.colors.canvas,
            border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.border)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar with Title and Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Meta & Instagram Verification",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AuraTheme.colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AuraTheme.colors.accent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = AuraTheme.colors.inkSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AuraTheme.colors.border)
                )

                // WebView Container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true

                                CookieManager.getInstance().setAcceptCookie(true)

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url?.toString() ?: ""
                                        return checkUrl(url)
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                        checkUrl(url ?: "")
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        checkUrl(url ?: "")
                                    }

                                    private fun checkUrl(url: String): Boolean {
                                        if (url.contains("access_token=")) {
                                            val token = url.substringAfter("access_token=").substringBefore("&")
                                            if (token.isNotBlank()) {
                                                onTokenReceived(token)
                                                return true
                                            }
                                        }
                                        return false
                                    }
                                }
                                loadUrl(authUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
