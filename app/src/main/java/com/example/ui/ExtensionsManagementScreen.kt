package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.MediaRepository
import com.example.model.CloudStreamRepo
import com.example.model.MovieProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsManagementScreen(
    repository: MediaRepository,
    onBack: () -> Unit,
    onOpenMovieBrowser: ((MovieProvider) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var repos by remember { mutableStateOf(repository.getSavedCloudStreamRepos()) }
    var allProviders by remember { mutableStateOf(repository.getAllMovieProviders()) }
    
    // Navigation State: null = Screen 1 (Repo list), non-null = Screen 2 (Repo plugins)
    var selectedRepoForPlugins by remember { mutableStateOf<CloudStreamRepo?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var selectedPluginCategory by remember { mutableStateOf("All") }
    var installingProviderId by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var newRepoUrl by remember { mutableStateOf("https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/refs/heads/builds/repo.json") }
    var newRepoName by remember { mutableStateOf("Phisher Repo") }
    var isSyncing by remember { mutableStateOf(false) }

    fun refreshExtensions() {
        repos = repository.getSavedCloudStreamRepos()
        allProviders = repository.getAllMovieProviders()
    }

    val pluginCategories = listOf("All", "Movies", "TV Series", "Anime", "Asian Dramas")

    // Stats calculations for bottom progress bar
    val totalCount = allProviders.size.coerceAtLeast(1)
    val downloadedCount = allProviders.count { it.isInstalled }
    val disabledCount = allProviders.count { it.isInstalled && !it.isEnabled }
    val notDownloadedCount = allProviders.count { !it.isInstalled }

    val downloadedRatio = (downloadedCount - disabledCount).toFloat() / totalCount.toFloat()
    val disabledRatio = disabledCount.toFloat() / totalCount.toFloat()

    // Back handler: if in Screen 2, go back to Screen 1; else onBack()
    BackHandler {
        if (selectedRepoForPlugins != null) {
            selectedRepoForPlugins = null
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchOpen) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    if (selectedRepoForPlugins != null) "Search plugins (${selectedRepoForPlugins!!.name})..." else "Search repositories or extensions...",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    } else {
                        Text(
                            text = selectedRepoForPlugins?.name ?: "Extensions",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedRepoForPlugins != null) {
                            selectedRepoForPlugins = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchOpen = !isSearchOpen }) {
                        Icon(
                            imageVector = if (isSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                    if (selectedRepoForPlugins != null) {
                        IconButton(onClick = {
                            isSyncing = true
                            coroutineScope.launch {
                                delay(600)
                                refreshExtensions()
                                isSyncing = false
                                Toast.makeText(context, "Plugins updated!", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color(0xFF38BDF8), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Color.White)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF020617))
            )
        },
        floatingActionButton = {
            if (selectedRepoForPlugins == null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddRepoDialog = true },
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add repository", fontWeight = FontWeight.Bold) }
                )
            }
        },
        containerColor = Color(0xFF020617)
    ) { paddingValues ->
        if (selectedRepoForPlugins == null) {
            // ==========================================
            // SCREEN 1: REPOSITORIES OVERVIEW (Screenshot 1)
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // List of Repositories
                items(repos, key = { it.id }) { repo ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clickable {
                                selectedRepoForPlugins = repo
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderZip,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = repo.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = repo.url,
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "View Plugins",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Preset Repository Suggestions (Phisher, MegaRepo, Bangla Hub)
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Popular Repositories",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                item {
                    val presets = listOf(
                        Triple("Phisher Repo", "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/refs/heads/builds/repo.json", "Multi-Language, Bangla, Hindi & Anime Extensions"),
                        Triple("Hexated Streams Repo", "https://raw.githubusercontent.com/Hexated/cloudstream-extensions-hexated/builds/repo.json", "Ultra Fast Hollywood & 4K Multi-Server Cinema"),
                        Triple("Bangla & Bollywood Hub", "https://raw.githubusercontent.com/cloudstream-bangla/repo/main/repo.json", "Chorki, Bioscope, Bongo & Indian HD Media")
                    )

                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { (name, url, desc) ->
                            val isAlreadyAdded = repos.any { it.url == url }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(desc, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    }
                                    if (isAlreadyAdded) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Added",
                                                color = Color(0xFF10B981),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val res = repository.installExtensionFromUrl(url)
                                                    refreshExtensions()
                                                    Toast.makeText(context, res.second, Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Install", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // BOTTOM STATISTICS BAR (Screenshot 1)
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0B1120),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Extensions",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Colored Multi-Segment Progress Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF334155))
                            ) {
                                if (downloadedRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(downloadedRatio.coerceAtLeast(0.01f))
                                            .background(Color(0xFF10B981))
                                    )
                                }
                                if (disabledRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(disabledRatio.coerceAtLeast(0.01f))
                                            .background(Color(0xFFEF4444))
                                    )
                                }
                                val notDownloadedRatio = 1f - (downloadedRatio + disabledRatio)
                                if (notDownloadedRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(notDownloadedRatio.coerceAtLeast(0.01f))
                                            .background(Color(0xFF475569))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Stats labels: Downloaded, Disabled, Not downloaded
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Downloaded: $downloadedCount", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("Disabled: $disabledCount", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("Not downloaded: $notDownloadedCount", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // SCREEN 2: REPOSITORY EXTENSION LIST (Screenshot 2)
            // ==========================================
            val repo = selectedRepoForPlugins!!

            val plugins = allProviders.filter { prov ->
                val matchesSearch = searchQuery.isBlank() ||
                        prov.name.contains(searchQuery, ignoreCase = true) ||
                        (prov.description != null && prov.description.contains(searchQuery, ignoreCase = true)) ||
                        prov.language.contains(searchQuery, ignoreCase = true)

                val matchesCat = when (selectedPluginCategory) {
                    "All" -> true
                    "Movies" -> prov.types.contains("Movie") || prov.category.contains("Movie", ignoreCase = true)
                    "TV Series" -> prov.types.contains("Series") || prov.types.contains("Tv") || prov.category.contains("Series", ignoreCase = true)
                    "Anime" -> prov.types.contains("Anime") || prov.name.contains("Anime", ignoreCase = true) || prov.category.contains("Anime", ignoreCase = true)
                    "Asian Dramas" -> prov.types.contains("KDrama") || prov.name.contains("Kisskh", ignoreCase = true) || prov.name.contains("Loklok", ignoreCase = true)
                    else -> true
                }

                matchesSearch && matchesCat
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // Filter Tabs: All, Movies, TV Series, Anime, Asian Dramas (Screenshot 2)
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pluginCategories) { cat ->
                            val isSelected = selectedPluginCategory == cat
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                                modifier = Modifier.clickable { selectedPluginCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Providers / Plugins items
                items(plugins, key = { it.id }) { provider ->
                    val isInstalling = installingProviderId == provider.id

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clickable {
                                onOpenMovieBrowser?.invoke(provider)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Provider Icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!provider.iconUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = provider.iconUrl,
                                        contentDescription = provider.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = when {
                                            provider.types.contains("Anime") -> Icons.Rounded.AutoAwesome
                                            else -> Icons.Rounded.Movie
                                        },
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Provider Title, Details and Description
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = provider.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${provider.language} • ${provider.version ?: "v26"} • ${provider.size ?: "72 kB"}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = provider.description ?: "Multi Language Movies and Series Provider (Mostly Hindi)",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Action button on right (Download Icon vs Checkmark/Trash)
                            if (isInstalling) {
                                CircularProgressIndicator(
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (provider.isInstalled) {
                                // Installed: Show Trash to uninstall or Checkmark
                                IconButton(
                                    onClick = {
                                        val updated = provider.copy(isInstalled = false, isEnabled = false)
                                        val currentList = repository.getAllMovieProviders().toMutableList()
                                        val idx = currentList.indexOfFirst { it.id == provider.id }
                                        if (idx >= 0) {
                                            currentList[idx] = updated
                                            repository.saveMovieProviders(currentList)
                                            refreshExtensions()
                                            Toast.makeText(context, "${provider.name} uninstalled", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Uninstall",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            } else {
                                // Not Downloaded: Show Download Icon (📥)
                                IconButton(
                                    onClick = {
                                        installingProviderId = provider.id
                                        coroutineScope.launch {
                                            delay(500)
                                            val updated = provider.copy(isInstalled = true, isEnabled = true)
                                            val currentList = repository.getAllMovieProviders().toMutableList()
                                            val idx = currentList.indexOfFirst { it.id == provider.id }
                                            if (idx >= 0) {
                                                currentList[idx] = updated
                                                repository.saveMovieProviders(currentList)
                                                refreshExtensions()
                                            }
                                            installingProviderId = null
                                            Toast.makeText(context, "${provider.name} installed successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FileDownload,
                                        contentDescription = "Download Extension",
                                        tint = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Custom Repo URL
    if (showAddRepoDialog) {
        AlertDialog(
            onDismissRequest = { showAddRepoDialog = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(18.dp),
            title = {
                Text("Add repository", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the URL of the repository JSON:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = newRepoName,
                        onValueChange = { newRepoName = it },
                        label = { Text("Repository Name", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRepoUrl,
                        onValueChange = { newRepoUrl = it },
                        label = { Text("repo.json URL", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRepoUrl.isNotBlank()) {
                            coroutineScope.launch {
                                val result = repository.installExtensionFromUrl(newRepoUrl.trim())
                                refreshExtensions()
                                showAddRepoDialog = false
                                Toast.makeText(context, result.second, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Add Repository", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRepoDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
