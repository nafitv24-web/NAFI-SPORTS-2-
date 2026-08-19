package com.example.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebStreamPlayer(
    embedUrl: String,
    title: String,
    modifier: Modifier = Modifier,
    onDirectStreamDetected: (String) -> Unit = {},
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var customViewContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var blockedAdsCount by remember { mutableIntStateOf(0) }

    val adFilterKeywords = remember {
        listOf(
            "adsterra", "popcash", "propellerads", "onclick", "syndication", "exoclick",
            "juicyads", "trafficjunky", "doubleclick", "googleadservices", "taboola",
            "outbrain", "bet365", "1xbet", "parimatch", "melbet", "adtraffic", "hilltopads",
            "yandex.ru/ads", "adtrue", "monetag", "adnxs", "popads", "revenuehits", "adservice"
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Custom Fullscreen View if HTML5 video requests full screen
        if (customViewContainer != null) {
            AndroidView(
                factory = { customViewContainer!! },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Main Web Player View
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewInstance = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            allowFileAccess = false
                            allowContentAccess = false
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 CloudStream/4.0"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: return null
                                val lower = reqUrl.lowercase()

                                // Block known aggressive ad networks
                                if (adFilterKeywords.any { lower.contains(it) }) {
                                    blockedAdsCount++
                                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                                }

                                // Sniff direct stream (.m3u8, .mp4)
                                if (lower.contains(".m3u8") || (lower.contains(".mp4") && !lower.contains("thumb"))) {
                                    onDirectStreamDetected(reqUrl)
                                }

                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                // Auto-trigger HTML5 video play if available
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        var videos = document.getElementsByTagName('video');
                                        for(var i=0; i<videos.length; i++) {
                                            videos[i].play();
                                        }
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress / 100f
                                if (newProgress >= 100) isLoading = false
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                customViewCallback = callback
                                if (view is FrameLayout) {
                                    customViewContainer = view
                                } else if (view != null) {
                                    val frame = FrameLayout(ctx).apply {
                                        addView(view)
                                    }
                                    customViewContainer = frame
                                }
                            }

                            override fun onHideCustomView() {
                                customViewContainer = null
                                customViewCallback?.onCustomViewHidden()
                                customViewCallback = null
                            }
                        }

                        loadUrl(embedUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Progress bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadProgress },
                color = Color(0xFF00E5FF),
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
            )
        }

        // Top Shield Badge Overlay (Shows AdBlock status and Refresh)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ad-Shield Active ($blockedAdsCount)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = { webViewInstance?.reload() },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reload", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
