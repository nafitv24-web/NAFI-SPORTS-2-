package com.example.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.MediaRepository
import com.example.model.ActiveUserInfo
import com.example.model.AppNotification
import com.example.model.AppUserAnalytics
import com.example.model.AppUpdateInfo
import com.example.model.CloudStreamRepo
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.MovieProvider
import com.example.model.NotificationType
import com.example.model.PlaylistInfo
import com.example.model.StreamServer
import com.example.player.VideoPlayerScreen
import com.example.ui.AppUpdateDialog
import com.example.ui.MovieBrowserScreen
import com.example.ui.NotificationCenterDialog
import com.example.util.NotificationHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val englishLabel: String) {
    EVENTS("‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö", "Events"),
    LIVE_TV("‡¶ü‡¶ø‡¶≠‡¶ø", "Live TV"),
    MOVIES("‡¶Æ‡ßÅ‡¶≠‡¶ø", "Movies"),
    PLAYLIST("‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü", "Playlist"),
    MENU("‡¶Æ‡ßá‡¶®‡ßÅ", "Menu")
}

enum class AdminTab(val label: String) {
    ANALYTICS("üë• ‡¶á‡¶â‡¶ú‡¶æ‡¶∞ ‡¶ì ‡¶ü‡ßç‡¶∞‡¶æ‡¶´‡¶ø‡¶ï"),
    TICKER("‡¶¨‡ßç‡¶∞‡ßá‡¶ï‡¶ø‡¶Ç ‡¶®‡¶ø‡¶â‡¶ú ‡¶¨‡¶æ‡¶∞"),
    CHANNELS("Live TV Channels"),
    MOVIES("Movies"),
    PLAYLISTS("Playlists"),
    SPORTS("Sports Matches"),
    BROADCAST("‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶® ‡¶™‡¶æ‡¶†‡¶æ‡¶®"),
    REPOSITORIES("CloudStream Repos"),
    APP_UPDATE("App Updates"),
    FIREBASE("Firebase Cloud")
}

