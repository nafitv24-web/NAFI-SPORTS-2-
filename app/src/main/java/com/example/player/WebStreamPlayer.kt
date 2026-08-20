package com.example.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.StreamServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebStreamPlayer(
    embedUrl: String,
    title: String,
    servers: List<StreamServer> = emptyList(),
    selectedServerIndex: Int = 0,
    onSelectServer: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    onDirectStreamDetected: (String) -> Unit = {},
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isExtractingNative by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var customViewContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var blockedAdsCount by remember { mutableIntStateOf(0) }
    var showServerSelector by remember { mutableStateOf(false) }

    // Comprehensive list of ad and popup tracking networks to completely block
    val adFilterKeywords = remember {
        listOf(
            "adsterra", "popcash", "propellerads", "onclick", "syndication", "exoclick",
            "juicyads", "trafficjunky", "doubleclick", "googleadservices", "taboola",
            "outbrain", "bet365", "1xbet", "parimatch", "melbet", "adtraffic", "hilltopads",
            "yandex.ru/ads", "adtrue", "monetag", "adnxs", "popads", "revenuehits", "adservice",
            "histats", "coinimp", "coinhive", "clickadu", "adcash", "adcolony", "admob",
            "banner", "popup", "adserver", "adskeeper", "trafficstars", "propeller",
            "vidoza", "openload", "gounlimited", "whomever"
        )
    }

    // Try background OkHttp extraction first before rendering WebView
    LaunchedEffect(embedUrl) {
        isExtractingNative = true
        val directResult = StreamExtractor.extractDirectStream(embedUrl)
        if (directResult != null && directResult.streamUrl.isNotBlank()) {
            onDirectStreamDetected(directResult.streamUrl)
            return@LaunchedEffect
        }
        isExtractingNative = false
    }

    DisposableEffect(embedUrl) {
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
            // Main Web Player View with Ad-Shield & Stream Sniffer
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
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        }

                        // Inject JS Interface for immediate stream URL bridging
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onFoundStream(url: String) {
                                if (url.isNotBlank() && !adFilterKeywords.any { url.contains(it, ignoreCase = true) }) {
                                    post {
                                        onDirectStreamDetected(url)
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onAdBlocked() {
                                post {
                                    blockedAdsCount++
                                }
                            }
                        }, "NativeStreamBridge")

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: return null
                                val lower = reqUrl.lowercase()

                                // 1. Sniff direct stream playlists (.m3u8, .mpd, master.m3u8)
                                if (lower.contains(".m3u8") || lower.contains(".mpd") || lower.contains("playlist.m3u8") || lower.contains("manifest.mpd")) {
                                    if (!adFilterKeywords.any { lower.contains(it) }) {
                                        post {
                                            onDirectStreamDetected(reqUrl)
                                        }
                                    }
                                }

                                // 2. Block ad trackers and malicious scripts
                                if (adFilterKeywords.any { lower.contains(it) }) {
                                    post { blockedAdsCount++ }
                                    return WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        ByteArrayInputStream("".toByteArray())
                                    )
                                }

                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false

                                // Inject Stream Sniffer & Ad-Bypasser JS
                                val adBlockAndSniffScript = """
                                    (function() {
                                        window.open = function() { return null; };
                                        window.alert = function() {};
                                        window.confirm = function() { return false; };

                                        var style = document.createElement('style');
                                        style.innerHTML = 'div[class*="ad"], div[id*="ad"], iframe[src*="ad"], div[class*="banner"], div[id*="banner"], div[class*="popup"], div[id*="pop"] { display: none !important; }';
                                        document.head.appendChild(style);

                                        var origOpen = XMLHttpRequest.prototype.open;
                                        XMLHttpRequest.prototype.open = function(method, url) {
                                            if (typeof url === 'string' && (url.includes('.m3u8') || url.includes('.mpd') || url.includes('master.txt'))) {
                                                if (window.NativeStreamBridge) {
                                                    window.NativeStreamBridge.onFoundStream(url);
                                                }
                                            }
                                            return origOpen.apply(this, arguments);
                                        };

                                        if (window.fetch) {
                                            var origFetch = window.fetch;
                                            window.fetch = function(input, init) {
                                                var url = typeof input === 'string' ? input : (input ? input.url : '');
                                                if (url && (url.includes('.m3u8') || url.includes('.mpd'))) {
                                                    if (window.NativeStreamBridge) {
                                                        window.NativeStreamBridge.onFoundStream(url);
                                                    }
                                                }
                                                return origFetch.apply(this, arguments);
                                            };
                                        }
                                    })();
                                """.trimIndent()

                                view?.evaluateJavascript(adBlockAndSniffScript, null)
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

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                blockedAdsCount++
                                return false
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

        // Top Header Overlay with Server Switcher & AdBlock status
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ad-Shield Active ($blockedAdsCount Blocked)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (servers.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.85f),
                            modifier = Modifier.clickable { showServerSelector = !showServerSelector }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Dns, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Server ${selectedServerIndex + 1}/${servers.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reload", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Expandable Server Selection Chips
            if (showServerSelector && servers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    servers.forEachIndexed { index, server ->
                        val isSelected = index == selectedServerIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF00E5FF) else Color.Black.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color.White else Color(0xFF475569)
                            ),
                            modifier = Modifier.clickable {
                                onSelectServer(index)
                                showServerSelector = false
                            }
                        ) {
                            Text(
                                text = server.name,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
