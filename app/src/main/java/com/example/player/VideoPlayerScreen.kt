package com.example.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.basicMarquee
import coil.compose.AsyncImage
import com.example.R
import com.example.model.MediaItem as AppMediaItem
import com.example.model.MediaType
import com.example.model.StreamServer
import kotlinx.coroutines.delay

enum class MxDragType { NONE, VERTICAL_LEFT, VERTICAL_RIGHT, HORIZONTAL }

data class VideoQualityOption(
    val label: String,
    val height: Int,
    val bitrate: Int = 0,
    val isAuto: Boolean = false
)

data class AudioTrackOption(
    val id: String = "",
    val language: String = "",
    val displayName: String = "",
    val groupIndex: Int = -1,
    val trackIndex: Int = -1
)

data class SubtitleTrackOption(
    val id: String = "",
    val language: String = "",
    val displayName: String = "",
    val isOff: Boolean = false,
    val groupIndex: Int = -1,
    val trackIndex: Int = -1
)

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    mediaItem: AppMediaItem,
    playlist: List<AppMediaItem> = emptyList(),
    isTvMode: Boolean = false,
    onSelectMedia: (AppMediaItem) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isScreenLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var currentMedia by remember(mediaItem) { mutableStateOf(mediaItem) }
    val servers = remember(currentMedia) { currentMedia.getAllServers() }
    var selectedServerIndex by remember(currentMedia) { mutableIntStateOf(0) }
    var currentUrl by remember(currentMedia, selectedServerIndex) {
        mutableStateOf(servers.getOrNull(selectedServerIndex)?.url ?: currentMedia.streamUrl)
    }

    val isWebEmbedUrl = remember(currentUrl) {
        StreamExtractor.isEmbedUrl(currentUrl)
    }
    var forceWebEngine by remember(currentMedia.id, currentUrl) { mutableStateOf(false) }
    val useWebPlayer = (isWebEmbedUrl || forceWebEngine)

    // Automatically resolve any 2embed/vidsrc/embed streams to native ExoPlayer URLs in the background
    LaunchedEffect(currentUrl) {
        if (StreamExtractor.isEmbedUrl(currentUrl)) {
            val directStream = StreamExtractor.extractDirectStream(currentUrl)
            if (directStream != null && directStream.streamUrl.isNotBlank()) {
                currentUrl = directStream.streamUrl
                forceWebEngine = false
            }
        }
    }

    var isFullscreen by rememberSaveable { mutableStateOf(isScreenLandscape || isTvMode) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var isActuallyBuffering by remember { mutableStateOf(true) }
    var hasStartedPlaying by remember(currentMedia.id, currentUrl) { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentVideoResolution by remember { mutableStateOf<String?>(null) }

    // MX Player Gestures: Volume & Brightness & Seeking State
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxAudioVolume = remember(audioManager) { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

    var currentVolumeFraction by remember {
        val initialVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        mutableFloatStateOf((initialVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f))
    }
    var currentBrightnessFraction by remember {
        val initialBright = activity?.window?.attributes?.screenBrightness ?: 0.5f
        mutableFloatStateOf(if (initialBright < 0f) 0.5f else initialBright.coerceIn(0.01f, 1.0f))
    }

    var gestureVolumePercent by remember { mutableIntStateOf((currentVolumeFraction * 100).toInt()) }
    var gestureBrightnessPercent by remember { mutableIntStateOf((currentBrightnessFraction * 100).toInt()) }
    var isVolumeGestureActive by remember { mutableStateOf(false) }
    var isBrightnessGestureActive by remember { mutableStateOf(false) }
    var isSeekGestureActive by remember { mutableStateOf(false) }
    var volumeGestureKey by remember { mutableLongStateOf(0L) }
    var brightnessGestureKey by remember { mutableLongStateOf(0L) }
    var seekGestureKey by remember { mutableLongStateOf(0L) }
    var seekGestureOffsetSec by remember { mutableIntStateOf(0) }
    var seekGestureTargetMs by remember { mutableLongStateOf(0L) }
    var doubleTapSeekLeft by remember { mutableStateOf(false) }
    var doubleTapSeekRight by remember { mutableStateOf(false) }

    // Video Quality, Audio Track & Subtitles (Screenshot 2: HD 720p, Hindi, English)
    var availableVideoQualities by remember {
        mutableStateOf(
            listOf(
                VideoQualityOption("Auto (Adaptive)", -1, isAuto = true),
                VideoQualityOption("1080p FHD", 1080),
                VideoQualityOption("720p HD", 720),
                VideoQualityOption("480p SD", 480),
                VideoQualityOption("360p Low", 360)
            )
        )
    }
    var selectedVideoQuality by remember { mutableStateOf(availableVideoQualities.first()) }
    var showQualityDialog by remember { mutableStateOf(false) }

    var availableAudioTracks by remember {
        mutableStateOf<List<AudioTrackOption>>(emptyList())
    }
    var selectedAudioTrack by remember { mutableStateOf<AudioTrackOption?>(null) }
    var showAudioDialog by remember { mutableStateOf(false) }

    var availableSubtitles by remember {
        mutableStateOf<List<SubtitleTrackOption>>(
            listOf(SubtitleTrackOption(id = "off", language = "", displayName = "অফ (Off)", isOff = true))
        )
    }
    var selectedSubtitle by remember { mutableStateOf<SubtitleTrackOption?>(null) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    // Auto-dismiss Gesture HUD Overlays after inactivity
    LaunchedEffect(volumeGestureKey) {
        if (volumeGestureKey > 0L && isVolumeGestureActive) {
            delay(1500)
            isVolumeGestureActive = false
        }
    }
    LaunchedEffect(brightnessGestureKey) {
        if (brightnessGestureKey > 0L && isBrightnessGestureActive) {
            delay(1500)
            isBrightnessGestureActive = false
        }
    }
    LaunchedEffect(seekGestureKey) {
        if (seekGestureKey > 0L && isSeekGestureActive) {
            delay(1500)
            isSeekGestureActive = false
        }
    }
    LaunchedEffect(doubleTapSeekLeft) {
        if (doubleTapSeekLeft) {
            delay(750)
            doubleTapSeekLeft = false
        }
    }
    LaunchedEffect(doubleTapSeekRight) {
        if (doubleTapSeekRight) {
            delay(750)
            doubleTapSeekRight = false
        }
    }

    // Shared Bandwidth Meter to accurately estimate network throughput and dynamically adapt video quality
    val bandwidthMeter = remember {
        androidx.media3.exoplayer.upstream.DefaultBandwidthMeter.Builder(context)
            .setResetOnNetworkTypeChange(false)
            .build()
    }

    // Smooth debounce for buffering overlay so micro network fluctuations don't flash intrusive cards over active video
    LaunchedEffect(isBuffering, hasStartedPlaying) {
        if (isBuffering) {
            val waitDelay = if (hasStartedPlaying) 800L else 150L
            delay(waitDelay)
            if (isBuffering) {
                isActuallyBuffering = true
            }
        } else {
            isActuallyBuffering = false
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var showQuickChannelDrawer by remember { mutableStateOf(false) }

    // TV Remote OSD & Number Input
    var showChannelOsd by remember { mutableStateOf(false) }
    var channelOsdKey by remember { mutableLongStateOf(0L) }
    var remoteNumberBuffer by remember { mutableStateOf("") }
    var remoteNumberKey by remember { mutableLongStateOf(0L) }

    // Auto-hide Channel OSD banner
    LaunchedEffect(channelOsdKey) {
        if (channelOsdKey > 0L) {
            showChannelOsd = true
            delay(3500)
            showChannelOsd = false
        }
    }

    // Auto-commit remote channel number input (e.g. user types "5" or "12")
    LaunchedEffect(remoteNumberKey) {
        if (remoteNumberKey > 0L && remoteNumberBuffer.isNotEmpty()) {
            delay(1200)
            val channelNum = remoteNumberBuffer.toIntOrNull()
            if (channelNum != null && playlist.isNotEmpty()) {
                val targetIdx = (channelNum - 1).coerceIn(0, playlist.size - 1)
                val nextItem = playlist[targetIdx]
                isBuffering = true
                currentMedia = nextItem
                selectedServerIndex = 0
                val newServers = nextItem.getAllServers()
                currentUrl = newServers.firstOrNull()?.url ?: nextItem.streamUrl
                errorMessage = null
                onSelectMedia(nextItem)
                channelOsdKey = System.currentTimeMillis()
            }
            remoteNumberBuffer = ""
        }
    }

    // Immediately trigger buffering state whenever channel or server URL changes
    LaunchedEffect(currentMedia.id, currentUrl) {
        hasStartedPlaying = false
        isBuffering = true
        isActuallyBuffering = true
        errorMessage = null
        currentPositionMs = 0L
        durationMs = 0L
        sliderPosition = 0f
        isDraggingSlider = false
    }

    // Intercept system/mobile back press to safely exit player and return to previous list
    BackHandler {
        if (showQuickChannelDrawer) {
            showQuickChannelDrawer = false
        } else {
            onBack()
        }
    }

    // Synchronize fullscreen state with device orientation if physically rotated or in TV Mode
    LaunchedEffect(isScreenLandscape, isTvMode) {
        if ((isScreenLandscape || isTvMode) && !isFullscreen) {
            isFullscreen = true
        }
        if (isTvMode) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // Reset orientation only when exiting the player screen entirely
    DisposableEffect(isTvMode) {
        onDispose {
            if (isTvMode) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    // Hide System Bars (Status Bar & Navigation Bar) completely in fullscreen mode
    DisposableEffect(isFullscreen, activity) {
        val window = activity?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, !isFullscreen)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                insetsController.hide(WindowInsetsCompat.Type.captionBar())
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    window.attributes.layoutInDisplayCutoutMode =
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                )
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        onDispose {
            val w = activity?.window
            if (w != null) {
                WindowCompat.setDecorFitsSystemWindows(w, true)
                val insetsController = WindowCompat.getInsetsController(w, w.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                @Suppress("DEPRECATION")
                w.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    // Re-enforce system bar hiding when fullscreen or controls change
    LaunchedEffect(isFullscreen, showControls) {
        if (isFullscreen) {
            val window = activity?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                insetsController.hide(WindowInsetsCompat.Type.captionBar())
            }
        }
    }

    // Auto hide controls after 4 seconds of inactivity
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4500)
            showControls = false
        }
    }

    // Setup ExoPlayer instance with custom http data source, headers and dynamic pipe parsing
    val exoPlayer = remember(currentUrl, currentMedia) {
        var cleanUrl = currentUrl.trim()
        var extractedUa: String? = currentMedia.userAgent
        var extractedReferer: String? = currentMedia.referrer
        var extractedOrigin: String? = currentMedia.origin
        var extractedCookie: String? = currentMedia.cookie
        val dynamicHeaders = mutableMapOf<String, String>()

        // Parse pipe syntax: http://stream.m3u8|User-Agent=...&Referer=...
        if (cleanUrl.contains("|")) {
            val parts = cleanUrl.split("|", limit = 2)
            cleanUrl = parts[0].trim()
            val pairs = parts[1].split("&")
            for (pair in pairs) {
                val kv = pair.split("=", limit = 2)
                if (kv.size == 2) {
                    val k = kv[0].trim()
                    val rawV = kv[1].trim()
                    val v = try {
                        java.net.URLDecoder.decode(rawV, "UTF-8")
                    } catch (_: Exception) {
                        rawV
                    }
                    when {
                        k.equals("User-Agent", ignoreCase = true) || k.equals("http-user-agent", ignoreCase = true) -> extractedUa = v
                        k.equals("Referer", ignoreCase = true) || k.equals("Referrer", ignoreCase = true) || k.equals("http-referrer", ignoreCase = true) || k.equals("http-referer", ignoreCase = true) -> extractedReferer = v
                        k.equals("Origin", ignoreCase = true) || k.equals("http-origin", ignoreCase = true) -> extractedOrigin = v
                        k.equals("Cookie", ignoreCase = true) || k.equals("http-cookie", ignoreCase = true) -> extractedCookie = v
                        else -> dynamicHeaders[k] = v
                    }
                }
            }
        }

        // Apply custom headers from MediaItem
        currentMedia.customHeaders?.let { dynamicHeaders.putAll(it) }

        // Domain-specific smart headers (Toffee, Bioscope, TSports, etc.)
        val isToffee = cleanUrl.contains("toffeelive.com", ignoreCase = true) ||
                cleanUrl.contains("toffee", ignoreCase = true) ||
                cleanUrl.contains("bldcmprod-cdn", ignoreCase = true) ||
                currentMedia.category.contains("toffee", ignoreCase = true)

        if (isToffee) {
            if (extractedUa.isNullOrBlank()) extractedUa = "Toffee (Linux;Android 14)"
            if (extractedReferer.isNullOrBlank()) extractedReferer = "https://toffeelive.com/"
            if (extractedOrigin.isNullOrBlank()) extractedOrigin = "https://toffeelive.com"
        }

        val isHakuna = cleanUrl.contains("hakunaymatata", ignoreCase = true) ||
                cleanUrl.contains("sacdn", ignoreCase = true)

        if (isHakuna) {
            if (extractedReferer.isNullOrBlank()) extractedReferer = "https://hakunaymatata.com/"
            if (extractedOrigin.isNullOrBlank()) extractedOrigin = "https://hakunaymatata.com"
        }

        val finalUserAgent = extractedUa ?: "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 NAFITV24"

        val requestHeaders = mutableMapOf<String, String>()
        requestHeaders["User-Agent"] = finalUserAgent
        if (!extractedReferer.isNullOrBlank()) {
            requestHeaders["Referer"] = extractedReferer
        }
        if (!extractedOrigin.isNullOrBlank()) {
            requestHeaders["Origin"] = extractedOrigin
        }
        if (!extractedCookie.isNullOrBlank()) {
            requestHeaders["Cookie"] = extractedCookie
        }
        requestHeaders["Accept"] = "*/*"
        requestHeaders["Connection"] = "keep-alive"
        requestHeaders["Accept-Encoding"] = "gzip, deflate"
        requestHeaders["Cache-Control"] = "no-cache"
        requestHeaders.putAll(dynamicHeaders)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(30000)
            .setUserAgent(finalUserAgent)
            .setTransferListener(bandwidthMeter)
            .setDefaultRequestProperties(requestHeaders)

        val (finalCleanUrl, drmConfig) = com.example.util.DrmHelper.extractDrmConfig(
            rawUrl = currentUrl,
            itemScheme = currentMedia.drmScheme,
            itemLicenseUrl = currentMedia.drmLicenseUrl,
            itemLicenseKey = currentMedia.drmLicenseKey,
            itemHeaders = currentMedia.drmHeaders,
            itemManifestType = currentMedia.manifestType
        )

        // Load error handling policy with 5 automatic retries for transient stream packet drops
        val loadErrorHandlingPolicy = androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy(5)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)
            .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

        if (drmConfig != null) {
            val drmSessionManager = com.example.util.DrmHelper.createDrmSessionManager(drmConfig, httpDataSourceFactory)
            if (drmSessionManager != null) {
                mediaSourceFactory.setDrmSessionManagerProvider { drmSessionManager }
            }
        }

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setEnableDecoderFallback(true)

        // High-responsiveness Adaptive Bitrate Track Selection:
        // Automatically steps down resolution in 300ms if bandwidth drops to prevent any buffering stalls,
        // and upgrades smoothly to HD when network throughput is proven stable.
        val adaptiveTrackSelectionFactory = androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection.Factory(
            /* minDurationForQualityIncreaseMs = */ 8000, // 8s sustained speed before upgrading
            /* maxDurationForQualityDecreaseMs = */ 300,  // 300ms ultra-rapid downscaling on network drop to prevent stalls
            /* minDurationToRetainAfterDiscardMs = */ 2000,
            /* bandwidthFraction = */ 0.70f               // 70% bandwidth headroom prevents buffer exhaustion
        )

        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context, adaptiveTrackSelectionFactory).apply {
            setParameters(
                buildUponParameters()
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowMultipleAdaptiveSelections(true)
                    .setTunnelingEnabled(false)
                    .setForceLowestBitrate(false)
                    .setForceHighestSupportedBitrate(false)
            )
        }

        // Anti-Buffering LoadControl: Deep buffer cushion ensures continuous seamless streaming without stuttering
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 35000,                  // 35s min buffer keeps sufficient chunks preloaded
                /* maxBufferMs = */ 120000,                 // 120s max buffer allows deep preloading
                /* bufferForPlaybackMs = */ 800,             // 800ms fast startup on channel switch
                /* bufferForPlaybackAfterRebufferMs = */ 2500 // 2500ms ensures smooth playback without repeated stalls
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30000, true)
            .setTargetBufferBytes(androidx.media3.common.C.LENGTH_UNSET)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setBandwidthMeter(bandwidthMeter)
            .setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            .build().apply {
                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(finalCleanUrl)

                val isLiveStream = currentMedia.isLive || currentMedia.type == MediaType.LIVE_TV || currentMedia.type == MediaType.LIVE_EVENT
                if (isLiveStream) {
                    mediaItemBuilder.setLiveConfiguration(
                        androidx.media3.common.MediaItem.LiveConfiguration.Builder()
                            .setTargetOffsetMs(20000L) // 20s safe distance from live broadcast edge prevents running out of chunks
                            .setMinOffsetMs(8000L)
                            .setMaxOffsetMs(60000L)
                            .setMinPlaybackSpeed(1.0f) // Keep standard 1.0x playback speed (prevents speeding up and stalling)
                            .setMaxPlaybackSpeed(1.0f)
                            .build()
                    )
                }

                if (drmConfig != null) {
                    val drmConfigBuilder = MediaItem.DrmConfiguration.Builder(drmConfig.schemeUuid)
                    if (!drmConfig.licenseUrl.isNullOrBlank()) {
                        drmConfigBuilder.setLicenseUri(drmConfig.licenseUrl)
                    }
                    if (drmConfig.headers.isNotEmpty()) {
                        drmConfigBuilder.setLicenseRequestHeaders(drmConfig.headers)
                    }
                    drmConfigBuilder.setMultiSession(true)
                    mediaItemBuilder.setDrmConfiguration(drmConfigBuilder.build())
                }

                val isMpd = finalCleanUrl.contains(".mpd", ignoreCase = true) ||
                        finalCleanUrl.contains("dash", ignoreCase = true) ||
                        drmConfig?.manifestType?.equals("mpd", ignoreCase = true) == true ||
                        currentMedia.manifestType?.equals("mpd", ignoreCase = true) == true

                val isM3u8 = finalCleanUrl.contains(".m3u8", ignoreCase = true) ||
                        finalCleanUrl.contains("hls", ignoreCase = true) ||
                        drmConfig?.manifestType?.equals("hls", ignoreCase = true) == true ||
                        currentMedia.manifestType?.equals("hls", ignoreCase = true) == true ||
                        isToffee

                if (isMpd) {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
                } else if (isM3u8) {
                    mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                }

                setMediaItem(mediaItemBuilder.build())
                playWhenReady = true
                prepare()

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                isBuffering = true
                                errorMessage = null
                            }
                            Player.STATE_READY -> {
                                isBuffering = false
                                isActuallyBuffering = false
                                hasStartedPlaying = true
                                errorMessage = null
                                isPlaying = playWhenReady
                                val dur = duration
                                if (dur > 0 && dur != C.TIME_UNSET) {
                                    durationMs = dur
                                }
                            }
                            Player.STATE_ENDED -> {
                                isBuffering = false
                                isPlaying = false
                            }
                            Player.STATE_IDLE -> {
                                isBuffering = false
                            }
                        }
                    }

                    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                        val dur = duration
                        if (dur > 0 && dur != C.TIME_UNSET) {
                            durationMs = dur
                        }
                    }

                    override fun onRenderedFirstFrame() {
                        isBuffering = false
                        isActuallyBuffering = false
                        hasStartedPlaying = true
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        val qualities = mutableListOf<VideoQualityOption>()
                        qualities.add(VideoQualityOption("অটো (Auto)", -1, isAuto = true))
                        val seenHeights = mutableSetOf<Int>()

                        val audios = mutableListOf<AudioTrackOption>()
                        val subs = mutableListOf<SubtitleTrackOption>()
                        subs.add(SubtitleTrackOption(id = "off", language = "", displayName = "বন্ধ (Off)", isOff = true))

                        for (groupIndex in 0 until tracks.groups.size) {
                            val group = tracks.groups[groupIndex]
                            when (group.type) {
                                C.TRACK_TYPE_VIDEO -> {
                                    for (trackIndex in 0 until group.length) {
                                        val format = group.getTrackFormat(trackIndex)
                                        if (format.height > 0 && seenHeights.add(format.height)) {
                                            val label = when {
                                                format.height >= 1080 -> "1080p FHD"
                                                format.height >= 720 -> "720p HD"
                                                format.height >= 480 -> "480p SD"
                                                format.height >= 360 -> "360p Low"
                                                else -> "${format.height}p"
                                            }
                                            qualities.add(VideoQualityOption(label, format.height))
                                        }
                                    }
                                }
                                C.TRACK_TYPE_AUDIO -> {
                                    for (trackIndex in 0 until group.length) {
                                        val format = group.getTrackFormat(trackIndex)
                                        val lang = format.language ?: ""
                                        val langName = when (lang.lowercase()) {
                                            "hi", "hin", "hindi" -> "Hindi (হিন্দি)"
                                            "bn", "ben", "bangla", "bengali" -> "Bengali (বাংলা)"
                                            "en", "eng", "english" -> "English (ইংরেজি)"
                                            "ta", "tam", "tamil" -> "Tamil (তামিল)"
                                            "te", "tel", "telugu" -> "Telugu (তেলেগু)"
                                            "ur", "urd", "urdu" -> "Urdu (উর্দু)"
                                            else -> format.label?.ifBlank { null } ?: if (lang.isNotBlank()) lang.uppercase() else "Audio Track ${audios.size + 1}"
                                        }
                                        audios.add(AudioTrackOption(id = "$groupIndex-$trackIndex", language = lang, displayName = langName, groupIndex = groupIndex, trackIndex = trackIndex))
                                    }
                                }
                                C.TRACK_TYPE_TEXT -> {
                                    for (trackIndex in 0 until group.length) {
                                        val format = group.getTrackFormat(trackIndex)
                                        val lang = format.language ?: ""
                                        val langName = when (lang.lowercase()) {
                                            "en", "eng", "english" -> "English Subtitles"
                                            "bn", "ben", "bangla" -> "Bangla Subtitles"
                                            "hi", "hin" -> "Hindi Subtitles"
                                            else -> format.label?.ifBlank { null } ?: if (lang.isNotBlank()) lang.uppercase() else "Subtitle ${subs.size}"
                                        }
                                        subs.add(SubtitleTrackOption(id = "$groupIndex-$trackIndex", language = lang, displayName = langName, isOff = false, groupIndex = groupIndex, trackIndex = trackIndex))
                                    }
                                }
                            }
                        }

                        if (qualities.size <= 1) {
                            qualities.clear()
                            qualities.add(VideoQualityOption("অটো (Auto)", -1, isAuto = true))
                            qualities.add(VideoQualityOption("1080p FHD", 1080))
                            qualities.add(VideoQualityOption("720p HD", 720))
                            qualities.add(VideoQualityOption("480p SD", 480))
                            qualities.add(VideoQualityOption("360p Low", 360))
                        }

                        availableVideoQualities = qualities.sortedByDescending { it.height }
                        if (audios.isNotEmpty()) {
                            availableAudioTracks = audios
                            if (selectedAudioTrack == null) {
                                selectedAudioTrack = audios.firstOrNull()
                            }
                        }
                        if (subs.size > 1) {
                            availableSubtitles = subs
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }

                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                        if (videoSize.width > 0 && videoSize.height > 0) {
                            val label = when {
                                videoSize.height >= 1080 -> "1080p HD"
                                videoSize.height >= 720 -> "720p HD"
                                videoSize.height >= 480 -> "480p SD"
                                videoSize.height >= 360 -> "360p Low"
                                else -> "${videoSize.height}p"
                            }
                            currentVideoResolution = label
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        isBuffering = false
                        if (servers.size > 1 && selectedServerIndex < servers.size - 1) {
                            selectedServerIndex++
                            currentUrl = servers[selectedServerIndex].url
                            errorMessage = "সার্ভার পরিবর্তন হচ্ছে: ${servers[selectedServerIndex].name}..."
                        } else if (isWebEmbedUrl) {
                            forceWebEngine = true
                            errorMessage = null
                        } else {
                            errorMessage = "ভিডিও লোড হচ্ছে না (${error.errorCodeName})। বিকল্প সার্ভার বেছে নিন অথবা পুনরায় চেষ্টা করুন।"
                        }
                    }
                })
            }
    }

    // Periodic time progress tracker
    LaunchedEffect(exoPlayer) {
        while (true) {
            val cur = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration
            if (dur > 0 && dur != C.TIME_UNSET) {
                durationMs = dur
            }
            if (!isDraggingSlider) {
                currentPositionMs = cur
            }
            delay(250)
        }
    }

    // Cleanup on dispose
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchedEffect(isFullscreen, showControls) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Handle Direct Channel Number input from Remote (0-9)
    fun handleNumberInput(digit: String) {
        remoteNumberBuffer = (remoteNumberBuffer + digit).take(4)
        remoteNumberKey = System.currentTimeMillis()
    }

    // Switch to Next / Previous Channel (Smoothly switches in fullscreen without exiting)
    fun switchChannel(delta: Int) {
        val list = if (playlist.isNotEmpty()) playlist else listOf(currentMedia)
        val currentIndex = list.indexOfFirst { it.id == currentMedia.id || it.streamUrl == currentMedia.streamUrl }
        if (currentIndex != -1 && list.isNotEmpty()) {
            val nextIndex = (currentIndex + delta).mod(list.size)
            val nextItem = list[nextIndex]
            isBuffering = true
            currentMedia = nextItem
            selectedServerIndex = 0
            val newServers = nextItem.getAllServers()
            currentUrl = newServers.firstOrNull()?.url ?: nextItem.streamUrl
            errorMessage = null
            onSelectMedia(nextItem)
            channelOsdKey = System.currentTimeMillis()
        }
    }

    fun toggleFullscreen() {
        val targetLandscape = !isFullscreen
        isFullscreen = targetLandscape
        activity?.requestedOrientation = if (targetLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun handleRemoteKeyEvent(keyEvent: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) return false
        val nativeEvent = keyEvent.nativeKeyEvent
        val keyCode = nativeEvent.keyCode
        return when (keyCode) {
            // Play / Pause & Toggle Controls
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_SPACE -> {
                if (showControls) {
                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                } else {
                    showControls = true
                }
                true
            }

            // UP / DOWN -> Change Channel (উপর-নিচে ক্লিক করলে চ্যানেল পরিবর্তন)
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> {
                switchChannel(-1)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                switchChannel(1)
                true
            }

            // LEFT -> Rewind / Seekbar Control (বাম পাশে ক্লিক করলে ভিডিও কন্ট্রোল বার/সিকবার রিওয়াইন্ড)
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                val effDuration = if (durationMs > 0) durationMs else exoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
                if (effDuration > 0) {
                    val target = maxOf(0L, currentPositionMs - 10000L)
                    exoPlayer.seekTo(target)
                    currentPositionMs = target
                    seekGestureTargetMs = target
                    seekGestureOffsetSec = -10
                    seekGestureKey = System.currentTimeMillis()
                    isSeekGestureActive = true
                    doubleTapSeekLeft = true
                    doubleTapSeekRight = false
                } else if (exoPlayer.isCurrentMediaItemSeekable) {
                    val cur = exoPlayer.currentPosition
                    val target = maxOf(0L, cur - 10000L)
                    exoPlayer.seekTo(target)
                    currentPositionMs = target
                    doubleTapSeekLeft = true
                    doubleTapSeekRight = false
                } else {
                    doubleTapSeekLeft = true
                    doubleTapSeekRight = false
                }
                showControls = true
                true
            }

            // RIGHT -> Forward / Seekbar Control (ডান পাশে ক্লিক করলে ভিডিও কন্ট্রোল বার/সিকবার ফরোয়ার্ড)
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                val effDuration = if (durationMs > 0) durationMs else exoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
                if (effDuration > 0) {
                    val target = minOf(effDuration, currentPositionMs + 10000L)
                    exoPlayer.seekTo(target)
                    currentPositionMs = target
                    seekGestureTargetMs = target
                    seekGestureOffsetSec = 10
                    seekGestureKey = System.currentTimeMillis()
                    isSeekGestureActive = true
                    doubleTapSeekRight = true
                    doubleTapSeekLeft = false
                } else if (exoPlayer.isCurrentMediaItemSeekable) {
                    val cur = exoPlayer.currentPosition
                    val target = cur + 10000L
                    exoPlayer.seekTo(target)
                    currentPositionMs = target
                    doubleTapSeekRight = true
                    doubleTapSeekLeft = false
                } else {
                    doubleTapSeekRight = true
                    doubleTapSeekLeft = false
                }
                showControls = true
                true
            }

            // Media Buttons
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                exoPlayer.play()
                isPlaying = true
                true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                exoPlayer.pause()
                isPlaying = false
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                switchChannel(1)
                true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                switchChannel(-1)
                true
            }

            // Remote Number Keys (0-9) to directly jump to channel
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> { handleNumberInput("0"); true }
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> { handleNumberInput("1"); true }
            KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> { handleNumberInput("2"); true }
            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> { handleNumberInput("3"); true }
            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> { handleNumberInput("4"); true }
            KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> { handleNumberInput("5"); true }
            KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> { handleNumberInput("6"); true }
            KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> { handleNumberInput("7"); true }
            KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> { handleNumberInput("8"); true }
            KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> { handleNumberInput("9"); true }

            // TV Menu / Info / Guide -> Toggle Channel Drawer
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_GUIDE, KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_PROG_RED, KeyEvent.KEYCODE_PROG_GREEN, KeyEvent.KEYCODE_PROG_YELLOW, KeyEvent.KEYCODE_PROG_BLUE -> {
                showQuickChannelDrawer = !showQuickChannelDrawer
                true
            }

            // Back / Escape
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (showQuickChannelDrawer) {
                    showQuickChannelDrawer = false
                    true
                } else if (isFullscreen) {
                    toggleFullscreen()
                    true
                } else {
                    onBack()
                    true
                }
            }
            else -> false
        }
    }

    // Formatting milliseconds to mm:ss
    fun formatTime(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun adjustBrightness(delta: Float) {
        currentBrightnessFraction = (currentBrightnessFraction + delta).coerceIn(0.01f, 1.0f)
        val lp = activity?.window?.attributes
        if (lp != null) {
            lp.screenBrightness = currentBrightnessFraction
            activity.window.attributes = lp
        }
        gestureBrightnessPercent = (currentBrightnessFraction * 100).toInt().coerceIn(1, 100)
        brightnessGestureKey = System.currentTimeMillis()
        isBrightnessGestureActive = true
        isVolumeGestureActive = false
        isSeekGestureActive = false
    }

    fun adjustVolume(delta: Float) {
        if (isMuted) {
            isMuted = false
            exoPlayer.volume = 1f
        }
        currentVolumeFraction = (currentVolumeFraction + delta).coerceIn(0f, 1f)
        val newVol = kotlin.math.round(currentVolumeFraction * maxAudioVolume).toInt().coerceIn(0, maxAudioVolume)
        try {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        } catch (_: Exception) {}
        gestureVolumePercent = (currentVolumeFraction * 100).toInt().coerceIn(0, 100)
        volumeGestureKey = System.currentTimeMillis()
        isVolumeGestureActive = true
        isBrightnessGestureActive = false
        isSeekGestureActive = false
    }

    fun adjustSeekDelta(deltaSec: Float) {
        val effDuration = if (durationMs > 0) durationMs else exoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
        if (effDuration > 0) {
            seekGestureOffsetSec += deltaSec.toInt()
            val target = (currentPositionMs + seekGestureOffsetSec * 1000L).coerceIn(0L, effDuration)
            seekGestureTargetMs = target
            seekGestureKey = System.currentTimeMillis()
            isSeekGestureActive = true
            isVolumeGestureActive = false
            isBrightnessGestureActive = false
        }
    }

    fun confirmSeek() {
        val effDuration = if (durationMs > 0) durationMs else exoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
        if (isSeekGestureActive && effDuration > 0) {
            exoPlayer.seekTo(seekGestureTargetMs)
            currentPositionMs = seekGestureTargetMs
            seekGestureOffsetSec = 0
        }
    }

    fun selectVideoQuality(quality: VideoQualityOption) {
        selectedVideoQuality = quality
        if (quality.height <= 0) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearVideoSizeConstraints()
                .build()
            android.widget.Toast.makeText(context, "ভিডিও কোয়ালিটি: অটো অ্যাডাপটিভ (Auto)", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(Int.MAX_VALUE, quality.height)
                .setMinVideoSize(0, quality.height)
                .build()
            android.widget.Toast.makeText(context, "ভিডিও কোয়ালিটি: ${quality.label}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun selectAudioTrack(audio: AudioTrackOption) {
        selectedAudioTrack = audio
        if (audio.language.isNotBlank()) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setPreferredAudioLanguage(audio.language)
                .build()
        }
        android.widget.Toast.makeText(context, "অডিও ভাষা: ${audio.displayName}", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun selectSubtitle(sub: SubtitleTrackOption) {
        selectedSubtitle = sub
        if (sub.isOff) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            android.widget.Toast.makeText(context, "সাবটাইটেল বন্ধ করা হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(sub.language)
                .build()
            android.widget.Toast.makeText(context, "সাবটাইটেল: ${sub.displayName}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val playerModifier = modifier
        .focusRequester(focusRequester)
        .focusable()
        .onKeyEvent { handleRemoteKeyEvent(it) }

    // Dialogs for Quality, Audio Track, and Subtitle Selection
    if (showQualityDialog) {
        VideoQualitySelectionDialog(
            currentSelection = selectedVideoQuality,
            availableOptions = availableVideoQualities,
            onSelect = { selectVideoQuality(it) },
            onDismiss = { showQualityDialog = false }
        )
    }

    if (showAudioDialog) {
        AudioTrackSelectionDialog(
            currentSelection = selectedAudioTrack,
            availableOptions = availableAudioTracks,
            onSelect = { selectAudioTrack(it) },
            onDismiss = { showAudioDialog = false }
        )
    }

    if (showSubtitleDialog) {
        SubtitleSelectionDialog(
            currentSelection = selectedSubtitle,
            availableOptions = availableSubtitles,
            onSelect = { selectSubtitle(it) },
            onDismiss = { showSubtitleDialog = false }
        )
    }

    if (isFullscreen) {
        // FULLSCREEN LANDSCAPE VIEW (Edge-to-Edge)
        Box(
            modifier = playerModifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls },
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2) {
                                if (durationMs > 0) {
                                    exoPlayer.seekTo(maxOf(0L, currentPositionMs - 10000L))
                                    doubleTapSeekLeft = true
                                    doubleTapSeekRight = false
                                }
                            } else {
                                if (durationMs > 0) {
                                    exoPlayer.seekTo(minOf(durationMs, currentPositionMs + 10000L))
                                    doubleTapSeekRight = true
                                    doubleTapSeekLeft = false
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    var dragType = MxDragType.NONE
                    var dragStartX = 0f
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStartX = offset.x
                            dragType = MxDragType.NONE
                            val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
                            currentVolumeFraction = (curVol.toFloat() / maxAudioVolume.toFloat()).coerceIn(0f, 1f)
                            val curBright = activity?.window?.attributes?.screenBrightness ?: -1f
                            currentBrightnessFraction = if (curBright < 0f) 0.5f else curBright.coerceIn(0.01f, 1.0f)
                        },
                        onDragEnd = {
                            if (dragType == MxDragType.HORIZONTAL) {
                                confirmSeek()
                            }
                            dragType = MxDragType.NONE
                        },
                        onDragCancel = { dragType = MxDragType.NONE },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (dragType == MxDragType.NONE) {
                                if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)) {
                                    dragType = if (dragStartX < size.width / 2) MxDragType.VERTICAL_LEFT else MxDragType.VERTICAL_RIGHT
                                } else {
                                    dragType = MxDragType.HORIZONTAL
                                }
                            }

                            when (dragType) {
                                MxDragType.VERTICAL_LEFT -> {
                                    val deltaPercent = -dragAmount.y / (size.height * 0.75f)
                                    adjustBrightness(deltaPercent)
                                }
                                MxDragType.VERTICAL_RIGHT -> {
                                    val deltaPercent = -dragAmount.y / (size.height * 0.75f)
                                    adjustVolume(deltaPercent)
                                }
                                MxDragType.HORIZONTAL -> {
                                    val deltaSec = (dragAmount.x / (size.width * 0.4f)) * 45f
                                    adjustSeekDelta(deltaSec)
                                }
                                MxDragType.NONE -> {}
                            }
                        }
                    )
                }
        ) {
            if (useWebPlayer) {
                WebStreamPlayer(
                    embedUrl = currentUrl,
                    title = currentMedia.title,
                    modifier = Modifier.fillMaxSize(),
                    onDirectStreamDetected = { directStream ->
                        currentUrl = directStream
                        forceWebEngine = false
                    },
                    onClose = { toggleFullscreen() }
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            this.resizeMode = resizeMode
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            setKeepContentOnPlayerReset(true)
                            keepScreenOn = true
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { playerView ->
                        playerView.player = exoPlayer
                        playerView.resizeMode = resizeMode
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Buffering Indicator with NAFI TV Logo & Bengali Loading text
            if (isActuallyBuffering) {
                PlayerBufferingIndicator(
                    mediaTitle = currentMedia.title,
                    isInitialLoad = !hasStartedPlaying,
                    isCompact = false
                )
            }

            // MX Player Gesture HUD Overlays (Left: Brightness, Right: Volume, Center: Seek, DoubleTap Ripple)
            if (isBrightnessGestureActive) {
                MxPlayerBrightnessHud(
                    percent = gestureBrightnessPercent,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 32.dp)
                )
            }

            if (isVolumeGestureActive) {
                MxPlayerVolumeHud(
                    percent = gestureVolumePercent,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 32.dp)
                )
            }

            if (isSeekGestureActive) {
                MxPlayerSeekHud(
                    targetMs = seekGestureTargetMs,
                    durationMs = durationMs,
                    offsetSec = seekGestureOffsetSec,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (doubleTapSeekLeft) {
                MxDoubleTapRipple(isForward = false, modifier = Modifier.align(Alignment.CenterStart))
            }
            if (doubleTapSeekRight) {
                MxDoubleTapRipple(isForward = true, modifier = Modifier.align(Alignment.CenterEnd))
            }

            // TV Channel Switch OSD Banner (Appears when changing channel with Remote Up/Down or Numbers)
            if (showChannelOsd) {
                val list = if (playlist.isNotEmpty()) playlist else listOf(currentMedia)
                val curIdx = list.indexOfFirst { it.id == currentMedia.id || it.streamUrl == currentMedia.streamUrl }.coerceAtLeast(0)
                TvChannelOsdBanner(
                    media = currentMedia,
                    currentIndex = curIdx,
                    totalCount = list.size,
                    selectedServerName = servers.getOrNull(selectedServerIndex)?.name,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            // TV Remote Number Dialing Overlay
            if (remoteNumberBuffer.isNotEmpty()) {
                TvNumberDialOverlay(
                    dialedNumber = remoteNumberBuffer,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            // Error Overlay
            if (errorMessage != null) {
                FullscreenErrorOverlay(
                    message = errorMessage ?: "",
                    onRetry = {
                        errorMessage = null
                        exoPlayer.seekTo(0)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                )
            }

            // Controls Overlay
            if (showControls) {
                FullscreenControlsOverlay(
                    media = currentMedia,
                    currentVideoResolution = currentVideoResolution,
                    servers = servers,
                    selectedServerIndex = selectedServerIndex,
                    onSelectServer = { index ->
                        selectedServerIndex = index
                        currentUrl = servers[index].url
                        errorMessage = null
                    },
                    isPlaying = isPlaying,
                    isMuted = isMuted,
                    playbackSpeed = playbackSpeed,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    isDraggingSlider = isDraggingSlider,
                    sliderPosition = sliderPosition,
                    selectedVideoQuality = selectedVideoQuality,
                    availableVideoQualities = availableVideoQualities,
                    onOpenQualityDialog = { showQualityDialog = true },
                    selectedAudioTrack = selectedAudioTrack,
                    availableAudioTracks = availableAudioTracks,
                    onOpenAudioDialog = { showAudioDialog = true },
                    selectedSubtitle = selectedSubtitle,
                    availableSubtitles = availableSubtitles,
                    onOpenSubtitleDialog = { showSubtitleDialog = true },
                    onSeekRewind10 = {
                        val cur = exoPlayer.currentPosition.coerceAtLeast(0L)
                        val seekTarget = maxOf(0L, cur - 10000L)
                        exoPlayer.seekTo(seekTarget)
                        currentPositionMs = seekTarget
                    },
                    onSeekForward10 = {
                        val cur = exoPlayer.currentPosition.coerceAtLeast(0L)
                        val effDuration = if (durationMs > 0) durationMs else exoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
                        val seekTarget = if (effDuration > 0) minOf(effDuration, cur + 10000L) else (cur + 10000L)
                        exoPlayer.seekTo(seekTarget)
                        currentPositionMs = seekTarget
                    },
                    onSliderChange = {
                        isDraggingSlider = true
                        sliderPosition = it
                    },
                    onSliderChangeFinished = {
                        isDraggingSlider = false
                        val effDuration = if (durationMs > 0) durationMs else exoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
                        if (effDuration > 0) {
                            val seekTo = (sliderPosition * effDuration).toLong()
                            exoPlayer.seekTo(seekTo)
                            currentPositionMs = seekTo
                        }
                    },
                    onPlayPause = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    onToggleMute = {
                        isMuted = !isMuted
                        exoPlayer.volume = if (isMuted) 0f else 1f
                    },
                    onToggleSpeed = {
                        playbackSpeed = when (playbackSpeed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                    },
                    onPrevChannel = { switchChannel(-1) },
                    onNextChannel = { switchChannel(1) },
                    onToggleAspect = {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    onToggleFullscreen = { toggleFullscreen() },
                    onToggleChannelDrawer = { showQuickChannelDrawer = !showQuickChannelDrawer },
                    onClose = { toggleFullscreen() }
                )
            }

            // Quick Channel Drawer in Fullscreen
            if (showQuickChannelDrawer && playlist.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(260.dp)
                        .align(Alignment.CenterEnd)
                        .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "চ্যানেল তালিকা (${playlist.size})",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            IconButton(onClick = { showQuickChannelDrawer = false }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(playlist) { item ->
                                val isCurrent = item.id == currentMedia.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isBuffering = true
                                            currentMedia = item
                                            selectedServerIndex = 0
                                            val newServers = item.getAllServers()
                                            currentUrl = newServers.firstOrNull()?.url ?: item.streamUrl
                                            errorMessage = null
                                            onSelectMedia(item)
                                            // Drawer stays open as requested
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)) else null,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0xFF1E293B)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = item.logoUrl ?: "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=100",
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.title,
                                                    color = if (isCurrent) Color(0xFF00E5FF) else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (isCurrent) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "▶ PLAYING",
                                                        color = Color(0xFF00E5FF),
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                            Text(
                                                text = item.category,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // PORTRAIT EMBEDDED PLAYER VIEW (Screenshot 4 layout)
        Column(
            modifier = playerModifier
                .fillMaxSize()
                .background(Color(0xFF0B1120))
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "NAFI TV 24",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF00E5FF).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (currentMedia.type == com.example.model.MediaType.MOVIE) "Movies" else "Live TV",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            toggleFullscreen()
                            android.widget.Toast.makeText(context, "টিভি মোড (TV Mode) সক্রিয় করা হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Tv,
                            contentDescription = "TV Mode",
                            tint = if (isFullscreen) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = "Protected", tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            errorMessage = null
                            exoPlayer.seekTo(0)
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            }

            // Embedded 16:9 Video Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = { offset ->
                                val cur = exoPlayer.currentPosition.coerceAtLeast(0L)
                                val effDuration = if (durationMs > 0) durationMs else exoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
                                if (offset.x < size.width / 2) {
                                    val seekTarget = maxOf(0L, cur - 10000L)
                                    exoPlayer.seekTo(seekTarget)
                                    currentPositionMs = seekTarget
                                    doubleTapSeekLeft = true
                                    doubleTapSeekRight = false
                                } else {
                                    val seekTarget = if (effDuration > 0) minOf(effDuration, cur + 10000L) else (cur + 10000L)
                                    exoPlayer.seekTo(seekTarget)
                                    currentPositionMs = seekTarget
                                    doubleTapSeekRight = true
                                    doubleTapSeekLeft = false
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        var dragType = MxDragType.NONE
                        var dragStartX = 0f
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartX = offset.x
                                dragType = MxDragType.NONE
                                val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
                                currentVolumeFraction = (curVol.toFloat() / maxAudioVolume.toFloat()).coerceIn(0f, 1f)
                                val curBright = activity?.window?.attributes?.screenBrightness ?: -1f
                                currentBrightnessFraction = if (curBright < 0f) 0.5f else curBright.coerceIn(0.01f, 1.0f)
                            },
                            onDragEnd = {
                                if (dragType == MxDragType.HORIZONTAL) {
                                    confirmSeek()
                                }
                                dragType = MxDragType.NONE
                            },
                            onDragCancel = { dragType = MxDragType.NONE },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragType == MxDragType.NONE) {
                                    if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)) {
                                        dragType = if (dragStartX < size.width / 2) MxDragType.VERTICAL_LEFT else MxDragType.VERTICAL_RIGHT
                                    } else {
                                        dragType = MxDragType.HORIZONTAL
                                    }
                                }

                                when (dragType) {
                                    MxDragType.VERTICAL_LEFT -> {
                                        val deltaPercent = -dragAmount.y / (size.height * 0.75f)
                                        adjustBrightness(deltaPercent)
                                    }
                                    MxDragType.VERTICAL_RIGHT -> {
                                        val deltaPercent = -dragAmount.y / (size.height * 0.75f)
                                        adjustVolume(deltaPercent)
                                    }
                                    MxDragType.HORIZONTAL -> {
                                        val deltaSec = (dragAmount.x / (size.width * 0.4f)) * 45f
                                        adjustSeekDelta(deltaSec)
                                    }
                                    MxDragType.NONE -> Unit
                                }
                            }
                        )
                    }
            ) {
                if (useWebPlayer) {
                    WebStreamPlayer(
                        embedUrl = currentUrl,
                        title = currentMedia.title,
                        modifier = Modifier.fillMaxSize(),
                        onDirectStreamDetected = { directStream ->
                            currentUrl = directStream
                            forceWebEngine = false
                        },
                        onClose = onBack
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                this.resizeMode = resizeMode
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                setKeepContentOnPlayerReset(true)
                                keepScreenOn = true
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { playerView ->
                            playerView.player = exoPlayer
                            playerView.resizeMode = resizeMode
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Top bar overlay inside video player: Close (X) circle button + Server tag + Quality / Audio Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = servers.getOrNull(selectedServerIndex)?.name?.take(10) ?: "MAIN",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Quality quick badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { showQualityDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Rounded.HighQuality, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = selectedVideoQuality?.label ?: if (currentVideoResolution != null) currentVideoResolution ?: "AUTO" else "AUTO",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Audio track quick badge
                        if (availableAudioTracks.size > 1 || (availableAudioTracks.isNotEmpty() && availableAudioTracks.first().language.isNotEmpty())) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { showAudioDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.Audiotrack, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = selectedAudioTrack?.displayName?.take(6) ?: "Audio",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Gesture Overlays
                if (isBrightnessGestureActive) {
                    MxPlayerBrightnessHud(
                        percent = gestureBrightnessPercent,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                    )
                }

                if (isVolumeGestureActive) {
                    MxPlayerVolumeHud(
                        percent = gestureVolumePercent,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                    )
                }

                if (isSeekGestureActive && durationMs > 0) {
                    MxPlayerSeekHud(
                        targetMs = seekGestureTargetMs,
                        durationMs = durationMs,
                        offsetSec = seekGestureOffsetSec,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (doubleTapSeekLeft) {
                    MxDoubleTapRipple(isForward = false, modifier = Modifier.align(Alignment.CenterStart))
                }

                if (doubleTapSeekRight) {
                    MxDoubleTapRipple(isForward = true, modifier = Modifier.align(Alignment.CenterEnd))
                }

                // TV Channel Switch OSD Banner
                if (showChannelOsd) {
                    val list = if (playlist.isNotEmpty()) playlist else listOf(currentMedia)
                    val curIdx = list.indexOfFirst { it.id == currentMedia.id || it.streamUrl == currentMedia.streamUrl }.coerceAtLeast(0)
                    TvChannelOsdBanner(
                        media = currentMedia,
                        currentIndex = curIdx,
                        totalCount = list.size,
                        selectedServerName = servers.getOrNull(selectedServerIndex)?.name,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }

                // TV Remote Number Dialing Overlay
                if (remoteNumberBuffer.isNotEmpty()) {
                    TvNumberDialOverlay(
                        dialedNumber = remoteNumberBuffer,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }

                // Buffering Overlay with NAFI TV Logo & Bengali Loading text
                if (isActuallyBuffering) {
                    PlayerBufferingIndicator(
                        mediaTitle = currentMedia.title,
                        isInitialLoad = !hasStartedPlaying,
                        isCompact = true
                    )
                }

                // Error Overlay
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.padding(16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                                Text(text = errorMessage ?: "", color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                                Button(
                                    onClick = {
                                        errorMessage = null
                                        exoPlayer.seekTo(0)
                                        exoPlayer.prepare()
                                        exoPlayer.play()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Bottom Controls Bar inside Embedded Player (Screenshot 4 style)
                if (showControls) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            // Time stamps & Scrubber Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(currentPositionMs),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                val progressFraction = if (durationMs > 0) {
                                    (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                Slider(
                                    value = if (isDraggingSlider) sliderPosition else progressFraction,
                                    onValueChange = {
                                        isDraggingSlider = true
                                        sliderPosition = it
                                    },
                                    onValueChangeFinished = {
                                        isDraggingSlider = false
                                        val effDuration = if (durationMs > 0) durationMs else exoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
                                        if (effDuration > 0) {
                                            val seekTo = (sliderPosition * effDuration).toLong()
                                            exoPlayer.seekTo(seekTo)
                                            currentPositionMs = seekTo
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                        .height(18.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF00E5FF),
                                        activeTrackColor = Color(0xFF00E5FF),
                                        inactiveTrackColor = Color(0xFF334155)
                                    )
                                )

                                Text(
                                    text = if (durationMs > 0) formatTime(durationMs) else if (currentMedia.isLive) "LIVE" else "00:00",
                                    color = if (currentMedia.isLive && durationMs <= 0) Color.Red else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Interactive Controls Row (Screenshot 4 icons)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Mute / Volume
                                IconButton(
                                    onClick = {
                                        isMuted = !isMuted
                                        exoPlayer.volume = if (isMuted) 0f else 1f
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                                        contentDescription = "Mute",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Playback Speed
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.clickable {
                                        playbackSpeed = when (playbackSpeed) {
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            1.5f -> 2.0f
                                            else -> 1.0f
                                        }
                                        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                                    }
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }

                                // Previous Item
                                IconButton(
                                    onClick = { switchChannel(-1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Play / Pause (Large Center Cyan Button)
                                IconButton(
                                    onClick = {
                                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Next Item
                                IconButton(
                                    onClick = { switchChannel(1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Next",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Aspect Ratio (Tv/Crop)
                                IconButton(
                                    onClick = {
                                        resizeMode = when (resizeMode) {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AspectRatio,
                                        contentDescription = "Aspect Ratio",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Fullscreen Toggle (Auto Rotates to Landscape)
                                IconButton(
                                    onClick = { toggleFullscreen() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Below Player Content in Portrait: Info, Servers, and Media Switcher
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                val isMovieOrSeries = currentMedia.type == MediaType.MOVIE ||
                        currentMedia.type == MediaType.SERIES ||
                        currentMedia.tournament == "NAFI_OTT" ||
                        (currentMedia.category ?: "").contains("Movie", ignoreCase = true) ||
                        (currentMedia.category ?: "").contains("Cinema", ignoreCase = true) ||
                        (currentMedia.category ?: "").contains("OTT", ignoreCase = true) ||
                        (currentMedia.category ?: "").contains("Series", ignoreCase = true) ||
                        (currentMedia.category ?: "").contains("Film", ignoreCase = true) ||
                        (currentMedia.category ?: "").contains("Anime", ignoreCase = true) ||
                        (currentMedia.category ?: "").contains("Drama", ignoreCase = true) ||
                        (currentMedia.category ?: "").contains("প্লেলিস্ট", ignoreCase = true)

                // Title & Details Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentMedia.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isMovieOrSeries) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                listOf(Color(0xFFE50914), Color(0xFFFF3D00))
                                            )
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (currentMedia.tournament == "NAFI_OTT" || (currentMedia.category ?: "").contains("OTT", ignoreCase = true)) "NAFI OTT" else "MOVIE",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${currentMedia.category ?: "Cinema"} • ${if (!currentMedia.quality.isNullOrBlank() && currentMedia.quality != "Default") currentMedia.quality else "1080p Full HD"}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                if (currentMedia.isLive) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "LIVE NOW",
                                        color = Color.Red,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = "${currentMedia.category} • ${currentMedia.quality}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Fullscreen Button Shortcut
                    Button(
                        onClick = { toggleFullscreen() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ফুল স্ক্রিন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Multi-Server Chips (Only display when more than 1 server available)
                if (servers.size > 1) {
                    Text(
                        text = "সার্ভার নির্বাচন (${servers.size} টি সার্ভার উপলব্ধ):",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(servers) { index, server ->
                            val isSelected = selectedServerIndex == index
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable {
                                    if (selectedServerIndex != index) {
                                        selectedServerIndex = index
                                        currentUrl = server.url
                                        isBuffering = true
                                        errorMessage = null
                                    }
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else if (isMovieOrSeries) Icons.Rounded.Movie else Icons.Rounded.Dns,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Black else Color(0xFF00E5FF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = server.name,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Related / Other Items in Portrait Mode
                val isLiveEvent = (currentMedia.type == MediaType.LIVE_EVENT) &&
                        (!currentMedia.team1.isNullOrBlank() && !currentMedia.team2.isNullOrBlank())

                if (isLiveEvent) {
                    Text(
                        text = "🏆 অন্যান্য লাইভ ম্যাচসমূহ (${playlist.size}):",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playlist) { sport ->
                            val isCurrent = sport.id == currentMedia.id || sport.streamUrl == currentMedia.streamUrl
                            val matchFullTitle = when {
                                !sport.tournament.isNullOrBlank() -> sport.tournament!!
                                !sport.title.isNullOrBlank() && !sport.title.equals("Live Match", ignoreCase = true) -> sport.title
                                !sport.team1.isNullOrBlank() && !sport.team2.isNullOrBlank() -> "${sport.category} 🏏 || ${sport.team1} vs ${sport.team2}"
                                else -> "${sport.category} || Live Match"
                            }
                            val sportServers = sport.getAllServers()
                            val isLiveNow = sport.status.equals("Live Now", ignoreCase = true) || sport.isLive

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!isCurrent) {
                                            isBuffering = true
                                            currentMedia = sport
                                            selectedServerIndex = 0
                                            val newServers = sport.getAllServers()
                                            currentUrl = newServers.firstOrNull()?.url ?: sport.streamUrl
                                            errorMessage = null
                                            onSelectMedia(sport)
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) Color(0xFF1E3A8A).copy(alpha = 0.95f) else Color(0xFF131D33)
                                ),
                                border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF))
                                         else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Top Title Banner
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (sport.category.contains("Football", ignoreCase = true)) Icons.Rounded.SportsSoccer else Icons.Rounded.SportsCricket,
                                                contentDescription = null,
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = matchFullTitle,
                                                color = Color(0xFFE2E8F0),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 800),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Surface(
                                                color = if (isLiveNow) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isLiveNow) Color(0xFFEF4444).copy(alpha = 0.6f) else Color(0xFFF59E0B).copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = if (isLiveNow) "LIVE" else "UPCOMING",
                                                    color = if (isLiveNow) Color(0xFFEF4444) else Color(0xFFFBBF24),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Teams & Score Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Team 1
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = sport.team1Logo ?: "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=100",
                                                    contentDescription = sport.team1,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.size(28.dp).clip(CircleShape)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = sport.team1 ?: "Team 1",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Middle VS / Score
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0F172A),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = if (!sport.score1.isNullOrBlank() && !sport.score2.isNullOrBlank()) "${sport.score1} - ${sport.score2}" else "VS",
                                                color = Color(0xFF00E5FF),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        // Team 2
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = sport.team2 ?: "Team 2",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = sport.team2Logo ?: "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=100",
                                                    contentDescription = sport.team2,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.size(28.dp).clip(CircleShape)
                                                )
                                            }
                                        }
                                    }

                                    // Servers Chips & Play Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        LazyRow(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            items(sportServers) { srv ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF1E293B),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                                    modifier = Modifier.clickable {
                                                        isBuffering = true
                                                        currentMedia = sport.copy(streamUrl = srv.url)
                                                        currentUrl = srv.url
                                                        selectedServerIndex = sportServers.indexOf(srv).coerceAtLeast(0)
                                                        errorMessage = null
                                                        onSelectMedia(sport.copy(streamUrl = srv.url))
                                                    }
                                                ) {
                                                    Text(
                                                        text = srv.name,
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isCurrent) Color(0xFF00E5FF) else if (isLiveNow) Color(0xFFDC2626) else Color(0xFF2563EB),
                                            modifier = Modifier.clickable {
                                                if (!isCurrent) {
                                                    isBuffering = true
                                                    currentMedia = sport
                                                    selectedServerIndex = 0
                                                    val newServers = sport.getAllServers()
                                                    currentUrl = newServers.firstOrNull()?.url ?: sport.streamUrl
                                                    errorMessage = null
                                                    onSelectMedia(sport)
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isCurrent) Icons.Rounded.Equalizer else Icons.Rounded.PlayArrow,
                                                    contentDescription = null,
                                                    tint = if (isCurrent) Color.Black else Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = if (isCurrent) "Playing" else if (isLiveNow) "Watch Live" else "Play",
                                                    color = if (isCurrent) Color.Black else Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (isMovieOrSeries) {
                    // DEDICATED MOVIE & OTT PLATFORM DETAILS & POSTER GRID VIEW
                    val moviePlaylist = playlist.filter {
                        it.type == MediaType.MOVIE ||
                        it.type == MediaType.SERIES ||
                        it.tournament == "NAFI_OTT" ||
                        (it.category ?: "").contains("Movie", ignoreCase = true) ||
                        (it.category ?: "").contains("OTT", ignoreCase = true) ||
                        (it.category ?: "").contains("Series", ignoreCase = true) ||
                        (it.category ?: "").contains("Drama", ignoreCase = true) ||
                        (it.category ?: "").contains("Anime", ignoreCase = true)
                    }

                    // Synopsis / Storyline
                    if (!currentMedia.description.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "📖 কাহিনী সংক্ষেপ (Storyline):",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentMedia.description!!,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Text(
                        text = if (currentMedia.tournament == "NAFI_OTT" || (currentMedia.category ?: "").contains("OTT", ignoreCase = true))
                            "🎬 NAFI OTT প্ল্যাটফর্ম মুভি সমূহ (${moviePlaylist.size}):"
                        else
                            "🎬 আরও মুভি ও সিরিজসমূহ (${moviePlaylist.size}):",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(if (moviePlaylist.isNotEmpty()) moviePlaylist else playlist) { movie ->
                            val isCurrent = movie.id == currentMedia.id || movie.streamUrl == currentMedia.streamUrl
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!isCurrent) {
                                            isBuffering = true
                                            currentMedia = movie
                                            selectedServerIndex = 0
                                            val newServers = movie.getAllServers()
                                            currentUrl = newServers.firstOrNull()?.url ?: movie.streamUrl
                                            errorMessage = null
                                            onSelectMedia(movie)
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) Color(0xFF1E3A8A) else Color(0xFF131D33)
                                ),
                                border = if (isCurrent)
                                    androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF))
                                else
                                    androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Movie Vertical Poster Box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(135.dp)
                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                            .background(Color(0xFF1E293B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!movie.logoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = movie.logoUrl,
                                                contentDescription = movie.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Movie,
                                                contentDescription = null,
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        // Quality / OTT Badge on top left
                                        Surface(
                                            color = if (movie.tournament == "NAFI_OTT" || (movie.category ?: "").contains("OTT", ignoreCase = true)) Color(0xFFE50914) else Color(0xFF0F172A).copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(bottomEnd = 6.dp),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                text = if (movie.tournament == "NAFI_OTT") "OTT" else (movie.quality ?: "HD"),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Currently Playing Indicator Overlay
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF00E5FF).copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF00E5FF)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.Black,
                                                        modifier = Modifier
                                                            .padding(6.dp)
                                                            .size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Movie Title & Category below poster
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = movie.title,
                                            color = if (isCurrent) Color(0xFF00E5FF) else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = movie.category ?: "Cinema",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Live TV Channels Grid (Clean, symmetrical, and uniform)
                    Text(
                        text = "📺 অন্যান্য টিভি চ্যানেলসমূহ (${playlist.size}):",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playlist) { item ->
                            val isCurrent = item.id == currentMedia.id || item.streamUrl == currentMedia.streamUrl
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clickable {
                                        isBuffering = true
                                        currentMedia = item
                                        selectedServerIndex = 0
                                        val newServers = item.getAllServers()
                                        currentUrl = newServers.firstOrNull()?.url ?: item.streamUrl
                                        errorMessage = null
                                        onSelectMedia(item)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                                ),
                                border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.8f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!item.logoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = item.logoUrl,
                                                contentDescription = item.title,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.size(38.dp).clip(CircleShape)
                                            )
                                        } else {
                                            val initials = item.title.take(3).uppercase()
                                            Text(
                                                text = initials,
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.title,
                                            color = if (isCurrent) Color(0xFF00E5FF) else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF0F172A),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF334155))
                                    ) {
                                        Text(
                                            text = item.country ?: item.category.take(8).ifBlank { "Live" },
                                            color = if (isCurrent) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// Fullscreen & Embedded Buffering / Loading Overlay Components
// (User Requested: কিছু চ্যানেল প্লে হতে সময় নেয় সেই গুলো প্লে হবার আগে আ্যপ লোগো দেখাবেন লোডিং হচ্ছে এই লেখা লোগোর নিচে থাকবে)
// ---------------------------------------------------------------------
@Composable
private fun PlayerBufferingIndicator(
    mediaTitle: String = "",
    isInitialLoad: Boolean = true,
    isCompact: Boolean = false
) {
    if (isInitialLoad) {
        // Initial channel load: Show branded animated NAFI TV logo with "লোডিং হচ্ছে..."
        PlayerBufferingLogoOverlay(mediaTitle = mediaTitle, isCompact = isCompact)
    } else {
        // Mid-stream micro buffering: Transparent, non-intrusive glowing circular spinner
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0F172A).copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.7f)),
                shadowElevation = 8.dp,
                modifier = Modifier.size(if (isCompact) 48.dp else 60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(if (isCompact) 26.dp else 34.dp),
                        color = Color(0xFF00E5FF),
                        strokeWidth = 3.dp,
                        trackColor = Color(0xFF334155)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerBufferingLogoOverlay(
    mediaTitle: String = "",
    isCompact: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "player_buffering_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.92f)),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF00E5FF).copy(alpha = glowAlpha),
                        Color(0xFF3B82F6).copy(alpha = glowAlpha),
                        Color(0xFFA855F7).copy(alpha = glowAlpha * 0.7f)
                    )
                )
            ),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (isCompact) 20.dp else 32.dp,
                    vertical = if (isCompact) 14.dp else 22.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
            ) {
                // Animated NAFI TV Logo with Glowing Ambient Aura
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(if (isCompact) 56.dp else 76.dp)
                ) {
                    // Outer Soft Halo
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 56.dp else 76.dp)
                            .scale(pulseScale * 1.12f)
                            .alpha(glowAlpha * 0.4f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF3B82F6), Color.Transparent)
                                )
                            )
                    )

                    // Sharp Logo
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "NAFI TV Logo",
                        modifier = Modifier
                            .size(if (isCompact) 48.dp else 64.dp)
                            .scale(pulseScale)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                // Bengali "লোডিং হচ্ছে..." text (As requested: "লোডিং হচ্ছে এই লেখা লোগোর নিচে থাকবে")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "লোডিং হচ্ছে...",
                        color = Color.White,
                        fontSize = if (isCompact) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    if (mediaTitle.isNotBlank()) {
                        Text(
                            text = mediaTitle,
                            color = Color(0xFF00E5FF),
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Smooth Glowing Progress Bar
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(if (isCompact) 100.dp else 130.dp)
                        .height(3.dp)
                        .clip(CircleShape),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF334155)
                )

                // Subtitle Badge
                Text(
                    text = "NAFI TV 24 • অটো অ্যাডাপটিভ স্ট্রিমিং",
                    color = Color(0xFF94A3B8),
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun FullscreenErrorOverlay(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
                Text(text = message, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("পুনরায় চেষ্টা করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FullscreenControlsOverlay(
    media: AppMediaItem,
    currentVideoResolution: String? = null,
    servers: List<StreamServer>,
    selectedServerIndex: Int,
    onSelectServer: (Int) -> Unit,
    isPlaying: Boolean,
    isMuted: Boolean,
    playbackSpeed: Float,
    currentPositionMs: Long,
    durationMs: Long,
    isDraggingSlider: Boolean,
    sliderPosition: Float,
    selectedVideoQuality: VideoQualityOption?,
    availableVideoQualities: List<VideoQualityOption>,
    onOpenQualityDialog: () -> Unit,
    selectedAudioTrack: AudioTrackOption?,
    availableAudioTracks: List<AudioTrackOption>,
    onOpenAudioDialog: () -> Unit,
    selectedSubtitle: SubtitleTrackOption?,
    availableSubtitles: List<SubtitleTrackOption>,
    onOpenSubtitleDialog: () -> Unit,
    onSeekRewind10: () -> Unit,
    onSeekForward10: () -> Unit,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeed: () -> Unit,
    onPrevChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onToggleAspect: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleChannelDrawer: () -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Fullscreen", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = media.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = if (currentVideoResolution != null) "${media.category} • $currentVideoResolution" else "${media.category} • AUTO (${media.quality})",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            // Top action buttons: Quality, Audio Track, Subtitles, Aspect Ratio, Channel List, Exit
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quality Selector Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { onOpenQualityDialog() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(Icons.Rounded.HighQuality, contentDescription = "Quality", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedVideoQuality?.label ?: if (availableVideoQualities.isNotEmpty()) "Quality" else "Auto",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Audio Language Button
                if (availableAudioTracks.size > 1 || (availableAudioTracks.isNotEmpty() && availableAudioTracks.first().language.isNotEmpty())) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onOpenAudioDialog() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Rounded.Audiotrack, contentDescription = "Audio", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedAudioTrack?.displayName?.take(10) ?: "Audio",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Subtitle / Closed Caption Button
                if (availableSubtitles.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onOpenSubtitleDialog() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Rounded.Subtitles, contentDescription = "Subtitles", tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedSubtitle?.isOff == false) selectedSubtitle.displayName.take(8) else "CC",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick Channel Drawer Toggle
                IconButton(onClick = onToggleChannelDrawer) {
                    Icon(Icons.Rounded.FormatListBulleted, contentDescription = "Channel List", tint = Color(0xFF00E5FF))
                }

                // Aspect Ratio
                IconButton(onClick = onToggleAspect) {
                    Icon(Icons.Rounded.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                }

                // Exit Fullscreen
                IconButton(onClick = onToggleFullscreen) {
                    Icon(Icons.Rounded.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color(0xFF00E5FF))
                }
            }
        }

        // Center Quick Skip Buttons
        if (durationMs > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 60.dp),
                horizontalArrangement = Arrangement.spacedBy(80.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSeekRewind10,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                IconButton(
                    onClick = onSeekForward10,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Bottom Controls Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Servers Row in Fullscreen
                if (servers.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(servers) { index, server ->
                            val isSelected = selectedServerIndex == index
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B),
                                modifier = Modifier.clickable { onSelectServer(index) }
                            ) {
                                Text(
                                    text = server.name,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Progress Scrubber
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val formatTime = { ms: Long ->
                        if (ms <= 0L) "00:00"
                        else String.format("%02d:%02d", (ms / 1000) / 60, (ms / 1000) % 60)
                    }

                    Text(
                        text = formatTime(currentPositionMs),
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val progressFraction = if (durationMs > 0) {
                        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = if (isDraggingSlider) sliderPosition else progressFraction,
                        onValueChange = onSliderChange,
                        onValueChangeFinished = onSliderChangeFinished,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .height(20.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    Text(
                        text = if (durationMs > 0) formatTime(durationMs) else if (media.isLive) "LIVE" else "00:00",
                        color = if (media.isLive && durationMs <= 0) Color.Red else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mute Toggle
                    IconButton(onClick = onToggleMute) {
                        Icon(
                            imageVector = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }

                    // Speed
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.clickable { onToggleSpeed() }
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Prev Channel
                    IconButton(onClick = onPrevChannel) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous Channel", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    // 10s Rewind
                    IconButton(onClick = onSeekRewind10) {
                        Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(26.dp))
                    }

                    // Center Play/Pause
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // 10s Forward
                    IconButton(onClick = onSeekForward10) {
                        Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(26.dp))
                    }

                    // Next Channel
                    IconButton(onClick = onNextChannel) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Next Channel", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    // Aspect Ratio
                    IconButton(onClick = onToggleAspect) {
                        Icon(Icons.Rounded.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                    }

                    // Fullscreen exit
                    IconButton(onClick = onToggleFullscreen) {
                        Icon(Icons.Rounded.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color(0xFF00E5FF))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MX PLAYER GESTURE HUDs & DIALOGS
// -------------------------------------------------------------

@Composable
fun MxPlayerVolumeHud(percent: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (percent == 0) Icons.Rounded.VolumeMute else if (percent < 50) Icons.Rounded.VolumeDown else Icons.Rounded.VolumeUp,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(26.dp)
            )
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .clip(CircleShape),
                color = Color(0xFF00E5FF),
                trackColor = Color(0xFF334155)
            )
            Text(
                text = "ভলিউম $percent%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MxPlayerBrightnessHud(percent: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFACC15).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.BrightnessHigh,
                contentDescription = null,
                tint = Color(0xFFFACC15),
                modifier = Modifier.size(26.dp)
            )
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .clip(CircleShape),
                color = Color(0xFFFACC15),
                trackColor = Color(0xFF334155)
            )
            Text(
                text = "ব্রাইটনেস $percent%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MxPlayerSeekHud(
    targetMs: Long,
    durationMs: Long,
    offsetSec: Int,
    modifier: Modifier = Modifier
) {
    val formatTime = { ms: Long ->
        if (ms <= 0L) "00:00"
        else String.format("%02d:%02d", (ms / 1000) / 60, (ms / 1000) % 60)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (offsetSec >= 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = "${if (offsetSec >= 0) "+$offsetSec" else "$offsetSec"} সেকেন্ড",
                    color = Color(0xFF00E5FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatTime(targetMs)} / ${formatTime(durationMs)}",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun MxDoubleTapRipple(isForward: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(horizontal = 40.dp),
        shape = CircleShape,
        color = Color(0xFF00E5FF).copy(alpha = 0.25f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isForward) Icons.Rounded.Forward10 else Icons.Rounded.Replay10,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = if (isForward) "+10s" else "-10s",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VideoQualitySelectionDialog(
    currentSelection: VideoQualityOption?,
    availableOptions: List<VideoQualityOption>,
    onSelect: (VideoQualityOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HighQuality, contentDescription = null, tint = Color(0xFF00E5FF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ভিডিও কোয়ালিটি নির্বাচন করুন", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Auto / Adaptive option
                val isAutoSelected = currentSelection == null || currentSelection.height <= 0
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAutoSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF0F172A),
                    border = if (isAutoSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(VideoQualityOption(label = "অটো (Auto Adapt)", height = 0, bitrate = 0, isAuto = true))
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("অটো অ্যাডাপটিভ (Auto)", color = if (isAutoSelected) Color(0xFF00E5FF) else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("ইন্টারনেট স্পিড অনুযায়ী স্বয়ংক্রিয় অ্যাডজাস্ট", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        if (isAutoSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Explicit heights
                availableOptions.forEach { opt ->
                    val isSelected = currentSelection?.height == opt.height
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF0F172A),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(opt)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(opt.label, color = if (isSelected) Color(0xFF00E5FF) else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (opt.bitrate > 0) {
                                    Text("${opt.bitrate / 1000} kbps", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = Color(0xFF00E5FF))
            }
        }
    )
}

@Composable
fun AudioTrackSelectionDialog(
    currentSelection: AudioTrackOption?,
    availableOptions: List<AudioTrackOption>,
    onSelect: (AudioTrackOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Audiotrack, contentDescription = null, tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(8.dp))
                Text("অডিও ভাষা পরিবর্তন করুন", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (availableOptions.isEmpty()) {
                    Text("এই ভিডিওতে অন্য কোনো অডিও ট্র্যাক নেই। ডিফল্ট অডিও চালু রয়েছে।", color = Color(0xFF94A3B8), fontSize = 12.sp)
                } else {
                    availableOptions.forEach { opt ->
                        val isSelected = currentSelection?.groupIndex == opt.groupIndex && currentSelection?.trackIndex == opt.trackIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF0F172A),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(opt)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(opt.displayName, color = if (isSelected) Color(0xFF38BDF8) else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("ভাষা কোড: ${opt.language.ifBlank { "Default" }}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                if (isSelected) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = Color(0xFF38BDF8))
            }
        }
    )
}

@Composable
fun SubtitleSelectionDialog(
    currentSelection: SubtitleTrackOption?,
    availableOptions: List<SubtitleTrackOption>,
    onSelect: (SubtitleTrackOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = Color(0xFFA855F7))
                Spacer(modifier = Modifier.width(8.dp))
                Text("সাবটাইটেল নির্বাচন করুন", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Off option
                val isOffSelected = currentSelection == null || currentSelection.isOff
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOffSelected) Color(0xFFA855F7).copy(alpha = 0.2f) else Color(0xFF0F172A),
                    border = if (isOffSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA855F7)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(SubtitleTrackOption(displayName = "বন্ধ (Off)", isOff = true))
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("সাবটাইটেল বন্ধ (Off)", color = if (isOffSelected) Color(0xFFA855F7) else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (isOffSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Available subtitles
                availableOptions.forEach { opt ->
                    val isSelected = !isOffSelected && currentSelection?.groupIndex == opt.groupIndex && currentSelection?.trackIndex == opt.trackIndex
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFFA855F7).copy(alpha = 0.2f) else Color(0xFF0F172A),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA855F7)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(opt)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(opt.displayName, color = if (isSelected) Color(0xFFA855F7) else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = Color(0xFFA855F7))
            }
        }
    )
}

@Composable
fun TvChannelOsdBanner(
    media: com.example.model.MediaItem,
    currentIndex: Int,
    totalCount: Int,
    selectedServerName: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.8f)),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Channel Logo
                AsyncImage(
                    model = media.logoUrl ?: "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=100",
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF00E5FF)
                        ) {
                            Text(
                                text = if (totalCount > 0) "CH ${currentIndex + 1}/$totalCount" else "LIVE",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (media.category.isNotBlank()) {
                            Text(
                                text = media.category,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = media.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (selectedServerName != null) {
                        Text(
                            text = "সার্ভার: $selectedServerName",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Remote hint legend bar
            Row(
                modifier = Modifier
                    .background(Color(0xFF1E293B).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "▲ ▼ চ্যানেল পরিবর্তন  •  ◀ ▶ সিকবার  •  OK প্লে/পজ  •  MENU চ্যানেল তালিকা",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun TvNumberDialOverlay(
    dialedNumber: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(24.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.Tv, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(28.dp))
            Column {
                Text(
                    text = "চ্যানেল নং [ $dialedNumber ]",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "চ্যানেলে যাওয়া হচ্ছে...",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp
                )
            }
        }
    }
}