@Composable
fun NafiTvMainApp(
    deepLinkRepoUrl: String? = null,
    onClearDeepLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val repository = remember { MediaRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(AppTab.EVENTS) }
    var selectedMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var activePlaybackPlaylist by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isTvMode by remember { mutableStateOf(false) }
    var activeUserMode by remember { mutableStateOf<AppUserMode?>(null) }
    var isAdminViewActive by remember { mutableStateOf(false) }
    var activeMovieBrowserProvider by remember { mutableStateOf<MovieProvider?>(null) }
    var isExtensionsManagementActive by remember { mutableStateOf(false) }
    var cloudStreamRepos by remember { mutableStateOf(repository.getSavedCloudStreamRepos()) }
    var allMovieProviders by remember { mutableStateOf(repository.getAllMovieProviders()) }

    // Deep link repository handler
    LaunchedEffect(deepLinkRepoUrl) {
        if (!deepLinkRepoUrl.isNullOrBlank()) {
            Toast.makeText(context, "‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø ‡¶™‡ßç‡¶∞‡¶∏‡ßá‡¶∏ ‡¶ï‡¶∞‡¶æ ‡¶π‡¶ö‡ßç‡¶õ‡ßá...", Toast.LENGTH_SHORT).show()
            val result = repository.installExtensionFromUrl(deepLinkRepoUrl)
            if (result.first) {
                cloudStreamRepos = repository.getSavedCloudStreamRepos()
                allMovieProviders = repository.getAllMovieProviders()
                Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                currentTab = AppTab.MOVIES
            } else {
                Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
            }
            onClearDeepLink()
        }
    }

    // App Exit Confirmation State
    var showExitConfirmationDialog by remember { mutableStateOf(false) }

    // Synchronize screen orientation based on selected mode
    LaunchedEffect(activeUserMode, isTvMode) {
        when (activeUserMode) {
            AppUserMode.REMOTE -> {
                isTvMode = true
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            AppUserMode.MOBILE -> {
                isTvMode = false
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            null -> {
                // On Welcome / Mode Selection Screen
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // App Update State
    var availableUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Notification Center State
    var notificationsList by remember { mutableStateOf(repository.getStoredNotifications()) }
    var showNotificationCenterDialog by remember { mutableStateOf(false) }

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> }
    )

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        NotificationHelper.initNotificationChannel(context)
    }

    // State lists - initialize with saved custom streams (no hardcoded channels)
    var sportsList by remember {
        val deleted = repository.getDeletedIds()
        val customSports = repository.getCustomStreams().filter { it.type == MediaType.LIVE_EVENT }.filterNot { deleted.contains(it.id) }
        mutableStateOf(customSports)
    }
    var liveTvList by remember {
        val deleted = repository.getDeletedIds()
        val customTv = repository.getCustomStreams().filter { it.type == MediaType.LIVE_TV }.filterNot { deleted.contains(it.id) }
        mutableStateOf(customTv)
    }
    var moviesList by remember {
        val deleted = repository.getDeletedIds()
        val customMov = repository.getCustomStreams().filter { it.type == MediaType.MOVIE || it.type == MediaType.SERIES }.filterNot { deleted.contains(it.id) }
        mutableStateOf(customMov)
    }
    var playlistsList by remember { mutableStateOf(repository.getInitialPlaylists() + repository.getCustomPlaylists()) }
    var adminPlaylistsList by remember { mutableStateOf(repository.getInitialPlaylists() + repository.getAdminPlaylists()) }
    var customList by remember { mutableStateOf(repository.getCustomStreams()) }
    var m3uList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var favoriteIds by remember { mutableStateOf(repository.getFavoriteIds()) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun checkForUpdates(isManualCheck: Boolean = false) {
        coroutineScope.launch {
            try {
                val updateInfo = repository.fetchAppUpdateInfo()
                if (updateInfo != null && com.example.util.AppUpdateHelper.isUpdateAvailable(updateInfo)) {
                    availableUpdateInfo = updateInfo
                    if (isManualCheck || updateInfo.isForceUpdate || !repository.isUpdateDismissed(updateInfo.versionCode, updateInfo.versionName)) {
                        showUpdateDialog = true
                    }
                } else {
                    if (isManualCheck) {
                        Toast.makeText(context, "‡¶Ü‡¶™‡¶®‡¶æ‡¶∞ ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™‡¶ü‡¶ø ‡¶≤‡ßá‡¶ü‡ßá‡¶∏‡ßç‡¶ü ‡¶≠‡¶æ‡¶∞‡ßç‡¶∏‡¶®‡ßá ‡¶Ü‡¶õ‡ßá (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (isManualCheck) {
                    Toast.makeText(context, "‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ö‡ßá‡¶ï ‡¶¨‡ßç‡¶Ø‡¶∞‡ßç‡¶• ‡¶π‡ßü‡ßá‡¶õ‡ßá", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Auto-fetch data (Firebase Firestore + RTDB + Sports M3U + TV M3U + Playlists + App Updates)
    fun refreshAllData() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                val deleted = repository.getDeletedIds()

                // 0. Fetch latest remote App Config (Live TV, Sports, Movies M3U URLs) from Firebase
                try {
                    repository.fetchAppConfigFromFirebase()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 1. Parallel asynchronous fetching of Tapmad Sports, Sports M3U, Live TV M3U, Movies M3U and Firebase Cloud
                val tapmadSportsDeferred = async {
                    try {
                        repository.fetchTapmadSportsMatches().filterNot { deleted.contains(it.id) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val sportsM3uUrl = repository.getSavedSportsM3uUrl()
                val sportsM3uDeferred = async {
                    if (sportsM3uUrl.isNotBlank()) {
                        repository.parseM3uFromUrl(sportsM3uUrl).map {
                            it.copy(
                                type = MediaType.LIVE_EVENT,
                                isLive = true,
                                status = if (it.status.isBlank()) "LIVE" else it.status
                            )
                        }
                    } else emptyList()
                }

                val liveTvM3uUrl = repository.getSavedLiveTvM3uUrl()
                val liveTvM3uDeferred = async {
                    if (liveTvM3uUrl.isNotBlank()) {
                        repository.parseM3uFromUrl(liveTvM3uUrl).map { it.copy(type = MediaType.LIVE_TV) }
                    } else emptyList()
                }

                val moviesM3uUrl = repository.getSavedMoviesM3uUrl()
                val moviesM3uDeferred = async {
                    if (moviesM3uUrl.isNotBlank()) {
                        repository.parseM3uFromUrl(moviesM3uUrl).map {
                            it.copy(
                                type = MediaType.MOVIE,
                                tournament = "NAFI_OTT",
                                category = if (it.category.isBlank() || it.category == "Unknown") "NAFI OTT PLATFORM" else "NAFI OTT ‚Ä¢ ${it.category}"
                            )
                        }
                    } else emptyList()
                }

                val fbItemsDeferred = async { repository.fetchFromFirebase().filterNot { deleted.contains(it.id) } }
                val fbPlaylistsDeferred = async { repository.fetchPlaylistsFromFirebase() }
                val remoteNotifsDeferred = async {
                    try {
                        repository.fetchRemoteNotifications()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                // Await all parallel fetches
                val tapmadSports = tapmadSportsDeferred.await()
                val parsedSportsM3u = sportsM3uDeferred.await().filterNot { deleted.contains(it.id) }
                val parsedTvM3u = liveTvM3uDeferred.await().filterNot { deleted.contains(it.id) }
                val parsedMoviesM3u = moviesM3uDeferred.await().filterNot { deleted.contains(it.id) }
                val fbItems = fbItemsDeferred.await()
                val fbPlaylists = fbPlaylistsDeferred.await()
                remoteNotifsDeferred.await()

                // Update notifications list
                notificationsList = repository.getStoredNotifications()

                val fbSports = fbItems.filter { it.type == MediaType.LIVE_EVENT }
                val fbTv = fbItems.filter { it.type == MediaType.LIVE_TV }
                val fbMov = fbItems.filter { it.type == MediaType.MOVIE || it.type == MediaType.SERIES }

                val customStreams = repository.getCustomStreams().filterNot { deleted.contains(it.id) }
                val customSports = customStreams.filter { it.type == MediaType.LIVE_EVENT }
                val customTv = customStreams.filter { it.type == MediaType.LIVE_TV }
                val customMov = customStreams.filter { it.type == MediaType.MOVIE || it.type == MediaType.SERIES }

                // 2. Playlists: separate admin/cloud playlists and user-local playlists
                val adminPlaylists = repository.getAdminPlaylists().filterNot { deleted.contains(it.id) }.map { it.copy(isAdmin = true, isReadOnly = true) }
                val userPlaylists = repository.getUserPlaylists().filterNot { deleted.contains(it.id) }.map { it.copy(isAdmin = false, isReadOnly = false) }

                // Admin panel ONLY sees Firebase Cloud playlists + Admin saved playlists
                val adminOnlyPlaylists = (fbPlaylists + adminPlaylists)
                    .distinctBy { it.id }
                    .filterNot { deleted.contains(it.id) }
                    .map { it.copy(isAdmin = true, isReadOnly = true) }
                adminPlaylistsList = adminOnlyPlaylists

                // Users see everything: Cloud/Admin playlists + User private playlists
                val allPlaylists = (adminOnlyPlaylists + userPlaylists)
                    .distinctBy { it.id }
                    .filterNot { deleted.contains(it.id) }
                playlistsList = allPlaylists
                val playlistIds = allPlaylists.map { it.id }.toSet()

                // 3. Set distinct channel, sports & movie lists from Tapmad Events + Firebase Cloud + Sports M3U + Admin custom additions (Excluding Playlists)
                sportsList = (tapmadSports + customSports + fbSports + parsedSportsM3u)
                    .filterNot { it.id.startsWith("pl_") || playlistIds.contains(it.id) }
                    .distinctBy { it.id }

                liveTvList = (customTv + fbTv + parsedTvM3u)
                    .filterNot { it.id.startsWith("pl_") || playlistIds.contains(it.id) }
                    .distinctBy { it.id }

                moviesList = (customMov + fbMov + parsedMoviesM3u)
                    .filterNot { it.id.startsWith("pl_") || playlistIds.contains(it.id) }
                    .distinctBy { it.id }

                m3uList = (parsedSportsM3u + parsedTvM3u + parsedMoviesM3u)
                    .filterNot { it.id.startsWith("pl_") || playlistIds.contains(it.id) }
                    .distinctBy { it.id }

                // 4. Custom streams & favorites
                customList = customStreams
                favoriteIds = repository.getFavoriteIds()

                // 5. Sync CloudStream repositories & providers from Firebase
                try {
                    val fbRepos = repository.fetchCloudStreamReposFromFirebase()
                    if (fbRepos.isNotEmpty()) {
                        val localRepos = repository.getSavedCloudStreamRepos()
                        val merged = (fbRepos + localRepos).distinctBy { it.id }
                        repository.saveCloudStreamRepos(merged)
                        cloudStreamRepos = merged
                        allMovieProviders = repository.getAllMovieProviders()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 6. App update check
                checkForUpdates(isManualCheck = false)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshAllData()
    }

    if (activeUserMode == null) {
        ModeSelectionScreen(
            repository = repository,
            onSelectMobileMode = {
                isTvMode = false
                activeUserMode = AppUserMode.MOBILE
            },
            onSelectRemoteMode = {
                isTvMode = true
                activeUserMode = AppUserMode.REMOTE
            },
            onExitApp = {
                showExitConfirmationDialog = true
            }
        )
    } else if (selectedMediaItem != null) {
        val currentPlayList = if (activePlaybackPlaylist.isNotEmpty()) {
            activePlaybackPlaylist
        } else {
            when (currentTab) {
                AppTab.LIVE_TV -> (liveTvList + customList.filter { it.type == MediaType.LIVE_TV } + m3uList.filter { it.type == MediaType.LIVE_TV }).distinctBy { it.id }
                AppTab.MOVIES -> (moviesList + customList.filter { it.type == MediaType.MOVIE || it.type == MediaType.SERIES } + m3uList.filter { it.type == MediaType.MOVIE }).distinctBy { it.id }
                AppTab.EVENTS -> sportsList.distinctBy { it.id }
                AppTab.PLAYLIST -> (customList + m3uList).distinctBy { it.id }
                else -> (liveTvList + sportsList + moviesList + customList + m3uList).distinctBy { it.id }
            }
        }
        VideoPlayerScreen(
            mediaItem = selectedMediaItem!!,
            playlist = currentPlayList,
            isTvMode = isTvMode,
            onSelectMedia = { selectedMediaItem = it },
            onBack = { selectedMediaItem = null }
        )
    } else if (activeMovieBrowserProvider != null) {
        // IN-APP MOVIE & CLOUDSTREAM WEBSITE BROWSER WITH AD-SHIELD & STREAM DETECTOR
        MovieBrowserScreen(
            repository = repository,
            provider = activeMovieBrowserProvider!!,
            onClose = { activeMovieBrowserProvider = null },
            onPlayDirectMedia = { item ->
                activePlaybackPlaylist = listOf(item)
                selectedMediaItem = item
            }
        )
    } else if (isExtensionsManagementActive) {
        // CLOUDSTREAM EXTENSIONS & REPOSITORIES MANAGEMENT SCREEN
        ExtensionsManagementScreen(
            repository = repository,
            onBack = {
                isExtensionsManagementActive = false
                cloudStreamRepos = repository.getSavedCloudStreamRepos()
                allMovieProviders = repository.getAllMovieProviders()
                refreshAllData()
            },
            onOpenMovieBrowser = { provider ->
                activeMovieBrowserProvider = provider
                isExtensionsManagementActive = false
                currentTab = AppTab.MOVIES
                cloudStreamRepos = repository.getSavedCloudStreamRepos()
                allMovieProviders = repository.getAllMovieProviders()
            }
        )
    } else if (isAdminViewActive) {
        // FULLSCREEN ADMIN CONTROL APP (Exact UI from Screenshot 1 & 3)
        AdminControlAppScreen(
            repository = repository,
            sportsList = sportsList,
            liveTvList = liveTvList + customList.filter { it.type == MediaType.LIVE_TV },
            moviesList = moviesList + customList.filter { it.type == MediaType.MOVIE || it.type == MediaType.SERIES },
            playlistsList = adminPlaylistsList,
            cloudStreamRepos = cloudStreamRepos,
            movieProviders = allMovieProviders,
            onOpenMovieProvider = { activeMovieBrowserProvider = it },
            onExitAdmin = { isAdminViewActive = false },
            onDataChanged = {
                cloudStreamRepos = repository.getSavedCloudStreamRepos()
                allMovieProviders = repository.getAllMovieProviders()
                refreshAllData()
            }
        )
    } else {
        // Intercept back press when at root screens to return to Mode Selection Screen
        BackHandler {
            if (currentTab != AppTab.EVENTS) {
                currentTab = AppTab.EVENTS
            } else {
                activeUserMode = null
            }
        }

        if (isTvMode) {
            // TV MODE: Sidebar Menu Rail on Left + Content on Right (Matching uploaded screenshot)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020617))
            ) {
                // Left Vertical Navigation Rail
                Surface(
                    modifier = Modifier
                        .width(86.dp)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    color = Color(0xFF0B1328),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            var isFocused by remember { mutableStateOf(false) }
                            val tabScale by animateFloatAsState(
                                targetValue = if (isFocused) 1.05f else 1.0f,
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                label = "tvTabScale"
                            )
                            val containerColor by animateColorAsState(
                                targetValue = when {
                                    isFocused -> Color(0xFF1E3A8A).copy(alpha = 0.65f)
                                    isSelected -> Color(0xFF2563EB).copy(alpha = 0.35f)
                                    else -> Color.Transparent
                                },
                                animationSpec = tween(150),
                                label = "tvTabColor"
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = containerColor,
                                border = when {
                                    isFocused -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8))
                                    isSelected -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
                                    else -> null
                                },
                                shadowElevation = if (isFocused) 4.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(tabScale)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .focusable()
                                    .clickable { currentTab = tab }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when (tab) {
                                            AppTab.EVENTS -> Icons.Rounded.EmojiEvents
                                            AppTab.LIVE_TV -> Icons.Rounded.Tv
                                            AppTab.MOVIES -> Icons.Rounded.Movie
                                            AppTab.PLAYLIST -> Icons.Rounded.Folder
                                            AppTab.MENU -> Icons.Rounded.Menu
                                        },
                                        contentDescription = tab.englishLabel,
                                        tint = if (isFocused || isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = tab.englishLabel,
                                        color = if (isFocused || isSelected) Color.White else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                    if (isFocused || isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF38BDF8))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Content Area with Top Action Header
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .navigationBarsPadding()
                        .background(Color(0xFF020617))
                ) {
                    // Top Action Bar Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "NAFI TV Logo",
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                if (currentTab == AppTab.EVENTS) {
                                    Text(
                                        text = "Live Events",
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "NAFI TV 24",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444))
                                            )
                                        }
                                        Text(
                                            text = currentTab.englishLabel,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Action Quick Controls (Mobile / TV Toggle + Refresh)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Notification Bell Button (TV)
                                val tvUnreadNotifCount = remember(notificationsList) { notificationsList.count { !it.isRead } }
                                var isTvNotifFocused by remember { mutableStateOf(false) }
                                val tvNotifScale by animateFloatAsState(
                                    targetValue = if (isTvNotifFocused) 1.08f else 1.0f,
                                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                                    label = "tvNotifScale"
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isTvNotifFocused) Color(0xFF1E3A8A).copy(alpha = 0.6f) else if (tvUnreadNotifCount > 0) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                    border = when {
                                        isTvNotifFocused -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8))
                                        tvUnreadNotifCount > 0 -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                                        else -> null
                                    },
                                    shadowElevation = if (isTvNotifFocused) 4.dp else 0.dp,
                                    modifier = Modifier
                                        .scale(tvNotifScale)
                                        .onFocusChanged { isTvNotifFocused = it.isFocused }
                                        .focusable()
                                        .clickable { showNotificationCenterDialog = true }
                                ) {
                                    Box(modifier = Modifier.padding(7.dp)) {
                                        Icon(
                                            imageVector = if (tvUnreadNotifCount > 0) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                                            contentDescription = "Notifications",
                                            tint = if (isTvNotifFocused || tvUnreadNotifCount > 0) Color(0xFF38BDF8) else Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        if (tvUnreadNotifCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444))
                                                    .align(Alignment.TopEnd)
                                            )
                                        }
                                    }
                                }

                                // Refresh Button
                                var isRefreshFocused by remember { mutableStateOf(false) }
                                val refreshScale by animateFloatAsState(
                                    targetValue = if (isRefreshFocused) 1.08f else 1.0f,
                                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                                    label = "refreshScale"
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isRefreshFocused) Color(0xFF1E3A8A).copy(alpha = 0.6f) else Color(0xFF1E293B),
                                    border = if (isRefreshFocused) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)) else null,
                                    shadowElevation = if (isRefreshFocused) 4.dp else 0.dp,
                                    modifier = Modifier
                                        .scale(refreshScale)
                                        .onFocusChanged { isRefreshFocused = it.isFocused }
                                        .focusable()
                                        .clickable { refreshAllData() }
                                ) {
                                    Box(modifier = Modifier.padding(7.dp)) {
                                        if (isRefreshing) {
                                            CircularProgressIndicator(
                                                color = Color(0xFF00E5FF),
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = "Refresh",
                                                tint = if (isRefreshFocused) Color(0xFF00E5FF) else Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Main Content in TV Mode
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        when (currentTab) {
                            AppTab.EVENTS -> EventsScreen(
                                sports = sportsList.distinctBy { it.id },
                                favoriteIds = favoriteIds,
                                isTvMode = isTvMode,
                                onSelectMedia = {
                                    selectedMediaItem = it
                                    activePlaybackPlaylist = sportsList.distinctBy { sp -> sp.id }
                                },
                                onToggleFavorite = { id ->
                                    repository.toggleFavorite(id)
                                    favoriteIds = repository.getFavoriteIds()
                                }
                            )

                            AppTab.LIVE_TV -> LiveTvTabScreen(
                                channels = (liveTvList + customList.filter { it.type == MediaType.LIVE_TV } + m3uList.filter { it.type == MediaType.LIVE_TV }).distinctBy { it.id },
                                favoriteIds = favoriteIds,
                                isTvMode = isTvMode,
                                onSelectMedia = {
                                    selectedMediaItem = it
                                    activePlaybackPlaylist = (liveTvList + customList.filter { ch -> ch.type == MediaType.LIVE_TV } + m3uList.filter { ch -> ch.type == MediaType.LIVE_TV }).distinctBy { ch -> ch.id }
                                },
                                onToggleFavorite = { id ->
                                    repository.toggleFavorite(id)
                                    favoriteIds = repository.getFavoriteIds()
                                }
                            )

                            AppTab.MOVIES -> MoviesTabScreen(
                                movies = moviesList,
                                favoriteIds = favoriteIds,
                                isTvMode = isTvMode,
                                onSelectMedia = { item ->
                                    selectedMediaItem = item
                                    activePlaybackPlaylist = moviesList
                                },
                                onToggleFavorite = { id ->
                                    repository.toggleFavorite(id)
                                    favoriteIds = repository.getFavoriteIds()
                                }
                            )

                            AppTab.PLAYLIST -> PlaylistTabScreen(
                                playlists = playlistsList,
                                repository = repository,
                                isTvMode = isTvMode,
                                onSelectMedia = { item, playlist ->
                                    selectedMediaItem = item
                                    activePlaybackPlaylist = playlist
                                },
                                onPlaylistsChanged = { refreshAllData() }
                            )

                            AppTab.MENU -> MenuScreen(
                                repository = repository,
                                customList = customList,
                                onOpenAdminApp = { isAdminViewActive = true },
                                onOpenExtensionManager = { isExtensionsManagementActive = true },
                                onCheckForUpdates = { checkForUpdates(isManualCheck = true) },
                                availableUpdateInfo = availableUpdateInfo,
                                onPlayDirectStream = { url, title ->
                                    val directItem = MediaItem(
                                        id = "direct_${System.currentTimeMillis()}",
                                        title = title.ifBlank { "Direct Stream" },
                                        category = "Direct Stream",
                                        type = MediaType.LIVE_TV,
                                        streamUrl = url,
                                        isLive = true
                                    )
                                    selectedMediaItem = directItem
                                    activePlaybackPlaylist = listOf(directItem)
                                },
                                onM3uLoaded = { list ->
                                    m3uList = list
                                    currentTab = AppTab.PLAYLIST
                                },
                                onCustomAdded = { item ->
                                    repository.saveCustomStream(item)
                                    customList = repository.getCustomStreams()
                                    coroutineScope.launch {
                                        repository.pushToFirebase(item)
                                    }
                                    Toast.makeText(context, "‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶§‡¶æ‡¶≤‡¶ø‡¶ï‡¶æ‡ßü ‡¶Ø‡ßÅ‡¶ï‡ßç‡¶§ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                },
                                onResetDefaults = {
                                    repository.resetToDefaults()
                                    sportsList = repository.getInitialSports()
                                    liveTvList = repository.getInitialLiveTv()
                                    moviesList = repository.getInitialMoviesSeries()
                                    customList = emptyList()
                                    m3uList = emptyList()
                                    favoriteIds = emptySet()
                                    Toast.makeText(context, "‡¶°‡¶ø‡¶´‡¶≤‡ßç‡¶ü ‡¶∞‡¶ø‡¶∏‡ßá‡¶ü ‡¶∏‡¶´‡¶≤ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    // TV Non-Intrusive Banner Ad (Zero disturbance, collapsible, hidden during video play)
                    NonIntrusiveAdMobBanner(isTvMode = true)
                }
            }
        } else {
            // MOBILE MODE: Standard Scaffold with Top Bar + Content + Bottom Navigation Bar
            Scaffold(
                topBar = {
                    // Top Action Bar Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "NAFI TV Logo",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                if (currentTab == AppTab.EVENTS) {
                                    Text(
                                        text = "Live Events",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "NAFI TV 24",
                                                color = Color.White,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444))
                                            )
                                        }
                                        Text(
                                            text = currentTab.englishLabel,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Action Quick Controls
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Notification Bell Button (Mobile)
                                val mobUnreadNotifCount = remember(notificationsList) { notificationsList.count { !it.isRead } }
                                Surface(
                                    shape = CircleShape,
                                    color = if (mobUnreadNotifCount > 0) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E293B),
                                    border = if (mobUnreadNotifCount > 0) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
                                    modifier = Modifier.clickable { showNotificationCenterDialog = true }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (mobUnreadNotifCount > 0) Icons.Rounded.NotificationsActive else Icons.Rounded.Notifications,
                                            contentDescription = "‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶®",
                                            tint = if (mobUnreadNotifCount > 0) Color(0xFF38BDF8) else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        if (mobUnreadNotifCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444))
                                                    .align(Alignment.TopEnd)
                                            )
                                        }
                                    }
                                }

                                // Refresh Button
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.clickable { refreshAllData() }
                                ) {
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        if (isRefreshing) {
                                            CircularProgressIndicator(
                                                color = Color(0xFF00E5FF),
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = "Refresh",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                    ) {
                        // Mobile Non-Intrusive Banner Ad (Collapses automatically if failed or dismissed)
                        NonIntrusiveAdMobBanner(isTvMode = false)

                        NavigationBar(
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White
                        ) {
                            AppTab.values().forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = when (tab) {
                                                AppTab.EVENTS -> Icons.Rounded.EmojiEvents
                                                AppTab.LIVE_TV -> Icons.Rounded.Tv
                                                AppTab.MOVIES -> Icons.Rounded.Movie
                                                AppTab.PLAYLIST -> Icons.Rounded.Folder
                                                AppTab.MENU -> Icons.Rounded.Menu
                                            },
                                            contentDescription = tab.englishLabel,
                                            tint = if (selected) Color(0xFF00E5FF) else Color(0xFF94A3B8)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.englishLabel,
                                            color = if (selected) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color(0xFF1E293B)
                                    )
                                )
                            }
                        }
                    }
                },
                containerColor = Color(0xFF020617)
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentTab) {
                        AppTab.EVENTS -> EventsScreen(
                            sports = sportsList.distinctBy { it.id },
                            favoriteIds = favoriteIds,
                            isTvMode = isTvMode,
                            onSelectMedia = {
                                selectedMediaItem = it
                                activePlaybackPlaylist = sportsList.distinctBy { sp -> sp.id }
                            },
                            onToggleFavorite = { id ->
                                repository.toggleFavorite(id)
                                favoriteIds = repository.getFavoriteIds()
                            }
                        )

                        AppTab.LIVE_TV -> LiveTvTabScreen(
                            channels = (liveTvList + customList.filter { it.type == MediaType.LIVE_TV } + m3uList.filter { it.type == MediaType.LIVE_TV }).distinctBy { it.id },
                            favoriteIds = favoriteIds,
                            isTvMode = isTvMode,
                            onSelectMedia = {
                                selectedMediaItem = it
                                activePlaybackPlaylist = (liveTvList + customList.filter { ch -> ch.type == MediaType.LIVE_TV } + m3uList.filter { ch -> ch.type == MediaType.LIVE_TV }).distinctBy { ch -> ch.id }
                            },
                            onToggleFavorite = { id ->
                                repository.toggleFavorite(id)
                                favoriteIds = repository.getFavoriteIds()
                            }
                        )

                        AppTab.MOVIES -> MoviesTabScreen(
                            movies = moviesList,
                            favoriteIds = favoriteIds,
                            isTvMode = isTvMode,
                            onSelectMedia = { item ->
                                selectedMediaItem = item
                                activePlaybackPlaylist = moviesList
                            },
                            onToggleFavorite = { id ->
                                repository.toggleFavorite(id)
                                favoriteIds = repository.getFavoriteIds()
                            }
                        )

                        AppTab.PLAYLIST -> PlaylistTabScreen(
                            playlists = playlistsList,
                            repository = repository,
                            isTvMode = isTvMode,
                            onSelectMedia = { item, playlist ->
                                selectedMediaItem = item
                                activePlaybackPlaylist = playlist
                            },
                            onPlaylistsChanged = { refreshAllData() }
                        )

                        AppTab.MENU -> MenuScreen(
                            repository = repository,
                            customList = customList,
                            onOpenAdminApp = { isAdminViewActive = true },
                            onOpenExtensionManager = { isExtensionsManagementActive = true },
                            onCheckForUpdates = { checkForUpdates(isManualCheck = true) },
                            availableUpdateInfo = availableUpdateInfo,
                            onPlayDirectStream = { url, title ->
                                val directItem = MediaItem(
                                    id = "direct_${System.currentTimeMillis()}",
                                    title = title.ifBlank { "Direct Stream" },
                                    category = "Direct Stream",
                                    type = MediaType.LIVE_TV,
                                    streamUrl = url,
                                    isLive = true
                                )
                                selectedMediaItem = directItem
                                activePlaybackPlaylist = listOf(directItem)
                            },
                            onM3uLoaded = { list ->
                                m3uList = list
                                currentTab = AppTab.PLAYLIST
                            },
                            onCustomAdded = { item ->
                                repository.saveCustomStream(item)
                                customList = repository.getCustomStreams()
                                Toast.makeText(context, "‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶§‡¶æ‡¶≤‡¶ø‡¶ï‡¶æ‡ßü ‡¶Ø‡ßÅ‡¶ï‡ßç‡¶§ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                            },
                            onResetDefaults = {
                                repository.resetToDefaults()
                                sportsList = repository.getInitialSports()
                                liveTvList = repository.getInitialLiveTv()
                                moviesList = repository.getInitialMoviesSeries()
                                customList = emptyList()
                                m3uList = emptyList()
                                favoriteIds = emptySet()
                                Toast.makeText(context, "‡¶°‡¶ø‡¶´‡¶≤‡ßç‡¶ü ‡¶∞‡¶ø‡¶∏‡ßá‡¶ü ‡¶∏‡¶´‡¶≤ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // In-App Update Dialog (Beautiful Animated M3 Popup for Auto-Updating App)
    if (showUpdateDialog && availableUpdateInfo != null) {
        val info = availableUpdateInfo!!
        AppUpdateDialog(
            updateInfo = info,
            onDismiss = {
                repository.dismissUpdate(info.versionCode, info.versionName)
                showUpdateDialog = false
            }
        )
    }

    // Notification Center Dialog (In-App notification board for matches, channels, movies and announcements)
    if (showNotificationCenterDialog) {
        NotificationCenterDialog(
            notifications = notificationsList,
            isTvMode = isTvMode,
            onDismiss = { showNotificationCenterDialog = false },
            onSelectNotification = { notif ->
                repository.markNotificationAsRead(notif.id)
                notificationsList = repository.getStoredNotifications()
                showNotificationCenterDialog = false
                when (notif.type) {
                    NotificationType.LIVE_EVENT -> {
                        currentTab = AppTab.EVENTS
                        if (!notif.targetId.isNullOrBlank()) {
                            val matched = sportsList.find { it.id == notif.targetId }
                            if (matched != null) {
                                selectedMediaItem = matched
                                activePlaybackPlaylist = sportsList
                            }
                        }
                    }
                    NotificationType.LIVE_TV -> {
                        currentTab = AppTab.LIVE_TV
                        if (!notif.targetId.isNullOrBlank()) {
                            val allTv = (liveTvList + customList.filter { it.type == MediaType.LIVE_TV } + m3uList.filter { it.type == MediaType.LIVE_TV }).distinctBy { it.id }
                            val matched = allTv.find { it.id == notif.targetId }
                            if (matched != null) {
                                selectedMediaItem = matched
                                activePlaybackPlaylist = allTv
                            }
                        }
                    }
                    NotificationType.MOVIE -> {
                        currentTab = AppTab.MOVIES
                        if (!notif.targetId.isNullOrBlank()) {
                            val matched = moviesList.find { it.id == notif.targetId }
                            if (matched != null) {
                                selectedMediaItem = matched
                                activePlaybackPlaylist = moviesList
                            }
                        }
                    }
                    NotificationType.PLAYLIST -> {
                        currentTab = AppTab.PLAYLIST
                    }
                    NotificationType.APP_UPDATE -> {
                        checkForUpdates(isManualCheck = true)
                    }
                    else -> {}
                }
            },
            onMarkAllRead = {
                repository.markAllNotificationsAsRead()
                notificationsList = repository.getStoredNotifications()
            },
            onClearAll = {
                repository.clearAllNotifications()
                notificationsList = emptyList()
            },
            onDeleteNotification = { id ->
                repository.deleteNotification(id)
                notificationsList = repository.getStoredNotifications()
            }
        )
    }

    // App Exit Confirmation Dialog (User requested: ‡¶Æ‡ßã‡¶¨‡¶æ‡¶á‡¶≤‡ßá‡¶∞ ‡¶¨‡ßç‡¶Ø‡¶æ‡¶ï ‡¶¨‡¶æ‡¶ü‡¶®‡ßá ‡¶ï‡ßç‡¶≤‡¶ø‡¶ï ‡¶ï‡¶∞‡¶≤‡ßá ‡¶Ø‡ßá‡¶® ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™ ‡¶•‡ßá‡¶ï‡ßá ‡¶¨‡ßá‡¶∞ ‡¶π‡¶ì‡ßü‡¶æ‡¶∞ ‡¶Ü‡¶ó‡ßá ‡¶™‡¶æ‡¶∞‡¶Æ‡¶ø‡¶∂‡¶® ‡¶ö‡¶æ‡ßü)
    if (showExitConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmationDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF1E293B),
            tonalElevation = 8.dp,
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "NAFI TV Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            },
            title = {
                Text(
                    text = "‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™ ‡¶•‡ßá‡¶ï‡ßá ‡¶¨‡ßá‡¶∞ ‡¶π‡¶§‡ßá ‡¶ö‡¶æ‡¶®?",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "‡¶Ü‡¶™‡¶®‡¶ø ‡¶ï‡¶ø ‡¶®‡¶ø‡¶∂‡ßç‡¶ö‡¶ø‡¶§‡¶≠‡¶æ‡¶¨‡ßá NAFI TV 24 ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™‡¶ü‡¶ø ‡¶¨‡¶®‡ßç‡¶ß ‡¶ï‡¶∞‡¶§‡ßá ‡¶ö‡¶æ‡¶®?",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmationDialog = false
                        activity?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("‡¶π‡ßç‡¶Ø‡¶æ‡¶Å, ‡¶¨‡ßá‡¶∞ ‡¶π‡¶®", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitConfirmationDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text("‡¶®‡¶æ, ‡¶•‡¶æ‡¶ï‡ßÅ‡¶®", fontSize = 13.sp)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// SCREEN: ADMIN CONTROL APP (Exact UI from Screenshot 3)
// -------------------------------------------------------------
@Composable
fun AdminControlAppScreen(
    repository: MediaRepository,
    sportsList: List<MediaItem>,
    liveTvList: List<MediaItem>,
    moviesList: List<MediaItem>,
    playlistsList: List<PlaylistInfo>,
    cloudStreamRepos: List<CloudStreamRepo> = emptyList(),
    movieProviders: List<MovieProvider> = emptyList(),
    onOpenMovieProvider: (MovieProvider) -> Unit = {},
    onExitAdmin: () -> Unit,
    onDataChanged: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // BackHandler to exit Admin View back to main app
    BackHandler {
        onExitAdmin()
    }

    var selectedAdminTab by remember { mutableStateOf(AdminTab.CHANNELS) }

    var userAnalytics by remember { mutableStateOf<AppUserAnalytics?>(null) }
    var isRefreshingAnalytics by remember { mutableStateOf(false) }

    fun loadAnalyticsData() {
        isRefreshingAnalytics = true
        coroutineScope.launch {
            try {
                userAnalytics = repository.fetchUserAnalytics()
            } catch (_: Exception) {}
            isRefreshingAnalytics = false
        }
    }

    LaunchedEffect(Unit) {
        loadAnalyticsData()
    }

    // CloudStream & Movie Website Form State
    var repoUrlInput by remember { mutableStateOf("") }
    var isFetchingRepo by remember { mutableStateOf(false) }
    var customSiteName by remember { mutableStateOf("") }
    var customSiteUrl by remember { mutableStateOf("") }
    var customSiteLogo by remember { mutableStateOf("") }
    var customSiteCategory by remember { mutableStateOf("Bangla & Hindi Movies") }
    var customSiteDesc by remember { mutableStateOf("") }
    var expandedRepoId by remember { mutableStateOf<String?>(null) }
    var repoToDelete by remember { mutableStateOf<CloudStreamRepo?>(null) }
    var providerToDelete by remember { mutableStateOf<MovieProvider?>(null) }

    // App Update Form State
    var updateVersionCode by remember { mutableStateOf((com.example.BuildConfig.VERSION_CODE + 1).toString()) }
    var updateVersionName by remember { mutableStateOf("2.5.3") }
    var updateDownloadUrl by remember { mutableStateOf("https://github.com/google/ai-studio/releases/download/v2.5.3/NAFITV24_v2.5.3.apk") }
    var updateReleaseNotes by remember {
        mutableStateOf("‚Ä¢ ‡¶®‡¶§‡ßÅ‡¶® ‡¶´‡¶æ‡¶∏‡ßç‡¶ü ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶∏‡ßç‡¶™‡ßã‡¶∞‡ßç‡¶ü‡¶∏ ‡¶Æ‡¶æ‡¶≤‡ßç‡¶ü‡¶ø-‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶Ø‡ßã‡¶ó ‡¶ï‡¶∞‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá\n‚Ä¢ ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶∏‡ßç‡¶ï‡ßã‡¶∞‡¶¨‡ßã‡¶∞‡ßç‡¶° ‡¶ì ‡¶ï‡¶æ‡¶â‡¶®‡ßç‡¶ü‡¶°‡¶æ‡¶â‡¶® ‡¶ü‡¶æ‡¶á‡¶Æ‡¶æ‡¶∞\n‚Ä¢ ‡¶´‡ßÅ‡¶≤‡¶∏‡ßç‡¶ï‡ßç‡¶∞‡¶ø‡¶® ‡¶ì ‡¶π‡¶æ‡¶á ‡¶ï‡ßã‡ßü‡¶æ‡¶≤‡¶ø‡¶ü‡¶ø ‡¶Ü‡¶≤‡ßç‡¶ü‡ßç‡¶∞‡¶æ ‡¶è‡¶á‡¶ö‡¶°‡¶ø ‡¶™‡ßç‡¶≤‡ßá‡ßü‡¶æ‡¶∞\n‚Ä¢ ‡¶¨‡¶æ‡¶ó ‡¶´‡¶ø‡¶ï‡ßç‡¶∏ ‡¶è‡¶¨‡¶Ç ‡¶´‡¶æ‡¶∏‡ßç‡¶ü ‡¶≤‡ßã‡¶°‡¶ø‡¶Ç ‡¶∏‡ßç‡¶™‡¶ø‡¶°")
    }
    var updateApkSize by remember { mutableStateOf("18.5 MB") }
    var updateReleaseDate by remember { mutableStateOf("15 Aug 2026") }
    var updateIsForce by remember { mutableStateOf(false) }
    var isSavingUpdate by remember { mutableStateOf(false) }
    var previewUpdateDialog by remember { mutableStateOf<AppUpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        val cached = repository.getCachedAppUpdateInfo()
        if (cached != null) {
            updateVersionCode = cached.versionCode.toString()
            updateVersionName = cached.versionName
            updateDownloadUrl = cached.downloadUrl
            updateReleaseNotes = cached.releaseNotes
            updateApkSize = cached.apkSize
            updateReleaseDate = cached.releaseDate
            updateIsForce = cached.isForceUpdate
        } else {
            coroutineScope.launch {
                val remote = repository.fetchAppUpdateInfo()
                if (remote != null) {
                    updateVersionCode = remote.versionCode.toString()
                    updateVersionName = remote.versionName
                    updateDownloadUrl = remote.downloadUrl
                    updateReleaseNotes = remote.releaseNotes
                    updateApkSize = remote.apkSize
                    updateReleaseDate = remote.releaseDate
                    updateIsForce = remote.isForceUpdate
                }
            }
        }
    }

    // Playlists Form State
    var playlistTitle by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }
    var playlistLogoUrl by remember { mutableStateOf("") }
    var playlistDescription by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<PlaylistInfo?>(null) }

    // Sports Form State
    var sportCategory by remember { mutableStateOf("Cricket") }
    var sportStatus by remember { mutableStateOf("‚óè Live Now") }
    var tournamentName by remember { mutableStateOf("") }
    var team1Name by remember { mutableStateOf("") }
    var team1Score by remember { mutableStateOf("") }
    var team1LogoUrl by remember { mutableStateOf("") }
    var team2Name by remember { mutableStateOf("") }
    var team2Score by remember { mutableStateOf("") }
    var team2LogoUrl by remember { mutableStateOf("") }
    var matchTimeFormatted by remember { mutableStateOf("") }
    var countdownHours by remember { mutableStateOf("") }

    // Sports Multi-Server dynamic list (Default 3 servers, expandable to unlimited!)
    var sportsServers by remember {
        mutableStateOf(
            listOf(
                StreamServer("T SPORTS", ""),
                StreamServer("TT", ""),
                StreamServer("TEMP", "")
            )
        )
    }

    // Match Edit Dialog State (CRITICAL USER REQUEST: ‡¶ñ‡ßá‡¶≤‡¶æ ‡¶è‡¶°‡¶ø‡¶ü ‡¶ì ‡¶ñ‡ßá‡¶≤‡¶æ ‡¶ö‡¶≤‡¶æ‡¶ï‡¶æ‡¶≤‡ßÄ‡¶® ‡¶®‡¶§‡ßÅ‡¶® ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶Ø‡ßã‡¶ó)
    var editingMatchItem by remember { mutableStateOf<MediaItem?>(null) }
    var editTournament by remember { mutableStateOf("") }
    var editSportCategory by remember { mutableStateOf("Cricket") }
    var editSportStatus by remember { mutableStateOf("‚óè Live Now") }
    var editTeam1Name by remember { mutableStateOf("") }
    var editTeam1Score by remember { mutableStateOf("") }
    var editTeam1Logo by remember { mutableStateOf("") }
    var editTeam2Name by remember { mutableStateOf("") }
    var editTeam2Score by remember { mutableStateOf("") }
    var editTeam2Logo by remember { mutableStateOf("") }
    var editMatchTime by remember { mutableStateOf("") }
    var editCountdownHours by remember { mutableStateOf("") }
    var editServers by remember { mutableStateOf<List<StreamServer>>(emptyList()) }
    var editSportDropdownExpanded by remember { mutableStateOf(false) }
    var editStatusDropdownExpanded by remember { mutableStateOf(false) }

    // Main M3U Source URLs for Live TV, Sports and Movies
    var liveTvM3uInput by remember { mutableStateOf(repository.getSavedLiveTvM3uUrl()) }
    var sportsM3uInput by remember { mutableStateOf(repository.getSavedSportsM3uUrl()) }
    var moviesM3uInput by remember { mutableStateOf(repository.getSavedMoviesM3uUrl()) }

    // Firebase Database State & Diagnostics
    var firebaseUrlInput by remember { mutableStateOf(repository.getSavedFirebaseUrl()) }
    var isTestingFirebase by remember { mutableStateOf(false) }
    var firebaseTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Channel Form Server 1 and Server 2
    var server1Url by remember { mutableStateOf("") }
    var server2Url by remember { mutableStateOf("") }

    // Score Update Dialog State
    var updatingItem by remember { mutableStateOf<MediaItem?>(null) }
    var updateScore1 by remember { mutableStateOf("") }
    var updateScore2 by remember { mutableStateOf("") }

    // Item Deletion Dialog State
    var itemToDelete by remember { mutableStateOf<MediaItem?>(null) }

    // Channel Form State
    var channelName by remember { mutableStateOf("") }
    var channelCategory by remember { mutableStateOf("Sports TV") }
    var channelLogoUrl by remember { mutableStateOf("") }

    // Channel (Live TV) Edit Dialog State
    var editingChannelItem by remember { mutableStateOf<MediaItem?>(null) }
    var editChannelName by remember { mutableStateOf("") }
    var editChannelCategory by remember { mutableStateOf("Sports TV") }
    var editChannelLogoUrl by remember { mutableStateOf("") }
    var editChannelServers by remember { mutableStateOf<List<StreamServer>>(emptyList()) }
    var editChannelCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // Playlist Edit Dialog State
    var editingPlaylistItem by remember { mutableStateOf<PlaylistInfo?>(null) }
    var editPlaylistTitle by remember { mutableStateOf("") }
    var editPlaylistUrl by remember { mutableStateOf("") }
    var editPlaylistLogoUrl by remember { mutableStateOf("") }
    var editPlaylistDescription by remember { mutableStateOf("") }

    // Movie Form State
    var movieTitle by remember { mutableStateOf("") }
    var movieCategory by remember { mutableStateOf("Bangla Movie") }
    var moviePosterUrl by remember { mutableStateOf("") }
    var movieDesc by remember { mutableStateOf("") }

    // Movie & Series Edit Dialog State
    var editingMovieItem by remember { mutableStateOf<MediaItem?>(null) }
    var editMovieTitle by remember { mutableStateOf("") }
    var editMovieCategory by remember { mutableStateOf("Bangla Movie") }
    var editMoviePosterUrl by remember { mutableStateOf("") }
    var editMovieDesc by remember { mutableStateOf("") }
    var editMovieServers by remember { mutableStateOf<List<StreamServer>>(emptyList()) }
    var editMovieCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val channelCategoryOptions = listOf("Sports TV", "News", "Entertainment", "Bangla", "Indian", "Kids", "Music", "Infotainment", "Religious")
    val movieCategoryOptions = listOf("Bangla Movie", "Hindi Dubbed", "Hollywood", "South Movie", "Web Series", "Natok", "Animation")

    val sportOptions = listOf("Cricket", "Football", "Tennis", "Basketball", "Racing", "Badminton")
    val statusOptions = listOf("‚óè Live Now", "Upcoming", "Finished")
    var sportDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    // Marquee News Ticker Form State
    var marqueeTickerInput by remember { mutableStateOf(repository.getMarqueeTickerText()) }
    var isSavingMarqueeTicker by remember { mutableStateOf(false) }

    // Broadcast Notifications Form State
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var broadcastType by remember { mutableStateOf(NotificationType.BROADCAST) }
    var broadcastTargetId by remember { mutableStateOf("") }
    var broadcastImageUrl by remember { mutableStateOf("") }
    var isSendingBroadcast by remember { mutableStateOf(false) }
    var broadcastTypeDropdownExpanded by remember { mutableStateOf(false) }
    var adminNotificationHistory by remember { mutableStateOf(repository.getStoredNotifications()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {
        // Admin Top Header Bar (Matching Screenshot 3)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "NAFI TV Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NAFI TV 24",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981)
                            ) {
                                Text(
                                    text = "ADMIN APP",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Firebase Admin Control App",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Box(modifier = Modifier.padding(6.dp)) {
                            Icon(Icons.Rounded.Smartphone, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Box(modifier = Modifier.padding(6.dp)) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.clickable { onDataChanged() }
                    ) {
                        Box(modifier = Modifier.padding(6.dp)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Welcome Card with Realtime Sync Badge
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB).copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "NAFI TV 24 - Admin Control App",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Firebase Realtime Sync pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF065F46).copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Firebase Realtime Sync",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "‡¶è‡¶ñ‡¶æ‡¶®‡ßá ‡¶§‡¶•‡ßç‡¶Ø ‡¶™‡¶∞‡¶ø‡¶¨‡¶∞‡ßç‡¶§‡¶® ‡¶ï‡¶∞‡¶≤‡ßá ‡¶∏‡¶æ‡¶•‡ßá ‡¶∏‡¶æ‡¶•‡ßá ‡¶á‡¶â‡¶ú‡¶æ‡¶∞ ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™‡ßá ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶π‡ßü‡ßá ‡¶Ø‡¶æ‡¶¨‡ßá‡•§",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Seed rich sample data with multi-servers & live scores
                                    coroutineScope.launch {
                                        val sampleSports = repository.getInitialSports()
                                        val sampleTv = repository.getInitialLiveTv()
                                        val sampleMov = repository.getInitialMoviesSeries()
                                        sampleSports.forEach { repository.saveCustomStream(it); repository.pushToFirebase(it) }
                                        sampleTv.forEach { repository.saveCustomStream(it); repository.pushToFirebase(it) }
                                        sampleMov.forEach { repository.saveCustomStream(it); repository.pushToFirebase(it) }
                                        onDataChanged()
                                        Toast.makeText(context, "‡¶∏‡ßç‡¶Ø‡¶æ‡¶Æ‡ßç‡¶™‡¶≤ ‡¶°‡ßá‡¶ü‡¶æ ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sample Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onExitAdmin,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("‡¶á‡¶â‡¶ú‡¶æ‡¶∞ ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™‡ßá ‡¶Ø‡¶æ‡¶®", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ITEM: Live User Statistics & Active Traffic Card (‡¶á‡¶â‡¶ú‡¶æ‡¶∞ ‡¶ì ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶ü‡ßç‡¶∞‡¶æ‡¶´‡¶ø‡¶ï)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Analytics,
                                        contentDescription = "Analytics",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "‡¶≤‡¶æ‡¶á‡¶≠ ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™ ‡¶á‡¶â‡¶ú‡¶æ‡¶∞ ‡¶ì ‡¶ü‡ßç‡¶∞‡¶æ‡¶´‡¶ø‡¶ï ‡¶Æ‡ßá‡¶ü‡ßç‡¶∞‡¶ø‡¶ï‡ßç‡¶∏",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "‡¶∞‡¶ø‡ßü‡ßá‡¶≤-‡¶ü‡¶æ‡¶á‡¶Æ ‡¶¨‡ßç‡¶Ø‡¶¨‡¶π‡¶æ‡¶∞‡¶ï‡¶æ‡¶∞‡ßÄ ‡¶Æ‡¶®‡¶ø‡¶ü‡¶∞‡¶ø‡¶Ç",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Refresh button
                            IconButton(
                                onClick = { loadAnalyticsData() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (isRefreshingAnalytics) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color(0xFF00E5FF),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh Analytics",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Two main stat cards: 1. Currently Active Users, 2. Total Lifetime Users
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Card 1: Currently Active Users
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedAdminTab = AdminTab.ANALYTICS },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "‡¶¨‡¶∞‡ßç‡¶§‡¶Æ‡¶æ‡¶®‡ßá ‡¶è‡¶ï‡ßç‡¶ü‡¶ø‡¶≠",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        // Live green dot
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                    }

                                    Text(
                                        text = "${userAnalytics?.activeUsers ?: 1} ‡¶ú‡¶®",
                                        color = Color(0xFF34D399),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )

                                    Text(
                                        text = "‚óè ‡¶Ö‡¶®‡¶≤‡¶æ‡¶á‡¶®‡ßá ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶∏‡¶ï‡ßç‡¶∞‡¶ø‡ßü",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Card 2: Total Registered Users
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedAdminTab = AdminTab.ANALYTICS },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "‡¶∏‡¶∞‡ßç‡¶¨‡¶Æ‡ßã‡¶ü ‡¶á‡¶â‡¶ú‡¶æ‡¶∞",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.PeopleAlt,
                                            contentDescription = null,
                                            tint = Color(0xFF60A5FA),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Text(
                                        text = "${userAnalytics?.totalUsers ?: 1} ‡¶ú‡¶®",
                                        color = Color(0xFF60A5FA),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )

                                    Text(
                                        text = "‡¶Æ‡ßã‡¶ü ‡¶á‡¶®‡¶∏‡ßç‡¶ü‡¶≤ ‡¶ì ‡¶¨‡ßç‡¶Ø‡¶¨‡¶π‡¶æ‡¶∞‡¶ï‡¶æ‡¶∞‡ßÄ",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Admin Segmented Filter Tabs (Channels / Movies / Playlists / Sports / Updates)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Button(
                            onClick = { selectedAdminTab = AdminTab.ANALYTICS },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAdminTab == AdminTab.ANALYTICS) Color(0xFF00E5FF) else Color(0xFF1E293B),
                                contentColor = if (selectedAdminTab == AdminTab.ANALYTICS) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Analytics, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("üë• ‡¶á‡¶â‡¶ú‡¶æ‡¶∞ ‡¶ì ‡¶ü‡ßç‡¶∞‡¶æ‡¶´‡¶ø‡¶ï (${userAnalytics?.activeUsers ?: 1})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedAdminTab = AdminTab.TICKER },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAdminTab == AdminTab.TICKER) Color(0xFFEC4899) else Color(0xFF1E293B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("‚ö° ‡¶¨‡ßç‡¶∞‡ßá‡¶ï‡¶ø‡¶Ç ‡¶®‡¶ø‡¶â‡¶ú ‡¶¨‡¶æ‡¶∞", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedAdminTab = AdminTab.CHANNELS },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAdminTab == AdminTab.CHANNELS) Color(0xFF2563EB) else Color(0xFF1E293B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("üì∫ Live (${liveTvList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedAdminTab = AdminTab.MOVIES },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAdminTab == AdminTab.MOVIES) Color(0xFF2563EB) else Color(0xFF1E293B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("üé¨ Movies (${moviesList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedAdminTab = AdminTab.PLAYLISTS },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAdminTab == AdminTab.PLAYLISTS) Color(0xFF2563EB) else Color(0xFF1E293B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("üìÇ Playlists (${playlistsList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedAdminTab = AdminTab.SPORTS },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAdminTab == AdminTab.SPORTS) Color(0xFF2563EB) else Color(0xFF1E293B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("üèÜ Sports (${sportsList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedAdminTab = AdminTab.REPOSITORIES },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAdminTab == AdminTab.REPOSITORIES) Color(0xFF8B5CF6) else Color(0xFF1E293B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Extension, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("üé¨ Repos & Sites (${cloudStreamRepos.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedAdminTab = AdminTab.APP_UPDATE },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAdminTab == AdminTab.APP_UPDATE) Color(0xFF10B981) else Color(0xFF1E293B),
                                contentColor = if (selectedAdminTab == AdminTab.APP_UPDATE) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.RocketLaunch, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("üöÄ App Updates", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedAdminTab = AdminTab.BROADCAST },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAdminTab == AdminTab.BROADCAST) Color(0xFF00E5FF) else Color(0xFF1E293B),
                                contentColor = if (selectedAdminTab == AdminTab.BROADCAST) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("üì¢ ‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶®", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedAdminTab = AdminTab.FIREBASE },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAdminTab == AdminTab.FIREBASE) Color(0xFFFF9800) else Color(0xFF1E293B),
                                contentColor = if (selectedAdminTab == AdminTab.FIREBASE) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("üî• Firebase Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // -------------------------------------------------------------
            // TAB: USER ANALYTICS & LIVE ACTIVE USERS
            // -------------------------------------------------------------
            if (selectedAdminTab == AdminTab.ANALYTICS) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Devices, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "‡¶ï‡¶æ‡¶®‡ßá‡¶ï‡ßç‡¶ü‡ßá‡¶° ‡¶á‡¶â‡¶ú‡¶æ‡¶∞ ‡¶°‡¶ø‡¶≠‡¶æ‡¶á‡¶∏ ‡¶∏‡¶Æ‡ßÇ‡¶π (${userAnalytics?.activeUsersList?.size ?: 0})",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = { loadAnalyticsData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("‡¶∞‡¶ø‡¶´‡ßç‡¶∞‡ßá‡¶∂", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            val userList = userAnalytics?.activeUsersList ?: emptyList()
                            if (userList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("‡¶ï‡ßã‡¶®‡ßã ‡¶∏‡¶ï‡ßç‡¶∞‡¶ø‡ßü ‡¶°‡¶ø‡¶≠‡¶æ‡¶á‡¶∏ ‡¶∞‡ßá‡¶ï‡¶∞‡ßç‡¶° ‡¶™‡¶æ‡¶ì‡ßü‡¶æ ‡¶Ø‡¶æ‡ßü‡¶®‡¶ø‡•§", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            } else {
                                userList.forEach { user ->
                                    val timeDiff = System.currentTimeMillis() - user.lastSeen
                                    val timeText = when {
                                        timeDiff < 60_000L -> "üü¢ ‡¶è‡¶á‡¶Æ‡¶æ‡¶§‡ßç‡¶∞ ‡¶∏‡¶ï‡ßç‡¶∞‡¶ø‡ßü"
                                        timeDiff < 3600_000L -> "${timeDiff / 60_000L} ‡¶Æ‡¶ø‡¶®‡¶ø‡¶ü ‡¶Ü‡¶ó‡ßá ‡¶∏‡¶ï‡ßç‡¶∞‡¶ø‡ßü"
                                        timeDiff < 86400_000L -> "${timeDiff / 3600_000L} ‡¶ò‡¶£‡ßç‡¶ü‡¶æ ‡¶Ü‡¶ó‡ßá ‡¶∏‡¶ï‡ßç‡¶∞‡¶ø‡ßü"
                                        else -> "${timeDiff / 86400_000L} ‡¶¶‡¶ø‡¶® ‡¶Ü‡¶ó‡ßá ‡¶∏‡¶ï‡ßç‡¶∞‡¶ø‡ßü"
                                    }
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.5.dp,
                                            if (user.isOnline) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF334155)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (user.isOnline) Color(0xFF10B981).copy(alpha = 0.2f)
                                                            else Color(0xFF64748B).copy(alpha = 0.2f)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (user.isOnline) Icons.Rounded.Smartphone else Icons.Rounded.PhoneAndroid,
                                                        contentDescription = null,
                                                        tint = if (user.isOnline) Color(0xFF34D399) else Color(0xFF94A3B8),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = user.deviceModel,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "ID: ${user.id.take(12)} ‚Ä¢ App ${user.appVersion}",
                                                        color = Color(0xFF64748B),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (user.isOnline) Color(0xFF065F46).copy(alpha = 0.4f) else Color(0xFF1E293B),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    0.5.dp,
                                                    if (user.isOnline) Color(0xFF10B981) else Color(0xFF475569)
                                                )
                                            ) {
                                                Text(
                                                    text = timeText,
                                                    color = if (user.isOnline) Color(0xFF34D399) else Color(0xFF94A3B8),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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

            // TAB: SCROLLING BREAKING NEWS TICKER MANAGEMENT
            // -------------------------------------------------------------
            if (selectedAdminTab == AdminTab.TICKER) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Header Banner
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEC4899).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Campaign, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(26.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "‚ö° ‡¶∏‡ßç‡¶ï‡ßç‡¶∞‡ßã‡¶≤‡¶ø‡¶Ç ‡¶¨‡ßç‡¶∞‡ßá‡¶ï‡¶ø‡¶Ç ‡¶®‡¶ø‡¶â‡¶ú ‡¶¨‡¶æ‡¶∞ ‡¶™‡¶∞‡¶ø‡¶¨‡¶∞‡ßç‡¶§‡¶®",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "‡¶Æ‡ßã‡¶° ‡¶∏‡¶ø‡¶≤‡ßá‡¶ï‡¶∂‡¶® ‡¶∏‡ßç‡¶ï‡ßç‡¶∞‡¶ø‡¶®‡ßá‡¶∞ 'GET STARTED' ‡¶è‡¶∞ ‡¶â‡¶™‡¶∞‡ßá ‡¶ö‡¶≤‡¶Æ‡¶æ‡¶® ‡¶¨‡ßç‡¶∞‡ßá‡¶ï‡¶ø‡¶Ç ‡¶®‡¶ø‡¶â‡¶ú ‡¶ü‡ßá‡¶ï‡ßç‡¶∏‡¶ü ‡¶™‡¶∞‡¶ø‡¶¨‡¶∞‡ßç‡¶§‡¶® ‡¶ì ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶° ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶ï‡¶∞‡ßÅ‡¶®",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Live Marquee Preview Box
                            Text(
                                text = "‡¶≤‡¶æ‡¶á‡¶≠ ‡¶™‡ßç‡¶∞‡¶ø‡¶≠‡¶ø‡¶â (‡¶á‡¶â‡¶ú‡¶æ‡¶∞‡¶∞‡¶æ ‡¶Ø‡ßá‡¶≠‡¶æ‡¶¨‡ßá ‡¶¶‡ßá‡¶ñ‡¶¨‡ßá‡¶®):",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.2.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF00E5FF).copy(alpha = 0.8f),
                                            Color(0xFF8B5CF6).copy(alpha = 0.9f),
                                            Color(0xFFEC4899).copy(alpha = 0.8f)
                                        )
                                    )
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF00E5FF), shape = CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = marqueeTickerInput.ifBlank { "‡¶è‡¶ñ‡¶æ‡¶®‡ßá ‡¶Ü‡¶™‡¶®‡¶æ‡¶∞ ‡¶¨‡ßç‡¶∞‡ßá‡¶ï‡¶ø‡¶Ç ‡¶®‡¶ø‡¶â‡¶ú ‡¶≤‡¶ø‡¶ñ‡ßÅ‡¶®..." },
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .basicMarquee(iterations = Int.MAX_VALUE, velocity = 40.dp)
                                    )
                                }
                            }

                            // Text Input Field
                            OutlinedTextField(
                                value = marqueeTickerInput,
                                onValueChange = { marqueeTickerInput = it },
                                label = { Text("‡¶∏‡ßç‡¶ï‡ßç‡¶∞‡ßã‡¶≤‡¶ø‡¶Ç ‡¶®‡¶ø‡¶â‡¶ú ‡¶ü‡ßá‡¶ï‡ßç‡¶∏‡¶ü ‡¶≤‡¶ø‡¶ñ‡ßÅ‡¶® *", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 6,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFEC4899),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                )
                            )

                            // Quick Preset News Templates
                            Text(
                                text = "‡¶™‡ßç‡¶∞‡¶ø‡¶∏‡ßá‡¶ü ‡¶ü‡ßá‡¶Æ‡¶™‡ßç‡¶≤‡ßá‡¶ü ‡¶®‡¶ø‡¶∞‡ßç‡¶¨‡¶æ‡¶ö‡¶® ‡¶ï‡¶∞‡ßÅ‡¶®:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        marqueeTickerInput = "üèÜ ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶ï‡ßç‡¶∞‡¶ø‡¶ï‡ßá‡¶ü ‡¶ì ‡¶´‡ßÅ‡¶ü‡¶¨‡¶≤ ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶∂‡ßÅ‡¶∞‡ßÅ ‡¶π‡ßü‡ßá‡¶õ‡ßá! ‡¶Ø‡ßá‡¶ï‡ßã‡¶®‡ßã ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶∏‡¶ø‡¶≤‡ßá‡¶ï‡ßç‡¶ü ‡¶ï‡¶∞‡ßá ‡¶∏‡¶∞‡¶æ‡¶∏‡¶∞‡¶ø ‡¶∏‡¶Æ‡ßç‡¶™‡ßÇ‡¶∞‡ßç‡¶£ HD ‡¶ï‡ßã‡ßü‡¶æ‡¶≤‡¶ø‡¶ü‡¶ø‡¶§‡ßá ‡¶â‡¶™‡¶≠‡ßã‡¶ó ‡¶ï‡¶∞‡ßÅ‡¶®‡•§ NAFI TV24"
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Text("‚öΩ ‡¶ñ‡ßá‡¶≤‡¶æ‡¶ß‡ßÅ‡¶≤‡¶æ‡¶∞ ‡¶®‡ßã‡¶ü‡¶ø‡¶∏", fontSize = 10.sp, color = Color(0xFF38BDF8), maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = {
                                        marqueeTickerInput = "‡¶¨‡¶æ‡¶Ç‡¶≤‡¶æ‡¶¶‡ßá‡¶∂ ‡¶¨‡ßç‡¶Ø‡¶æ‡¶Ç‡¶ï‡ßá‡¶∞ ‡¶®‡¶§‡ßÅ‡¶® ‡¶Æ‡ßÅ‡¶¶‡ßç‡¶∞‡¶æ‡¶®‡ßÄ‡¶§‡¶ø ‡¶ò‡ßã‡¶∑‡¶£‡¶æ‡•§ ‡¶™‡ßÅ‡¶Å‡¶ú‡¶ø‡¶¨‡¶æ‡¶ú‡¶æ‡¶∞‡ßá ‡¶ä‡¶∞‡ßç‡¶ß‡ßç‡¶¨‡¶ó‡¶§‡¶ø‡•§ NAFI TV24 ‡¶è ‡¶ï‡ßç‡¶∞‡¶ø‡¶ï‡ßá‡¶ü, ‡¶´‡ßÅ‡¶ü‡¶¨‡¶≤ ‡¶ì ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶ü‡¶ø‡¶≠‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶∏‡¶Æ‡ßç‡¶™‡ßÇ‡¶∞‡ßç‡¶£ ‡¶¨‡¶ø‡¶®‡¶æ‡¶Æ‡ßÇ‡¶≤‡ßç‡¶Ø‡ßá ‡¶â‡¶™‡¶≠‡ßã‡¶ó ‡¶ï‡¶∞‡ßÅ‡¶®‡•§"
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Text("üì∞ ‡¶ú‡¶æ‡¶§‡ßÄ‡ßü ‡¶∏‡¶Ç‡¶¨‡¶æ‡¶¶", fontSize = 10.sp, color = Color(0xFFF472B6), maxLines = 1)
                                }
                            }

                            // Save & Cloud Sync Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (marqueeTickerInput.isBlank()) {
                                            Toast.makeText(context, "‡¶Ö‡¶®‡ßÅ‡¶ó‡ßç‡¶∞‡¶π ‡¶ï‡¶∞‡ßá ‡¶®‡¶ø‡¶â‡¶ú ‡¶ü‡ßá‡¶ï‡ßç‡¶∏‡¶ü ‡¶≤‡¶ø‡¶ñ‡ßÅ‡¶®!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isSavingMarqueeTicker = true
                                        coroutineScope.launch {
                                            repository.saveMarqueeTickerText(marqueeTickerInput.trim())
                                            repository.pushMarqueeTickerToFirebase(marqueeTickerInput.trim())
                                            isSavingMarqueeTicker = false
                                            Toast.makeText(context, "‚úÖ ‡¶®‡¶ø‡¶â‡¶ú ‡¶¨‡¶æ‡¶∞ ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ì ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEC4899),
                                        contentColor = Color.White
                                    ),
                                    enabled = !isSavingMarqueeTicker
                                ) {
                                    if (isSavingMarqueeTicker) {
                                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("‡¶∏‡¶Ç‡¶∞‡¶ï‡ßç‡¶∑‡¶£ ‡¶π‡¶ö‡ßç‡¶õ‡ßá...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ì ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        marqueeTickerInput = MediaRepository.DEFAULT_MARQUEE_TEXT
                                        coroutineScope.launch {
                                            repository.saveMarqueeTickerText(MediaRepository.DEFAULT_MARQUEE_TEXT)
                                            repository.pushMarqueeTickerToFirebase(MediaRepository.DEFAULT_MARQUEE_TEXT)
                                            Toast.makeText(context, "‡¶°‡¶ø‡¶´‡¶≤‡ßç‡¶ü ‡¶ü‡ßá‡¶ï‡ßç‡¶∏‡¶ü ‡¶∞‡¶ø‡¶∏‡ßç‡¶ü‡ßã‡¶∞ ‡¶ï‡¶∞‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                                ) {
                                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 1 CONTENT: SPORTS MATCHES ADMIN (Multi-Server Support)
            // -------------------------------------------------------------
            if (selectedAdminTab == AdminTab.SPORTS) {
                // 1. Primary M3U Playlist Manager for Sports
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.SportsSoccer, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("‚öΩ ‡¶∏‡ßç‡¶™‡ßã‡¶∞‡ßç‡¶ü‡¶∏ ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö M3U ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶≤‡¶ø‡¶Ç‡¶ï (Sports M3U)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(
                                text = "‡¶è‡¶ñ‡¶æ‡¶®‡ßá ‡¶è‡¶ï ‡¶¨‡¶æ ‡¶è‡¶ï‡¶æ‡¶ß‡¶ø‡¶ï M3U ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶¶‡¶ø‡¶§‡ßá ‡¶™‡¶æ‡¶∞‡¶¨‡ßá‡¶® (‡¶™‡ßç‡¶∞‡¶§‡¶ø ‡¶≤‡¶æ‡¶á‡¶®‡ßá ‡¶è‡¶ï‡¶ü‡¶ø ‡¶ï‡¶∞‡ßá ‡¶Ö‡¶•‡¶¨‡¶æ ‡¶ï‡¶Æ‡¶æ ‡¶¶‡¶ø‡ßü‡ßá)‡•§ ‡¶∏‡¶Æ‡¶∏‡ßç‡¶§ ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶∏‡ßç‡¶¨‡ßü‡¶Ç‡¶ï‡ßç‡¶∞‡¶ø‡ßü‡¶≠‡¶æ‡¶¨‡ßá ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶° ‡¶°‡ßá‡¶ü‡¶æ‡¶¨‡ßá‡¶∏ ‡¶¶‡¶ø‡ßü‡ßá ‡¶á‡¶â‡¶ú‡¶æ‡¶∞‡ßá‡¶∞ ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™‡ßá ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá ‡¶Ø‡¶æ‡¶¨‡ßá‡•§",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            OutlinedTextField(
                                value = sportsM3uInput,
                                onValueChange = { sportsM3uInput = it },
                                placeholder = { Text("https://url1.m3u\nhttps://url2.m3u", color = Color(0xFF64748B), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                minLines = 2,
                                maxLines = 5
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (sportsM3uInput.isNotBlank()) {
                                            val url = sportsM3uInput.trim()
                                            repository.saveSportsM3uUrl(url)
                                            coroutineScope.launch {
                                                repository.pushAppConfigToFirebase(sportsM3u = url)
                                            }
                                            onDataChanged()
                                            Toast.makeText(context, "‚úÖ ‡¶∏‡ßç‡¶™‡ßã‡¶∞‡ßç‡¶ü‡¶∏ M3U ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶∏‡ßá‡¶≠ ‡¶ì ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "‡¶∏‡¶†‡¶ø‡¶ï ‡¶∏‡ßç‡¶™‡ßã‡¶∞‡ßç‡¶ü‡¶∏ M3U ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶¶‡¶ø‡¶®", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1.3f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("‡¶∏‡ßá‡¶≠ ‡¶ì ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        sportsM3uInput = MediaRepository.DEFAULT_SPORTS_M3U_URL
                                        repository.saveSportsM3uUrl(MediaRepository.DEFAULT_SPORTS_M3U_URL)
                                        coroutineScope.launch {
                                            repository.pushAppConfigToFirebase(sportsM3u = MediaRepository.DEFAULT_SPORTS_M3U_URL)
                                        }
                                        onDataChanged()
                                        Toast.makeText(context, "‡¶°‡¶ø‡¶´‡¶≤‡ßç‡¶ü ‡¶∏‡ßç‡¶™‡ßã‡¶∞‡ßç‡¶ü‡¶∏ M3U ‡¶∞‡¶ø‡¶∏‡ßá‡¶ü ‡¶ì ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá‡¶õ‡ßá", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                                ) {
                                    Text("‡¶°‡¶ø‡¶´‡¶≤‡ßç‡¶ü ‡¶≤‡¶ø‡¶Ç‡¶ï", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Create Live or Upcoming Sports Match",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Sport Category & Status Dropdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Category
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = sportCategory,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { sportDropdownExpanded = !sportDropdownExpanded }) {
                                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.White)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().clickable { sportDropdownExpanded = true },
                                        colors = customFieldColors(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    DropdownMenu(
                                        expanded = sportDropdownExpanded,
                                        onDismissRequest = { sportDropdownExpanded = false },
                                        modifier = Modifier.background(Color(0xFF1E293B))
                                    ) {
                                        sportOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt, color = Color.White) },
                                                onClick = {
                                                    sportCategory = opt
                                                    sportDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Status
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = sportStatus,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { statusDropdownExpanded = !statusDropdownExpanded }) {
                                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.White)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().clickable { statusDropdownExpanded = true },
                                        colors = customFieldColors(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    DropdownMenu(
                                        expanded = statusDropdownExpanded,
                                        onDismissRequest = { statusDropdownExpanded = false },
                                        modifier = Modifier.background(Color(0xFF1E293B))
                                    ) {
                                        statusOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt, color = Color.White) },
                                                onClick = {
                                                    sportStatus = opt
                                                    statusDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Preset Quick Fill Buttons
                            Text("‚ö° ‡¶¶‡ßç‡¶∞‡ßÅ‡¶§ ‡¶™‡ßç‡¶∞‡¶ø‡¶∏‡ßá‡¶ü ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶§‡ßà‡¶∞‡¶ø ‡¶ï‡¶∞‡ßÅ‡¶® (Quick Presets):", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.clickable {
                                            sportCategory = "Cricket"
                                            sportStatus = "Upcoming"
                                            tournamentName = "Cricket üèè || Bangladesh vs Australia Test Series 2026"
                                            team1Name = "Bangladesh"
                                            team1LogoUrl = "https://flagcdn.com/w160/bd.png"
                                            team2Name = "Australia"
                                            team2LogoUrl = "https://flagcdn.com/w160/au.png"
                                            matchTimeFormatted = "06:00 AM, Aug 23"
                                            countdownHours = ""
                                            sportsServers = listOf(
                                                StreamServer("T SPORTS", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
                                                StreamServer("TT", "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"),
                                                StreamServer("TEMP", "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8")
                                            )
                                        }
                                    ) {
                                        Text("üáßüá© BD vs AUS üá¶üá∫", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.clickable {
                                            sportCategory = "Cricket"
                                            sportStatus = "‚óè Live Now"
                                            tournamentName = "NF Women | The Hundred Women's Competition 2026"
                                            team1Name = "Trent Rockets"
                                            team1LogoUrl = "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=160"
                                            team2Name = "Southern Brave"
                                            team2LogoUrl = "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=160"
                                            matchTimeFormatted = "Live Now"
                                            countdownHours = "113"
                                            sportsServers = listOf(
                                                StreamServer("T SPORTS", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
                                                StreamServer("TT", "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"),
                                                StreamServer("TEMP", "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8")
                                            )
                                        }
                                    ) {
                                        Text("üöÄ Trent vs Brave üõ°Ô∏è", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.clickable {
                                            sportCategory = "Cricket"
                                            sportStatus = "Upcoming"
                                            tournamentName = "Cricket üèè || Sri Lanka vs India Test Series 2026"
                                            team1Name = "Sri Lanka"
                                            team1LogoUrl = "https://flagcdn.com/w160/lk.png"
                                            team2Name = "India"
                                            team2LogoUrl = "https://flagcdn.com/w160/in.png"
                                            matchTimeFormatted = "10:30 AM, Aug 15"
                                            countdownHours = ""
                                            sportsServers = listOf(
                                                StreamServer("T SPORTS", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
                                                StreamServer("TT", "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"),
                                                StreamServer("TEMP", "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8")
                                            )
                                        }
                                    ) {
                                        Text("üá±üá∞ SL vs IND üáÆüá≥", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                    }
                                }
                            }

                            // Tournament Banner Name
                            OutlinedTextField(
                                value = tournamentName,
                                onValueChange = { tournamentName = it },
                                placeholder = { Text("Tournament (e.g. Cricket üèè || Bangladesh vs Australia)", color = Color(0xFF64748B), fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Team 1 Section
                            Text("üõ°Ô∏è Team 1 Details:", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = team1Name,
                                    onValueChange = { team1Name = it },
                                    placeholder = { Text("Team 1 Name (e.g. Bangladesh)", color = Color(0xFF64748B), fontSize = 13.sp) },
                                    modifier = Modifier.weight(1.2f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = team1Score,
                                    onValueChange = { team1Score = it },
                                    placeholder = { Text("Score (e.g. 154/4)", color = Color(0xFF64748B), fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = team1LogoUrl,
                                onValueChange = { team1LogoUrl = it },
                                placeholder = { Text("Team 1 Logo URL (or flagcdn.com/w160/bd.png)", color = Color(0xFF64748B), fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Team 2 Section
                            Text("üõ°Ô∏è Team 2 Details:", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = team2Name,
                                    onValueChange = { team2Name = it },
                                    placeholder = { Text("Team 2 Name (e.g. Australia)", color = Color(0xFF64748B), fontSize = 13.sp) },
                                    modifier = Modifier.weight(1.2f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = team2Score,
                                    onValueChange = { team2Score = it },
                                    placeholder = { Text("Score (e.g. 142/8)", color = Color(0xFF64748B), fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = team2LogoUrl,
                                onValueChange = { team2LogoUrl = it },
                                placeholder = { Text("Team 2 Logo URL (or flagcdn.com/w160/au.png)", color = Color(0xFF64748B), fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Match Time & Countdown Section
                            Text("‚è±Ô∏è ‡¶∏‡¶Æ‡¶Ø‡¶º ‡¶ì ‡¶ï‡¶æ‡¶â‡¶®‡ßç‡¶ü‡¶°‡¶æ‡¶â‡¶® ‡¶ü‡¶æ‡¶á‡¶Æ‡¶æ‡¶∞ (Match Time & Countdown):", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = matchTimeFormatted,
                                    onValueChange = { matchTimeFormatted = it },
                                    placeholder = { Text("Time (e.g. 06:30 AM, Aug 13)", color = Color(0xFF64748B), fontSize = 12.sp) },
                                    modifier = Modifier.weight(1.2f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = countdownHours,
                                    onValueChange = { countdownHours = it },
                                    placeholder = { Text("Countdown (e.g. 113 hrs)", color = Color(0xFF64748B), fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }

                            // Dynamic Multi-Server Streams Section (CRITICAL USER REQUEST: ‡¶è‡¶ï‡¶æ‡¶ß‡¶ø‡¶ï ‡¶Æ‡¶æ‡¶≤‡ßç‡¶ü‡¶ø ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶Ø‡ßã‡¶ó)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "‚ö° ‡¶Æ‡¶æ‡¶≤‡ßç‡¶ü‡¶ø-‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ ‡¶≤‡¶ø‡¶ô‡ßç‡¶ï‡¶∏‡¶Æ‡ßÇ‡¶π (${sportsServers.size} ‡¶ü‡¶ø ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞):",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "‡¶Ü‡¶®‡¶≤‡¶ø‡¶Æ‡¶ø‡¶ü‡ßá‡¶° ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶∏‡¶æ‡¶™‡ßã‡¶∞‡ßç‡¶ü",
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Dynamic list of server inputs
                            sportsServers.forEachIndexed { index, server ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = server.name,
                                        onValueChange = { newName ->
                                            sportsServers = sportsServers.toMutableList().also {
                                                it[index] = it[index].copy(name = newName)
                                            }
                                        },
                                        placeholder = { Text("Server ${index + 1} Name", color = Color(0xFF64748B), fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = customFieldColors(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = server.url,
                                        onValueChange = { newUrl ->
                                            sportsServers = sportsServers.toMutableList().also {
                                                it[index] = it[index].copy(url = newUrl)
                                            }
                                        },
                                        placeholder = { Text("Stream URL (.m3u8 / mp4)", color = Color(0xFF64748B), fontSize = 11.sp) },
                                        modifier = Modifier.weight(2f),
                                        colors = customFieldColors(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    if (sportsServers.size > 1) {
                                        IconButton(
                                            onClick = {
                                                sportsServers = sportsServers.toMutableList().also { it.removeAt(index) }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                contentDescription = "‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶∏‡¶∞‡¶æ‡¶®",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Add More Server Button
                            OutlinedButton(
                                onClick = {
                                    val nextNum = sportsServers.size + 1
                                    val defaultName = when (nextNum) {
                                        1 -> "T SPORTS"
                                        2 -> "TT"
                                        3 -> "TEMP"
                                        4 -> "HD SERVER 4"
                                        5 -> "SERVER 5 (4K)"
                                        6 -> "SERVER 6 (HLS)"
                                        else -> "SERVER $nextNum"
                                    }
                                    sportsServers = sportsServers + StreamServer(defaultName, "")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("‚ûï ‡¶Ü‡¶∞‡¶ì ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞/‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶≤‡¶ø‡¶ô‡ßç‡¶ï ‡¶Ø‡ßã‡¶ó ‡¶ï‡¶∞‡ßÅ‡¶® (+ Add Server)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Publish Match Button
                            Button(
                                onClick = {
                                    val validServers = sportsServers.mapNotNull {
                                        if (it.url.isNotBlank()) StreamServer(it.name.ifBlank { "Server" }, it.url.trim()) else null
                                    }

                                    if ((team1Name.isNotBlank() || tournamentName.isNotBlank()) && validServers.isNotEmpty()) {
                                        val matchTitle = if (team1Name.isNotBlank() && team2Name.isNotBlank()) {
                                             "$team1Name vs $team2Name"
                                        } else {
                                            tournamentName.ifBlank { "Live Match" }
                                        }

                                        val parsedCountdown: Long? = when {
                                            countdownHours.isNotBlank() && countdownHours.toDoubleOrNull() != null -> {
                                                val hrs = countdownHours.toDouble()
                                                System.currentTimeMillis() + (hrs * 3600 * 1000L).toLong()
                                            }
                                            matchTimeFormatted.isNotBlank() -> {
                                                parseEventTimeStringToEpochMillis(matchTimeFormatted)
                                            }
                                            else -> null
                                        }

                                        val matchItem = MediaItem(
                                            id = "sport_${System.currentTimeMillis()}",
                                            title = matchTitle,
                                            tournament = tournamentName.ifBlank { null },
                                            category = sportCategory,
                                            type = MediaType.LIVE_EVENT,
                                            streamUrl = validServers.first().url,
                                            backupUrl = validServers.getOrNull(1)?.url,
                                            servers = validServers,
                                            isLive = sportStatus.contains("Live", ignoreCase = true),
                                            status = sportStatus,
                                            eventTime = matchTimeFormatted.ifBlank { sportStatus },
                                            team1 = team1Name.takeIf { it.isNotBlank() },
                                            team1Logo = team1LogoUrl.takeIf { it.isNotBlank() },
                                            team2 = team2Name.takeIf { it.isNotBlank() },
                                            team2Logo = team2LogoUrl.takeIf { it.isNotBlank() },
                                            matchTimeFormatted = matchTimeFormatted.takeIf { it.isNotBlank() },
                                            countdownTargetSeconds = parsedCountdown,
                                            score1 = team1Score.takeIf { it.isNotBlank() },
                                            score2 = team2Score.takeIf { it.isNotBlank() },
                                            quality = "1080p FHD"
                                        )

                                        repository.saveCustomStream(matchItem)
                                        coroutineScope.launch {
                                            repository.pushToFirebase(matchItem)
                                            val notif = AppNotification(
                                                title = "‚öΩ ‡¶®‡¶§‡ßÅ‡¶® ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö: ${matchItem.title}",
                                                message = if (!matchItem.tournament.isNullOrBlank()) "${matchItem.tournament} - ‡¶è‡¶ñ‡¶®‡¶á ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶â‡¶™‡¶≠‡ßã‡¶ó ‡¶ï‡¶∞‡ßÅ‡¶®!" else "‡¶®‡¶§‡ßÅ‡¶® ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶Ø‡ßã‡¶ó ‡¶ï‡¶∞‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá!",
                                                type = NotificationType.LIVE_EVENT,
                                                targetId = matchItem.id,
                                                imageUrl = matchItem.team1Logo ?: matchItem.team2Logo
                                            )
                                            repository.broadcastNotification(notif)
                                            NotificationHelper.showSystemNotification(context, notif)
                                            adminNotificationHistory = repository.getStoredNotifications()
                                        }
                                        onDataChanged()

                                        // Reset fields
                                        tournamentName = ""
                                        team1Name = ""
                                        team1Score = ""
                                        team1LogoUrl = ""
                                        team2Name = ""
                                        team2Score = ""
                                        team2LogoUrl = ""
                                        matchTimeFormatted = ""
                                        countdownHours = ""
                                        sportsServers = listOf(
                                            StreamServer("T SPORTS", ""),
                                            StreamServer("TT", ""),
                                            StreamServer("TEMP", "")
                                        )
                                        Toast.makeText(context, "‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá ‡¶™‡¶æ‡¶¨‡¶≤‡¶ø‡¶∂ ‡¶ì Firebase ‡¶è ‡¶∏‡¶Ç‡¶∞‡¶ï‡ßç‡¶∑‡¶ø‡¶§ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "‡¶ü‡¶ø‡¶Æ ‡¶è‡¶∞ ‡¶®‡¶æ‡¶Æ ‡¶è‡¶¨‡¶Ç ‡¶ï‡¶Æ‡¶™‡¶ï‡ßç‡¶∑‡ßá ‡¶è‡¶ï‡¶ü‡¶ø ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶≤‡¶ø‡¶ô‡ßç‡¶ï ‡¶≤‡¶ø‡¶ñ‡ßÅ‡¶®", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publish Match to Firebase", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Firestore Sports Events List Header
                item {
                    Text(
                        text = "Firestore Sports Events List:",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Sports Items List
                items(sportsList) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    color = Color(0xFF2563EB).copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB))
                                ) {
                                    Text(
                                        text = item.tournament ?: item.category,
                                        color = Color(0xFF60A5FA),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        maxLines = 1
                                    )
                                }

                                Surface(
                                    color = if (item.isLive) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (item.isLive) "Live" else "Upcoming",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Match Title & Scores
                            Text(
                                text = if (item.score1 != null && item.score2 != null && item.team1 != null && item.team2 != null) {
                                    "${item.team1} (${item.score1}) vs ${item.team2} (${item.score2})"
                                } else {
                                    item.title
                                },
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Servers Chip Count
                            val serverCount = item.getAllServers().size
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Dns, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$serverCount ‡¶ü‡¶ø ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶∏‡¶ï‡ßç‡¶∞‡¶ø‡ßü",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp
                                )
                            }

                            // Action Buttons (Edit Match, Update Score, Delete)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Full Edit Match Button
                                Button(
                                    onClick = {
                                        editingMatchItem = item
                                        editTournament = item.tournament ?: ""
                                        editSportCategory = item.category
                                        editSportStatus = if (item.isLive) "‚óè Live Now" else (item.status ?: "Upcoming")
                                        editTeam1Name = item.team1 ?: ""
                                        editTeam1Score = item.score1 ?: ""
                                        editTeam1Logo = item.team1Logo ?: ""
                                        editTeam2Name = item.team2 ?: ""
                                        editTeam2Score = item.score2 ?: ""
                                        editTeam2Logo = item.team2Logo ?: ""
                                        editMatchTime = item.matchTimeFormatted ?: item.eventTime ?: ""
                                        editCountdownHours = item.countdownTargetSeconds?.let { (it / 3600).toString() } ?: ""
                                        val curServers = item.getAllServers()
                                        editServers = if (curServers.isNotEmpty()) curServers else listOf(StreamServer("Server 1", item.streamUrl))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("‡¶ñ‡ßá‡¶≤‡¶æ ‡¶è‡¶°‡¶ø‡¶ü", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Quick Score Update Button
                                Button(
                                    onClick = {
                                        updatingItem = item
                                        updateScore1 = item.score1 ?: ""
                                        updateScore2 = item.score2 ?: ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Scoreboard, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("‡¶∏‡ßç‡¶ï‡ßã‡¶∞", fontSize = 12.sp)
                                }

                                IconButton(
                                    onClick = {
                                        itemToDelete = item
                                    }
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 2 CONTENT: LIVE TV CHANNELS ADMIN
            // -------------------------------------------------------------
            if (selectedAdminTab == AdminTab.CHANNELS) {
                // 1. Primary M3U Playlist Manager for Live TV Channels (User: "‡¶∏‡¶¨ ‡¶•‡ßá‡¶ï‡ßá ‡¶≠‡¶æ‡¶≤‡ßã ‡¶π‡¶Ø‡¶º ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶Ö‡¶™‡¶∂‡¶®‡ßá ‡¶è‡¶∞‡¶ï‡¶Æ ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶è‡¶° ‡¶ï‡¶∞‡¶æ ‡¶è‡¶°‡¶Æ‡¶ø‡¶® ‡¶™‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶•‡ßá‡¶ï‡ßá")
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Link, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("üì° ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶ü‡¶ø‡¶≠‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶≤‡¶ø‡¶Ç‡¶ï (Live TV M3U)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(
                                text = "‡¶è‡¶ñ‡¶æ‡¶®‡ßá ‡¶è‡¶ï ‡¶¨‡¶æ ‡¶è‡¶ï‡¶æ‡¶ß‡¶ø‡¶ï M3U ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶¶‡¶ø‡¶§‡ßá ‡¶™‡¶æ‡¶∞‡¶¨‡ßá‡¶® (‡¶™‡ßç‡¶∞‡¶§‡¶ø ‡¶≤‡¶æ‡¶á‡¶®‡ßá ‡¶è‡¶ï‡¶ü‡¶ø ‡¶ï‡¶∞‡ßá ‡¶Ö‡¶•‡¶¨‡¶æ ‡¶ï‡¶Æ‡¶æ ‡¶¶‡¶ø‡ßü‡ßá)‡•§ ‡¶∏‡¶∞‡¶æ‡¶∏‡¶∞‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶≤‡ßã‡¶° ‡¶π‡¶¨‡ßá ‡¶ì ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶•‡¶æ‡¶ï‡¶¨‡ßá:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            OutlinedTextField(
                                value = liveTvM3uInput,
                                onValueChange = { liveTvM3uInput = it },
                                placeholder = { Text("https://url1.m3u\nhttps://url2.m3u", color = Color(0xFF64748B), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                minLines = 2,
                                maxLines = 5
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (liveTvM3uInput.isNotBlank()) {
                                            val url = liveTvM3uInput.trim()
                                            repository.saveLiveTvM3uUrl(url)
                                            coroutineScope.launch {
                                                repository.pushAppConfigToFirebase(liveTvM3u = url)
                                            }
                                            onDataChanged()
                                            Toast.makeText(context, "‚úÖ ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶ü‡¶ø‡¶≠‡¶ø M3U ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶∏‡ßá‡¶≠ ‡¶ì ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "‡¶∏‡¶†‡¶ø‡¶ï M3U ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶¶‡¶ø‡¶®", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1.3f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("‡¶∏‡ßá‡¶≠ ‡¶ì ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        liveTvM3uInput = MediaRepository.DEFAULT_LIVE_TV_M3U_URL
                                        repository.saveLiveTvM3uUrl(MediaRepository.DEFAULT_LIVE_TV_M3U_URL)
                                        coroutineScope.launch {
                                            repository.pushAppConfigToFirebase(liveTvM3u = MediaRepository.DEFAULT_LIVE_TV_M3U_URL)
                                        }
                                        onDataChanged()
                                        Toast.makeText(context, "‡¶°‡¶ø‡¶´‡¶≤‡ßç‡¶ü Nafitv24.m3u ‡¶≤‡¶ø‡¶ô‡ßç‡¶ï ‡¶∞‡¶ø‡¶∏‡ßá‡¶ü ‡¶ì ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá‡¶õ‡ßá", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                                ) {
                                    Text("‡¶°‡¶ø‡¶´‡¶≤‡ßç‡¶ü ‡¶≤‡¶ø‡¶Ç‡¶ï", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "‚ûï Add Single Custom Channel (‡¶è‡¶ï‡¶ï ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            OutlinedTextField(
                                value = channelName,
                                onValueChange = { channelName = it },
                                placeholder = { Text("Channel Name (e.g. T Sports HD)", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = server1Url,
                                onValueChange = { server1Url = it },
                                placeholder = { Text("Server 1 (Primary Stream URL)", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = server2Url,
                                onValueChange = { server2Url = it },
                                placeholder = { Text("Server 2 (Backup Stream URL - Optional)", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = channelLogoUrl,
                                onValueChange = { channelLogoUrl = it },
                                placeholder = { Text("Channel Logo URL (Optional)", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    if (channelName.isNotBlank() && server1Url.isNotBlank()) {
                                        val servers = mutableListOf<StreamServer>()
                                        servers.add(StreamServer("‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡ßß (Main)", server1Url.trim()))
                                        if (server2Url.isNotBlank()) servers.add(StreamServer("‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡ß® (Backup)", server2Url.trim()))

                                        val item = MediaItem(
                                            id = "tv_${System.currentTimeMillis()}",
                                            title = channelName.trim(),
                                            category = channelCategory,
                                            type = MediaType.LIVE_TV,
                                            streamUrl = server1Url.trim(),
                                            backupUrl = server2Url.trim().takeIf { it.isNotBlank() },
                                            servers = servers,
                                            logoUrl = channelLogoUrl.trim().takeIf { it.isNotBlank() },
                                            isLive = true
                                        )
                                        repository.saveCustomStream(item)
                                        coroutineScope.launch {
                                            repository.pushToFirebase(item)
                                            val notif = AppNotification(
                                                title = "üì∫ ‡¶®‡¶§‡ßÅ‡¶® ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤: ${item.title}",
                                                message = "${item.category} ‡¶ï‡ßç‡¶Ø‡¶æ‡¶ü‡¶æ‡¶ó‡¶∞‡¶ø‡¶§‡ßá ‡¶®‡¶§‡ßÅ‡¶® ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶Ø‡ßÅ‡¶ï‡ßç‡¶§ ‡¶π‡ßü‡ßá‡¶õ‡ßá‡•§ ‡¶â‡¶™‡¶≠‡ßã‡¶ó ‡¶ï‡¶∞‡ßÅ‡¶®!",
                                                type = NotificationType.LIVE_TV,
                                                targetId = item.id,
                                                imageUrl = item.logoUrl
                                            )
                                            repository.broadcastNotification(notif)
                                            NotificationHelper.showSystemNotification(context, notif)
                                            adminNotificationHistory = repository.getStoredNotifications()
                                        }
                                        onDataChanged()
                                        channelName = ""
                                        server1Url = ""
                                        server2Url = ""
                                        channelLogoUrl = ""
                                        Toast.makeText(context, "‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶™‡¶æ‡¶¨‡¶≤‡¶ø‡¶∂ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Publish Channel to Firestore", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(liveTvList) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${item.category} ‚Ä¢ ${item.getAllServers().size} ‡¶ü‡¶ø ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                            // Edit Channel Button
                            Button(
                                onClick = {
                                    editingChannelItem = item
                                    editChannelName = item.title
                                    editChannelCategory = item.category
                                    editChannelLogoUrl = item.logoUrl ?: ""
                                    val curServers = item.getAllServers()
                                    editChannelServers = if (curServers.isNotEmpty()) curServers else listOf(StreamServer("‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡ßß (Main)", item.streamUrl))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("‡¶è‡¶°‡¶ø‡¶ü", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    itemToDelete = item
                                }
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 3 CONTENT: MOVIES & SERIES ADMIN
            // -------------------------------------------------------------
            if (selectedAdminTab == AdminTab.MOVIES) {
                // 1. Primary M3U Playlist Manager for Movies (User: "‡¶è‡¶¨‡¶Ç ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶Ö‡¶™‡¶∂‡¶®‡ßá‡¶ì ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø ‡¶≤‡¶ø‡¶ô‡ßç‡¶ï ‡¶è‡¶° ‡¶ï‡¶∞‡¶æ ‡¶è‡¶§‡ßá firebase ‡¶â‡¶™‡¶∞ ‡¶ö‡¶æ‡¶™ ‡¶™‡¶°‡¶º‡¶¨‡ßá ‡¶ï‡¶Æ")
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Movie, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("üé¨ ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶ì ‡¶∏‡¶ø‡¶∞‡¶ø‡¶ú ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶≤‡¶ø‡¶Ç‡¶ï (Movies M3U)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(
                                text = "‡¶è‡¶ñ‡¶æ‡¶®‡ßá ‡¶è‡¶ï ‡¶¨‡¶æ ‡¶è‡¶ï‡¶æ‡¶ß‡¶ø‡¶ï ‡¶Æ‡ßÅ‡¶≠‡¶ø M3U ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶≤‡¶ø‡¶ô‡ßç‡¶ï ‡¶Ø‡ßã‡¶ó ‡¶ï‡¶∞‡¶§‡ßá ‡¶™‡¶æ‡¶∞‡¶¨‡ßá‡¶® (‡¶™‡ßç‡¶∞‡¶§‡¶ø ‡¶≤‡¶æ‡¶á‡¶®‡ßá ‡¶è‡¶ï‡¶ü‡¶ø ‡¶ï‡¶∞‡ßá ‡¶Ö‡¶•‡¶¨‡¶æ ‡¶ï‡¶Æ‡¶æ ‡¶¶‡¶ø‡ßü‡ßá)‡•§ ‡¶è‡¶§‡ßá Firebase ‡¶è ‡¶ö‡¶æ‡¶™ ‡¶™‡ßú‡¶¨‡ßá ‡¶®‡¶æ ‡¶è‡¶¨‡¶Ç ‡¶∏‡¶π‡¶ú‡ßá ‡¶¨‡ßç‡¶∞‡¶æ‡¶â‡¶ú ‡¶ï‡¶∞‡¶æ ‡¶Ø‡¶æ‡¶¨‡ßá:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            OutlinedTextField(
                                value = moviesM3uInput,
                                onValueChange = { moviesM3uInput = it },
                                placeholder = { Text("https://url1.m3u\nhttps://url2.m3u", color = Color(0xFF64748B), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                minLines = 2,
                                maxLines = 5
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (moviesM3uInput.isNotBlank()) {
                                            val url = moviesM3uInput.trim()
                                            repository.saveMoviesM3uUrl(url)
                                            coroutineScope.launch {
                                                repository.pushAppConfigToFirebase(moviesM3u = url)
                                            }
                                            onDataChanged()
                                            Toast.makeText(context, "‚úÖ ‡¶Æ‡ßÅ‡¶≠‡¶ø M3U ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶∏‡ßá‡¶≠ ‡¶ì ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "‡¶Ö‡¶®‡ßÅ‡¶ó‡ßç‡¶∞‡¶π ‡¶ï‡¶∞‡ßá ‡¶è‡¶ï‡¶ü‡¶ø ‡¶∏‡¶†‡¶ø‡¶ï ‡¶Æ‡ßÅ‡¶≠‡¶ø M3U URL ‡¶¶‡¶ø‡¶®", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1.3f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("‡¶∏‡ßá‡¶≠ ‡¶ì ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        moviesM3uInput = MediaRepository.DEFAULT_MOVIES_M3U_URL
                                        repository.saveMoviesM3uUrl(MediaRepository.DEFAULT_MOVIES_M3U_URL)
                                        coroutineScope.launch {
                                            repository.pushAppConfigToFirebase(moviesM3u = MediaRepository.DEFAULT_MOVIES_M3U_URL)
                                        }
                                        onDataChanged()
                                        Toast.makeText(context, "‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶∞‡¶ø‡¶∏‡ßá‡¶ü ‡¶ì ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá‡¶õ‡ßá", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                                ) {
                                    Text("‡¶°‡¶ø‡¶´‡¶≤‡ßç‡¶ü ‡¶≤‡¶ø‡¶Ç‡¶ï", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "‚ûï Add Single Movie or Series (‡¶è‡¶ï‡¶ï ‡¶Æ‡ßÅ‡¶≠‡¶ø)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            OutlinedTextField(
                                value = movieTitle,
                                onValueChange = { movieTitle = it },
                                placeholder = { Text("Movie Title (e.g. Toofan 2026)", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = server1Url,
                                onValueChange = { server1Url = it },
                                placeholder = { Text("Server 1 (Video Stream URL)", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = server2Url,
                                onValueChange = { server2Url = it },
                                placeholder = { Text("Server 2 (Fast Alternative URL)", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = moviePosterUrl,
                                onValueChange = { moviePosterUrl = it },
                                placeholder = { Text("Poster Image URL", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    if (movieTitle.isNotBlank() && server1Url.isNotBlank()) {
                                        val servers = mutableListOf<StreamServer>()
                                        servers.add(StreamServer("‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡ßß (HD)", server1Url.trim()))
                                        if (server2Url.isNotBlank()) servers.add(StreamServer("‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡ß® (4K)", server2Url.trim()))

                                        val item = MediaItem(
                                            id = "mov_${System.currentTimeMillis()}",
                                            title = movieTitle.trim(),
                                            category = movieCategory,
                                            type = MediaType.MOVIE,
                                            streamUrl = server1Url.trim(),
                                            backupUrl = server2Url.trim().takeIf { it.isNotBlank() },
                                            servers = servers,
                                            logoUrl = moviePosterUrl.trim().takeIf { it.isNotBlank() },
                                            isLive = false
                                        )
                                        repository.saveCustomStream(item)
                                        coroutineScope.launch {
                                            repository.pushToFirebase(item)
                                            val notif = AppNotification(
                                                title = "üé¨ ‡¶®‡¶§‡ßÅ‡¶® ‡¶Æ‡ßÅ‡¶≠‡¶ø: ${item.title}",
                                                message = "${item.category} ‡¶ï‡ßç‡¶Ø‡¶æ‡¶ü‡¶æ‡¶ó‡¶∞‡¶ø‡¶§‡ßá ‡¶®‡¶§‡ßÅ‡¶® ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶Ø‡ßã‡¶ó ‡¶ï‡¶∞‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá‡•§ ‡¶è‡¶ñ‡¶®‡¶á ‡¶¶‡ßá‡¶ñ‡ßÅ‡¶®!",
                                                type = NotificationType.MOVIE,
                                                targetId = item.id,
                                                imageUrl = item.logoUrl
                                            )
                                            repository.broadcastNotification(notif)
                                            NotificationHelper.showSystemNotification(context, notif)
                                            adminNotificationHistory = repository.getStoredNotifications()
                                        }
                                        onDataChanged()
                                        movieTitle = ""
                                        server1Url = ""
                                        server2Url = ""
                                        moviePosterUrl = ""
                                        Toast.makeText(context, "‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶Ø‡ßÅ‡¶ï‡ßç‡¶§ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Publish Movie to Firestore", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(moviesList) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${item.category} ‚Ä¢ ${item.getAllServers().size} ‡¶ü‡¶ø ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                            // Edit Movie Button
                            Button(
                                onClick = {
                                    editingMovieItem = item
                                    editMovieTitle = item.title
                                    editMovieCategory = item.category
                                    editMoviePosterUrl = item.logoUrl ?: ""
                                    editMovieDesc = item.description ?: ""
                                    val curServers = item.getAllServers()
                                    editMovieServers = if (curServers.isNotEmpty()) curServers else listOf(StreamServer("‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡ßß (HD)", item.streamUrl))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("‡¶è‡¶°‡¶ø‡¶ü", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    itemToDelete = item
                                }
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 4 CONTENT: PLAYLISTS ADMIN (M3U Playlists Management)
            // -------------------------------------------------------------
            if (selectedAdminTab == AdminTab.PLAYLISTS) {
                // Central M3U Links Hub
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.FeaturedPlayList, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("üåê ‡¶∏‡ßá‡¶®‡ßç‡¶ü‡ßç‡¶∞‡¶æ‡¶≤ M3U ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶ï‡¶®‡¶´‡¶ø‡¶ó‡¶æ‡¶∞‡ßá‡¶∂‡¶®", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(
                                text = "‡¶è‡¶ñ‡¶æ‡¶®‡ßá ‡¶•‡¶æ‡¶ï‡¶æ M3U ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶•‡ßá‡¶ï‡ßá ‡¶∏‡¶∞‡¶æ‡¶∏‡¶∞‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤, ‡¶ñ‡ßá‡¶≤‡¶æ ‡¶ì ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶≤‡ßã‡¶° ‡¶π‡¶¨‡ßá (Firebase ‡¶°‡ßá‡¶ü‡¶æ‡¶¨‡ßá‡¶∏‡ßá ‡¶≤‡ßã‡¶° ‡¶π‡¶¨‡ßá ‡¶®‡¶æ)‡•§ ‡¶™‡ßç‡¶∞‡¶§‡¶ø‡¶ü‡¶ø‡¶§‡ßá ‡¶è‡¶ï ‡¶¨‡¶æ ‡¶è‡¶ï‡¶æ‡¶ß‡¶ø‡¶ï M3U ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶¶‡¶ø‡¶§‡ßá ‡¶™‡¶æ‡¶∞‡ßá‡¶® (‡¶™‡ßç‡¶∞‡¶§‡¶ø ‡¶≤‡¶æ‡¶á‡¶®‡ßá ‡¶è‡¶ï‡¶ü‡¶ø ‡¶ï‡¶∞‡ßá ‡¶Ö‡¶•‡¶¨‡¶æ ‡¶ï‡¶Æ‡¶æ ‡¶¶‡¶ø‡ßü‡ßá):",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )

                            // 1. Live TV Channels M3U URL
                            Text("1. ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶ü‡¶ø‡¶≠‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ M3U URL (‡¶è‡¶ï‡¶æ‡¶ß‡¶ø‡¶ï ‡¶¶‡ßá‡¶ì‡ßü‡¶æ ‡¶Ø‡¶æ‡¶¨‡ßá):", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = liveTvM3uInput,
                                onValueChange = { liveTvM3uInput = it },
                                placeholder = { Text("https://url1.m3u\nhttps://url2.m3u", color = Color(0xFF64748B), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                minLines = 2,
                                maxLines = 5
                            )

                            // 2. Sports Matches M3U URL
                            Text("2. ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶∏‡ßç‡¶™‡ßã‡¶∞‡ßç‡¶ü‡¶∏ ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö M3U URL (‡¶è‡¶ï‡¶æ‡¶ß‡¶ø‡¶ï ‡¶¶‡ßá‡¶ì‡ßü‡¶æ ‡¶Ø‡¶æ‡¶¨‡ßá):", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = sportsM3uInput,
                                onValueChange = { sportsM3uInput = it },
                                placeholder = { Text("https://url1.m3u\nhttps://url2.m3u", color = Color(0xFF64748B), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                minLines = 2,
                                maxLines = 5
                            )

                            // 3. Movies M3U URL
                            Text("3. ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶ì ‡¶∏‡¶ø‡¶∞‡¶ø‡¶ú M3U URL (‡¶è‡¶ï‡¶æ‡¶ß‡¶ø‡¶ï ‡¶¶‡ßá‡¶ì‡ßü‡¶æ ‡¶Ø‡¶æ‡¶¨‡ßá):", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = moviesM3uInput,
                                onValueChange = { moviesM3uInput = it },
                                placeholder = { Text("https://url1.m3u\nhttps://url2.m3u", color = Color(0xFF64748B), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                minLines = 2,
                                maxLines = 5
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val liveUrl = liveTvM3uInput.trim()
                                        val sportsUrl = sportsM3uInput.trim()
                                        val movUrl = moviesM3uInput.trim()
                                        repository.saveLiveTvM3uUrl(liveUrl)
                                        repository.saveSportsM3uUrl(sportsUrl)
                                        repository.saveMoviesM3uUrl(movUrl)
                                        coroutineScope.launch {
                                            repository.pushAppConfigToFirebase(liveTvM3u = liveUrl, sportsM3u = sportsUrl, moviesM3u = movUrl)
                                        }
                                        onDataChanged()
                                        Toast.makeText(context, "‚úÖ ‡¶∏‡¶ï‡¶≤ M3U ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶∏‡ßá‡¶≠ ‡¶ì ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶∏‡¶Æ‡ßç‡¶™‡¶®‡ßç‡¶® ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1.3f).height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("‡¶∏‡¶¨ M3U ‡¶∏‡ßá‡¶≠ ‡¶ì ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        liveTvM3uInput = MediaRepository.DEFAULT_LIVE_TV_M3U_URL
                                        sportsM3uInput = MediaRepository.DEFAULT_SPORTS_M3U_URL
                                        moviesM3uInput = MediaRepository.DEFAULT_MOVIES_M3U_URL
                                        repository.saveLiveTvM3uUrl(MediaRepository.DEFAULT_LIVE_TV_M3U_URL)
                                        repository.saveSportsM3uUrl(MediaRepository.DEFAULT_SPORTS_M3U_URL)
                                        repository.saveMoviesM3uUrl(MediaRepository.DEFAULT_MOVIES_M3U_URL)
                                        coroutineScope.launch {
                                            repository.pushAppConfigToFirebase(
                                                liveTvM3u = MediaRepository.DEFAULT_LIVE_TV_M3U_URL,
                                                sportsM3u = MediaRepository.DEFAULT_SPORTS_M3U_URL,
                                                moviesM3u = MediaRepository.DEFAULT_MOVIES_M3U_URL
                                            )
                                        }
                                        onDataChanged()
                                        Toast.makeText(context, "‡¶∏‡¶ï‡¶≤ ‡¶°‡¶ø‡¶´‡¶≤‡ßç‡¶ü M3U ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶∞‡¶ø‡¶∏‡ßá‡¶ü ‡¶ì ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá‡¶õ‡ßá", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                                ) {
                                    Text("‡¶°‡¶ø‡¶´‡¶≤‡ßç‡¶ü ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶∞‡¶ø‡¶∏‡ßá‡¶ü", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "‚ûï Add Custom M3U Playlist (‡¶ï‡¶æ‡¶∏‡ßç‡¶ü‡¶Æ ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            OutlinedTextField(
                                value = playlistTitle,
                                onValueChange = { playlistTitle = it },
                                placeholder = { Text("Playlist Name (e.g. NAFI TV 24 Official)", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = playlistUrl,
                                onValueChange = { playlistUrl = it },
                                placeholder = { Text("Playlist M3U / M3U8 URL (Raw Github/Server)", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = playlistLogoUrl,
                                onValueChange = { playlistLogoUrl = it },
                                placeholder = { Text("Logo / Banner Image URL (Optional)", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = playlistDescription,
                                onValueChange = { playlistDescription = it },
                                placeholder = { Text("Short Description (Optional)", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    if (playlistTitle.isNotBlank() && playlistUrl.isNotBlank()) {
                                        val newPl = PlaylistInfo(
                                            id = "pl_admin_${System.currentTimeMillis()}",
                                            title = playlistTitle.trim(),
                                            url = playlistUrl.trim(),
                                            logoUrl = playlistLogoUrl.trim().takeIf { it.isNotBlank() },
                                            description = playlistDescription.trim().takeIf { it.isNotBlank() },
                                            channelCount = 0
                                        )
                                        repository.saveAdminPlaylist(newPl)
                                        coroutineScope.launch {
                                            repository.pushPlaylistToFirebase(newPl)
                                        }
                                        onDataChanged()
                                        playlistTitle = ""
                                        playlistUrl = ""
                                        playlistLogoUrl = ""
                                        playlistDescription = ""
                                        Toast.makeText(context, "‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶Ø‡ßÅ‡¶ï‡ßç‡¶§ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Publish Playlist to Firestore", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(playlistsList) { playlist ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!playlist.logoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = playlist.logoUrl,
                                        contentDescription = playlist.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = Color(0xFF00E5FF))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlist.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(playlist.url, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            // Edit Playlist Button
                            Button(
                                onClick = {
                                    editingPlaylistItem = playlist
                                    editPlaylistTitle = playlist.title
                                    editPlaylistUrl = playlist.url
                                    editPlaylistLogoUrl = playlist.logoUrl ?: ""
                                    editPlaylistDescription = playlist.description ?: ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("‡¶è‡¶°‡¶ø‡¶ü", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    playlistToDelete = playlist
                                }
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 5 CONTENT: IN-APP UPDATE & VERSION MANAGEMENT
            // -------------------------------------------------------------
            if (selectedAdminTab == AdminTab.APP_UPDATE) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "‡¶á‡¶®-‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™ ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ì ‡¶≠‡¶æ‡¶∞‡ßç‡¶∏‡¶® ‡¶ï‡¶®‡ßç‡¶ü‡ßç‡¶∞‡ßã‡¶≤",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "‡¶è‡¶ñ‡¶æ‡¶®‡ßá ‡¶®‡¶§‡ßÅ‡¶® ‡¶≠‡¶æ‡¶∞‡ßç‡¶∏‡¶® ‡¶ì ‡¶°‡¶ø‡¶∞‡ßá‡¶ï‡ßç‡¶ü APK ‡¶°‡¶æ‡¶â‡¶®‡¶≤‡ßã‡¶° ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶¶‡¶ø‡ßü‡ßá ‡¶™‡¶æ‡¶¨‡¶≤‡¶ø‡¶∂ ‡¶ï‡¶∞‡¶≤‡ßá ‡¶∏‡¶Æ‡¶∏‡ßç‡¶§ ‡¶á‡¶â‡¶ú‡¶æ‡¶∞‡¶¶‡ßá‡¶∞ ‡¶°‡¶ø‡¶≠‡¶æ‡¶á‡¶∏‡ßá ‡¶∏‡ßÅ‡¶®‡ßç‡¶¶‡¶∞ ‡¶á‡¶®-‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™ ‡¶°‡¶æ‡¶â‡¶®‡¶≤‡ßã‡¶° ‡¶ì ‡¶á‡¶®‡¶∏‡ßç‡¶ü‡¶≤ ‡¶™‡¶™‡¶Ü‡¶™ ‡¶ö‡¶≤‡ßá ‡¶Ø‡¶æ‡¶¨‡ßá‡•§",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )

                            // Current vs Cloud Version Status Banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("‡¶¨‡¶∞‡ßç‡¶§‡¶Æ‡¶æ‡¶® ‡¶¨‡¶ø‡¶≤‡ßç‡¶° ‡¶≠‡¶æ‡¶∞‡ßç‡¶∏‡¶®:", color = Color(0xFF64748B), fontSize = 11.sp)
                                        Text("v${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                                    ) {
                                        Text(
                                            text = "‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°: v$updateVersionName (Code $updateVersionCode)",
                                            color = Color(0xFF34D399),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Version Code and Version Name Fields
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = updateVersionCode,
                                    onValueChange = { updateVersionCode = it },
                                    label = { Text("Version Code (‡¶Ø‡ßá‡¶Æ‡¶® 26, 27)", fontSize = 11.sp) },
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = updateVersionName,
                                    onValueChange = { updateVersionName = it },
                                    label = { Text("Version Name (‡¶Ø‡ßá‡¶Æ‡¶® 2.5.2)", fontSize = 11.sp) },
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        updateVersionCode = (com.example.BuildConfig.VERSION_CODE + 1).toString()
                                        updateVersionName = "2.5.3"
                                        Toast.makeText(context, "‡¶™‡¶∞‡¶¨‡¶∞‡ßç‡¶§‡ßÄ ‡¶≠‡¶æ‡¶∞‡ßç‡¶∏‡¶® ‡¶∏‡ßá‡¶ü ‡¶ï‡¶∞‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá (v2.5.3, Code ${com.example.BuildConfig.VERSION_CODE + 1})", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("v2.5.3 ‡¶∏‡ßá‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶® (Next)", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        updateVersionCode = com.example.BuildConfig.VERSION_CODE.toString()
                                        updateVersionName = com.example.BuildConfig.VERSION_NAME
                                        Toast.makeText(context, "‡¶¨‡¶∞‡ßç‡¶§‡¶Æ‡¶æ‡¶® ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™ ‡¶≠‡¶æ‡¶∞‡ßç‡¶∏‡¶® ‡¶™‡ßÇ‡¶∞‡¶£ ‡¶ï‡¶∞‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá (v${com.example.BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Sync, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("‡¶¨‡¶∞‡ßç‡¶§‡¶Æ‡¶æ‡¶® ‡¶≠‡¶æ‡¶∞‡ßç‡¶∏‡¶® (v${com.example.BuildConfig.VERSION_NAME})", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Direct APK Download URL
                            OutlinedTextField(
                                value = updateDownloadUrl,
                                onValueChange = { updateDownloadUrl = it },
                                label = { Text("Direct APK Download URL (.apk ‡¶≤‡¶ø‡¶Ç‡¶ï)") },
                                placeholder = { Text("https://example.com/nafitv24_v2.5.apk", color = Color(0xFF475569)) },
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // APK Size & Release Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = updateApkSize,
                                    onValueChange = { updateApkSize = it },
                                    label = { Text("APK Size (‡¶Ø‡ßá‡¶Æ‡¶® 18.5 MB)", fontSize = 11.sp) },
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = updateReleaseDate,
                                    onValueChange = { updateReleaseDate = it },
                                    label = { Text("Release Date (‡¶Ø‡ßá‡¶Æ‡¶® 15 Aug 2026)", fontSize = 11.sp) },
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            // Release Notes Field
                            OutlinedTextField(
                                value = updateReleaseNotes,
                                onValueChange = { updateReleaseNotes = it },
                                label = { Text("‡¶®‡¶§‡ßÅ‡¶® ‡¶´‡¶ø‡¶ö‡¶æ‡¶∞ ‡¶ì ‡¶ö‡ßá‡¶û‡ßç‡¶ú‡¶≤‡¶ó (Release Notes)") },
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Mandatory Force Update Toggle Switch
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("‡¶¨‡¶æ‡¶ß‡ßç‡¶Ø‡¶§‡¶æ‡¶Æ‡ßÇ‡¶≤‡¶ï ‡¶Ü‡¶™‡¶°‡ßá‡¶ü (Force Update)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("‡¶ö‡¶æ‡¶≤‡ßÅ ‡¶∞‡¶æ‡¶ñ‡¶≤‡ßá ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶®‡¶æ ‡¶ï‡¶∞‡¶æ ‡¶™‡¶∞‡ßç‡¶Ø‡¶®‡ßç‡¶§ ‡¶¨‡ßç‡¶Ø‡¶¨‡¶π‡¶æ‡¶∞‡¶ï‡¶æ‡¶∞‡ßÄ ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™‡ßá‡¶∞ ‡¶Æ‡ßá‡¶®‡ßÅ‡¶§‡ßá ‡¶¢‡ßÅ‡¶ï‡¶§‡ßá ‡¶™‡¶æ‡¶∞‡¶¨‡ßá ‡¶®‡¶æ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = updateIsForce,
                                        onCheckedChange = { updateIsForce = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFFEF4444)
                                        )
                                    )
                                }
                            }

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        previewUpdateDialog = AppUpdateInfo(
                                            versionCode = updateVersionCode.toIntOrNull() ?: 2,
                                            versionName = updateVersionName.ifBlank { "2.5.0" },
                                            downloadUrl = updateDownloadUrl,
                                            releaseNotes = updateReleaseNotes,
                                            isForceUpdate = updateIsForce,
                                            apkSize = updateApkSize,
                                            releaseDate = updateReleaseDate
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                                ) {
                                    Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("‡¶™‡¶™‡¶Ü‡¶™ ‡¶™‡ßç‡¶∞‡¶ø‡¶≠‡¶ø‡¶â", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val enteredCode = updateVersionCode.toIntOrNull() ?: (com.example.BuildConfig.VERSION_CODE + 1)
                                        val isNewer = com.example.util.AppUpdateHelper.isVersionNameNewer(updateVersionName, com.example.BuildConfig.VERSION_NAME)
                                        val finalCode = if (isNewer && enteredCode <= com.example.BuildConfig.VERSION_CODE) {
                                            com.example.BuildConfig.VERSION_CODE + 1
                                        } else {
                                            enteredCode
                                        }

                                        val info = AppUpdateInfo(
                                            versionCode = finalCode,
                                            versionName = updateVersionName.ifBlank { "2.5.3" },
                                            downloadUrl = updateDownloadUrl.trim(),
                                            releaseNotes = updateReleaseNotes.trim(),
                                            isForceUpdate = updateIsForce,
                                            apkSize = updateApkSize.trim(),
                                            releaseDate = updateReleaseDate.trim()
                                        )
                                        coroutineScope.launch {
                                            isSavingUpdate = true
                                            val ok = repository.pushAppUpdateInfo(info)
                                            isSavingUpdate = false
                                            if (ok) {
                                                Toast.makeText(context, "‡¶®‡¶§‡ßÅ‡¶® ‡¶≠‡¶æ‡¶∞‡ßç‡¶∏‡¶® (v${info.versionName}, Code ${info.versionCode}) ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá Firebase ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶™‡¶æ‡¶¨‡¶≤‡¶ø‡¶∂ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Firebase ‡¶è ‡¶∏‡ßá‡¶≠ ‡¶π‡ßü‡ßá‡¶õ‡ßá ‡¶ì ‡¶ï‡ßç‡¶Ø‡¶æ‡¶∂‡ßá ‡¶∏‡¶Ç‡¶∞‡¶ï‡ßç‡¶∑‡¶ø‡¶§ ‡¶π‡ßü‡ßá‡¶õ‡ßá", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isSavingUpdate) "‡¶™‡¶æ‡¶¨‡¶≤‡¶ø‡¶∂ ‡¶π‡¶ö‡ßç‡¶õ‡ßá..." else "Firebase ‡¶è ‡¶™‡¶æ‡¶¨‡¶≤‡¶ø‡¶∂ ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB: CLOUDSTREAM EXTENSION REPOSITORIES & MOVIE SITES ADMIN
            // -------------------------------------------------------------
            if (selectedAdminTab == AdminTab.REPOSITORIES) {
                // 1. CloudStream Extension Repository Importer Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Extension, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("üé¨ CloudStream ‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø ‡¶á‡¶Æ‡¶™‡ßã‡¶∞‡ßç‡¶ü‡¶æ‡¶∞", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Text(
                                text = "CloudStream ‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶Ø‡ßã‡¶ó ‡¶ï‡¶∞‡ßÅ‡¶®‡•§ ‡¶è‡¶á ‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø‡¶∞ ‡¶∏‡¶ï‡¶≤ ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶ì‡ßü‡ßá‡¶¨‡¶∏‡¶æ‡¶á‡¶ü ‡¶è‡¶¨‡¶Ç ‡¶™‡ßç‡¶≤‡¶æ‡¶ó‡¶á‡¶® ‡¶∏‡ßç‡¶¨‡ßü‡¶Ç‡¶ï‡ßç‡¶∞‡¶ø‡ßü‡¶≠‡¶æ‡¶¨‡ßá ‡¶á‡¶â‡¶ú‡¶æ‡¶∞ ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶Ö‡¶™‡¶∂‡¶®‡ßá ‡¶™‡ßç‡¶∞‡¶¨‡ßá‡¶∂‡¶Ø‡ßã‡¶ó‡ßç‡¶Ø ‡¶π‡¶¨‡ßá ‡¶è‡¶¨‡¶Ç ‡¶´‡¶æ‡ßü‡¶æ‡¶∞‡¶¨‡ßá‡¶∏‡ßá ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶•‡¶æ‡¶ï‡¶¨‡ßá‡•§",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            // Preset Fast-Add Chips
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("‚ö° ‡¶¶‡ßç‡¶∞‡ßÅ‡¶§ ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶° ‡¶ï‡¶∞‡¶æ‡¶∞ ‡¶ú‡¶®‡ßç‡¶Ø ‡¶™‡ßç‡¶∞‡¶ø‡¶∏‡ßá‡¶ü ‡¶ö‡¶ø‡¶™‡¶∏:", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)),
                                        modifier = Modifier.clickable {
                                            repoUrlInput = "cloudstreamrepo://raw.githubusercontent.com/Hexated/cloudstream-extensions-hexated/builds/repo.json"
                                        }
                                    ) {
                                        Text("‚ö° Hexated Repo", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6)),
                                        modifier = Modifier.clickable {
                                            repoUrlInput = "cloudstreamrepo://raw.githubusercontent.com/stormunblessed/cloudstream-extensions-storm/refs/heads/builds/repo.json"
                                        }
                                    ) {
                                        Text("üå™Ô∏è Storm Repo", color = Color(0xFFDDD6FE), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                                        modifier = Modifier.clickable {
                                            repoUrlInput = "cloudstreamrepo://raw.githubusercontent.com/stormunblessed/cloudstream-extensions-storm/refs/heads/builds/repo.json"
                                        }
                                    ) {
                                        Text("üöÄ Storm", color = Color(0xFFA7F3D0), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = repoUrlInput,
                                onValueChange = { repoUrlInput = it },
                                label = { Text("CloudStream Repo URL (cloudstreamrepo:// ‡¶¨‡¶æ https://)") },
                                placeholder = { Text("cloudstreamrepo://raw.githubusercontent.com/.../repo.json", color = Color(0xFF64748B), fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                maxLines = 3
                            )

                            Button(
                                onClick = {
                                    val url = repoUrlInput.trim()
                                    if (url.isBlank()) {
                                        Toast.makeText(context, "‡¶Ö‡¶®‡ßÅ‡¶ó‡ßç‡¶∞‡¶π ‡¶ï‡¶∞‡ßá ‡¶è‡¶ï‡¶ü‡¶ø ‡¶∏‡¶†‡¶ø‡¶ï CloudStream Repo URL ‡¶¶‡¶ø‡¶®", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        isFetchingRepo = true
                                        try {
                                            val parsedRepo = repository.parseCloudStreamRepo(url)
                                            if (parsedRepo != null && parsedRepo.providers.isNotEmpty()) {
                                                val existing = repository.getSavedCloudStreamRepos()
                                                val updated = (listOf(parsedRepo) + existing.filterNot { it.id == parsedRepo.id })
                                                repository.saveCloudStreamRepos(updated)
                                                repository.pushCloudStreamReposToFirebase(updated)
                                                repoUrlInput = ""
                                                onDataChanged()
                                                Toast.makeText(
                                                    context,
                                                    "‚úÖ ${parsedRepo.name} (${parsedRepo.providers.size} ‡¶ü‡¶ø ‡¶∏‡¶æ‡¶á‡¶ü/‡¶è‡¶ï‡ßç‡¶∏‡¶ü‡ßá‡¶®‡¶∂‡¶®) ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá ‡¶è‡¶° ‡¶π‡ßü‡ßá‡¶õ‡ßá ‡¶ì Firebase ‡¶è ‡¶∏‡ßá‡¶≠ ‡¶π‡ßü‡ßá‡¶õ‡ßá!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø ‡¶´‡ßá‡¶ö ‡¶ï‡¶∞‡¶æ ‡¶Ø‡¶æ‡ßü‡¶®‡¶ø‡•§ URL ‡¶∏‡¶†‡¶ø‡¶ï ‡¶ï‡¶ø‡¶®‡¶æ ‡¶ì ‡¶á‡¶®‡ßç‡¶ü‡¶æ‡¶∞‡¶®‡ßá‡¶ü ‡¶ï‡¶æ‡¶®‡ßá‡¶ï‡¶∂‡¶® ‡¶ö‡ßá‡¶ï ‡¶ï‡¶∞‡ßÅ‡¶®‡•§",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "‡¶§‡ßç‡¶∞‡ßÅ‡¶ü‡¶ø: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isFetchingRepo = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6), contentColor = Color.White),
                                enabled = !isFetchingRepo
                            ) {
                                if (isFetchingRepo) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø ‡¶ì ‡¶∏‡¶æ‡¶á‡¶ü ‡¶´‡ßá‡¶ö ‡¶π‡¶ö‡ßç‡¶õ‡ßá...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Rounded.DownloadDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("üì• ‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø ‡¶´‡ßá‡¶ö ‡¶ì ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶∏‡ßá‡¶≠ ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // 2. Custom Movie Website Add Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("üåê ‡¶ï‡¶æ‡¶∏‡ßç‡¶ü‡¶Æ ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶ì‡ßü‡ßá‡¶¨‡¶∏‡¶æ‡¶á‡¶ü ‡¶Ø‡ßã‡¶ó ‡¶ï‡¶∞‡ßÅ‡¶®", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Text(
                                text = "‡¶Ø‡ßá‡¶ï‡ßã‡¶®‡ßã ‡¶´‡ßç‡¶∞‡¶ø ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶¨‡¶æ ‡¶≠‡¶ø‡¶°‡¶ø‡¶ì ‡¶ì‡ßü‡ßá‡¶¨‡¶∏‡¶æ‡¶á‡¶ü ‡¶∏‡¶∞‡¶æ‡¶∏‡¶∞‡¶ø ‡¶è‡¶ñ‡¶æ‡¶®‡ßá ‡¶Ø‡ßÅ‡¶ï‡ßç‡¶§ ‡¶ï‡¶∞‡ßÅ‡¶®‡•§ ‡¶á‡¶â‡¶ú‡¶æ‡¶∞‡¶∞‡¶æ ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™‡ßá‡¶∞ ‡¶≠‡ßá‡¶§‡¶∞‡ßá ‡¶•‡ßá‡¶ï‡ßá‡¶á ‡¶¨‡ßç‡¶∞‡¶æ‡¶â‡¶ú ‡¶ï‡¶∞‡ßá ‡¶´‡ßÅ‡¶≤‡¶∏‡ßç‡¶ï‡ßç‡¶∞‡¶ø‡¶®‡ßá ‡¶™‡ßç‡¶≤‡ßá ‡¶ï‡¶∞‡¶§‡ßá ‡¶™‡¶æ‡¶∞‡¶¨‡ßá‡¶®‡•§",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )

                            OutlinedTextField(
                                value = customSiteName,
                                onValueChange = { customSiteName = it },
                                label = { Text("‡¶∏‡¶æ‡¶á‡¶ü‡ßá‡¶∞ ‡¶®‡¶æ‡¶Æ (‡¶Ø‡ßá‡¶Æ‡¶®: BollyFlix / MovieHD)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = customSiteUrl,
                                onValueChange = { customSiteUrl = it },
                                label = { Text("‡¶ì‡ßü‡ßá‡¶¨‡¶∏‡¶æ‡¶á‡¶ü URL (‡¶Ø‡ßá‡¶Æ‡¶®: https://bollyflix.lat)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customSiteCategory,
                                    onValueChange = { customSiteCategory = it },
                                    label = { Text("‡¶ï‡ßç‡¶Ø‡¶æ‡¶ü‡¶æ‡¶ó‡¶∞‡¶ø") },
                                    modifier = Modifier.weight(1f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = customSiteLogo,
                                    onValueChange = { customSiteLogo = it },
                                    label = { Text("‡¶≤‡ßã‡¶ó‡ßã URL (‡¶ê‡¶ö‡ßç‡¶õ‡¶ø‡¶ï)") },
                                    modifier = Modifier.weight(1f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }

                            Button(
                                onClick = {
                                    if (customSiteName.isBlank() || customSiteUrl.isBlank()) {
                                        Toast.makeText(context, "‡¶Ö‡¶®‡ßÅ‡¶ó‡ßç‡¶∞‡¶π ‡¶ï‡¶∞‡ßá ‡¶∏‡¶æ‡¶á‡¶ü‡ßá‡¶∞ ‡¶®‡¶æ‡¶Æ ‡¶è‡¶¨‡¶Ç URL ‡¶™‡ßÇ‡¶∞‡¶£ ‡¶ï‡¶∞‡ßÅ‡¶®", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val newProvider = MovieProvider(
                                        id = "custom_site_${System.currentTimeMillis()}",
                                        name = customSiteName.trim(),
                                        siteUrl = customSiteUrl.trim(),
                                        iconUrl = customSiteLogo.trim().ifBlank { null },
                                        types = listOf(customSiteCategory.trim().ifBlank { "Movies & Series" }),
                                        description = customSiteDesc.trim().ifBlank { "Direct Movie Website" },
                                        isCustom = true
                                    )
                                    val currentCustom = repository.getCustomMovieProviders()
                                    val updated = (listOf(newProvider) + currentCustom.filterNot { it.id == newProvider.id })
                                    repository.saveCustomMovieProviders(updated)
                                    coroutineScope.launch {
                                        repository.pushCloudStreamReposToFirebase(repository.getSavedCloudStreamRepos())
                                    }
                                    customSiteName = ""
                                    customSiteUrl = ""
                                    customSiteLogo = ""
                                    customSiteDesc = ""
                                    onDataChanged()
                                    Toast.makeText(context, "‚úÖ '${newProvider.name}' ‡¶∏‡¶æ‡¶á‡¶ü ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶∏‡ßá‡¶ï‡¶∂‡¶®‡ßá ‡¶Ø‡ßÅ‡¶ï‡ßç‡¶§ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("‚ûï ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶∏‡¶æ‡¶á‡¶ü ‡¶Ø‡ßÅ‡¶ï‡ßç‡¶§ ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 3. Section Header for Installed Repositories
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "üì¶ ‡¶á‡¶®‡¶∏‡ßç‡¶ü‡¶≤ ‡¶ï‡¶∞‡¶æ ‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø (${cloudStreamRepos.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "‡¶Æ‡ßã‡¶ü ‡¶∏‡¶æ‡¶á‡¶ü: ${movieProviders.size}",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Render each installed CloudStream Repo
                items(cloudStreamRepos) { repo ->
                    val isExpanded = expandedRepoId == repo.id
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (repo.enabled) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color(0xFF475569)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Extension,
                                        contentDescription = null,
                                        tint = if (repo.enabled) Color(0xFF8B5CF6) else Color(0xFF64748B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = repo.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${repo.providers.size} ‡¶ü‡¶ø ‡¶∏‡¶æ‡¶á‡¶ü ‚Ä¢ v${repo.manifestVersion}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Expand / Collapse button
                                    IconButton(onClick = {
                                        expandedRepoId = if (isExpanded) null else repo.id
                                    }) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = "Expand",
                                            tint = Color(0xFF00E5FF)
                                        )
                                    }

                                    // Delete Repo
                                    IconButton(onClick = {
                                        repoToDelete = repo
                                    }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            // URL string
                            Text(
                                text = repo.url,
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Action buttons: Refresh, Push to Firebase
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val refreshed = repository.parseCloudStreamRepo(repo.url)
                                            if (refreshed != null) {
                                                val all = repository.getSavedCloudStreamRepos()
                                                val updated = all.map { if (it.id == repo.id) refreshed else it }
                                                repository.saveCloudStreamRepos(updated)
                                                repository.pushCloudStreamReposToFirebase(updated)
                                                onDataChanged()
                                                Toast.makeText(context, "‚úÖ '${repo.name}' ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ï‡¶∞‡¶æ ‡¶Ø‡¶æ‡ßü‡¶®‡¶ø", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDDD6FE))
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("‡¶∞‡¶ø‡¶´‡ßç‡¶∞‡ßá‡¶∂", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.pushCloudStreamReposToFirebase(repository.getSavedCloudStreamRepos())
                                            Toast.makeText(context, "‚úÖ '${repo.name}' Firebase ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶™‡ßÅ‡¶∂ ‡¶ï‡¶∞‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Firebase ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Expanded List of Websites / Providers inside this Repo
                            if (isExpanded) {
                                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "üåê ‡¶è‡¶á ‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø‡¶∞ ‡¶ì‡ßü‡ßá‡¶¨‡¶∏‡¶æ‡¶á‡¶ü ‡¶§‡¶æ‡¶≤‡¶ø‡¶ï‡¶æ (${repo.providers.size} ‡¶ü‡¶ø):",
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )

                                repo.providers.forEach { provider ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF1E293B)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (!provider.logoUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = provider.logoUrl,
                                                            contentDescription = provider.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Icon(Icons.Rounded.Movie, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = provider.name,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = "${provider.category} ‚Ä¢ ${provider.status}",
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }

                                            // Test Visit in In-App Browser button
                                            Button(
                                                onClick = {
                                                    onOpenMovieProvider(provider)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("‡¶≠‡¶ø‡¶ú‡¶ø‡¶ü ‡¶ì ‡¶∞‡¶æ‡¶®", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Custom Providers Section
                val customProviders = movieProviders.filter { it.isCustom }
                if (customProviders.isNotEmpty()) {
                    item {
                        Text(
                            text = "‚≠ê ‡¶ï‡¶æ‡¶∏‡ßç‡¶ü‡¶Æ ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶∏‡¶æ‡¶á‡¶ü (${customProviders.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(customProviders) { provider ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Language, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = provider.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(text = provider.url, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { onOpenMovieProvider(provider) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("‡¶∞‡¶æ‡¶®", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(onClick = { providerToDelete = provider }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 6 CONTENT: FIREBASE CLOUD REALTIME DATABASE SETTINGS
            // -------------------------------------------------------------
            if (selectedAdminTab == AdminTab.FIREBASE) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("üî• Firebase Realtime Database ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶∏‡ßá‡¶ü‡¶ø‡¶Ç‡¶∏", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Text(
                                text = "‡¶è‡¶ñ‡¶æ‡¶®‡ßá ‡¶Ü‡¶™‡¶®‡¶æ‡¶∞ Firebase Realtime Database URL ‡¶∏‡ßá‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶®‡•§ ‡¶è‡¶ñ‡¶æ‡¶® ‡¶•‡ßá‡¶ï‡ßá ‡¶Ø‡ßã‡¶ó ‡¶ï‡¶∞‡¶æ ‡¶∏‡¶ï‡¶≤ ‡¶ñ‡ßá‡¶≤‡¶æ, ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶è‡¶¨‡¶Ç ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶∏‡¶∞‡¶æ‡¶∏‡¶∞‡¶ø Firebase ‡¶°‡ßá‡¶ü‡¶æ‡¶¨‡ßá‡¶∏‡ßá ‡¶∏‡ßá‡¶≠ ‡¶π‡¶¨‡ßá ‡¶è‡¶¨‡¶Ç ‡¶á‡¶®‡¶∏‡ßç‡¶ü‡¶≤ ‡¶ï‡¶∞‡¶æ ‡¶∏‡¶ï‡¶≤ ‡¶á‡¶â‡¶ú‡¶æ‡¶∞‡ßá‡¶∞ ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™‡ßá ‡¶∏‡¶∞‡¶æ‡¶∏‡¶∞‡¶ø ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶¶‡ßá‡¶ñ‡¶æ ‡¶Ø‡¶æ‡¶¨‡ßá‡•§",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            OutlinedTextField(
                                value = firebaseUrlInput,
                                onValueChange = {
                                    firebaseUrlInput = it
                                    firebaseTestResult = null
                                },
                                label = { Text("Firebase Database URL") },
                                placeholder = { Text("https://your-project.firebaseio.com/", color = Color(0xFF64748B)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Test Connection & Save Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isTestingFirebase = true
                                            firebaseTestResult = repository.testFirebaseConnection(firebaseUrlInput.trim())
                                            isTestingFirebase = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    if (isTestingFirebase) {
                                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("‡¶ü‡ßá‡¶∏‡ßç‡¶ü ‡¶π‡¶ö‡ßç‡¶õ‡ßá...", fontSize = 11.sp)
                                    } else {
                                        Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("‡¶ü‡ßá‡¶∏‡ßç‡¶ü ‡¶ï‡¶æ‡¶®‡ßá‡¶ï‡¶∂‡¶®", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (firebaseUrlInput.isNotBlank()) {
                                            repository.saveFirebaseUrl(firebaseUrlInput.trim())
                                            onDataChanged()
                                            Toast.makeText(context, "‚úÖ Firebase URL ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá ‡¶∏‡ßá‡¶≠ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "‡¶Ö‡¶®‡ßÅ‡¶ó‡ßç‡¶∞‡¶π ‡¶ï‡¶∞‡ßá ‡¶è‡¶ï‡¶ü‡¶ø ‡¶∏‡¶†‡¶ø‡¶ï Firebase URL ‡¶¶‡¶ø‡¶®", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("‡¶∏‡ßá‡¶≠ ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Test Result Banner
                            firebaseTestResult?.let { res ->
                                Surface(
                                    color = if (res.first) Color(0xFF065F46).copy(alpha = 0.35f) else Color(0xFF7F1D1D).copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (res.first) Color(0xFF10B981) else Color(0xFFEF4444)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (res.first) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = if (res.first) Color(0xFF10B981) else Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = res.second,
                                            color = if (res.first) Color(0xFFA7F3D0) else Color(0xFFFECACA),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                            // Firebase Rules Setup Instructions Box
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "üìã Firebase ‡¶°‡ßá‡¶ü‡¶æ‡¶¨‡ßá‡¶∏ ‡¶∞‡ßÅ‡¶≤‡¶∏ (Rules):",
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Firebase Console > Realtime Database > Rules ‡¶ü‡ßç‡¶Ø‡¶æ‡¶¨‡ßá ‡¶ó‡¶ø‡ßü‡ßá ‡¶®‡¶ø‡¶ö‡ßá‡¶∞ ‡¶∞‡ßÅ‡¶≤‡¶∏ ‡¶¶‡¶ø‡ßü‡ßá 'Publish' ‡¶ï‡¶∞‡ßÅ‡¶®:",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )
                                    Surface(
                                        color = Color(0xFF020617),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "{\n  \"rules\": {\n    \".read\": true,\n    \".write\": true\n  }\n}",
                                            color = Color(0xFF10B981),
                                            fontSize = 11.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 7 CONTENT: BROADCAST NOTIFICATIONS TO USERS
            // -------------------------------------------------------------
            if (selectedAdminTab == AdminTab.BROADCAST) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Header Banner
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "üì¢ ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶¨‡ßç‡¶∞‡¶°‡¶ï‡¶æ‡¶∏‡ßç‡¶ü ‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶®",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "‡¶∏‡¶¨ ‡¶¨‡ßç‡¶Ø‡¶¨‡¶π‡¶æ‡¶∞‡¶ï‡¶æ‡¶∞‡ßÄ‡¶¶‡ßá‡¶∞ ‡¶ï‡¶æ‡¶õ‡ßá ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö, ‡¶ü‡¶ø‡¶≠‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶¨‡¶æ ‡¶¨‡¶ø‡¶∂‡ßá‡¶∑ ‡¶®‡ßã‡¶ü‡¶ø‡¶∏ ‡¶™‡¶æ‡¶†‡¶æ‡¶®",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Notification Input Form
                            OutlinedTextField(
                                value = broadcastTitle,
                                onValueChange = { broadcastTitle = it },
                                label = { Text("‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶® ‡¶ü‡¶æ‡¶á‡¶ü‡ßá‡¶≤ * (‡¶Ø‡ßá‡¶Æ‡¶®: ‚öΩ ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶∂‡ßÅ‡¶∞‡ßÅ!)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                )
                            )

                            OutlinedTextField(
                                value = broadcastMessage,
                                onValueChange = { broadcastMessage = it },
                                label = { Text("‡¶¨‡¶ø‡¶∏‡ßç‡¶§‡¶æ‡¶∞‡¶ø‡¶§ ‡¶¨‡¶æ‡¶∞‡ßç‡¶§‡¶æ * (‡¶Ø‡ßá‡¶Æ‡¶®: ‡¶∞‡¶ø‡ßü‡¶æ‡¶≤ ‡¶Æ‡¶æ‡¶¶‡ßç‡¶∞‡¶ø‡¶¶ ‡¶¨‡¶®‡¶æ‡¶Æ ‡¶¨‡¶æ‡¶∞‡ßç‡¶∏‡ßá‡¶≤‡ßã‡¶®‡¶æ ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶∂‡ßÅ‡¶∞‡ßÅ ‡¶π‡ßü‡ßá‡¶õ‡ßá)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A)
                                )
                            )

                            // Type Dropdown & Optional Image URL
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { broadcastTypeDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        val typeName = when (broadcastType) {
                                            NotificationType.LIVE_EVENT -> "‚öΩ ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶®‡ßã‡¶ü‡¶ø‡¶∏"
                                            NotificationType.LIVE_TV -> "üì∫ ‡¶ü‡¶ø‡¶≠‡¶ø ‡¶®‡ßã‡¶ü‡¶ø‡¶∏"
                                            NotificationType.MOVIE -> "üé¨ ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶®‡ßã‡¶ü‡¶ø‡¶∏"
                                            NotificationType.PLAYLIST -> "üìÇ ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü"
                                            NotificationType.APP_UPDATE -> "üöÄ ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™ ‡¶Ü‡¶™‡¶°‡ßá‡¶ü"
                                            else -> "üì¢ ‡¶∏‡¶æ‡¶ß‡¶æ‡¶∞‡¶£ ‡¶®‡ßã‡¶ü‡¶ø‡¶∏"
                                        }
                                        Text(text = "‡¶ï‡ßç‡¶Ø‡¶æ‡¶ü‡¶æ‡¶ó‡¶∞‡¶ø: $typeName", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    DropdownMenu(
                                        expanded = broadcastTypeDropdownExpanded,
                                        onDismissRequest = { broadcastTypeDropdownExpanded = false },
                                        modifier = Modifier.background(Color(0xFF1E293B))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("üì¢ ‡¶∏‡¶æ‡¶ß‡¶æ‡¶∞‡¶£ ‡¶®‡ßã‡¶ü‡¶ø‡¶∏", color = Color.White, fontSize = 12.sp) },
                                            onClick = {
                                                broadcastType = NotificationType.BROADCAST
                                                broadcastTypeDropdownExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("‚öΩ ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö", color = Color(0xFF10B981), fontSize = 12.sp) },
                                            onClick = {
                                                broadcastType = NotificationType.LIVE_EVENT
                                                broadcastTypeDropdownExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("üì∫ ‡¶ü‡¶ø‡¶≠‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤", color = Color(0xFF00E5FF), fontSize = 12.sp) },
                                            onClick = {
                                                broadcastType = NotificationType.LIVE_TV
                                                broadcastTypeDropdownExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("üé¨ ‡¶®‡¶§‡ßÅ‡¶® ‡¶Æ‡ßÅ‡¶≠‡¶ø", color = Color(0xFF8B5CF6), fontSize = 12.sp) },
                                            onClick = {
                                                broadcastType = NotificationType.MOVIE
                                                broadcastTypeDropdownExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("üìÇ ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü", color = Color(0xFFEC4899), fontSize = 12.sp) },
                                            onClick = {
                                                broadcastType = NotificationType.PLAYLIST
                                                broadcastTypeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = broadcastImageUrl,
                                    onValueChange = { broadcastImageUrl = it },
                                    label = { Text("‡¶á‡¶Æ‡ßá‡¶ú URL (‡¶ê‡¶ö‡ßç‡¶õ‡¶ø‡¶ï)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A)
                                    )
                                )
                            }

                            // Send Button
                            Button(
                                onClick = {
                                    if (broadcastTitle.isBlank()) {
                                        Toast.makeText(context, "‡¶Ö‡¶®‡ßÅ‡¶ó‡ßç‡¶∞‡¶π ‡¶ï‡¶∞‡ßá ‡¶ü‡¶æ‡¶á‡¶ü‡ßá‡¶≤ ‡¶¶‡¶ø‡¶®", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        isSendingBroadcast = true
                                        val newNotif = AppNotification(
                                            title = broadcastTitle.trim(),
                                            message = broadcastMessage.trim(),
                                            type = broadcastType,
                                            imageUrl = broadcastImageUrl.trim().ifEmpty { null },
                                            targetId = broadcastTargetId.trim().ifEmpty { null },
                                            timestamp = System.currentTimeMillis()
                                        )
                                        repository.broadcastNotification(newNotif)
                                        NotificationHelper.showSystemNotification(context, newNotif)
                                        adminNotificationHistory = repository.getStoredNotifications()
                                        isSendingBroadcast = false
                                        broadcastTitle = ""
                                        broadcastMessage = ""
                                        broadcastImageUrl = ""
                                        broadcastTargetId = ""
                                        Toast.makeText(context, "‚úÖ ‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶® ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá ‡¶™‡¶æ‡¶†‡¶æ‡¶®‡ßã ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E5FF),
                                    contentColor = Color.Black
                                ),
                                enabled = !isSendingBroadcast
                            ) {
                                if (isSendingBroadcast) {
                                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶® ‡¶™‡¶æ‡¶†‡¶æ‡¶®‡ßã ‡¶π‡¶ö‡ßç‡¶õ‡ßá...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶® ‡¶™‡¶æ‡¶†‡¶æ‡¶® ‡¶ì ‡¶¨‡ßç‡¶∞‡¶°‡¶ï‡¶æ‡¶∏‡ßç‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 6.dp))

                            // Recent Sent Notifications
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "üìú ‡¶™‡ßç‡¶∞‡ßá‡¶∞‡¶ø‡¶§ ‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶® ‡¶π‡¶ø‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø (${adminNotificationHistory.size} ‡¶ü‡¶ø)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (adminNotificationHistory.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            repository.clearAllNotifications()
                                            adminNotificationHistory = emptyList()
                                            Toast.makeText(context, "‡¶∏‡¶ï‡¶≤ ‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶® ‡¶Æ‡ßÅ‡¶õ‡ßá ‡¶´‡ßá‡¶≤‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("‡¶∏‡¶¨ ‡¶Æ‡ßÅ‡¶õ‡ßÅ‡¶®", color = Color(0xFFEF4444), fontSize = 11.sp)
                                    }
                                }
                            }

                            if (adminNotificationHistory.isEmpty()) {
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                        Text("‡¶è‡¶ñ‡¶®‡¶ì ‡¶ï‡ßã‡¶®‡ßã ‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶® ‡¶™‡¶æ‡¶†‡¶æ‡¶®‡ßã ‡¶π‡ßü‡¶®‡¶ø", color = Color(0xFF64748B), fontSize = 12.sp)
                                    }
                                }
                            } else {
                                adminNotificationHistory.take(15).forEach { notif ->
                                    Surface(
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = notif.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (notif.message.isNotBlank()) {
                                                    Text(
                                                        text = notif.message,
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 11.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    repository.deleteNotification(notif.id)
                                                    adminNotificationHistory = repository.getStoredNotifications()
                                                }
                                            ) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
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
    }

    // Dialog for Previewing App Update
    if (previewUpdateDialog != null) {
        AppUpdateDialog(
            updateInfo = previewUpdateDialog!!,
            onDismiss = { previewUpdateDialog = null }
        )
    }

    // Dialog for Deleting Playlist (Confirmation)
    if (playlistToDelete != null) {
        val target = playlistToDelete!!
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶°‡¶ø‡¶≤‡¶ø‡¶ü ‡¶®‡¶ø‡¶∂‡ßç‡¶ö‡¶ø‡¶§‡¶ï‡¶∞‡¶£", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "‡¶Ü‡¶™‡¶®‡¶ø ‡¶ï‡¶ø '${target.title}' ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü‡¶ü‡¶ø ‡¶∏‡ßç‡¶•‡¶æ‡ßü‡ßÄ‡¶≠‡¶æ‡¶¨‡ßá ‡¶°‡¶ø‡¶≤‡¶ø‡¶ü ‡¶ï‡¶∞‡¶§‡ßá ‡¶ö‡¶æ‡¶®?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = target
                        playlistToDelete = null
                        coroutineScope.launch {
                            repository.deletePlaylist(toRemove.id)
                            onDataChanged()
                            Toast.makeText(context, "${toRemove.title} ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶°‡¶ø‡¶≤‡¶ø‡¶ü ‡¶ï‡¶∞‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { playlistToDelete = null },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        )
    }

    // =========================================================================
    // FULL MATCH & MULTI-SERVER EDIT DIALOG (CRITICAL USER REQUIREMENT)
    // =========================================================================
    if (editingMatchItem != null) {
        val target = editingMatchItem!!
        Dialog(
            onDismissRequest = { editingMatchItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF2563EB).copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "‡¶ñ‡ßá‡¶≤‡¶æ ‡¶ì ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶è‡¶°‡¶ø‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶®",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "‡¶≤‡¶æ‡¶á‡¶≠ ‡¶ö‡¶≤‡¶æ‡¶ï‡¶æ‡¶≤‡ßÄ‡¶® ‡¶®‡¶§‡ßÅ‡¶® ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶Ø‡ßã‡¶ó ‡¶¨‡¶æ ‡¶™‡¶∞‡¶ø‡¶¨‡¶∞‡ßç‡¶§‡¶®",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        IconButton(onClick = { editingMatchItem = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable Edit Form
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tournament
                        item {
                            OutlinedTextField(
                                value = editTournament,
                                onValueChange = { editTournament = it },
                                label = { Text("‡¶ü‡ßÅ‡¶∞‡ßç‡¶®‡¶æ‡¶Æ‡ßá‡¶®‡ßç‡¶ü / ‡¶∏‡¶ø‡¶∞‡¶ø‡¶ú (Tournament / Series)") },
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Category & Status
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Category Dropdown
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = editSportCategory,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("‡¶ñ‡ßá‡¶≤‡¶æ (Category)", fontSize = 11.sp) },
                                        trailingIcon = {
                                            IconButton(onClick = { editSportDropdownExpanded = true }) {
                                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color(0xFF00E5FF))
                                            }
                                        },
                                        colors = customFieldColors(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    DropdownMenu(
                                        expanded = editSportDropdownExpanded,
                                        onDismissRequest = { editSportDropdownExpanded = false },
                                        modifier = Modifier.background(Color(0xFF1E293B))
                                    ) {
                                        sportOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option, color = Color.White) },
                                                onClick = {
                                                    editSportCategory = option
                                                    editSportDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Status Dropdown
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = editSportStatus,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("‡¶Ö‡¶¨‡¶∏‡ßç‡¶•‡¶æ (Status)", fontSize = 11.sp) },
                                        trailingIcon = {
                                            IconButton(onClick = { editStatusDropdownExpanded = true }) {
                                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color(0xFF00E5FF))
                                            }
                                        },
                                        colors = customFieldColors(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    DropdownMenu(
                                        expanded = editStatusDropdownExpanded,
                                        onDismissRequest = { editStatusDropdownExpanded = false },
                                        modifier = Modifier.background(Color(0xFF1E293B))
                                    ) {
                                        statusOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option, color = Color.White) },
                                                onClick = {
                                                    editSportStatus = option
                                                    editStatusDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Team 1 Name & Score
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editTeam1Name,
                                    onValueChange = { editTeam1Name = it },
                                    label = { Text("Team 1 Name") },
                                    modifier = Modifier.weight(1.3f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editTeam1Score,
                                    onValueChange = { editTeam1Score = it },
                                    label = { Text("Team 1 Score") },
                                    placeholder = { Text("e.g. 182/4", color = Color(0xFF64748B), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Team 1 Logo URL
                        item {
                            OutlinedTextField(
                                value = editTeam1Logo,
                                onValueChange = { editTeam1Logo = it },
                                label = { Text("Team 1 Logo URL (Optional)") },
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Team 2 Name & Score
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editTeam2Name,
                                    onValueChange = { editTeam2Name = it },
                                    label = { Text("Team 2 Name") },
                                    modifier = Modifier.weight(1.3f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editTeam2Score,
                                    onValueChange = { editTeam2Score = it },
                                    label = { Text("Team 2 Score") },
                                    placeholder = { Text("e.g. 150/8", color = Color(0xFF64748B), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Team 2 Logo URL
                        item {
                            OutlinedTextField(
                                value = editTeam2Logo,
                                onValueChange = { editTeam2Logo = it },
                                label = { Text("Team 2 Logo URL (Optional)") },
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Match Time & Countdown
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editMatchTime,
                                    onValueChange = { editMatchTime = it },
                                    label = { Text("Time (e.g. 06:30 AM)") },
                                    modifier = Modifier.weight(1.2f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editCountdownHours,
                                    onValueChange = { editCountdownHours = it },
                                    label = { Text("Countdown (hrs)") },
                                    modifier = Modifier.weight(1f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Multi-Server Header
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "‚ö° ‡¶Æ‡¶æ‡¶≤‡ßç‡¶ü‡¶ø-‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ ‡¶≤‡¶ø‡¶ô‡ßç‡¶ï‡¶∏‡¶Æ‡ßÇ‡¶π (${editServers.size} ‡¶ü‡¶ø ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞):",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "‡¶ñ‡ßá‡¶≤‡¶æ ‡¶ö‡¶≤‡¶æ‡¶ï‡¶æ‡¶≤‡ßÄ‡¶® ‡¶è‡¶°‡¶ø‡¶ü‡ßá‡¶¨‡¶≤",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Dynamic Server Inputs for Edit
                        items(editServers.size) { index ->
                            val server = editServers[index]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = server.name,
                                    onValueChange = { newName ->
                                        editServers = editServers.toMutableList().also {
                                            it[index] = it[index].copy(name = newName)
                                        }
                                    },
                                    placeholder = { Text("‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶®‡¶æ‡¶Æ", color = Color(0xFF64748B), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = server.url,
                                    onValueChange = { newUrl ->
                                        editServers = editServers.toMutableList().also {
                                            it[index] = it[index].copy(url = newUrl)
                                        }
                                    },
                                    placeholder = { Text("Stream URL (.m3u8 / mp4)", color = Color(0xFF64748B), fontSize = 11.sp) },
                                    modifier = Modifier.weight(2f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                if (editServers.size > 1) {
                                    IconButton(
                                        onClick = {
                                            editServers = editServers.toMutableList().also { it.removeAt(index) }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Remove Server",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Add Server Button in Edit Dialog
                        item {
                            OutlinedButton(
                                onClick = {
                                    val nextNum = editServers.size + 1
                                    val defaultName = when (nextNum) {
                                        1 -> "T SPORTS"
                                        2 -> "TT"
                                        3 -> "TEMP"
                                        4 -> "HD SERVER 4"
                                        5 -> "SERVER 5 (4K)"
                                        6 -> "SERVER 6 (HLS)"
                                        else -> "SERVER $nextNum"
                                    }
                                    editServers = editServers + StreamServer(defaultName, "")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("‚ûï ‡¶Ü‡¶∞‡¶ì ‡¶®‡¶§‡ßÅ‡¶® ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞/‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶≤‡¶ø‡¶ô‡ßç‡¶ï ‡¶Ø‡ßã‡¶ó ‡¶ï‡¶∞‡ßÅ‡¶® (+ Add Server)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Action Buttons (Save & Update / Cancel)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { editingMatchItem = null },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Text("‡¶¨‡¶æ‡¶§‡¶ø‡¶≤ (Cancel)", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val validServers = editServers.mapNotNull {
                                    if (it.url.isNotBlank()) StreamServer(it.name.ifBlank { "Server" }, it.url.trim()) else null
                                }

                                if (validServers.isNotEmpty()) {
                                    val matchTitle = if (editTeam1Name.isNotBlank() && editTeam2Name.isNotBlank()) {
                                        "$editTeam1Name vs $editTeam2Name"
                                    } else {
                                        editTournament.ifBlank { target.title }
                                    }

                                    val parsedCountdown: Long? = when {
                                        editCountdownHours.isNotBlank() && editCountdownHours.toDoubleOrNull() != null -> {
                                            val hrs = editCountdownHours.toDouble()
                                            System.currentTimeMillis() + (hrs * 3600 * 1000L).toLong()
                                        }
                                        editMatchTime.isNotBlank() -> {
                                            parseEventTimeStringToEpochMillis(editMatchTime)
                                        }
                                        else -> target.countdownTargetSeconds
                                    }

                                    val updatedMatch = target.copy(
                                        title = matchTitle,
                                        tournament = editTournament.takeIf { it.isNotBlank() },
                                        category = editSportCategory,
                                        streamUrl = validServers.first().url,
                                        backupUrl = validServers.getOrNull(1)?.url,
                                        servers = validServers,
                                        isLive = editSportStatus.contains("Live", ignoreCase = true),
                                        status = editSportStatus,
                                        eventTime = editMatchTime.ifBlank { editSportStatus },
                                        team1 = editTeam1Name.takeIf { it.isNotBlank() },
                                        team1Logo = editTeam1Logo.takeIf { it.isNotBlank() },
                                        team2 = editTeam2Name.takeIf { it.isNotBlank() },
                                        team2Logo = editTeam2Logo.takeIf { it.isNotBlank() },
                                        matchTimeFormatted = editMatchTime.takeIf { it.isNotBlank() },
                                        countdownTargetSeconds = parsedCountdown,
                                        score1 = editTeam1Score.takeIf { it.isNotBlank() },
                                        score2 = editTeam2Score.takeIf { it.isNotBlank() }
                                    )

                                    repository.saveCustomStream(updatedMatch)
                                    coroutineScope.launch {
                                        repository.pushToFirebase(updatedMatch)
                                    }
                                    onDataChanged()
                                    editingMatchItem = null
                                    Toast.makeText(context, "‡¶ñ‡ßá‡¶≤‡¶æ ‡¶ì ‡¶∏‡¶Æ‡¶∏‡ßç‡¶§ ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶≤‡¶ø‡¶ô‡ßç‡¶ï ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "‡¶ï‡¶Æ‡¶™‡¶ï‡ßç‡¶∑‡ßá ‡¶è‡¶ï‡¶ü‡¶ø ‡¶ï‡¶æ‡¶∞‡ßç‡¶Ø‡¶ï‡¶∞ ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶¶‡¶ø‡¶®", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 2. LIVE TV CHANNEL EDIT DIALOG (User: ‡¶è‡¶°‡¶Æ‡¶ø‡¶® ‡¶™‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤‡ßá ‡¶∏‡¶¨‡¶ó‡ßÅ‡¶≤‡ßã ‡¶è‡¶°‡¶ø‡¶ü ‡¶ï‡¶∞‡¶æ‡¶∞ ‡¶Ö‡¶™‡¶∂‡¶®)
    // =========================================================================
    if (editingChannelItem != null) {
        val target = editingChannelItem!!
        Dialog(
            onDismissRequest = { editingChannelItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.90f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF2563EB).copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.LiveTv, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "‡¶ü‡¶ø‡¶≠‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶ì ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶è‡¶°‡¶ø‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶®",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = target.title,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        IconButton(onClick = { editingChannelItem = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable Edit Form
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Channel Name
                        item {
                            OutlinedTextField(
                                value = editChannelName,
                                onValueChange = { editChannelName = it },
                                label = { Text("‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤‡ßá‡¶∞ ‡¶®‡¶æ‡¶Æ (Channel Name)") },
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Category Dropdown
                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = editChannelCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("‡¶ï‡ßç‡¶Ø‡¶æ‡¶ü‡¶æ‡¶ó‡¶∞‡¶ø (Category)") },
                                    trailingIcon = {
                                        IconButton(onClick = { editChannelCategoryDropdownExpanded = !editChannelCategoryDropdownExpanded }) {
                                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.White)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { editChannelCategoryDropdownExpanded = true },
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                DropdownMenu(
                                    expanded = editChannelCategoryDropdownExpanded,
                                    onDismissRequest = { editChannelCategoryDropdownExpanded = false },
                                    modifier = Modifier.background(Color(0xFF1E293B))
                                ) {
                                    channelCategoryOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt, color = Color.White) },
                                            onClick = {
                                                editChannelCategory = opt
                                                editChannelCategoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Logo URL
                        item {
                            OutlinedTextField(
                                value = editChannelLogoUrl,
                                onValueChange = { editChannelLogoUrl = it },
                                label = { Text("‡¶≤‡ßã‡¶ó‡ßã URL (Logo Image URL)") },
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Multi-Servers Section Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Dns, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ‡¶ø‡¶Ç ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶ì ‡¶¨‡ßç‡¶Ø‡¶æ‡¶ï‡¶Ü‡¶™ ‡¶≤‡¶ø‡¶Ç‡¶ï (${editChannelServers.size} ‡¶ü‡¶ø ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞):",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Dynamic Server Inputs
                        itemsIndexed(editChannelServers) { idx, srv ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = srv.name,
                                    onValueChange = { newName ->
                                        editChannelServers = editChannelServers.toMutableList().also {
                                            it[idx] = it[idx].copy(name = newName)
                                        }
                                    },
                                    placeholder = { Text("Server ${idx + 1} Name", color = Color(0xFF64748B), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = srv.url,
                                    onValueChange = { newUrl ->
                                        editChannelServers = editChannelServers.toMutableList().also {
                                            it[idx] = it[idx].copy(url = newUrl)
                                        }
                                    },
                                    placeholder = { Text("Stream URL (.m3u8 / .mpd)", color = Color(0xFF64748B), fontSize = 11.sp) },
                                    modifier = Modifier.weight(2f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                if (editChannelServers.size > 1) {
                                    IconButton(
                                        onClick = {
                                            editChannelServers = editChannelServers.toMutableList().also { it.removeAt(idx) }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶∏‡¶∞‡¶æ‡¶®",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Add Server Button
                        item {
                            OutlinedButton(
                                onClick = {
                                    val nextNum = editChannelServers.size + 1
                                    editChannelServers = editChannelServers + StreamServer("‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ $nextNum", "")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("‚ûï ‡¶Ü‡¶∞‡¶ì ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶Ø‡ßã‡¶ó ‡¶ï‡¶∞‡ßÅ‡¶® (+ Add Server)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingChannelItem = null },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Text("‡¶¨‡¶æ‡¶§‡¶ø‡¶≤ (Cancel)", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val validServers = editChannelServers.mapNotNull {
                                    if (it.url.isNotBlank()) StreamServer(it.name.ifBlank { "Server" }, it.url.trim()) else null
                                }

                                if (editChannelName.isNotBlank() && validServers.isNotEmpty()) {
                                    val updatedChannel = target.copy(
                                        title = editChannelName.trim(),
                                        category = editChannelCategory,
                                        type = MediaType.LIVE_TV,
                                        streamUrl = validServers.first().url,
                                        backupUrl = validServers.getOrNull(1)?.url,
                                        servers = validServers,
                                        logoUrl = editChannelLogoUrl.trim().takeIf { it.isNotBlank() },
                                        isLive = true
                                    )

                                    repository.saveCustomStream(updatedChannel)
                                    coroutineScope.launch {
                                        repository.pushToFirebase(updatedChannel)
                                    }
                                    onDataChanged()
                                    editingChannelItem = null
                                    Toast.makeText(context, "${updatedChannel.title} ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ì ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤‡ßá‡¶∞ ‡¶®‡¶æ‡¶Æ ‡¶è‡¶¨‡¶Ç ‡¶ï‡¶Æ‡¶™‡¶ï‡ßç‡¶∑‡ßá ‡¶è‡¶ï‡¶ü‡¶ø ‡¶ï‡¶æ‡¶∞‡ßç‡¶Ø‡¶ï‡¶∞ ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶¶‡¶ø‡¶®", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 3. PLAYLIST EDIT DIALOG (User: ‡¶è‡¶°‡¶Æ‡¶ø‡¶® ‡¶™‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤‡ßá ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶è‡¶°‡¶ø‡¶ü ‡¶ï‡¶∞‡¶æ‡¶∞ ‡¶Ö‡¶™‡¶∂‡¶®)
    // =========================================================================
    if (editingPlaylistItem != null) {
        val target = editingPlaylistItem!!
        Dialog(
            onDismissRequest = { editingPlaylistItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.FeaturedPlayList, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶è‡¶°‡¶ø‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶®",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = target.title,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        IconButton(onClick = { editingPlaylistItem = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    // Playlist Title
                    OutlinedTextField(
                        value = editPlaylistTitle,
                        onValueChange = { editPlaylistTitle = it },
                        label = { Text("‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü‡ßá‡¶∞ ‡¶®‡¶æ‡¶Æ (Playlist Name)") },
                        colors = customFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Playlist M3U URL
                    OutlinedTextField(
                        value = editPlaylistUrl,
                        onValueChange = { editPlaylistUrl = it },
                        label = { Text("‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü M3U / M3U8 URL") },
                        colors = customFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Logo / Banner URL
                    OutlinedTextField(
                        value = editPlaylistLogoUrl,
                        onValueChange = { editPlaylistLogoUrl = it },
                        label = { Text("‡¶≤‡ßã‡¶ó‡ßã / ‡¶¨‡ßç‡¶Ø‡¶æ‡¶®‡¶æ‡¶∞ URL (‡¶ê‡¶ö‡ßç‡¶õ‡¶ø‡¶ï)") },
                        colors = customFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Description
                    OutlinedTextField(
                        value = editPlaylistDescription,
                        onValueChange = { editPlaylistDescription = it },
                        label = { Text("‡¶¨‡¶ø‡¶¨‡¶∞‡¶£ (Short Description)") },
                        colors = customFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = Color(0xFF334155))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingPlaylistItem = null },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Text("‡¶¨‡¶æ‡¶§‡¶ø‡¶≤", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (editPlaylistTitle.isNotBlank() && editPlaylistUrl.isNotBlank()) {
                                    val updatedPl = target.copy(
                                        title = editPlaylistTitle.trim(),
                                        url = editPlaylistUrl.trim(),
                                        logoUrl = editPlaylistLogoUrl.trim().takeIf { it.isNotBlank() },
                                        description = editPlaylistDescription.trim().takeIf { it.isNotBlank() }
                                    )

                                    repository.saveAdminPlaylist(updatedPl)
                                    coroutineScope.launch {
                                        repository.pushPlaylistToFirebase(updatedPl)
                                    }
                                    onDataChanged()
                                    editingPlaylistItem = null
                                    Toast.makeText(context, "${updatedPl.title} ‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü‡ßá‡¶∞ ‡¶®‡¶æ‡¶Æ ‡¶è‡¶¨‡¶Ç M3U URL ‡¶â‡¶≠‡ßü‡¶á ‡¶™‡ßÇ‡¶∞‡¶£ ‡¶ï‡¶∞‡ßÅ‡¶®", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶∏‡ßá‡¶≠ ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 4. MOVIE & SERIES EDIT DIALOG (User: ‡¶è‡¶°‡¶Æ‡¶ø‡¶® ‡¶™‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤‡ßá ‡¶∏‡¶¨‡¶ó‡ßÅ‡¶≤‡ßã ‡¶Ö‡¶™‡¶∂‡¶® ‡¶è‡¶°‡¶ø‡¶ü ‡¶ï‡¶∞‡¶æ‡¶∞ ‡¶∏‡ßÅ‡¶¨‡¶ø‡¶ß‡¶æ)
    // =========================================================================
    if (editingMovieItem != null) {
        val target = editingMovieItem!!
        Dialog(
            onDismissRequest = { editingMovieItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.90f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.MovieFilter, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶ì ‡¶∏‡¶ø‡¶∞‡¶ø‡¶ú ‡¶è‡¶°‡¶ø‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶®",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = target.title,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        IconButton(onClick = { editingMovieItem = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable Edit Form
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Movie Title
                        item {
                            OutlinedTextField(
                                value = editMovieTitle,
                                onValueChange = { editMovieTitle = it },
                                label = { Text("‡¶Æ‡ßÅ‡¶≠‡¶ø‡¶∞ ‡¶®‡¶æ‡¶Æ / ‡¶∂‡¶ø‡¶∞‡ßã‡¶®‡¶æ‡¶Æ (Title)") },
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Category Dropdown
                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = editMovieCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("‡¶ï‡ßç‡¶Ø‡¶æ‡¶ü‡¶æ‡¶ó‡¶∞‡¶ø (Category)") },
                                    trailingIcon = {
                                        IconButton(onClick = { editMovieCategoryDropdownExpanded = !editMovieCategoryDropdownExpanded }) {
                                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.White)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { editMovieCategoryDropdownExpanded = true },
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                DropdownMenu(
                                    expanded = editMovieCategoryDropdownExpanded,
                                    onDismissRequest = { editMovieCategoryDropdownExpanded = false },
                                    modifier = Modifier.background(Color(0xFF1E293B))
                                ) {
                                    movieCategoryOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt, color = Color.White) },
                                            onClick = {
                                                editMovieCategory = opt
                                                editMovieCategoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Poster URL
                        item {
                            OutlinedTextField(
                                value = editMoviePosterUrl,
                                onValueChange = { editMoviePosterUrl = it },
                                label = { Text("‡¶™‡ßã‡¶∏‡ßç‡¶ü‡¶æ‡¶∞ / ‡¶•‡¶æ‡¶Æ‡ßç‡¶¨‡¶®‡ßá‡¶á‡¶≤ URL (Poster URL)") },
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Description
                        item {
                            OutlinedTextField(
                                value = editMovieDesc,
                                onValueChange = { editMovieDesc = it },
                                label = { Text("‡¶Æ‡ßÅ‡¶≠‡¶ø‡¶∞ ‡¶¨‡¶ø‡¶¨‡¶∞‡¶£ (Description)") },
                                colors = customFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Multi-Servers Section Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Dns, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ‡¶ø‡¶Ç ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ (${editMovieServers.size} ‡¶ü‡¶ø ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞):",
                                    color = Color(0xFFF59E0B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Dynamic Server Inputs
                        itemsIndexed(editMovieServers) { idx, srv ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = srv.name,
                                    onValueChange = { newName ->
                                        editMovieServers = editMovieServers.toMutableList().also {
                                            it[idx] = it[idx].copy(name = newName)
                                        }
                                    },
                                    placeholder = { Text("Server ${idx + 1} Name", color = Color(0xFF64748B), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = srv.url,
                                    onValueChange = { newUrl ->
                                        editMovieServers = editMovieServers.toMutableList().also {
                                            it[idx] = it[idx].copy(url = newUrl)
                                        }
                                    },
                                    placeholder = { Text("Movie Stream URL (mp4 / m3u8)", color = Color(0xFF64748B), fontSize = 11.sp) },
                                    modifier = Modifier.weight(2f),
                                    colors = customFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                if (editMovieServers.size > 1) {
                                    IconButton(
                                        onClick = {
                                            editMovieServers = editMovieServers.toMutableList().also { it.removeAt(idx) }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶∏‡¶∞‡¶æ‡¶®",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Add Server Button
                        item {
                            OutlinedButton(
                                onClick = {
                                    val nextNum = editMovieServers.size + 1
                                    val defaultName = if (nextNum == 2) "‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡ß® (4K)" else "‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ $nextNum"
                                    editMovieServers = editMovieServers + StreamServer(defaultName, "")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B))
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("‚ûï ‡¶Ü‡¶∞‡¶ì ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶Ø‡ßã‡¶ó ‡¶ï‡¶∞‡ßÅ‡¶® (+ Add Server)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingMovieItem = null },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Text("‡¶¨‡¶æ‡¶§‡¶ø‡¶≤ (Cancel)", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val validServers = editMovieServers.mapNotNull {
                                    if (it.url.isNotBlank()) StreamServer(it.name.ifBlank { "HD Server" }, it.url.trim()) else null
                                }

                                if (editMovieTitle.isNotBlank() && validServers.isNotEmpty()) {
                                    val updatedMovie = target.copy(
                                        title = editMovieTitle.trim(),
                                        category = editMovieCategory,
                                        type = MediaType.MOVIE,
                                        streamUrl = validServers.first().url,
                                        backupUrl = validServers.getOrNull(1)?.url,
                                        servers = validServers,
                                        logoUrl = editMoviePosterUrl.trim().takeIf { it.isNotBlank() },
                                        description = editMovieDesc.trim().takeIf { it.isNotBlank() },
                                        isLive = false
                                    )

                                    repository.saveCustomStream(updatedMovie)
                                    coroutineScope.launch {
                                        repository.pushToFirebase(updatedMovie)
                                    }
                                    onDataChanged()
                                    editingMovieItem = null
                                    Toast.makeText(context, "${updatedMovie.title} ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ì ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶°‡ßá ‡¶∏‡¶ø‡¶ô‡ßç‡¶ï ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "‡¶Æ‡ßÅ‡¶≠‡¶ø‡¶∞ ‡¶®‡¶æ‡¶Æ ‡¶è‡¶¨‡¶Ç ‡¶ï‡¶Æ‡¶™‡¶ï‡ßç‡¶∑‡ßá ‡¶è‡¶ï‡¶ü‡¶ø ‡¶ï‡¶æ‡¶∞‡ßç‡¶Ø‡¶ï‡¶∞ ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶¶‡¶ø‡¶®", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black)
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶®", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Dialog for Live Score Updating
    if (updatingItem != null) {
        val target = updatingItem!!
        AlertDialog(
            onDismissRequest = { updatingItem = null },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("‡¶≤‡¶æ‡¶á‡¶≠ ‡¶∏‡ßç‡¶ï‡ßã‡¶∞ ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶®", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${target.team1 ?: "Team 1"} vs ${target.team2 ?: "Team 2"}", color = Color(0xFF00E5FF), fontSize = 13.sp)
                    OutlinedTextField(
                        value = updateScore1,
                        onValueChange = { updateScore1 = it },
                        label = { Text("${target.team1 ?: "Team 1"} Score") },
                        colors = customFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = updateScore2,
                        onValueChange = { updateScore2 = it },
                        label = { Text("${target.team2 ?: "Team 2"} Score") },
                        colors = customFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = target.copy(score1 = updateScore1.trim(), score2 = updateScore2.trim())
                        repository.saveCustomStream(updated)
                        coroutineScope.launch {
                            repository.pushToFirebase(updated)
                        }
                        onDataChanged()
                        updatingItem = null
                        Toast.makeText(context, "‡¶∏‡ßç‡¶ï‡ßã‡¶∞ ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                ) {
                    Text("‡¶∏‡¶Ç‡¶∞‡¶ï‡ßç‡¶∑‡¶£", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { updatingItem = null }) {
                    Text("‡¶¨‡¶æ‡¶§‡¶ø‡¶≤", color = Color.White)
                }
            }
        )
    }

    // Dialog for Deleting Item (Confirmation)
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("‡¶Ü‡¶á‡¶ü‡ßá‡¶Æ ‡¶°‡¶ø‡¶≤‡¶ø‡¶ü ‡¶®‡¶ø‡¶∂‡ßç‡¶ö‡¶ø‡¶§‡¶ï‡¶∞‡¶£", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "‡¶Ü‡¶™‡¶®‡¶ø ‡¶ï‡¶ø '${target.title}' ‡¶®‡¶ø‡¶∂‡ßç‡¶ö‡¶ø‡¶§‡¶≠‡¶æ‡¶¨‡ßá ‡¶°‡¶ø‡¶≤‡¶ø‡¶ü ‡¶ï‡¶∞‡¶§‡ßá ‡¶ö‡¶æ‡¶®?\n‡¶è‡¶ü‡¶ø ‡¶≤‡ßã‡¶ï‡¶æ‡¶≤ ‡¶Æ‡ßá‡¶Æ‡ßã‡¶∞‡¶ø ‡¶ì ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶° ‡¶°‡ßá‡¶ü‡¶æ‡¶¨‡ßá‡¶∏ ‡¶•‡ßá‡¶ï‡ßá ‡¶∏‡ßç‡¶•‡¶æ‡ßü‡ßÄ‡¶≠‡¶æ‡¶¨‡ßá ‡¶Æ‡ßÅ‡¶õ‡ßá ‡¶Ø‡¶æ‡¶¨‡ßá‡•§",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = target
                        itemToDelete = null
                        coroutineScope.launch {
                            repository.deleteMediaItem(toRemove)
                            onDataChanged()
                            Toast.makeText(context, "${toRemove.title} ‡¶∏‡¶´‡¶≤‡¶≠‡¶æ‡¶¨‡ßá ‡¶°‡¶ø‡¶≤‡¶ø‡¶ü ‡¶ï‡¶∞‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete (‡¶Æ‡ßÅ‡¶õ‡ßá ‡¶´‡ßá‡¶≤‡ßÅ‡¶®)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { itemToDelete = null },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text("Cancel (‡¶¨‡¶æ‡¶§‡¶ø‡¶≤)", fontSize = 12.sp)
                }
            }
        )
    }

    // -------------------------------------------------------------
    // DELETE CLOUDSTREAM REPOSITORY CONFIRMATION DIALOG
    // -------------------------------------------------------------
    repoToDelete?.let { repo ->
        AlertDialog(
            onDismissRequest = { repoToDelete = null },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø ‡¶°‡¶ø‡¶≤‡¶ø‡¶ü", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "‡¶Ü‡¶™‡¶®‡¶ø ‡¶ï‡¶ø '${repo.name}' (${repo.providers.size} ‡¶ü‡¶ø ‡¶∏‡¶æ‡¶á‡¶ü) ‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø‡¶ü‡¶ø ‡¶Æ‡ßÅ‡¶õ‡ßá ‡¶´‡ßá‡¶≤‡¶§‡ßá ‡¶ö‡¶æ‡¶®?\n‡¶è‡¶ü‡¶ø ‡¶≤‡ßã‡¶ï‡¶æ‡¶≤ ‡¶è‡¶¨‡¶Ç ‡¶´‡¶æ‡ßü‡¶æ‡¶∞‡¶¨‡ßá‡¶∏ ‡¶ï‡ßç‡¶≤‡¶æ‡¶â‡¶° ‡¶°‡ßá‡¶ü‡¶æ‡¶¨‡ßá‡¶∏ ‡¶•‡ßá‡¶ï‡ßá ‡¶Æ‡ßÅ‡¶õ‡ßá ‡¶Ø‡¶æ‡¶¨‡ßá‡•§",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = repo
                        repoToDelete = null
                        val current = repository.getSavedCloudStreamRepos()
                        val updated = current.filterNot { it.id == toRemove.id }
                        repository.saveCloudStreamRepos(updated)
                        coroutineScope.launch {
                            repository.pushCloudStreamReposToFirebase(updated)
                        }
                        onDataChanged()
                        Toast.makeText(context, "‚úÖ '${toRemove.name}' ‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø ‡¶Æ‡ßÅ‡¶õ‡ßá ‡¶´‡ßá‡¶≤‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete (‡¶Æ‡ßÅ‡¶õ‡ßá ‡¶´‡ßá‡¶≤‡ßÅ‡¶®)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { repoToDelete = null },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        )
    }

    // -------------------------------------------------------------
    // DELETE CUSTOM MOVIE PROVIDER CONFIRMATION DIALOG
    // -------------------------------------------------------------
    providerToDelete?.let { prov ->
        AlertDialog(
            onDismissRequest = { providerToDelete = null },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶∏‡¶æ‡¶á‡¶ü ‡¶°‡¶ø‡¶≤‡¶ø‡¶ü", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "‡¶Ü‡¶™‡¶®‡¶ø ‡¶ï‡¶ø '${prov.name}' ‡¶Æ‡ßÅ‡¶≠‡¶ø ‡¶ì‡ßü‡ßá‡¶¨‡¶∏‡¶æ‡¶á‡¶ü‡¶ü‡¶ø ‡¶Æ‡ßÅ‡¶õ‡ßá ‡¶´‡ßá‡¶≤‡¶§‡ßá ‡¶ö‡¶æ‡¶®?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = prov
                        providerToDelete = null
                        val current = repository.getCustomMovieProviders()
                        val updated = current.filterNot { it.id == toRemove.id }
                        repository.saveCustomMovieProviders(updated)
                        onDataChanged()
                        Toast.makeText(context, "‚úÖ '${toRemove.name}' ‡¶∏‡¶æ‡¶á‡¶ü ‡¶Æ‡ßÅ‡¶õ‡ßá ‡¶´‡ßá‡¶≤‡¶æ ‡¶π‡ßü‡ßá‡¶õ‡ßá", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { providerToDelete = null },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// MENU & ADMIN CONTROL PANEL SCREEN (Matching Screenshot 1 & 2)
// -------------------------------------------------------------
@Composable
fun MenuScreen(
    repository: MediaRepository,
    customList: List<MediaItem>,
    onOpenAdminApp: () -> Unit,
    onOpenExtensionManager: () -> Unit = {},
    onPlayDirectStream: (url: String, title: String) -> Unit,
    onM3uLoaded: (List<MediaItem>) -> Unit,
    onCustomAdded: (MediaItem) -> Unit,
    onResetDefaults: () -> Unit,
    onCheckForUpdates: () -> Unit = {},
    availableUpdateInfo: AppUpdateInfo? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Direct Stream State
    var directUrl by remember { mutableStateOf("") }
    var directTitle by remember { mutableStateOf("") }

    // 2. Load Custom M3U State
    var remoteM3uUrl by remember { mutableStateOf("") }
    var isLoadingM3u by remember { mutableStateOf(false) }

    // File picker launcher for .m3u / .m3u8
    val m3uFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val parsed = repository.parseM3uFromUri(it)
            if (parsed.isNotEmpty()) {
                onM3uLoaded(parsed)
                Toast.makeText(context, "${parsed.size} ‡¶ü‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶´‡¶æ‡¶á‡¶≤ ‡¶•‡ßá‡¶ï‡ßá ‡¶≤‡ßã‡¶° ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü ‡¶´‡¶æ‡¶á‡¶≤‡¶ü‡¶ø ‡¶™‡ßú‡¶æ ‡¶Ø‡¶æ‡ßü‡¶®‡¶ø!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 3. Add Custom TV Channel State
    var channelName by remember { mutableStateOf("") }
    var channelCategory by remember { mutableStateOf("Sports") }
    var channelStreamUrl by remember { mutableStateOf("") }
    var channelLogoUrl by remember { mutableStateOf("") }
    val categoryOptions = listOf("Sports", "News", "Entertainment", "Movie", "Music", "Kids", "Infotainment", "Religious")
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // 4. Admin Privacy / Login Dialog
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var adminPinInput by remember { mutableStateOf("") }
    var adminLoginError by remember { mutableStateOf<String?>(null) }

    // 5. Reset Defaults Dialog
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CARD 1: Play Direct Stream Link (HLS / DASH / MP4)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play Direct Stream Link (HLS / DASH / MP4)",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = directUrl,
                        onValueChange = { directUrl = it },
                        placeholder = { Text("Enter stream URL (e.g. https://.../stream.m3u8)", color = Color(0xFF64748B), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = customFieldColors(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = directTitle,
                            onValueChange = { directTitle = it },
                            placeholder = { Text("Stream Title (Optional)", color = Color(0xFF64748B), fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            colors = customFieldColors(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (directUrl.isNotBlank()) {
                                    onPlayDirectStream(directUrl.trim(), directTitle.trim())
                                } else {
                                    Toast.makeText(context, "‡¶¶‡ßü‡¶æ ‡¶ï‡¶∞‡ßá ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ ‡¶≤‡¶ø‡¶ô‡ßç‡¶ï ‡¶≤‡¶ø‡¶ñ‡ßÅ‡¶®", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Play Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // CARD 2: Load Custom M3U Playlist
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Load Custom M3U Playlist",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = remoteM3uUrl,
                            onValueChange = { remoteM3uUrl = it },
                            placeholder = { Text("Remote M3U URL (e.g. https://.../list.m3u)", color = Color(0xFF64748B), fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            colors = customFieldColors(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (remoteM3uUrl.isNotBlank()) {
                                    isLoadingM3u = true
                                    repository.saveM3uUrl(remoteM3uUrl.trim())
                                    coroutineScope.launch {
                                        val parsed = repository.parseM3uFromUrl(remoteM3uUrl.trim())
                                        isLoadingM3u = false
                                        if (parsed.isNotEmpty()) {
                                            onM3uLoaded(parsed)
                                            Toast.makeText(context, "${parsed.size} ‡¶ü‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶≤‡ßã‡¶° ‡¶π‡ßü‡ßá‡¶õ‡ßá!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "M3U ‡¶≤‡¶ø‡¶ô‡ßç‡¶ï‡¶ü‡¶ø ‡¶ï‡¶æ‡¶ú ‡¶ï‡¶∞‡¶õ‡ßá ‡¶®‡¶æ", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            if (isLoadingM3u) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Load", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Upload .m3u / .m3u8 File from Device button
                    OutlinedButton(
                        onClick = { m3uFileLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upload .m3u / .m3u8 File from Device",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // CARD 3: Add Custom TV Channel
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircleOutline,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Custom TV Channel",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = channelName,
                        onValueChange = { channelName = it },
                        placeholder = { Text("Channel Name (e.g. Sports HD)", color = Color(0xFF64748B), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = customFieldColors(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = channelCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = Color.White
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryDropdownExpanded = true },
                            colors = customFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            categoryOptions.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = Color.White) },
                                    onClick = {
                                        channelCategory = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = channelStreamUrl,
                        onValueChange = { channelStreamUrl = it },
                        placeholder = { Text("Stream URL (.m3u8 or video link)", color = Color(0xFF64748B), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = customFieldColors(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = channelLogoUrl,
                        onValueChange = { channelLogoUrl = it },
                        placeholder = { Text("Logo Image URL (Optional)", color = Color(0xFF64748B), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = customFieldColors(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (channelName.isNotBlank() && channelStreamUrl.isNotBlank()) {
                                val item = MediaItem(
                                    id = "channel_${System.currentTimeMillis()}",
                                    title = channelName.trim(),
                                    category = channelCategory,
                                    type = if (channelCategory.equals("Movie", true)) MediaType.MOVIE else MediaType.LIVE_TV,
                                    streamUrl = channelStreamUrl.trim(),
                                    servers = listOf(StreamServer("‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡ßß", channelStreamUrl.trim())),
                                    logoUrl = channelLogoUrl.trim().ifBlank { null },
                                    isLive = !channelCategory.equals("Movie", true)
                                )
                                onCustomAdded(item)
                                channelName = ""
                                channelStreamUrl = ""
                                channelLogoUrl = ""
                            } else {
                                Toast.makeText(context, "‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤‡ßá‡¶∞ ‡¶®‡¶æ‡¶Æ ‡¶è‡¶¨‡¶Ç ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶≤‡¶ø‡¶ñ‡ßÅ‡¶®", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlaylistAdd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add to Live TV List",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // CARD 4: ‡¶è‡¶ï‡ßç‡¶∏‡¶ü‡ßá‡¶®‡¶∂‡¶® ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶ú‡¶æ‡¶∞ (CloudStream Extensions & Repositories)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Extension,
                                contentDescription = "Extensions",
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "‡¶è‡¶ï‡ßç‡¶∏‡¶ü‡ßá‡¶®‡¶∂‡¶® ‡¶ì ‡¶∞‡¶ø‡¶™‡ßã‡¶ú‡¶ø‡¶ü‡¶∞‡¶ø ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶ú‡¶æ‡¶∞",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Phisher, CloudStream ‡¶∞‡¶ø‡¶™‡ßã, ‡¶ï‡¶æ‡¶∏‡ßç‡¶ü‡¶Æ URL ‡¶ì ‡¶≤‡ßã‡¶ï‡¶æ‡¶≤ JSON ‡¶™‡ßç‡¶≤‡¶æ‡¶ó‡¶á‡¶® ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶ú ‡¶ï‡¶∞‡ßÅ‡¶®",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onOpenExtensionManager,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5CF6),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SettingsSuggest,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Manage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // CARD 5: App Version & In-App Update Check
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RocketLaunch,
                                    contentDescription = "Update Rocket",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™ ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ì ‡¶≠‡¶æ‡¶∞‡ßç‡¶∏‡¶®",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "‡¶¨‡¶∞‡ßç‡¶§‡¶Æ‡¶æ‡¶® ‡¶≠‡¶æ‡¶∞‡ßç‡¶∏‡¶®: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        val isNewUpdateReady = com.example.util.AppUpdateHelper.isUpdateAvailable(availableUpdateInfo)
                        if (availableUpdateInfo != null && isNewUpdateReady) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                            ) {
                                Text(
                                    text = "v${availableUpdateInfo.versionName} ‡¶™‡ßç‡¶∞‡¶∏‡ßç‡¶§‡ßÅ‡¶§!",
                                    color = Color(0xFFF87171),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "‡¶®‡¶§‡ßÅ‡¶® ‡¶®‡¶§‡ßÅ‡¶® ‡¶ü‡¶ø‡¶≠‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤, ‡¶´‡¶æ‡¶∏‡ßç‡¶ü ‡¶∏‡ßç‡¶™‡ßã‡¶∞‡ßç‡¶ü‡¶∏ ‡¶∏‡¶æ‡¶∞‡ßç‡¶≠‡¶æ‡¶∞ ‡¶è‡¶¨‡¶Ç ‡¶â‡¶®‡ßç‡¶®‡¶§ ‡¶≠‡¶ø‡¶°‡¶ø‡¶ì ‡¶™‡ßç‡¶≤‡ßá‡ßü‡¶æ‡¶∞ ‡¶∏‡ßÅ‡¶¨‡¶ø‡¶ß‡¶æ‡¶∞ ‡¶ú‡¶®‡ßç‡¶Ø ‡¶Ö‡ßç‡¶Ø‡¶æ‡¶™ ‡¶®‡¶ø‡ßü‡¶Æ‡¶ø‡¶§ ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶∞‡¶æ‡¶ñ‡ßÅ‡¶®‡•§",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    val hasUpdate = com.example.util.AppUpdateHelper.isUpdateAvailable(availableUpdateInfo)
                    Button(
                        onClick = onCheckForUpdates,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUpdate) Color(0xFF10B981) else Color(0xFF2563EB),
                            contentColor = if (hasUpdate) Color.Black else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (hasUpdate) Icons.Rounded.Download else Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasUpdate && availableUpdateInfo != null) "üì• ‡¶è‡¶ñ‡¶®‡¶á ‡¶®‡¶§‡ßÅ‡¶® ‡¶≠‡¶æ‡¶∞‡ßç‡¶∏‡¶® ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ï‡¶∞‡ßÅ‡¶® (v${availableUpdateInfo.versionName})" else "üöÄ ‡¶®‡¶§‡ßÅ‡¶® ‡¶Ü‡¶™‡¶°‡ßá‡¶ü ‡¶ö‡ßá‡¶ï ‡¶ï‡¶∞‡ßÅ‡¶® (Check Updates)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // CARD: Admin Control Panel (‡¶è‡¶°‡¶Æ‡¶ø‡¶® ‡¶ï‡¶®‡ßç‡¶ü‡ßç‡¶∞‡ßã‡¶≤ ‡¶™‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = "Admin Panel",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "‡¶è‡¶°‡¶Æ‡¶ø‡¶® ‡¶ï‡¶®‡ßç‡¶ü‡ßç‡¶∞‡ßã‡¶≤ ‡¶™‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ (Admin Panel)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤, ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö, ‡¶Æ‡ßÅ‡¶≠‡¶ø‡¶ú, ‡¶®‡ßã‡¶ü‡¶ø‡¶´‡¶ø‡¶ï‡ßá‡¶∂‡¶® ‡¶ì ‡¶¨‡ßç‡¶∞‡ßá‡¶ï‡¶ø‡¶Ç ‡¶®‡¶ø‡¶â‡¶ú ‡¶ï‡¶®‡ßç‡¶ü‡ßç‡¶∞‡ßã‡¶≤",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showAdminLoginDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Login",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // CARD 6: About NAFI TV 24
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "NAFI TV Logo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Column {
                            Text(
                                text = "NAFI TV 24",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ultimate Live TV & Sports Streaming App",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Text(
                        text = "NAFI TV 24 is a full-featured Live TV, Sports & M3U Media Streaming application. Features include custom HLS stream decoding, auto-failover servers, mobile & TV remote compatible layouts, M3U file upload parsing, and live matchup countdowns.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Version ${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { showAdminLoginDialog = true }
                        )

                        // Discreet admin secret trigger (small subtle icon)
                        IconButton(
                            onClick = { showAdminLoginDialog = true },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // DIALOG: Admin Privacy Password Login (Hidden & Protected)
    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminLoginDialog = false
                adminPinInput = ""
                adminLoginError = null
            },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("‡¶è‡¶°‡¶Æ‡¶ø‡¶® ‡¶™‡¶æ‡¶∏‡¶ì‡¶Ø‡¶º‡¶æ‡¶∞‡ßç‡¶° ‡¶¶‡¶ø‡¶®", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "‡¶è‡¶°‡¶Æ‡¶ø‡¶® ‡¶™‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤‡ßá ‡¶™‡ßç‡¶∞‡¶¨‡ßá‡¶∂ ‡¶ï‡¶∞‡¶æ‡¶∞ ‡¶ú‡¶®‡ßç‡¶Ø ‡¶ó‡ßã‡¶™‡¶® ‡¶™‡¶æ‡¶∏‡¶ì‡¶Ø‡¶º‡¶æ‡¶∞‡ßç‡¶° ‡¶¶‡¶ø‡¶®:",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = { adminPinInput = it },
                        placeholder = { Text("‚Ä¢‚Ä¢‚Ä¢‚Ä¢‚Ä¢‚Ä¢‚Ä¢‚Ä¢", color = Color(0xFF64748B)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = customFieldColors(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (adminLoginError != null) {
                        Text(
                            text = adminLoginError ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (repository.verifyAdminPin(adminPinInput)) {
                            showAdminLoginDialog = false
                            adminPinInput = ""
                            adminLoginError = null
                            onOpenAdminApp()
                        } else {
                            adminLoginError = "‡¶≠‡ßÅ‡¶≤ ‡¶™‡¶æ‡¶∏‡¶ì‡¶Ø‡¶º‡¶æ‡¶∞‡ßç‡¶°! ‡¶™‡ßÅ‡¶®‡¶∞‡¶æ‡ßü ‡¶ö‡ßá‡¶∑‡ßç‡¶ü‡¶æ ‡¶ï‡¶∞‡ßÅ‡¶®‡•§"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                ) {
                    Text("‡¶≤‡¶ó‡¶á‡¶®", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminLoginDialog = false }) {
                    Text("‡¶¨‡¶æ‡¶§‡¶ø‡¶≤", color = Color.White)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 1: EVENTS / SPORTS SCREEN (Exact match with user screenshot)
// -------------------------------------------------------------
@Composable
fun TeamLogoBadge(
    teamName: String,
    logoUrl: String?,
    modifier: Modifier = Modifier
) {
    val fallbackFlagUrl = when {
        teamName.contains("Bangladesh", ignoreCase = true) || teamName.contains("BD", ignoreCase = true) -> "https://flagcdn.com/w160/bd.png"
        teamName.contains("Australia", ignoreCase = true) || teamName.contains("AUS", ignoreCase = true) -> "https://flagcdn.com/w160/au.png"
        teamName.contains("Pakistan", ignoreCase = true) || teamName.contains("PAK", ignoreCase = true) -> "https://flagcdn.com/w160/pk.png"
        teamName.contains("England", ignoreCase = true) || teamName.contains("ENG", ignoreCase = true) -> "https://flagcdn.com/w160/gb-eng.png"
        teamName.contains("India", ignoreCase = true) || teamName.contains("IND", ignoreCase = true) -> "https://flagcdn.com/w160/in.png"
        teamName.contains("Sri Lanka", ignoreCase = true) || teamName.contains("SL", ignoreCase = true) -> "https://flagcdn.com/w160/lk.png"
        teamName.contains("New Zealand", ignoreCase = true) || teamName.contains("NZ", ignoreCase = true) -> "https://flagcdn.com/w160/nz.png"
        teamName.contains("South Africa", ignoreCase = true) || teamName.contains("SA", ignoreCase = true) -> "https://flagcdn.com/w160/za.png"
        teamName.contains("West Indies", ignoreCase = true) || teamName.contains("WI", ignoreCase = true) -> "https://flagcdn.com/w160/jm.png"
        teamName.contains("Afghanistan", ignoreCase = true) || teamName.contains("AFG", ignoreCase = true) -> "https://flagcdn.com/w160/af.png"
        teamName.contains("Ireland", ignoreCase = true) || teamName.contains("IRE", ignoreCase = true) -> "https://flagcdn.com/w160/ie.png"
        teamName.contains("Scotland", ignoreCase = true) || teamName.contains("SCO", ignoreCase = true) -> "https://flagcdn.com/w160/gb-sct.png"
        teamName.contains("Wales", ignoreCase = true) -> "https://flagcdn.com/w160/gb-wls.png"
        teamName.contains("Poland", ignoreCase = true) -> "https://flagcdn.com/w160/pl.png"
        teamName.contains("Hungary", ignoreCase = true) -> "https://flagcdn.com/w160/hu.png"
        teamName.contains("Netherlands", ignoreCase = true) -> "https://flagcdn.com/w160/nl.png"
        teamName.contains("Zimbabwe", ignoreCase = true) || teamName.contains("ZIM", ignoreCase = true) -> "https://flagcdn.com/w160/zw.png"
        teamName.contains("Nepal", ignoreCase = true) -> "https://flagcdn.com/w160/np.png"
        teamName.contains("Oman", ignoreCase = true) -> "https://flagcdn.com/w160/om.png"
        teamName.contains("USA", ignoreCase = true) || teamName.contains("United States", ignoreCase = true) -> "https://flagcdn.com/w160/us.png"
        teamName.contains("Canada", ignoreCase = true) -> "https://flagcdn.com/w160/ca.png"
        teamName.contains("UAE", ignoreCase = true) -> "https://flagcdn.com/w160/ae.png"
        teamName.contains("Germany", ignoreCase = true) -> "https://flagcdn.com/w160/de.png"
        teamName.contains("Spain", ignoreCase = true) -> "https://flagcdn.com/w160/es.png"
        teamName.contains("France", ignoreCase = true) -> "https://flagcdn.com/w160/fr.png"
        teamName.contains("Italy", ignoreCase = true) -> "https://flagcdn.com/w160/it.png"
        teamName.contains("Argentina", ignoreCase = true) -> "https://flagcdn.com/w160/ar.png"
        teamName.contains("Brazil", ignoreCase = true) -> "https://flagcdn.com/w160/br.png"
        teamName.contains("Portugal", ignoreCase = true) -> "https://flagcdn.com/w160/pt.png"
        teamName.contains("Trent Rockets", ignoreCase = true) -> "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=160"
        teamName.contains("Southern Brave", ignoreCase = true) -> "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=160"
        teamName.contains("Real Madrid", ignoreCase = true) -> "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=160"
        teamName.contains("Barcelona", ignoreCase = true) -> "https://images.unsplash.com/photo-1518091043644-c1d4457512c6?w=160"
        teamName.contains("Bayern", ignoreCase = true) -> "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=160"
        teamName.contains("Aston Villa", ignoreCase = true) -> "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=160"
        else -> null
    }

    val finalUrl = logoUrl?.takeIf { it.isNotBlank() } ?: fallbackFlagUrl

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier.size(44.dp)
    ) {
        if (finalUrl != null) {
            AsyncImage(
                model = finalUrl,
                contentDescription = teamName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2563EB).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = teamName.take(2).uppercase(),
                    color = Color(0xFF60A5FA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun parseEventTimeStringToEpochMillis(timeStr: String?): Long? {
    if (timeStr.isNullOrBlank()) return null
    var clean = timeStr.trim()
    if (clean.equals("Live", ignoreCase = true) || clean.equals("Live Now", ignoreCase = true) || clean.equals("UPCOMING", ignoreCase = true)) return null

    // Convert Bangla numerals to English numerals
    val banglaDigits = charArrayOf('‡ß¶', '‡ßß', '‡ß®', '‡ß©', '‡ß™', '‡ß´', '‡ß¨', '‡ß≠', '‡ßÆ', '‡ßØ')
    for (i in banglaDigits.indices) {
        clean = clean.replace(banglaDigits[i], ('0'.code + i).toChar())
    }

    // Clean common prefixes
    clean = clean.replace("‡¶∏‡¶Æ‡¶Ø‡¶º:", "", ignoreCase = true)
        .replace("Time:", "", ignoreCase = true)
        .replace("Date:", "", ignoreCase = true)
        .trim()

    val nowGmt6Cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+6"))
    val currentYear = nowGmt6Cal.get(java.util.Calendar.YEAR)
    val currentMonth = nowGmt6Cal.get(java.util.Calendar.MONTH)
    val currentDay = nowGmt6Cal.get(java.util.Calendar.DAY_OF_MONTH)

    val patterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd",
        "hh:mm a, dd MMM yyyy",
        "hh:mm a, MMM dd yyyy",
        "dd MMM yyyy, hh:mm a",
        "MMM dd yyyy, hh:mm a",
        "hh:mm a, dd MMM",
        "hh:mm a, MMM dd",
        "dd MMM, hh:mm a",
        "MMM dd, hh:mm a",
        "dd/MM/yyyy HH:mm",
        "dd-MM-yyyy HH:mm",
        "yyyy/MM/dd HH:mm",
        "hh:mm a",
        "HH:mm"
    )

    // Check in GMT+6 first (Bangladesh standard time used in match playlists & BD live streaming), then system local, then UTC
    val timeZonesToCheck = listOf("GMT+6", java.util.TimeZone.getDefault().id, "UTC").distinct()

    for (timeZoneId in timeZonesToCheck) {
        val tz = java.util.TimeZone.getTimeZone(timeZoneId)
        for (pattern in patterns) {
            try {
                val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                sdf.timeZone = tz
                sdf.isLenient = false
                val date = sdf.parse(clean)
                if (date != null) {
                    val targetCal = java.util.Calendar.getInstance(tz).apply { time = date }
                    if (!pattern.contains("yyyy") && !pattern.contains("yyyy-")) {
                        targetCal.set(java.util.Calendar.YEAR, currentYear)
                    }
                    if (!pattern.contains("dd") && !pattern.contains("MMM") && !pattern.contains("MM")) {
                        targetCal.set(java.util.Calendar.YEAR, currentYear)
                        targetCal.set(java.util.Calendar.MONTH, currentMonth)
                        targetCal.set(java.util.Calendar.DAY_OF_MONTH, currentDay)
                    }
                    return targetCal.timeInMillis
                }
            } catch (_: Exception) {}
        }
    }
    return null
}

fun calculateEventRemainingSeconds(sport: MediaItem, tickCount: Long): Long {
    // If explicitly marked Live, no countdown
    if (sport.isLive || sport.status.equals("LIVE", ignoreCase = true) || sport.status.contains("LIVE NOW", ignoreCase = true)) {
        return 0L
    }

    val nowMillis = System.currentTimeMillis()

    // 1. Check countdownTargetSeconds epoch timestamp first (exact epoch from DB/API)
    val raw = sport.countdownTargetSeconds
    if (raw != null && raw > 0L) {
        if (raw > 1_000_000_000_000L) { // Milliseconds epoch timestamp
            val diff = raw - nowMillis
            return maxOf(0L, diff / 1000L)
        } else if (raw > 1_000_000_000L) { // Seconds epoch timestamp
            val nowSec = nowMillis / 1000L
            return maxOf(0L, raw - nowSec)
        }
    }

    // 2. Parse exact scheduled time string (e.g. "06:00 AM, 23 Aug" or "2026-08-23 06:00:00")
    val timeStr = sport.matchTimeFormatted?.takeIf { it.isNotBlank() } ?: sport.eventTime?.takeIf { it.isNotBlank() }
    val parsedEpoch = parseEventTimeStringToEpochMillis(timeStr)
    if (parsedEpoch != null) {
        val diff = parsedEpoch - nowMillis
        return maxOf(0L, diff / 1000L)
    }

    // 3. Relative seconds fallback if passed as small integer
    if (raw != null && raw in 1L..1_000_000_000L) {
        return maxOf(0L, raw - tickCount)
    }

    return 0L
}

fun isEventLiveNow(sport: MediaItem, tickCount: Long): Boolean {
    if (sport.isLive || sport.status.equals("LIVE", ignoreCase = true) || sport.status.contains("LIVE NOW", ignoreCase = true)) {
        return true
    }
    val rem = calculateEventRemainingSeconds(sport, tickCount)
    if (rem > 0L) {
        return false
    }

    // If remaining seconds is 0, check if event has scheduled time in the recent past (within 8 hours match duration)
    val timeStr = sport.matchTimeFormatted?.takeIf { it.isNotBlank() } ?: sport.eventTime?.takeIf { it.isNotBlank() }
    val scheduledTimeMillis = (sport.countdownTargetSeconds?.takeIf { it > 1_000_000_000_000L })
        ?: parseEventTimeStringToEpochMillis(timeStr)

    if (scheduledTimeMillis != null) {
        val elapsed = System.currentTimeMillis() - scheduledTimeMillis
        if (elapsed in 0L..(8 * 3600 * 1000L)) {
            return true
        }
    }
    return false
}

@Composable
fun EventsScreen(
    sports: List<MediaItem>,
    favoriteIds: Set<String>,
    isTvMode: Boolean = false,
    onSelectMedia: (MediaItem) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedStatus by remember { mutableStateOf("All") }

    // Dynamic categories extracted from all sports matches
    val categories = remember(sports) {
        val defaultCats = listOf("All", "Cricket", "Football", "Hockey", "More")
        val uniqueCats = sports.map { it.category.trim() }.filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }.distinct()
        (defaultCats + uniqueCats).distinct()
    }
    val statusFilters = listOf("All", "üî¥ Live", "Upcoming", "Today", "Recent Results")

    // Live ticking countdown state
    var tickCount by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            tickCount++
        }
    }

    val filteredSports = sports.filter { item ->
        val isLive = isEventLiveNow(item, tickCount)
        val catMatches = when (selectedCategory) {
            "All" -> true
            "Cricket" -> item.category.contains("Cricket", ignoreCase = true) || item.tournament?.contains("Cricket", ignoreCase = true) == true || item.title.contains("Cricket", ignoreCase = true) || item.team1?.contains("Cricket", ignoreCase = true) == true
            "Football" -> item.category.contains("Football", ignoreCase = true) || item.tournament?.contains("Football", ignoreCase = true) == true || item.title.contains("Football", ignoreCase = true) || item.team1?.contains("Football", ignoreCase = true) == true
            "Hockey" -> item.category.contains("Hockey", ignoreCase = true) || item.tournament?.contains("Hockey", ignoreCase = true) == true || item.title.contains("Hockey", ignoreCase = true)
            "More" -> !item.category.contains("Cricket", ignoreCase = true) && !item.category.contains("Football", ignoreCase = true)
            else -> item.category.contains(selectedCategory, ignoreCase = true) || item.tournament?.contains(selectedCategory, ignoreCase = true) == true || item.title.contains(selectedCategory, ignoreCase = true)
        }
        val statusMatches = when (selectedStatus) {
            "All" -> true
            "üî¥ Live" -> isLive || item.isLive || item.status.contains("Live", ignoreCase = true)
            "Upcoming" -> !isLive && !item.isLive
            "Today" -> true
            "Recent Results" -> !isLive && !item.isLive
            else -> true
        }
        catMatches && statusMatches
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {
        // TOP FILTER HEADERS (Sticky & neatly placed)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    var isCatFocused by remember { mutableStateOf(false) }
                    val catScale by animateFloatAsState(
                        targetValue = if (isCatFocused) 1.05f else 1.0f,
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                        label = "catScale"
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            isCatFocused -> Color(0xFF1E3A8A).copy(alpha = 0.8f)
                            isSelected -> Color(0xFF0284C7)
                            else -> Color(0xFF1E293B)
                        },
                        border = when {
                            isCatFocused -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8))
                            isSelected -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                            else -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                        },
                        modifier = Modifier
                            .scale(catScale)
                            .onFocusChanged { isCatFocused = it.isFocused }
                            .focusable()
                            .clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            color = if (isCatFocused || isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = if (isCatFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Status Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(statusFilters) { status ->
                    val isSelected = selectedStatus == status
                    var isChipFocused by remember { mutableStateOf(false) }
                    val statusScale by animateFloatAsState(
                        targetValue = if (isChipFocused) 1.05f else 1.0f,
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                        label = "statusScale"
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            isChipFocused -> Color(0xFF1E3A8A).copy(alpha = 0.8f)
                            isSelected -> Color(0xFF2563EB)
                            else -> Color(0xFF1E293B)
                        },
                        border = when {
                            isChipFocused -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8))
                            isSelected -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF60A5FA))
                            else -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                        },
                        modifier = Modifier
                            .scale(statusScale)
                            .onFocusChanged { isChipFocused = it.isFocused }
                            .focusable()
                            .clickable { selectedStatus = status }
                    ) {
                        Text(
                            text = status,
                            color = when {
                                isChipFocused || isSelected -> Color.White
                                status == "üî¥ Live" -> Color(0xFFEF4444)
                                status == "Upcoming" -> Color(0xFFFBBF24)
                                else -> Color(0xFF94A3B8)
                            },
                            fontSize = 12.sp,
                            fontWeight = if (isChipFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // MAIN EVENTS CONTAINER (TV: 2-column Grid, Mobile: List)
        if (filteredSports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "‡¶ï‡ßã‡¶®‡ßã ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶¨‡¶æ ‡¶á‡¶≠‡ßá‡¶®‡ßç‡¶ü ‡¶™‡¶æ‡¶ì‡ßü‡¶æ ‡¶Ø‡¶æ‡ßü‡¶®‡¶ø",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (isTvMode) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSports, key = { it.id }) { sport ->
                    val remainingSecs = calculateEventRemainingSeconds(sport, tickCount)
                    val isLiveNow = isEventLiveNow(sport, tickCount)
                    val isAdminCustomEvent = sport.id.startsWith("custom_") || sport.id.startsWith("admin_")

                    if (isAdminCustomEvent) {
                        AdminEventMatchCard(
                            sport = sport,
                            isLiveNow = isLiveNow,
                            remainingSecs = remainingSecs,
                            isTvMode = true,
                            onSelectMedia = onSelectMedia
                        )
                    } else {
                        JsonPosterEventCard(
                            sport = sport,
                            isLiveNow = isLiveNow,
                            remainingSecs = remainingSecs,
                            isTvMode = true,
                            onSelectMedia = onSelectMedia
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSports, key = { it.id }) { sport ->
                    val remainingSecs = calculateEventRemainingSeconds(sport, tickCount)
                    val isLiveNow = isEventLiveNow(sport, tickCount)
                    val isAdminCustomEvent = sport.id.startsWith("custom_") || sport.id.startsWith("admin_")

                    if (isAdminCustomEvent) {
                        AdminEventMatchCard(
                            sport = sport,
                            isLiveNow = isLiveNow,
                            remainingSecs = remainingSecs,
                            isTvMode = false,
                            onSelectMedia = onSelectMedia
                        )
                    } else {
                        JsonPosterEventCard(
                            sport = sport,
                            isLiveNow = isLiveNow,
                            remainingSecs = remainingSecs,
                            isTvMode = false,
                            onSelectMedia = onSelectMedia
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminEventMatchCard(
    sport: MediaItem,
    isLiveNow: Boolean,
    remainingSecs: Long,
    isTvMode: Boolean = false,
    onSelectMedia: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    var showNoLinkDialog by remember { mutableStateOf(false) }

    val sportEmoji = when {
        sport.category.contains("Cricket", ignoreCase = true) -> "üèè"
        sport.category.contains("Football", ignoreCase = true) || sport.category.contains("Soccer", ignoreCase = true) -> "‚öΩ"
        sport.category.contains("Hockey", ignoreCase = true) -> "üèë"
        sport.category.contains("Kabaddi", ignoreCase = true) -> "ü§º"
        else -> "üèÜ"
    }

    val matchFullTitle = when {
        !sport.tournament.isNullOrBlank() && sport.tournament!!.contains("Series", ignoreCase = true) -> "$sportEmoji | ${sport.tournament}"
        !sport.tournament.isNullOrBlank() && !sport.tournament!!.equals("Sports", ignoreCase = true) -> "$sportEmoji | ${sport.tournament}"
        !sport.title.isNullOrBlank() && !sport.title.equals("Live Match", ignoreCase = true) -> "$sportEmoji | ${sport.title}"
        !sport.team1.isNullOrBlank() && !sport.team2.isNullOrBlank() -> "$sportEmoji | ${sport.team1} vs ${sport.team2}"
        else -> "$sportEmoji | Live Event"
    }
    val servers = sport.getAllServers()
    var isCardFocused by remember { mutableStateOf(false) }

    val hasVideoLink = (sport.streamUrl.isNotBlank() && !sport.streamUrl.equals("null", ignoreCase = true)) ||
            servers.any { it.url.isNotBlank() && !it.url.equals("null", ignoreCase = true) }

    val handleAdminMatchClick: (MediaItem) -> Unit = { targetItem ->
        val linkToPlay = targetItem.streamUrl.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: targetItem.getAllServers().firstOrNull { it.url.isNotBlank() && !it.url.equals("null", ignoreCase = true) }?.url

        if (!linkToPlay.isNullOrBlank()) {
            onSelectMedia(targetItem.copy(streamUrl = linkToPlay))
        } else {
            showNoLinkDialog = true
            Toast.makeText(context, "‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶∂‡ßÅ‡¶∞‡ßÅ ‡¶π‡¶ì‡¶Ø‡¶º‡¶æ‡¶∞ ‡¶∏‡¶æ‡¶•‡ßá ‡¶∏‡¶æ‡¶•‡ßá ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶Ü‡¶∏‡¶¨‡ßá ‡¶Ö‡¶™‡ßá‡¶ï‡ßç‡¶∑‡¶æ ‡¶ï‡¶∞‡ßÅ‡¶® ‡¶ß‡¶®‡ßç‡¶Ø‡¶¨‡¶æ‡¶¶", Toast.LENGTH_SHORT).show()
        }
    }

    if (showNoLinkDialog) {
        AlertDialog(
            onDismissRequest = { showNoLinkDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "‡¶≤‡¶æ‡¶á‡¶≠ ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ ‡¶®‡ßã‡¶ü‡¶ø‡¶∂",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶∂‡ßÅ‡¶∞‡ßÅ ‡¶π‡¶ì‡¶Ø‡¶º‡¶æ‡¶∞ ‡¶∏‡¶æ‡¶•‡ßá ‡¶∏‡¶æ‡¶•‡ßá ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶Ü‡¶∏‡¶¨‡ßá ‡¶Ö‡¶™‡ßá‡¶ï‡ßç‡¶∑‡¶æ ‡¶ï‡¶∞‡ßÅ‡¶® ‡¶ß‡¶®‡ßç‡¶Ø‡¶¨‡¶æ‡¶¶",
                    color = Color(0xFFE2E8F0),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showNoLinkDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("‡¶†‡¶ø‡¶ï ‡¶Ü‡¶õ‡ßá", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        )
    }

    val adminCardScale by animateFloatAsState(
        targetValue = if (isCardFocused) 1.035f else 1.0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "adminSportCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(adminCardScale)
            .onFocusChanged { isCardFocused = it.isFocused }
            .focusable()
            .clickable {
                handleAdminMatchClick(sport)
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCardFocused) Color(0xFF1E293B) else Color(0xFF0D1B2A)
        ),
        border = if (isCardFocused) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F).copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCardFocused) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Full-Width Notice Box for Match Title & Status
            Surface(
                color = Color(0xFF132238),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = matchFullTitle,
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 800
                            ),
                        maxLines = 1,
                        softWrap = false
                    )

                    // Status Badge (UPCOMING or LIVE)
                    if (isLiveNow) {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                                Text(
                                    text = "LIVE",
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "UPCOMING",
                                color = Color(0xFFFBBF24),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Teams, Countdown / Score and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team 1 (Left)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogoBadge(
                        teamName = sport.team1 ?: "Team 1",
                        logoUrl = sport.team1Logo
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = sport.team1 ?: "Team 1",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center: Live Score or Countdown Timer or Match Time
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1.6f)
                ) {
                    if (sport.score1 != null && sport.score2 != null && (sport.score1!!.isNotBlank() || sport.score2!!.isNotBlank())) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = sport.score1 ?: "0",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text("-", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Text(
                                    text = sport.score2 ?: "0",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    } else if (!isLiveNow && remainingSecs > 0L) {
                        val days = remainingSecs / 86400L
                        val hours = (remainingSecs % 86400L) / 3600L
                        val mins = (remainingSecs % 3600L) / 60L
                        val secs = remainingSecs % 60L
                        val countdownStr = if (days > 0) {
                            String.format("‚åõ %dd %02dh %02dm %02ds", days, hours, mins, secs)
                        } else {
                            String.format("‚åõ %02dh %02dm %02ds", hours, mins, secs)
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF3D1214),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.8f))
                        ) {
                            Text(
                                text = countdownStr,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF60A5FA))
                                )
                                Text(
                                    text = sport.matchTimeFormatted ?: sport.eventTime ?: "LIVE MATCH",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Team 2 (Right)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogoBadge(
                        teamName = sport.team2 ?: "Team 2",
                        logoUrl = sport.team2Logo
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = sport.team2 ?: "Team 2",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Multi-Server Selector Row & Play Action
            HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Multi Server Chips
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(servers) { srv ->
                        var isServerFocused by remember { mutableStateOf(false) }
                        val srvScale by animateFloatAsState(
                            targetValue = if (isServerFocused) 1.04f else 1.0f,
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            label = "srvScale"
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isServerFocused) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                            border = when {
                                isServerFocused -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8))
                                else -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            },
                            modifier = Modifier
                                .scale(srvScale)
                                .onFocusChanged { isServerFocused = it.isFocused }
                                .focusable()
                                .clickable {
                                    handleAdminMatchClick(sport.copy(streamUrl = srv.url))
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = if (isServerFocused) Color(0xFF38BDF8) else Color(0xFF60A5FA),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = srv.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isServerFocused) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play Button
                var isPlayBtnFocused by remember { mutableStateOf(false) }
                val playBtnScale by animateFloatAsState(
                    targetValue = if (isPlayBtnFocused) 1.04f else 1.0f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    label = "playBtnScale"
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isPlayBtnFocused -> Color(0xFF0284C7)
                        isLiveNow -> Color(0xFFDC2626)
                        else -> Color(0xFF2563EB)
                    },
                    border = if (isPlayBtnFocused) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)) else null,
                    modifier = Modifier
                        .scale(playBtnScale)
                        .onFocusChanged { isPlayBtnFocused = it.isFocused }
                        .focusable()
                        .clickable {
                            handleAdminMatchClick(sport)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Play",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JsonPosterEventCard(
    sport: MediaItem,
    isLiveNow: Boolean,
    remainingSecs: Long,
    isTvMode: Boolean = false,
    onSelectMedia: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    var isCardFocused by remember { mutableStateOf(false) }
    var isPlayBtnFocused by remember { mutableStateOf(false) }
    var showNoLinkDialog by remember { mutableStateOf(false) }

    val stageHeader = if (!sport.status.isNullOrBlank() && !sport.status.equals("LIVE", ignoreCase = true) && !sport.status.equals("UPCOMING", ignoreCase = true)) {
        sport.status.uppercase()
    } else {
        "GROUP STAGE"
    }

    val rawTag = sport.tournament?.takeIf { it.isNotBlank() } ?: "${sport.category} 2026"
    val tournamentTag = rawTag.replace("Tapmad BD", "", ignoreCase = true)
        .replace("Tapmad", "", ignoreCase = true)
        .trim()

    val playlistSource = when {
        sport.id.startsWith("custom_") || sport.id.startsWith("admin_") -> "‡¶è‡¶°‡¶Æ‡¶ø‡¶® ‡¶™‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤"
        sport.id.startsWith("rtdb_") -> "Firebase RTDB"
        else -> ""
    }

    val hasVideoLink = (sport.streamUrl.isNotBlank() && !sport.streamUrl.equals("null", ignoreCase = true)) ||
            sport.getAllServers().any { it.url.isNotBlank() && !it.url.equals("null", ignoreCase = true) }

    val handleEventClick: () -> Unit = {
        if (hasVideoLink) {
            onSelectMedia(sport)
        } else {
            showNoLinkDialog = true
            Toast.makeText(context, "‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶∂‡ßÅ‡¶∞‡ßÅ ‡¶π‡¶ì‡¶Ø‡¶º‡¶æ‡¶∞ ‡¶∏‡¶æ‡¶•‡ßá ‡¶∏‡¶æ‡¶•‡ßá ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶Ü‡¶∏‡¶¨‡ßá ‡¶Ö‡¶™‡ßá‡¶ï‡ßç‡¶∑‡¶æ ‡¶ï‡¶∞‡ßÅ‡¶® ‡¶ß‡¶®‡ßç‡¶Ø‡¶¨‡¶æ‡¶¶", Toast.LENGTH_SHORT).show()
        }
    }

    if (showNoLinkDialog) {
        AlertDialog(
            onDismissRequest = { showNoLinkDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "‡¶≤‡¶æ‡¶á‡¶≠ ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ ‡¶®‡ßã‡¶ü‡¶ø‡¶∂",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶∂‡ßÅ‡¶∞‡ßÅ ‡¶π‡¶ì‡¶Ø‡¶º‡¶æ‡¶∞ ‡¶∏‡¶æ‡¶•‡ßá ‡¶∏‡¶æ‡¶•‡ßá ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶Ü‡¶∏‡¶¨‡ßá ‡¶Ö‡¶™‡ßá‡¶ï‡ßç‡¶∑‡¶æ ‡¶ï‡¶∞‡ßÅ‡¶® ‡¶ß‡¶®‡ßç‡¶Ø‡¶¨‡¶æ‡¶¶",
                    color = Color(0xFFE2E8F0),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showNoLinkDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("‡¶†‡¶ø‡¶ï ‡¶Ü‡¶õ‡ßá", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        )
    }

    val sportCardScale by animateFloatAsState(
        targetValue = if (isCardFocused) 1.035f else 1.0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "sportCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(sportCardScale)
            .onFocusChanged { isCardFocused = it.isFocused }
            .focusable()
            .clickable { handleEventClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCardFocused) Color(0xFF1E293B) else Color(0xFF0F172A)
        ),
        border = if (isCardFocused) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCardFocused) 8.dp else 3.dp)
    ) {
        if (isTvMode) {
            // -------------------------------------------------------------
            // TV MODE: HORIZONTAL SIDE-BY-SIDE CARD (Details & Countdown always visible)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Poster Thumbnail Column (Image + Tournament text below)
                Column(
                    modifier = Modifier.width(140.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(86.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF090E1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        val bannerModel = sport.logoUrl ?: sport.team1Logo ?: "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?w=800"
                        AsyncImage(
                            model = bannerModel,
                            contentDescription = sport.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top-Right Status Badge (LIVE / UPCOMING)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLiveNow) Color(0xFFDC2626) else Color(0xFFF59E0B),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = if (isLiveNow) "‚Ä¢ LIVE" else "‚Ä¢ UPCOMING",
                                color = if (isLiveNow) Color.White else Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Tournament Tag BELOW the photo in TV Mode
                    if (tournamentTag.isNotBlank()) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "üèÜ", fontSize = 9.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = tournamentTag,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right: Details, Countdown Timer & Action Button
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Stage Header & Time / Status Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stageHeader,
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                            maxLines = 1
                        )

                        Text(
                            text = if (isLiveNow) "üî¥ LIVE" else (sport.matchTimeFormatted ?: sport.eventTime ?: "UPCOMING"),
                            color = if (isLiveNow) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Match Title
                    Text(
                        text = sport.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Countdown / Live Status Surface
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF090E1A),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, if (isLiveNow) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isLiveNow) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö‡¶ü‡¶ø ‡¶è‡¶ñ‡¶® ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶ö‡¶≤‡¶õ‡ßá",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (remainingSecs > 0L) {
                                val days = remainingSecs / 86400L
                                val hours = (remainingSecs % 86400L) / 3600L
                                val mins = (remainingSecs % 3600L) / 60L
                                val secs = remainingSecs % 60L
                                val countdownText = if (days > 0) {
                                    "${days}d ${hours}h ${mins}m ${secs}s"
                                } else {
                                    "${hours}h ${mins}m ${secs}s"
                                }
                                Text(
                                    text = "‚åõ ‡¶¨‡¶æ‡¶ï‡¶ø: $countdownText",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                val timeDisplay = sport.matchTimeFormatted ?: sport.eventTime ?: "‡¶∂‡ßÄ‡¶ò‡ßç‡¶∞‡¶á ‡¶∂‡ßÅ‡¶∞‡ßÅ ‡¶π‡¶¨‡ßá"
                                Text(
                                    text = "üïí $timeDisplay",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Action Button (Play / Link Notice)
                    val playBtnScale by animateFloatAsState(
                        targetValue = if (isPlayBtnFocused) 1.02f else 1.0f,
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                        label = "playBtnScale"
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isPlayBtnFocused -> Color(0xFF0284C7)
                            !hasVideoLink -> Color(0xFF334155)
                            isLiveNow -> Color(0xFFDC2626)
                            else -> Color(0xFF2563EB)
                        },
                        border = if (isPlayBtnFocused) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .scale(playBtnScale)
                            .onFocusChanged { isPlayBtnFocused = it.isFocused }
                            .focusable()
                            .clickable { handleEventClick() }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasVideoLink) Icons.Rounded.PlayArrow else Icons.Rounded.HourglassEmpty,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasVideoLink) (if (isLiveNow) "‡¶≤‡¶æ‡¶á‡¶≠ ‡¶¶‡ßá‡¶ñ‡ßÅ‡¶®" else "‡¶ì‡¶™‡ßá‡¶® ‡¶ï‡¶∞‡ßÅ‡¶®") else "‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶Ü‡¶∏‡¶õ‡ßá",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // -------------------------------------------------------------
            // MOBILE PORTRAIT MODE: Full banner card with details below
            // -------------------------------------------------------------
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Top Poster Image / Banner (Full uncropped view without text overlay on flags/teams)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(Color(0xFF090E1A)),
                    contentAlignment = Alignment.Center
                ) {
                    val bannerModel = sport.logoUrl ?: sport.team1Logo ?: "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?w=800"
                    AsyncImage(
                        model = bannerModel,
                        contentDescription = sport.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top-Right Status Badge (‚Ä¢ LIVE / ‚Ä¢ UPCOMING)
                    if (isLiveNow) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDC2626),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "‚Ä¢ UPCOMING",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // 2. Body Details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tournament Badge / Event Tag BELOW the photo
                    if (tournamentTag.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "üèÜ", fontSize = 13.sp)
                                Text(
                                    text = tournamentTag,
                                    color = Color(0xFFF1F5F9),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Stage Header & Optional Playlist Source
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stageHeader,
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )

                        if (playlistSource.isNotBlank() && !playlistSource.contains("Tapmad", ignoreCase = true)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlaylistPlay,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "‡¶™‡ßç‡¶≤‡ßá‡¶≤‡¶ø‡¶∏‡ßç‡¶ü: $playlistSource",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Match Title
                    Text(
                        text = sport.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp
                    )

                    // 3. Dark Countdown / Status Container
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF090E1A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶∂‡ßÅ‡¶∞‡ßÅ ‡¶π‡¶ì‡¶Ø‡¶º‡¶æ‡¶∞ ‡¶¨‡¶æ‡¶ï‡¶ø ‡¶Ü‡¶õ‡ßá:",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = if (isLiveNow) "LIVE NOW" else (sport.matchTimeFormatted ?: "UPCOMING"),
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isLiveNow) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö‡¶ü‡¶ø ‡¶è‡¶ñ‡¶® ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶ö‡¶≤‡¶õ‡ßá",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (remainingSecs > 0L) {
                                val days = remainingSecs / 86400L
                                val hours = (remainingSecs % 86400L) / 3600L
                                val mins = (remainingSecs % 3600L) / 60L
                                val secs = remainingSecs % 60L
                                val countdownText = if (days > 0) {
                                    "${days} ‡¶¶‡¶ø‡¶® ${hours} ‡¶ò‡¶®‡ßç‡¶ü‡¶æ ${mins} ‡¶Æ‡¶ø‡¶®‡¶ø‡¶ü ${secs} ‡¶∏‡ßá‡¶ï‡ßá‡¶®‡ßç‡¶°"
                                } else {
                                    "${hours} ‡¶ò‡¶®‡ßç‡¶ü‡¶æ ${mins} ‡¶Æ‡¶ø‡¶®‡¶ø‡¶ü ${secs} ‡¶∏‡ßá‡¶ï‡ßá‡¶®‡ßç‡¶°"
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "‚åõ $countdownText",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶∂‡ßÄ‡¶ò‡ßç‡¶∞‡¶á ‡¶∂‡ßÅ‡¶∞‡ßÅ ‡¶π‡¶¨‡ßá",
                                        color = Color(0xFF60A5FA),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // 4. Warning notice if no video link is present
                    if (!hasVideoLink) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF451A03).copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "‡¶Æ‡ßç‡¶Ø‡¶æ‡¶ö ‡¶∂‡ßÅ‡¶∞‡ßÅ ‡¶π‡¶ì‡¶Ø‡¶º‡¶æ‡¶∞ ‡¶∏‡¶æ‡¶•‡ßá ‡¶∏‡¶æ‡¶•‡ßá ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶Ü‡¶∏‡¶¨‡ßá ‡¶Ö‡¶™‡ßá‡¶ï‡ßç‡¶∑‡¶æ ‡¶ï‡¶∞‡ßÅ‡¶® ‡¶ß‡¶®‡ßç‡¶Ø‡¶¨‡¶æ‡¶¶",
                                    color = Color(0xFFFEF3C7),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // 5. Red Full-Width Action Button (‚ñ∂ ‡¶≤‡¶æ‡¶á‡¶≠ ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ ‡¶ì‡¶™‡ßá‡¶® ‡¶ï‡¶∞‡ßÅ‡¶®)
                    val playBtnScale by animateFloatAsState(
                        targetValue = if (isPlayBtnFocused) 1.02f else 1.0f,
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                        label = "playBtnScale"
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isPlayBtnFocused -> Color(0xFF0284C7)
                            !hasVideoLink -> Color(0xFF334155)
                            else -> Color(0xFFDC2626)
                        },
                        border = if (isPlayBtnFocused) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(playBtnScale)
                            .onFocusChanged { isPlayBtnFocused = it.isFocused }
                            .focusable()
                            .clickable { handleEventClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasVideoLink) Icons.Rounded.PlayArrow else Icons.Rounded.HourglassEmpty,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasVideoLink) "‡¶≤‡¶æ‡¶á‡¶≠ ‡¶∏‡ßç‡¶ü‡ßç‡¶∞‡¶ø‡¶Æ ‡¶ì‡¶™‡ßá‡¶® ‡¶ï‡¶∞‡ßÅ‡¶®" else "‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶≤‡¶ø‡¶Ç‡¶ï ‡¶∂‡ßÄ‡¶ò‡ßç‡¶∞‡¶á ‡¶Ü‡¶∏‡¶õ‡ßá",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: LIVE TV SCREEN (Screenshot 3 layout)
// -------------------------------------------------------------
@Composable
fun LiveTvTabScreen(
    channels: List<MediaItem>,
    favoriteIds: Set<String>,
    isTvMode: Boolean = false,
    onSelectMedia: (MediaItem) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf(
        "All",
        "‚ù§Ô∏è Favorites",
        "Bangladeshi",
        "Entertainment",
        "Islamic",
        "Sports",
        "News",
        "Kids",
        "Movies",
        "Music",
        "Infotainment"
    )

    val filtered = channels.filter { item ->
        val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                (item.country != null && item.country.contains(searchQuery, ignoreCase = true))

        val matchesCategory = when (selectedCategory) {
            "All" -> true
            "‚ù§Ô∏è Favorites" -> favoriteIds.contains(item.id)
            "Bangladeshi" -> item.category.contains("bangla", ignoreCase = true) ||
                    item.category.contains("bd", ignoreCase = true) ||
                    item.country?.equals("BD", ignoreCase = true) == true ||
                    item.title.contains("BTV", ignoreCase = true) ||
                    item.title.contains("Somoy", ignoreCase = true) ||
                    item.title.contains("Jamuna", ignoreCase = true) ||
                    item.title.contains("Channel 24", ignoreCase = true)
            "Entertainment" -> item.category.contains("entertain", ignoreCase = true) ||
                    item.category.contains("drama", ignoreCase = true) ||
                    item.category.contains("general", ignoreCase = true)
            "Islamic" -> item.category.contains("islam", ignoreCase = true) ||
                    item.category.contains("quran", ignoreCase = true) ||
                    item.category.contains("peace", ignoreCase = true) ||
                    item.title.contains("Peace", ignoreCase = true) ||
                    item.title.contains("Quran", ignoreCase = true)
            "Sports" -> item.category.contains("sport", ignoreCase = true) ||
                    item.category.contains("cricket", ignoreCase = true) ||
                    item.category.contains("football", ignoreCase = true) ||
                    item.title.contains("Sports", ignoreCase = true) ||
                    item.title.contains("Ten", ignoreCase = true)
            "News" -> item.category.contains("news", ignoreCase = true) ||
                    item.title.contains("News", ignoreCase = true) ||
                    item.title.contains("Somoy", ignoreCase = true)
            "Kids" -> item.category.contains("kid", ignoreCase = true) ||
                    item.category.contains("cartoon", ignoreCase = true) ||
                    item.category.contains("animation", ignoreCase = true)
            "Movies" -> item.category.contains("movie", ignoreCase = true) ||
                    item.category.contains("cinema", ignoreCase = true)
            "Music" -> item.category.contains("music", ignoreCase = true) ||
                    item.category.contains("song", ignoreCase = true)
            "Infotainment" -> item.category.contains("info", ignoreCase = true) ||
                    item.category.contains("doc", ignoreCase = true) ||
                    item.category.contains("nat geo", ignoreCase = true) ||
                    item.category.contains("discovery", ignoreCase = true)
            else -> item.category.contains(selectedCategory, ignoreCase = true)
        }

        matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("‡¶ü‡¶ø‡¶≠‡¶ø ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶ñ‡ßÅ‡¶Å‡¶ú‡ßÅ‡¶® (‡¶Ø‡ßá‡¶Æ‡¶®: Somoy, T Sports, BTV)", color = Color(0xFF94A3B8), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF00E5FF)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            shape = RoundedCornerShape(14.dp),
            colors = customFieldColors(),
            singleLine = true
        )

        // Filter Categories Horizontal Scroll (Screenshot 3 style)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                var isCatFocused by remember { mutableStateOf(false) }
                val catScale by animateFloatAsState(
                    targetValue = if (isCatFocused) 1.12f else if (isSelected) 1.04f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
                    label = "liveCatScale"
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        isCatFocused -> Color(0xFF00E5FF)
                        isSelected -> Color(0xFF2563EB)
                        else -> Color(0xFF1E293B)
                    },
                    border = when {
                        isCatFocused -> androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFD600))
                        isSelected -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF))
                        else -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    },
                    shadowElevation = if (isCatFocused) 12.dp else 0.dp,
                    modifier = Modifier
                        .scale(catScale)
                        .onFocusChanged { isCatFocused = it.isFocused }
                        .focusable()
                        .clickable { selectedCategory = category }
                ) {
                    Text(
                        text = category,
                        color = if (isCatFocused) Color.Black else if (isSelected) Color.White else Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = if (isCatFocused || isSelected) FontWeight.Black else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Section Title with dynamic count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "üìÅ Live Channels (${filtered.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF00E5FF).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Auto-Sync",
                    color = Color(0xFF00E5FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Channels Grid (Adaptive & Spacious for TV Mode, shows full details, categories and servers)
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.TvOff, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                    Text("‡¶ï‡ßã‡¶®‡ßã ‡¶ö‡ßç‡¶Ø‡¶æ‡¶®‡ßá‡¶≤ ‡¶™‡¶æ‡¶ì‡ßü‡¶æ ‡¶Ø‡¶æ‡ßü‡¶®‡¶ø", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("‡¶Ö‡¶®‡ßç‡¶Ø ‡¶ï‡¶ø-‡¶ì‡ßü‡¶æ‡¶∞‡ßç‡¶° ‡¶¶‡¶ø‡ßü‡ßá ‡¶∏‡¶æ‡¶∞‡ßç‡¶ö ‡¶ï‡¶∞‡ßÅ‡¶® ‡¶Ö‡¶•‡¶¨‡¶æ ‡¶ï‡ßç‡¶Ø‡¶æ‡¶ü‡¶æ‡¶ó‡¶∞‡¶ø ‡¶™‡¶∞‡¶ø‡¶¨‡¶∞‡ßç‡¶§‡¶® ‡¶ï‡¶∞‡ßÅ‡¶®‡•§", color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(if (isTvMode) 4 else 3),
                contentPadding = PaddingValues(horizontal = if (isTvMode) 14.dp else 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 12.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (isTvMode) 12.dp else 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { channel ->
                    val isFav = favoriteIds.contains(channel.id)
                    var isCardFocused by remember { mutableStateOf(false) }
                    val cardScale by animateFloatAsState(
                        targetValue = if (isCardFocused) (if (isTvMode) 1.10f else 1.06f) else 1.0f,
                        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
                        label = "channelCardScale"
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isTvMode) 168.dp else 142.dp)
                            .scale(cardScale)
                            .onFocusChanged { isCardFocused = it.isFocused }
                            .focusable()
                            .clickable { onSelectMedia(channel) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCardFocused) Color(0xFF0284C7).copy(alpha = 0.45f) else Color(0xFF1E293B)
                        ),
                        border = when {
                            isCardFocused -> androidx.compose.foundation.BorderStroke(3.5.dp, Color(0xFF00E5FF))
                            else -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.8f))
                        },
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isCardFocused) 16.dp else 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Focus badge on top left
                            if (isCardFocused) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 14.dp, bottomEnd = 8.dp),
                                    color = Color(0xFF00E5FF),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "‚ñ∂ PLAY",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (channel.servers.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 14.dp, bottomEnd = 8.dp),
                                    color = Color(0xFF10B981).copy(alphaxúÏ}mè‹∆ïÓw˝
™£kt√£÷tœãFB∆ŒºZ≥;“(öñºAv±‡t≥g±õí≠ÒXpsÅ‹‡"»ıπN '˜z}aèåƒ±É˝‡uæƒe‡?∞˚∂Nã,íU≈™"ªg‰‰ ëßª…b±^Œ[ùÛk›ZlØ≠[◊,˘wË:Åµn›èˇl€û{<nn¿ø#gµ{˛‰0≤É®U⁄bÀzÆÙ‘ûÛN‘T∫(BW£˛5æ˝cÎ∆Û˛â=;^;tÇgN∂C˜]ÁÖuq˛ı≈˘_.ŒˇtÒÚóÁ$7‘∆ ®Ô{>¬¸∑˝ˆâ9Í7˝qtà∫ÅÓ_kØ¥√âﬁ≠o;ÓÒ	º·nÚ°ΩÈ{ıVTâ7€{0p««Õ?pﬂE∞=ÙÎJ{0Y∞–¯Fn—E_îœ?P˘U/§Wº∏&˝˘÷-k◊~Ü˙9V‰{éu4ç"lŸ˙bb0~“&ˆ˙˛xﬂSæ˝ÒñÁˆü¢xé˛Ó·“Á7ÈRt-ÎE˘lqF_iDyrg<Põº?ö›5Â˘KD˘î´lwlµ≠ÓéÏcÁâ”èFtáV”—P∑pa˚ë?úA;ô}«¡oõ~0pµ˝Éà–†n;a?p'ëãV‚5¥%Eπ„(€iÃHöãÔÏÓÓÏ.#jë˛¶_Ø.ﬂ^^€¨¿¶ÒƒvVï&V~E…ñC}ûé&—x}]œªoø,¥©πH3\k-œµ™/·¥ëdè°õ”˝∂Ö˛qÇ{…UﬁYyõ¥A`èè⁄f˙)nµÚCqã!kﬂ?ˆ-wlaÒemπAﬂsJÔﬂÙﬂQ€ü¶”D+ŸΩgËvße≠ É!ªbYm'≠ı=w“$ÔwxbOç[èÏ˛”„ ¯Eìëı:`ﬁ“Ï‡µñnÁ••ÂŒ J´›˜'gM€õúÿhb†àYlOµòíl9÷¶å¡¨\ß¢ÃCKËqÄDZ¯`Íy¡¶gèü6[™mmÑg„˛0su≠/6mÆ+zZóõ”#7Ú4‘:¶ΩC¥ì¨¶€ªn§◊XïDâ∑ìñóìù¥§.Ë3≠öÔ(µ+_êÓ©Ø"Ü≠Ìç›»µΩP˘ﬁgà˘ªÒM˘ÈoGˆSßπ‘jO''Ë€°™»“≥[Äb€ÖˆFwÒ1F	Ê2ãªù€›E&BIhcxàÍ7[:Ÿ%ÿYFfô„N˝Y˜ÚQ0*J/9úÿ}ƒπy∫‘	ë&C≠Úñò≈ŸÉEÖ˘ÇÌéˆÛ|ƒm¨QΩÌ¢çıM«!;∑K){È*jT@ÛîaFC±@w%§-;Ï˙˝iËX•qqgewóU˙+8r[≠ã}ÒfÎ\ä√adø≥èñ;p◊Æ˙]>RäáûäÓÇ	;à?∂w<$}BWÉ;¬‚≈7Öˇéïz+zÖ{t,rÉÃÒí*?Sq}‘¿•ñ5∏î9«~pfΩÜ·t°ø6Ì¡1ö∏G˛iiËµM≈PBs'Ñ˜lûëÓ´MRbDIx»ì¯Ô¨6nr8Ü®≥Í%ï	ı-ˆCl˘Xè""≈–i&ÚâÅÇ⁄∞«É¿wÔ õd4ÒCß=ÑÓŸ†∑âá‰0
|§!3ÖoÕ(z¸4‘:cä2Î~º¢€Ó€&÷s´±Ô>s¨ﬁìÜäé%Œ`∂]ìRtáa‘w¥∏4mñ«©ùë´ÔVv˝Ær]øöK≈CGÔŒ21wΩ(c!˜	≠b!kÛ†˙xPqÈwv∫wñT˝ëîÍ‡5À∑WVVÔ(Ú ù°“Á9@yæC¶]ˇL¶8“[õ€+;Õëbç>;°X }g‡NG˙Ì)3ïÂò
~ √X¥nV_]j,A≈ 5˚ïˇKÒ€Ï7È'Ú◊ãkH’D
‡Õ*Ù66≠•ª÷˝É'{;á÷·÷£ùùVs€ú~d˝›·¡§Zﬁ_zåÊˇôãÊÂ5ÎmÁ»:t¯Ä¥O€Ûè[’;ÚÉ-ÃVÏ#œπ6úé„ßıÏ£√~‡8ÒŸƒy◊⁄w√Ë˚∞§ÌΩ»ΩAVŸ0>–Ÿ†+ùË˚à°eˇJeˆ]dü˚ûcÉ]1¥ë‹&?˚„C«C/åΩk5ì∆[÷Õ7¨«cÍÃü¢K…cíÎÆQÊıÃ¨–±É˛…ßRÀèŒ¨ ) £#¥õû[£iÔz!mÁ`ÿl4ZÒºí€†/HPÂ^~ÔÜÁqo«c∏Î€Ndª^(m%Œ7ﬂhéë‹kQSMÌˆŸÿ‹Båî±XKÉE0¸ëÂ˘6íYÒYì¿Å”{4.÷ÿé¶‚XDƒ]Ù¨iq+me=È[ì4¬
∏qÎ¿Ü◊Bó∆}áuÄ∫O4„!- Ïÿ˝xY4=ôÌ-¢wÄ∂Rçµ3 9S@3@øÉ2‡G±&`Ωˆöuæt~2EK®Ÿx<~:ˆO«çY.~‡lŸ!∞(ò:≈ãﬂrêX∑=Ó≈<¡áuì‰Â€}‚à
°WBù$ΩÒj|•&wÚË“Âı:3Ø'ø}˚?˘èØﬂOÏ√F+fN…L]ŸpÒb‰LÛªI
KÅ›◊ÏÎ“)∆Õ£FWp'ydG˝'<ƒAègûÜ&ïŒË{Ôq«çΩ …∏g∫ÀôoYS…J´⁄Z77`AÆØ[∞maπÂS~Z…„Œ–›ÖÁ¿óÍ∏&öüÑ’≠[ß'ŒÿjÊo©„•	å/˛ZXúp)≥ú“n„7q9ÿ≤Cweg/ﬁ ≈µ™>Å“%Q≠Y„µQ˛ÿVéid>fwzVnj
L #bëßntÚ–#¨óÊ¡u7r2Ã∏j⁄aÔ;£ItÜÛ>LÍ¨ƒ~ﬁ¯YÙÀ¸{§BÒm‘ÇÍ€†ªÓ€tùÁéü¢A∞'âL[∞Óßíé—x‰≤æê»;¯ôÎe!nX/xÇêÙ≤}ÏD¡√iÑÖJ™EI#å%~Fã#SH+Ï∏ÂcO NE$±$Ö£‚ÒÈ.Ævn«Kó{§ÿƒku”Nüq0ç¿k< õr◊uºA3?éS'+F≤Fé?~ó¿È—±É£ X%pm¿ºk‚Ÿ}Áƒ˜àù]‰rbÎ∂qq˛≈≈Àü‚p»o¨ãÛ_¡ˇ/^~tÒÚÁÁüìê…o d˛˝˙¸k|5˙ﬂÔüYÕãÛ/Ò’_†Ow≠ø≥OÌÒÇ’Û˝!¸wﬂÒ¨áˆô»gh∫°RE˚˜ŒÚ∆“Êö¿˙cl‹¯†£pYéÕd€A ;òüÜá«üe£ƒ»¥.cÄ.–xÆÇc∞Áát◊cû«’œ≤JÀJ¯˙†»∆fWJÌDô'ÑÛ‚[ûè,Aƒ€π†ë}uYpL©Eö(ï„Ã≤cKæo°Sp.p¸aÔYá„ã¿ã«L√»·ùéG$lÊF˝7¶R0˘ï—f hï ∆§I„…,d≈˙H 7±≠Fu˙Ó˚ˆªgÖsòxí·@éˇ¬º%‰OÆ”ZÁ5ú#_ôﬂÁ»GwDg!ç‹RM#l=T¯‰ÖÌces⁄ú1ºæû4¿π;∞‡»6äOlÂñ3v ¥8=ññ$(5aè]§Ã8ª» ç6B|;ü#GvÄ$‰ìXH–‰(9?Ó¥;›!—%»èÙE·ß≈Â¯'ÙÁêœ<IG–Ü>ú8}ú	®	ÕÅ=ö†ˇ>ÇüpT⁄jwàl©»«NÀ¸_ÿ>§_á‡æ*‡“û}ÑcµXîo≈c—(a’@Rﬂ∑dóv≈C*b∞i ÊàôôGZ{ÅªKÓLV\ÊæÓ Í“Œ¶¯>jºÏ|é ªƒ«Æ˘z .˘•úC~ww{uqQ‚êœáÚs∫πÁPôZ:| Oej7ZÉˇt«sûŸ±h‰Ï”4tñ%ø!ù ûv˚ßIôäxH⁄˛˜Ç(êl∞”
$R1ËG±ó!∫∏ù$<¢˚O·"¨}‰m ®ãœi&Ú≥zÊBmL·Ö˘ú(ÇCB€¯åîQlÚ!˘;›ùµ›E…±DV9ïù¿dNZÚΩDÊπ≈v)ìG˙•q>£|S‘ïn„EuúÇ√9Î”CÀQ†ÓrC·TwçB“@ö…"9—çP„-jYrD]yÂπYeMy7rlr*ÄgPœB¢˘,‚\ïÂ5∏ƒÎ	Ê»`˝‡‚Â/ê≠â˛µr÷ÎÔqN∂_œˇbÅaä>√áœ–Ôçnü<⁄-≥ù¡“îwÌg∏_øDO∂†üÁﬂ‹L∫CÛ?Fø|N∫˜πˇH.˘1µÒ%`Sˇ˝këÓ√øü~íâå±\‡=€3eÑ<mô∏I_{çÔ˚Œ/d Ùz÷'zÓ;7√ËIœ>Ûßvå%^ÆwücxO‡üfÉ7¿∞mû˚&11D[c√t¡ºU,f9ºÜFÍ˛‘ã‹õª±ˇiŸ~‰·≈∏e>í»‰c¨¿‘ù8∑BbænŸ”»G√a5§8€ò”f<£Jæ|"V"¡B≤˚ë˚ÃπÁ˛b	Ô»ç§EûÅDI¯õç‰ıê~>çU¥—ÃƒA>˚ˆt‹?q;√!Z∆˘◊&S¬5∏Õz√Í®Ñïúû∏hπ7âì]-
Â)Ã˘tW4Â/¥⁄0M§c/Ó´≈%‰gd›jÊøzΩ@-ZÓ†î>ƒ<XA≤í–ä∂áÌqÆ√h<ú†è~m..Xºπâ^H⁄xÏ≤˜¡ë}¶ù'èˇ'ÈzwCËU5ó €±‘öæoÄœG¿Ù{ñJ=îî=+=îœ@2
EØ %I≥JŒ©IÜÑnfÑ@çñÁFó±»ò}:≠NÚ-ÔvV‘≈âÕöÃ≥‚]ñ›~ l¶Qk6skŸf¢{öy∂¢î⁄/s/+Fg&nŒñ◊pÂ‰£+ìÉÃ0∞Ó^⁄áq~¿Z›vÜ6“d¬v}H<#Õ˘∫Ë*…≤´’4Õ∑D0Kµ3UÈlí¨ö$™ÊV
=W∂ﬁºãÿ_M¬ª∑naTÉ∞=áœO`oMN¸»øŸYYZ]^^Ï,≠Æv◊nÆ-ﬂÈwnﬂvnØ⁄ù7O◊◊µ–KxYØ˘ÓiÁ9âS_∑_#¸TOóë⁄U ˘uÇé)ﬂd…≠Ãi¥÷ç@õ¡-%jÂºÿMç~;@q¨ñ—Ω@ƒﬁÓ!”
ŸTÍÜ9lM!È%üM[÷®§urÊüo˝Œ⁄P?sHˇ.Ω;T˜á“e”KarXã‚€—N∏EƒîöRŒ™º◊™†‡$çPuéwË+#≠SÕ|åö≥‹Ä.3E#EòR÷wá}‡˘Õπ™iFI?7Ö9>"ƒ|'∫√´ö¶“∏Ò<5∂_∑:/–FæÒúcWø–P+(’î'ƒz1;µÊ∞ò!¶)üú∞Öñ¥Å6ÍœAQ∫LæâRÌåzªk!3-%‹ZHg◊i…/ =N≠∑ı%ÃÃ8ëëD ™7G®»zv:ùÌÂ5Õùß9 @fÏ(¡´¸„øXª;Ω«èv∂≠√áΩ˝Ω∑Óı¯/P%ÄJñ*'“FjÇî°T1Ay∆*∏G¶t•7PY–è.g=ÿy€z¥≥ø≥q∏Û›ﬁ5∆`ØﬂïM£~•2<äjÉÊà˝k@ï◊'´OØ’1a¿∆_»†ÚE‘ù”"Bv[a—∑÷∑ˇ˝ˇ[úp¶8ôªã›’Ü2HCs£3]ãØ&¿Çº•w)”(â8‚˙±Ò∫¬Q{:OY,Í gVyJrHˇíCØg?Ù)–›ñ–'@∂äN˝•^GyOÕ‰âå‹‘@ØbI≤Fó è≈Úd¢éqb˝zˆZ÷˛©4“Ø{Z_âêl€S|f≠)¡)ë@=x©ÜN‰?òNFz∆ M¨c€ã∞ä⁄à	®Ø’‡É5µy8úÃœÃ√—
‹ ¸…º•˚]Ê-{„°o»V∑ˇlŸ
ºOc°†’\ÆATÖH˙3êåU{'”——1rœ'ô¿U8B∂"ü∞¬VG|
˝◊Û¨±söÂ≈©ËÚ´üYõF»MªQ’tJÕ¬›,â≈PSñáO|"Â‰√R!ÕIÉ|Õ}"ÅåHË÷4»ò†ñ ê=9 ”h¢?Ò“Æ'¸3OWËl5NB"É÷*¶$Êì°å†∆ıÂΩ6;∆ºº¯>iF^å¡…L*ƒÃÆ$øwÑ{2*∆‚3ØÖÀ¸à«Ÿäz“˙*iö√K^\LxM=Ü4”.ıﬂñ0(ñyTsÏ éj÷$å”Jãœè∂äv€zfêvúlÊf6fñ˚=“’2µèjÅÍXÌ@¬¸í&®W~…L«üÂ…3ê>Ç˜Ãç
”:2îhòn¢b‘ü€’äœeâk•}48D»µ\C¿.Kum"†8‘ÏÏ)i◊™ùêŒ˙L®Ú…fM”≠£ë‘ ?ƒ⁄øãıÑÊ˙üÛ’r·¯¡»÷¨
E…¨lKıï∞»ÙãÁ≥ ⁄P« &˘UwJË˝¬¢*|uÎñı»Ém‚ ^íˇÃ+É¡√Èc ÙöÙ|ÓÅ=4-;¢6≠¿ÑÖ›ﬁ|Í∞àp´L&∆1{Â∆zô`ekÄ†wµÓ9ˆ†ÑÅ+©ZsMNSÑ!ÅAUMSD«PÙ»÷≠ù©hcÿ’µÈDßé3ñ6®¢{)´◊≥–Cı›gµπ®Í¨Ù¢ÈΩØŸ8óö+çÉG{oÌ=ÿÿØAP1™•æ∫)&A-s®óTg±≥Rh¨H3[¬&ıÕ™√Â!\‘6œ‹yÉn∫ë∞<Àb7û'™…,â´2T7í‚√ä®öaRgaîy≈dŒ±r¶z˜‡ÏâÛ,«è`,’ëèM˚ƒuN-Ä)Rk§ÄM•µ¢Ñ”Ç≤\˜®r‰ZOçR§ıÁèg˝!Ÿû8°_S©|ˆYÜñGZ `˛"=„)ü*&I@j6∑`+Ó9>`¸e>û∫à»…ÂÆ˝ÃZÁó ÎºZ¸Ê‡¸0ﬂÎ?æƒ’†7–º9úK<h¶Û-+?°Ì≈µÈfeÿREΩainò∏< ‚‰¢MªØá%≈ún5@Jï¸∏ÄÀÃÓÎÍü ∆h2ŸßŸZÜ›F–2I„¶Gg®ôëv¨n›*&¨Û¬Kß©≈Â‚Ç9ªõ`X[ﬁ*0,'[‹LÔ2)îZ¨Ã£Ï “C[AÙhu`∂ÿ»<“X¶UÑ2ÀÜRWêuL ‚l’‚◊¡ô°T˜Å¥n}˜LvàbÑ`≥;¥nYKÜ'Ö¬*q∞ˇ+sB<™˚txπﬁ”·—,OÜw]CƒûzPùXR‘vX"h67˜ùad˝ê‰;YG  6,ÁÏI‰2´›,naUÊÒPåL1íxÛ…¡‹1¿q 2Ÿﬂ@ÊgÌ@±ØjƒÊ√±5≠Ómkg©∂Ùb IqZD|D™ˆàzêÅ*V6zÆ˛]äh),¡°1è„„+…RF¨ë©qó÷fÃN¥!ºÄLôâQ!•Yá‡Q∫îÌd8†@ÍŒaYLy6¥ÆÕGHúuT©ŸÇ[ywÂŒŒ‚fHB qîöªYDÊwñ&Ri&«Á©öH àıÄ∞hP€◊⁄¶z7•Z%:–§:}åH≤œy	Èá•õâfR\·–	4–0#dÖ∏Ωd3U˘@±aiYM‚qó7Ô¨uÍ3%ÄT‰ø12•*r´66‘¯ˆ√è≠œãÈÖu<ª¬I0Ü◊X˝–ehA@WõÁ—bÂ8Ÿ≥’¿`YbJÀΩhÉ§Á{ÌTzxijS◊ök¿g6FˆF“&â–C_,4íî%3É‡2dy’‘tÏ}‚Ù#ÊpÕ{+ßª'Î+6¸ﬂ»©DÒ≈°≠W`∑±~œºõπªåà=ù™ ã≈zøY˙$êâWDÔj≠Àuëg)]©cµ–-˝9ªH≈NTéwâG5A/ÊB:ÿpäcÃ–§ÈjÜe4√£85˜≤Ri§E˙“K∆/]Í•Z[‚X∑Nƒñ*€§Aµ˘<ﬂàáøW6©y‡+%â£!›%Êÿ•’óÙ%Ø∏ÔböˇS\Ô≥P∑s7ÆÀk›≤q©OÎ≠¿X\õπ¥<˘L\cIËa◊™Ü≠(∑Y\·,	=≠√©_9[´R±Ï∆~Ùø?∑nÚ‚ö7ûÁ
1cóDK¬≠¥|fRì”_Âkgº‘µfû≠|∏ﬁÑ»»îπriDgÚ±∂uoªD†ñ„–Àê0Wäfïˇ¿ˇE)˚W$ÁŸÔûµè#lC‹˚ì§¶ñ+à®ÌcÓrP≠UhiÀÒº∞ΩÎVV8ØƒÅmÇπ”â∞œiâ›l0|f†%€°Œ¯˚ñ;£ˆı„ídµí√\9{≥Ä‚˛I¨?DS÷W©?5	IØ≠\-”…íò˛eıò˛π«Ògc˜Ò6.ﬁœ_yå˛\ÍŸ2aˆ…Î(‹∆	≠gó¢vhΩV8}ÂzYÿºB^RıPyAx|f£Ã,<æ‰Ìt√‡≥3?ß–˜˘Ñªó,£™aÌôÈVk/«	)ô4•PıπzáûÖö´xZuC…Ø@Ëx=°‚µÑÜWW˝ÊÏ°˘É|Dq ¥ï†≈∞Ò›ö¨5‘U¨?h{>‡ﬂ˛˙+Î‡Ô+·ä‡yÃ$`ñ¿"ØF /ó∂ö˜∂ã_˛˚ÚŒhÌ‘zj•	]?∆úOyrj»ÆHMΩ∞HF2ô$ã9Ç»òmËz˝Ê±èÀØRﬂùâÉ&5®˙æ{R+“∫Ó=©24£©ïπ0´LÖπ¨vıC?§©j¶AMôl&A]ÍâöÖz TwNÄ±Ê»â˘_˘é®ë:°˙ÛUÏ2°ˆjb£zh}ùfõfƒº0BæØ‘Z%3®¢>õ"Íl@qÿ<˘tâ˚∫FÿIÌ(≤yÖæœóá$°¡'¯OTÿu÷ÎPØUï4@OÊ]Ø‰Bß^Ø
™ßm]b¿xm‚≥/©´Yõm(ˇU˙≥j ˜\∏¨0{»º⁄«>‰‚/“™Eµ ™KC”àÏæ“	b•l≈àÎÆFﬁjuÈ®Ä™CΩ¨¸ï#X<†u≈Dk/ƒôƒ<WåqVäi÷0¡ÍÄ˘’_á3^63$æ∆˛ãˇ∆ v¢ïá∑]€ÛèÒo0E¢≈◊âõÑe°I Ûoª~ùπ:HÆ"Ê)<&ÇZd†‡y±ôØß¨È·Ü¸hg4âŒ ¥9ÛYhÄ£ã=å«ë>4˜mw€+çzæ‚q‡±8ΩíAd¥≤˚“oª·»√GŒO¶NYÚBœ0åyıUVïùW∆Qb)w9ëuX:q]1Ô( EôÊ0ìPp*·˚ÅÔyM∫&»G«÷™ê(Øâ4i‰ÖÆ“§£,Qa—],÷”R)ƒ¨ê!S†T0fx±ZxQ-‡¢R†ÖyÄÖ¯ó“∞†*ÒàäaÏÚQí•õ¡MÈ≥$J€∏h’Õ“òák˘c»∫Ó!vÄ¯ )”sÁx1f£füMêzÒ/JŸjNX√”x£ïj~˙Æx∆Wf/™©ËfßÎFßÍ˙Á’Ã∫Yû'äi≈‹ØïR∞îdÄvrùâ€“ô–>MD⁄IòÎ,pØw8≠≠G\
ìä£∞„¨g√<éCΩö,G/öë]Ójy¸¢eËå‹öôœZû˘,’Œ|ÄRÉªZ83è¥f¥Ò#‘Ú›åKG≈ëS¡â√Nl∑¨<WµQãCf2nﬂ˛Ò_íQ#œ17Â0¶¸∏UìƒÛî0◊…R	ú*h6å∏,/W™ú'[xä.ªV™Ä¨7_sgU∂‘Ey¨‹Øo›äùK…zBokÆß‡˜ãÛØ/^˛Ú‚¸#¸Ôü.Œøπ8ˇ{˛¯ﬂ˛ë¸çæ˝ˇNæ˝˚!˙˝˛ˆß°y„yˆ¯?ˇÜﬂfÎnù	ËtÇ‰8(uÈH•Ö±Í‘ì kˇjÎF$üïù+H^u±µ§Ävûœ∞´ﬂ˛c|€?ïﬁ‰Üá±+ìqÛ≤‹ıu“iK ±@≤L?IŒ0KŸ6⁄{Ö¬ÍJZ ê©&®›#™0öiCº≤Àôôãóé¸àπ÷Ú√ Jv‘™ÊºÁÈ“ã´E{Ã&p#É±=÷(i_Ctl&‰"∑“âk9ø∆W7VvÛ2«]®Ÿ®]eîFÅ∏l{lTÜWm–™§X®©¢Ïùogœﬂ”ŒÂƒ0È`·H∂¶…ò3S·pnË#ƒ∆=,ì≈∑1Òlbó
bp˝$?∆±ÆòHÈˆ±†Ç79ºÁ&ÙJ‘«0í∏ÏÒq-‰‹˜¸áû}ñ:¿ôùúx¢ØŸû¥ßÅ'nR~»)º-á êˆI†Yã†U‚§~2˙I.˜˛'ˆKV©cörΩ≠¢Ó*`JÜ:JπBHO˝ñ˘åL$/∞|»≤}M$ô¸”	S/´î+Äq*À$Xﬂãj„m;Íüƒa¸S´yq˛{lb¸€≈ÀügLìVcAÓ¢.‡Qü+›€rr/Ÿ€–Âx≥°©eÁ˚RH∞∆ñá∂XcAÏô)}ç‰πÚ≈5d"[Ûf™•Åﬁ∆¶µ|◊z∏øÒ£˝Ω√ûu∏ıhgÁÅ’$)∞.q9„˚Kè≠◊¨¿lΩ˛¿	≠{”£Vı.¸`´‚†ˆ^N«}bœ>:Ïé≥ÈI¸ux◊⁄Gˇ˘>Ωlo<Ùﬂ k$pP;n‰gw-Ã™%_êh8”]k”˜=«é˝ˆvΩ9¸êV»£íœo¥ „Òò˚c⁄ä”ÇÓNÆÅe˜‚]WXd¿Œ∆:«æè4Ÿ-Ú±›üp»…\¯S§ó9áà’√F°±[ôb—ÅÔ‹…ëoÉ˚ˆÿ>∆{ú<!˜}Ú®¯Œ¿≤˚ë˚ÃIfY‹ìÚ7ﬂhØﬂã§):C0c§]»ÀçÏMÅ‡[pZ•≠∫·æoÉπ°‘«,
‹ﬂ'Ω!¯ã?ú:¡ôºÅFÉΩ;˘#≠$XÚ&†pzã	‡"AHVø#àFP∑–€cj≤∂”Áû¯ßÉ}Á∏Ω7ß≠ˆ¸ÙúZ¶∂Áo£â£∆ËhÏÅ5◊w&®K†(ÒO∞pk>ÖÈ”%tÍF'Óÿ≤ì‡F6—}˜ê)Ô!±Êå·—‡Î»-eé_:wENÎ)¨ﬂuãYì…eúµn51W«ˇ6Ê°UKü‘úxw-vT§ùö§] rÇ∂gO«HgUqÉ¨[Q0Õ&åE®óE˘Ü›E‡¢¬<ÜÚ ˆ–A‚˛an4–;eg»p{Ö±1ø¥›0â¯\ﬂ£ˆ»~Í`—ÛÃ´qq˛˛≈9®©ÓˇÇª4q∞m‰Â/¿è˙Ú8O·ó/±+Ù3rn>ˇÍ‚ÂGË+˛>Ä˜ı:íÛ§˚;ﬁÍ›˚Á√{èz≠6ÏIé∫ûìÛV+KM$dvﬁÅUéÙ7ﬁ´:m@Iã–ûÈ?Ì‡j+6-
·X¯[4 S≈Ï¸2.__úA/˚íº˜ü·•°âﬂ¢·ú ièú0D¢‚Ö˙ º∞ÜÓ¸2ú∑‰-NÃ® Q&ÙM˘¶˛äu9ƒíÎíπâTjdpe1w±»$í°W‡ôXœäœc]'d$q3øÚÛSá=≈¢‡ı¬NA≥:!êÜÙº◊z—F -⁄Ï}ƒm⁄1=Ñ8Ÿ¯<'3Vô~R‘Df/
{ª¿·b"aóµ¬´0}uF<ø6∂da[8a"A◊”–Âºp]G¸∆ÕzÔ=‹d:DÎÎ¬N JπtÒ•—ÿ“Å}Ô=©üDõ§¯ëºtè«~‡lŸ°Û‡ÚVÈ´È6\h5?æØΩñy{·~K˛‰´Ñyï ‚Uâ˚|≥”È.2VS~ë°]}œ±8˚9˚L·…ÑFîàÊQœU≈›.∏‹ïP`Õ¬Ädæå≤tQóê\?„ë¢Œ∆#ô«#ækRv$P~êıaˇœfi5?€Ó+	ë»Ä``ôr≠#sU˜f	ˆ˝iú5îF1¿F‘[€µÜ™$zÑjJ°q†®yÿ'Îs[U:Q–Àπ™îo%ﬂ•†2C|˛¥⁄ˇ°˜hg„~CÂ@Æä˚3”é:Ür≈«Æj≈Gâ[PóRıêH¡{¨Üp¶hhtMÃtƒ¿bŒhh+MaéÔ˙ø(G¶ëCŒúçŸ95ÇˇLΩ `‰∂€Ì9˙l‹x^∞dr—TjŒÉﬂ$ÙÏÀHtjãs”‘€≤˛¨Ñ#ãÓÎTã9√¿	OLî¢¯VΩàùÌÅñìõ¡“ÂVã“∆Js‘DÁ∞x®d∫í≤ˆ_ÜW¡·Hw/πJ‹S=kTˆ&¯m‹†?ıÏ‡a‡£uÓçËÅZ¬≠+^$‰‡PÓ≠$!ü6{Î^˛ƒæ,)3„ùI“p€Bƒ¶·¬%lÅcJ›π:+∑‘F.IpLãºJ‘°øÜ•/Œ?.e YæªÂ˘”¡¡p(çH(Œ’Â€Àk•!åUX)…r¶G¡èü˜t|˘˘Ì€[1
+¬m√ˆïÀ˝9ÈŸıh+@^‰Á‰/&†˚‚¸gÁü&7êÒ?ƒÒ‚p˝Wçˇàº?9Á¯%m<€ﬁ≈ßü»bä!fXü¬k3∂·ﬂÒïd4+‡Ñ))
äSı»°äi\Ñó„Ò≈k-≤d>ˇ=fÈx˙“âhËÁz»ø·V∫ïî‹ÑCÛqËú‰8§pı¡4Ç¸âº◊ÆÎxÇ\gqë!éKö{Ω?∆eâH,ûpÆìÕçDéƒKﬂ9A{≥çÁÒ∏ﬂxûs≤º∏yq˛æê)¸è=˙ﬂÔ»$à§d¬©ÚªA∏=+k¿&q˜8Lìº©À§™ÑË±Q`ªÛ\Òâ8Kxg,Œä|•3fªqŒRqÿTæÕ¢l	¡(5∂–8∂Ka†Ñ±V,ió*ˇÕÕç/ITëÖ2veäOÃA˚”0ÚGxœ«|SÙ ‘Oœ' /û à„–E\(9Ä⁄:q'¸@ÄÙDïB‘vEÀ®4Hß^ülÄÄfùLDgü0±‚	mUç$•É)EËìJBQ&7Hv˛ä⁄ìÔ2Z/™ß6Ì‡za^à_/JÀ@µ;›aj—∞!Ú∏>ûjy<†πó»J ‰•É%>µíœdô^<+Q!¨X¯(≥»næ°·•…∂í,¸L±f®‘≠à¶WH˚V∑&y©Ï–hîÖÀ˙Ùww∑W¡‰≥C©¸ÃÆi!:†˘£Rò*¥˛)Øÿ√d“b∞µ¸¿Õ–ïîóLYÅ"¶+∑ºdd\]∑©Saﬂ¿Àw,#,€™g<j√s¢«ö·√FYxX6åü0Ü}˘,;•îyJ˙pÙÆ\∫≥ ¯Jê>ñæÛÌ?ŸäâáU≥JÀOÎ58V9W˜•Q(Pª÷j¬…≥ÎOCk˘&©açD˜ô?çêùlıû``=‡l!e'§º	b.z◊Òî©∞¯*W∑∏ˆ2YKı◊^ñ LKTÒ*≈µS.yÄymm•ÊgUZ;s‚Ÿä¸Ω2\íˆàÔÑ8ﬁ\[?N⁄Éƒf∫∞æö⁄Hˆ°ˆj.®ùˆêSPª≥òZ	´ØBAmfà^·J⁄q≤en6V◊R÷∞¨Vè#UöË®(‹√”òòu|iπ„πPàÖ|uãt≥õÔ*“Õ¨ÜW¨Hw~L◊ ¢πf]∑õ]µ‘ÌÇ„ËRÅ™bK§™`EÅÆˆ Îjq˘UPÔhÈ«“∂Í™ˆxîV´í´Dy‚Ñ6ê‡¸¬¢XQçU‘è2\,Ïu$ ≈™ÃJ•Äf•¯ΩœS]Îu´£Zÿ®bq •kuFh£]•]1s≤éËvè¸âÂ9√Ú›Œao+©˛*óTÿÑÔF’ºøÇöÍ©.VöØD©ÃoGA¿æ∫4ze∑(Æx…õÌ|¥ÃJ{YËàŸÀ∫Róﬁ>ÁòÖTÇ+[SµòP•∫ìz%8í€‘¿Ø5@ù!ß,ï;9±≥yR›ÙåÅµÔ˚ñ;∂ﬁ∫°AcßÌïWD*≠π¬RÂ¬8æ5WÔ0ıÛ.´óôLZƒ•Ç»˚bi°y{°XÅI”l[j™æê+€cm8K≠lÈ»£MD\›HDõG™•ïxDÀ-Â∫§«ÃÅ∏±u¥UçÚóú6ÕJ*Ò®Íé£ƒ€yÀÈô—íZa^nÀ’v†˙’¬–^·†Æ±π6Œ}œLo;≤¡ªÿjO''Ë€°ép“WªÄhf^‹+ìE¶TROÖj4vhs¸2≠Lu”“™y“X"ä∫ìö	SZ¯Tñ∂ü'Fb˜pÒ¬-Í€øÇb⁄†rr/˜j)e5]mÉm˛2œÿû™ >˙JG<ï™(So”.[≈∂sπˆVä∞–’ª≥mAØ5Y‚è^K≤ÍœÃ∞/ÈGUk±&é∑¨…Ò∞%¥Xßc Ÿ√'@!Äí(5¢å√§èhØ>Å5	ÀìG“ˆßU+*ñßö‘é:œæ‘∑â¶⁄XI≥£B Å.sáÿN≤û[çXh®ƒ—Ê©ÜìJJß\˙æs⁄tµ:hy™ËF7x†vAuñ*Wg©n›(c—˜	_Æj—Ò'†zyêa1D’¡´ñoØ¨¨ﬁ—‡U@∫√d∆≥ÄÚ|ã,	˝·2¨m&¢JGyl#<ñD¬Õ⁄TfJÀ51%¸PÜ1i7†∑’Ÿâ™—]Ìä⁄K¢\À˛U.!ıı>≤œS»‹m7p˙Äˇå±r≠Ê¡$rGhi0byÔ	RrÔ˚GÆ«¨?¸ö+§(í(\«9æÈ<∞»·¯Î§âÿ6¡⁄∑8»∂·ÔQy>√BòpQ`[9,vJxíü2Ëº9\XNÛ`X∏yx˙∑^A¯÷‰r.èFæw
‹#Á¬Õ∆»'è„†≈ˆ¸	Eå≈Îsc¿¿AìåˆÃ-\ÀK’≥§‚AJÜB·Ì™@’≤h*#6ä7ùË‘q≤$Ω≠k≥T°¢ä„ya˚‹¢4Í°::bl±‰ª:î2
ërrN∆tﬂ,ôeéÉóU¡ﬂ®ÇQÆâπp˛ıãÙå,¸√.Ü¯Ä4W%e¨Bô;ÿÜQF&oV:i˙AWΩD©:P)§Èöaâ»â:™7ˆ"≠ ë :‡á*^]sW´ÿˇÀ∏!oÎ#)©èN
/Y¿ïîo¯#E˝â¬;˝;˛˘mdI•¿`Ò@-≤nr»…‚7\Y¬Ωªéb†˙PÖfD\Èe∫%|M~≈¢Ú…ºiJÿX%ãß¥‰^È˝¸r|‚e#n±B=·ôâÔ5©¯Æê’∫òj¬ÖDÁÏ•´M83™ï@WeÚ-L¯Ut[	„óÜUé’Ñ€
T*QU0ïƒÇú˚âìëõøúÏ(Ü3‘ÑãÍß¿7¡?`wC\g.;≥¨&î	¥˚lÚ|ˆÈÿÌõæCq+à≥Xgäı≈liyπ„,ÜÅ8®ä& „îàÿı,hπ:CûÑâxùÙ@µÏDITG«ßeÜõ.-5p£]R´¡m«∫éî|®õ¡4<iCÅºÿõ∫«?Æ∏≈©l:§ê)®$çB{foÉ#˘†∞YìÈ≠qWR‡∏V€®TÓw$ÖïÅ*óØ“*8”»‘‡›x∏éy$%i:ÀÔ±
AíZ>RHgπ‰z4$$ÀpbÅÙè’¸¯—˛@§ê˝/Äç•¯ÕøJ™=|ù|†∑ËÙ9ho/„≤ìüaUÓ
¸sR´ÚúˆÉDÕSVÓÄ*d˝≤ΩXe†≈;®¢a%Øv˝‡i{~IËö!/2sä-ì%9AM*1ÉÊLUﬂ¢æõı0âÄT5—ûy«P‹äpœº”>}ºgπâTﬁπ`Ê‰‚/	˘Yñﬂ!Æ‹Z†°˘'üï±°˘ì~Y‡–≤±T®Ùı*†G'¯¡“√R}Q‚^õ~4˚âSßpˇ∑j#¬ΩZÖv™)‰Ù`‚åÁ£/PIb¶/¨»Ïz|äôZ)2È)≠|~%è:Rµµû·˚üx`>K¢µCâ%+cm≈Zàπï)ÎÚ)æ„2Ïƒ˚3VQb;‡K¨¨‡ìb±ıq7:›#E^JCîb0˙∫ïaLèøÄÍ*céñWóå£¡ÅãLì,”•œ´£"*—‘ø˝À˜ì§êë˛ÈÜîMÃ≥ÄNÔ	Ñcùÿ°µú „⁄„|Ñ∫…pNa[G∂gèë0ƒ◊9ˆ4ráSœzÊ:≈ ]H^%∞›ÆÿnÓXb¡ 8¿%«U usÁùµcÍ™µ?+P›ákPu]D]j?YÎ≈P◊ªÂx∫¿:·ËÊæ3ûVÜ”%U=ÇA≠º®Ωö·y”‚™k U;.ÉóéÉÉW:ñúÄMˆàˆr·~}q‰ˇ∂ÿ ñÍRxAIÉW’7-`èé!/≥hÁà≈õ©hGwÚ´ãºÀÓ+∆Mµ¥zgÉ[Û˚j Ô2S_• ﬂ`w3lï¡4®§ﬁ™¬pIƒrÃL <Ù› Ú•™úƒbÃpDY7≠]‘ı–äN=”öÏ;´HV
ô‘ »u òÈ"b ŸY=¨¸‡6”"?ﬂÑdb+#Wá‘PÕPƒQGîÔ◊ÅeäEq£Ú}—MÇÁòúIìF∞5"F’Åe÷ó‹NøzXnä8∫PZ∞9@Wau√ÇÚT-L(OÏ¡X;:µb,zèv6ÓK ˜™P!"iquewy5ë¥|{mÂ∂f˙4K∞bÍÌeggicm#€KRz¡¨ó˙wi#w‘°Ñ≠ôôÆáïßÏÈÜ|	r¬∂õ»~øÔ>szœjÇ	T S&‡À ∑	H5.Õ∫ZV`dÈí◊¿≈≥K™D4…[h	‹_z‹¬%#ˆU0‰Å¥pÉ*„•UCzNö°¢aÍuc‰kM∏"-'‰“w
âEWì∞í*xﬁ~WJ¯…S∆…Ãòo5ÄJ˘PaËpr¯◊ £‘¯œè˛œo`ØjbÑU∆	2≠Õ”h∆xFëƒÅfÖî,7|¯Q\sNòAB ¨B6{W√üR˝êD&8BkÍôÛz+¬ ZHKö≤t)(93êO,ô Á{ﬂÔ?≠; ÚN˘Ÿ¥àJœΩªÊmõ£S•ë(Ô_úQ‰ppÆ.¢¡Ù(1‚Á∂tjæ)ë¿ôìç©.ÙÆÆ›b†ï‰Ó ó≤UÁöﬂ[Ï,Xﬂ[Ï∂€ÌñıöuÄ∑öﬂ•õJÖŸ2çå	WÃô)ÚÈåLç
fÏßV l-ëôÉ≤Úãﬁ—’ÑLsSÂ•¸j3åƒπBçÔ›»«T∞pL∆gßª≥∂ª8+úWfrÈµÆ™U§√Lñn¸(¥p†âµL≈ì’Ñx84·¡Õ>2öëΩî‚Â©˜'{úV¡¯Ç3X}=ñ…p1RCÚ≠IXñZ ´àƒY±˙ÖÄ*©¯∆J_÷6∏Ôà’**./?(V{*®ŒE76'N√º˘íÙ§π©ø⁄∑l˛d‡üéaAõ≠ÁùâìqwÃÓ0KºÌÜ#79?ô:8|≤∞ÈpÄbùªéù@A0Ê∂ŸôÿãúQe√S-ÛÚCú„Ä~˚i>ÇõÁﬁ,‰îF≈…H!¢pp>±ëÀ°RÔæ_È,ñ∑|´Ór√çõ‘3ö)ô=oÜãú8Yæ)Ê‚_çÖº3p#Ω5º∫∏±≤ªÒ]√tŸˆ|&ÈØ}ÅóÌoI¬ŸbñÕ]…ƒ ⁄]F‘öˇzﬁv<'%tVt‹ŸÔ˙ä&CÛ*≠ÈÀˆõñ_Qz	±˚¿9÷¡ÿ;C&æòÓÕ>“S£¿&˚Ì»Ò¸”ÚQ“rï	+úZ 5∫ÀcßKõQUF_ùR}jıï≥é£€* 
<z•Í¯5n<O2Ñ^¥≠™òÑ¯æ0¨ÇÆÈ˚3≠ÌWO®Ç¨J É~©–e^^I±Çﬁ™vóÊW2P›≈]ø7˚µ¶≤*D⁄ â≤^Öh#€€÷√˝çÌÔˆ¨˚€˚÷ˆﬁ∆˛¡[VÛp:ô¯àiçè≥ê¸≠U_/`ŸqìÒY∆Ñ~<@œ^íÂEq‡áizk1¨4p&~Ë‚™3ÎÃáÏEâß«í!p}=˛¯–~ÊN˚}í≈Y‰≠“Ê
W˚„$57N>Ã	•tÅ¥f±@v∂˜z¢í¯ ≠˛â”ZÛö»Ya◊…l≤ãT£ºçF.ø~=π
⁄"W…‹˚˚ˆtåﬁb∞3¢ﬂ„x¢±Á€HjçÏßÜX
øÉ¨Á∆∑˛Î|˝æÖç˝v?[ë@|… Ùø◊ü0~Ry~æ¡≈ßÛW¯æó¯ﬂ/pÀü‡üˇ§ˆ|å$c∆»!Ô3e†Cﬂ`B¯	iª0R8Ù;∫`E‚·ŸﬂyVÔﬁ?Ô<x´’Üœ—§
V6LØà˜ÒÚTîx^æ@¶∑(Ñî∏PñCp_ÖgHñsµëI€+·ŸaJüâ@ŸﬁŸﬂÈÌ§cÎ‡¡Óﬁ£˚ΩΩÉsg±±+b8œ?√:»yÊÅØ”a¯ÜWÅyß‰øe]ìπ}m≈8E∆€;q9hopM¢MÕ=ç<î∑±›!‹Çı’ù‚∏®ﬁ∂É1R≠å|Tí`≈nIŸÄ*Ö|ÄTNm®óêHÖÛœﬁ,A‹…˙¡÷PÜ4ÂLkÏ≥‰∂@{ü†o}C÷¸7	öÓW¯->Lˆœó∞˛YØ1 ÷Í?6§[ãÓ`ÆªƒÛœì≥-48Ü]|-¥JÜn0äc DÔßQ˛Ejƒ§íØ=¿˚ËqË…©Âá%t*†ÿAy∆íê°Êf¨'ä?S¿ˇL‡ñ…èyxxÔ‡QO»)U≠•#£NkÓdâÓÑ?3BÂßÏHCe)m”·›%Îô‚Îî5“îD≥‡gœÃø¯–LI#˙Tf˙Ûÿ6¿_êg¨4uÂ≈9¬vK8r|6Œªû-Ä\¢÷"≈©î7´4hÁÒ·∆Ê˛vï‰Ìa™–∆ôïˇÒ√á¿™?˛[xï Úœµ·t,∑xrVŒ›‰»™ŸºIÜ8eÏw-¿ì≤ÂlúD´∫k5[Ä8ÛxLÌ§åÌ¬˛|ç.?P™cåÊkﬂGz—˘ÿÓOÉ Õ%sYÄvZEá}GÔRÃØ≠Ã1W≈wxÓ‰»áC1{l„ÕDûê˚>yTrß¬êH∫º)xùJ⁄Ò–{ˆëâ;K≥çΩYHÍÉöªP"(~~´e-∆’cweºkË8d˙ 5Ùq‡ïÙ&ﬂìÙæ7ÔZiÖch"&«p•◊br[°¡âÜßàwi6ò‹&lâ∫G^IGH8:úhœåêL!ïúr√ô¨¸Îıó≈Œ∆Ëœ—î7;1àäfSÙÆBs∆¨–kíΩ3i6i◊{»ÊBkm1⁄1•Ìgê˚Hàù¿¶‘ò]‡Åˆ4ÚÈh7GX∂π£“ÂM¸âx6F£≈ﬂë=∑8¿JB¡Õ§B˚»ÕÔP∫√÷„«¥ëÜû;OO6MrMË ôM9J6BrQt‚Ÿk2~\ˇõ© ^˛Ó'>6äŸæµ—0@Ñf„$ä&wo›B
öÿÏ◊a˙}8Ò\§<‹B‚˚@Bú/±…õ÷–iË¡¸O∫K ∏6o¿O-öÍ3A¯)€ã‹®ß¸y›ZÃ¸&vÆ¸ÓgV}>%⁄.AK˛÷Òˇ»˙9~¶€À_‹ƒ?~Cº£‡∫ö{3∞Û§∆ﬂvë}ùå<.»Œ˝O@aZì∆íÍÌçˆhi⁄‡lÁ-	ñO≠ì£-3‚ÿ-Ÿ1Óh8±–8sê™ÎUfd9ß*ÃvTπEıó÷Ò)¬W1K\bíÓ}€∑0ƒø«K:© Ùq˛Ø¯Á®ŒëÅœlêÿv•]¿˙óÛn<é˚.˘*5&RÉ»≈g€§âá…WMƒ]ëºä â4∂Ÿp‘=K#=a˜BÄTí<∆+¥P•r!Rnf%dW
%dUBc≤·/$ ®x—i`ObÎåD0≥^Ùçø{&dT÷Q	 Id™äËô("¸X*R6Ô9ˆÄ”iÄiÆ´^§*"e]i•*i§ÿçºÈDßé√/D-ÛH‘Î[RÜS∫PâÉ£ÃÉ)RY°‡‡≤B¡AÖP-àñSSç˙“À¿*¬åe‰-YUt±Nù©s∫}Ìà∏*»bf•€X*9öQjß¸*” $†Úî
∞Ñ•+,"ıêA&)uôî’ØeVƒfEÂZJ=7[bæn+ŸU≥(_[F3V‹Á≈≤°∏ﬁÂgÙê˛oH§
ûéØòY}˚o÷§sÜîoì	“´h	ƒNSß¨¶%PΩ%ﬂÄ∏•çŸ†‡KIÕ;Ù£§Ê]ERxÍ¨û√S7Íü@`|ºÄûÖiƒ!K5*Y“¡”		óÊk.àK˘î¥Ju[±ë≠ %ı»®ÙBZq∏¸⁄L±˘¨^•zÅ§Ü %6Zº»ß
XÑl¿xÈº»,HŒ.xT∂˘î∞ndhLÓÄ Î©(ÒD,o´F¯/Â˙ú ∫©nyq†äzbÃÀVì zDKîT›™Aÿ◊QöH[i»®öb\2ÙD˚™4ËzeÏÿªﬁfS)äÃ©j§ìÃó†§é<@»\πxe%DÁÚ$DÁo‚ª"!ˆ›±"é“Ã$DÁo¢ú®Ñ 3Pﬂ∆ìå˙ïù+ ‘æÂ€má# UﬂòF˛M8õ∑ÜÅè$;çÃâ„Ít∏RÅT¥Ëc#+Ô…Ω-Û‰ññXùeH_\ìKÚ√$Î2Á{˝R^∑7Qèx&SZ!#∫N0ÕZN>Äf&ÔSmÍ;#î∑h’ÿP’iZèp6tÒã›˚je«jê∑
óÅÙÂ≠<Ü ì)ò‡≈xTs£≥ëæf¢yf≤H¥QÜÜ∫∆≈VÛQØÌc'˙/   ˇˇÏ]Õn€FæÁ)6F`»Ä¡‘±õ6ä6˛Icƒä[I{Xâ≤	–íAJnç†áhÉ¢=“K[‰êV=•r(íS_%è–GËŒ˛êKjπ‹]íäùjÜ%ëKÓœÏÓÃ|Ûÿ•è2lr$ñ¨¿±É◊úAdÈ@L°Ôxç5Â≥ƒª‡Ÿ˜3„éõ¸c / }Ôbá\Ò-ÏS’KCK¨Å”Zr‰—$ )»Û]∞ßsNÍÃG’Ô”«?P—#—ì7ïÎ]Ù›ÿfáR◊ÒY#∞5sGÁÊzo±∏ÉÕ÷÷ˆ>∫’⁄kVPª‹vïüé\1Z<~Fã ±’ÎÂ	Kπc˘‘sq8 3éäëÿËèädí' N¯ÕÚ~W—m ”6<Á¿A¿ytπ'ﬁµU8ˆ/-“ÜÄÌ!ûadGÁèî‘%eRªæˆ¡⁄á≈‰d:‹hÉ®∫ xyÎé£—àå7[‘
ïZπ∑8Äá·_’óó`Æ ãöö°lpDπ©hHt…¿ÎÎ˜o&ø2˘`ÒÁ°∆=V≠µ¥¢Ænç›p‰]øv„˝∫g€£·`. W x¥Ñ© Q6@Ò–ØbHtr‘l¥YÕ÷20pÔ6?uoDwÍû˛wº”zÊ˛(t˝@x∏ñπ`
œÚp*ÙÎ#t9˚’◊5AIƒOÍQY¨"˘7st⁄Í˜e†≈‰Çí†≈ÖŒ ãt‹Ùxyê≠
Jåu~4v‚1–æÀZ+Ì˙˚íkùª√3ﬂ∑ìÎß.+ZnÁu∫‘¥Pã—`∆´u6îÃj…Œ#Ë‡¥9Ñ◊é1q<0yûıBﬁ<Öi‘π√Çˆl≠{Uß=<?”\XQa·ª¶B¬£~Ìƒ„%≈;„øx∂˚ô@/ïËæç7ìüÄ≠~˝ÖFèïRw!∫—˚ =: À…—UhÑs<8®[Hv 4a.$Á@Hl\ê⁄QPPå·≠®˝6EáM∞≈A¯‚""Kéß[,uy®›é¿íp3Ê ˙[~‰RáíÁÛ@ŸeîkG¿jRÈXd3?#∑<ÀxKöˆ°Ñﬁh>—<^Ù»„e,	TP5˝a"äñAÉ!_Rûà’º∞hU0{ÀìM0’››¶ä∆w%cü¡k5˛–ƒˇ·5}	•Ë‡∆Ó »“ÓÄeÚÓÀ‰Òô·§ÉGYzç¥J·≈MÛ/A√<º'@zªÊÍòÓˆBkø˚Aè6ó˛lﬂ^˛<n%î0Nà>√Ùot@ã∏¥Ô}ŸÜFäÏEÊ)L|bÁ…rö¯îlÊ8x Ù‡ √˝”ÔUú2®„yMºœ¯xﬂ6e§á¬)%ãÌ≥≤2&cõmÛjíC!˚èçç3¬KıNé¸¥˚◊&·I/u|>’Ò0 ™ì*ôW ∫Ö@<Ãá&1û…2Æ EÒÈ•HÊ≈LÇÖµ|J)‚F"Zf’)π>R~Í‡J9ﬂ +¬"°≠U”É(ygU%EafpØ1lA›uø˝Hœ01ëÔ˛(jaÙ‰Û«:∫"luïtDeyoä≈¿ öf˝eŸn∫∏ﬂS(´¿y≠ñƒ†9gU#Å‹qHÔX”Dî‘‚†Ã¢|	ùì	KíG›C–$å7lá√ÉOËùAœÔ∫x©jtßﬂV¡qLëÌÀ("∞ZN˘¢ü¥∏*@;nLyöÅ†ºNL&>rga:	Z 
£EIb!Òr*{H¡ T‘ŸÑ’b…ÎÏ«î˜ºé~V{<‘∫ 0‚˚!ñmÛÄ.Á-/◊L-Ÿ˛æIh &Øc›ûù¶Ãt∞_¬=ÑL ãCùP∫¸JÕÈ©–<PR?/©õó‘Àg•öæ%ï¥º*:S‘Nı,©r÷™jŒD≈¥V-≠UJkU“FÖ‘ì∞äT∆˙ÕÌPC,ø®õÍ3©¥Vr~uíÍHÎÌıS
˛“:é ~¡›/h:˘s%7_	/†∆UG}sÑ7ª0‰+ÎH [ØEnˇú9∂§"l	¡ûol∏§’Ô˚]|™ò„J˛/.ÛíbbÅ-I”EW*"ôßJ0IúUò2c◊*ÑMb.V8ﬁ6ËjÆö-Á@˝⁄T “ä∫eÿ›YZ˜£{«ÔîüûÁüù<´T^n⁄¿≠\‹ÇC¥à>ÜAØ–Ó|“¶K¡§’±uõÿπ’ÜB;
«´X¯jîYHMÕÖb◊ô÷ÓXÒÈUœê|~lp4À«ÃP30œôx@é(}eûç„ÕË1n2D¨œ	^˙Í;ÚÛﬂ≈Ò§\ßî
∂ö	ëìÅU≤‰ŒV!¡®ÌëﬁrÌqÔﬁq0t{ıy“´Ä+∞$ù #ÍSpk˝LŒ´JÀ®>cÜ¸S≈i>;€üw–≠ùÌ›-¥Ÿ⁄mÌÌ£€€ªÌÌΩÍìxJéÑ∏∑¶ŒÌIv!zinàÔˆz‘Ëûè"◊éW3˛πe±ÊÕ¬¸Eô uo`ówH^◊úl	q•9W]Z∫Ù   ˇˇ Ú@Å