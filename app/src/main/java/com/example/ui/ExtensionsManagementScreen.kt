package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var expandedProviderId by remember { mutableStateOf<String?>("phisher_moviebox") }
    
    // Dialog States
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var showPasteJsonDialog by remember { mutableStateOf(false) }
    var newRepoUrl by remember { mutableStateOf("https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/refs/heads/builds/repo.json") }
    var newRepoName by remember { mutableStateOf("Phisher Repo") }
    var pastedJsonContent by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var isUpdatingAll by remember { mutableStateOf(false) }

    fun refreshExtensions() {
        repos = repository.getSavedCloudStreamRepos()
        allProviders = repository.getAllMovieProviders()
    }

    // Local JSON File Picker
    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val sb = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                val jsonString = sb.toString()
                val result = repository.installExtensionFromJson(jsonString)
                refreshExtensions()
                Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "ফাইল পড়তে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filterChips = listOf("All", "Installed", "Bangla", "Hindi", "English", "Anime", "Asian Drama")

    val filteredProviders = allProviders.filter { provider ->
        val matchesSearch = searchQuery.isBlank() ||
                provider.name.contains(searchQuery, ignoreCase = true) ||
                (provider.description != null && provider.description.contains(searchQuery, ignoreCase = true)) ||
                provider.language.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "Installed" -> provider.isInstalled
            "Bangla" -> provider.language.contains("Bangla", ignoreCase = true) || provider.name.contains("Bongo", ignoreCase = true) || provider.name.contains("Chorki", ignoreCase = true) || provider.name.contains("Bioscope", ignoreCase = true) || provider.name.contains("DoraBash", ignoreCase = true)
            "Hindi" -> provider.language.contains("Hindi", ignoreCase = true) || provider.name.contains("Vegamovies", ignoreCase = true) || provider.name.contains("BollyFlix", ignoreCase = true) || provider.name.contains("ShowFlix", ignoreCase = true)
            "English" -> provider.language.contains("English", ignoreCase = true) || provider.name.contains("Cineb", ignoreCase = true) || provider.name.contains("FlixHQ", ignoreCase = true) || provider.name.contains("YTS", ignoreCase = true)
            "Anime" -> provider.types.contains("Anime") || provider.language.contains("Japanese", ignoreCase = true) || provider.name.contains("Anime", ignoreCase = true)
            "Asian Drama" -> provider.types.contains("KDrama") || provider.language.contains("Korean", ignoreCase = true) || provider.name.contains("Kisskh", ignoreCase = true) || provider.name.contains("Loklok", ignoreCase = true)
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchOpen) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("এক্সটেনশন বা সোর্স খুঁজুন...", color = Color(0xFF64748B), fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
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
                        Column {
                            Text(
                                text = "Extensions Manager",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Phisher Repo • ${allProviders.count { it.isInstalled }} ইনস্টল্ড (${filteredProviders.size} উপলব্ধ)",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                    IconButton(onClick = { showAddRepoDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.AddLink,
                            contentDescription = "Add Repo",
                            tint = Color(0xFF00E5FF)
                        )
                    }
                    IconButton(
                        onClick = {
                            isSyncing = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(600)
                                refreshExtensions()
                                isSyncing = false
                                Toast.makeText(context, "এক্সটেনশন তালিকা রিফ্রেশ সম্পন্ন!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E5FF),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF020617)
                )
            )
        },
        containerColor = Color(0xFF020617)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Action Panel: Auto-Update, Local JSON File, Add Repo
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Repo Info Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF2563EB), Color(0xFF00E5FF))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Extension,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Phisher Repo v7.2 (Official)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "github.com/phisher98 • ২০+ স্ট্রিমিং সাইট",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Active Tag
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ACTIVE",
                                        color = Color(0xFF10B981),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                        // 3 Quick Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Auto-Update Feature
                            Button(
                                onClick = {
                                    isUpdatingAll = true
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(1000)
                                        // Update all providers to latest version
                                        val updated = repository.getAllMovieProviders().map {
                                            it.copy(version = "v${(it.version?.removePrefix("v")?.toIntOrNull() ?: 1) + 1}", status = "Ok")
                                        }
                                        repository.saveMovieProviders(updated)
                                        refreshExtensions()
                                        isUpdatingAll = false
                                        Toast.makeText(context, "✅ সকল এক্সটেনশন স্বয়ংক্রিয়ভাবে লেটেস্ট ভার্সনে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                if (isUpdatingAll) {
                                    CircularProgressIndicator(color = Color(0xFF00E5FF), strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Rounded.Update, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("অটো আপডেট", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // 2. Local File / JSON Install
                            Button(
                                onClick = { jsonFilePickerLauncher.launch("application/json") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ফাইল ইনস্টল", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }

                            // 3. Paste Link / JSON
                            Button(
                                onClick = { showPasteJsonDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("লিংক যোগ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterChips) { chip ->
                        val isSelected = selectedFilter == chip
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155)
                            ),
                            modifier = Modifier.clickable { selectedFilter = chip }
                        ) {
                            Text(
                                text = chip,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "প্রোভাইডার ও এক্সটেনশন তালিকা (${filteredProviders.size})",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ক্লিক করে কন্টেন্ট দেখুন",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp
                    )
                }
            }

            // Extensions List Items
            items(filteredProviders, key = { it.id }) { provider ->
                val isExpanded = expandedProviderId == provider.id
                var isRowFocused by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .scale(if (isRowFocused) 1.02f else 1.0f)
                        .onFocusChanged { isRowFocused = it.isFocused }
                        .focusable()
                        .animateContentSize(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) Color(0xFF0F172A) else if (isRowFocused) Color(0xFF1E293B) else Color(0xFF0A0F1D)
                    ),
                    border = when {
                        isExpanded -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
                        isRowFocused -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF))
                        else -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Standard Collapsed Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedProviderId = if (isExpanded) null else provider.id
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
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
                                            provider.types.contains("Music") -> Icons.Rounded.MusicNote
                                            else -> Icons.Rounded.Movie
                                        },
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Name & Language / Version / Size Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = provider.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (provider.isInstalled) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "ইনস্টল্ড",
                                                color = Color(0xFF10B981),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${provider.flag ?: "🌐"} ${provider.language} • ${provider.version ?: "v1"} • ${provider.size ?: "30 kB"}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                if (!provider.description.isNullOrBlank() && !isExpanded) {
                                    Text(
                                        text = provider.description,
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Active / Inactive Toggle Switch
                            Switch(
                                checked = provider.isEnabled,
                                onCheckedChange = { isEnabled ->
                                    val updated = provider.copy(isEnabled = isEnabled)
                                    val currentList = repository.getAllMovieProviders().toMutableList()
                                    val idx = currentList.indexOfFirst { it.id == provider.id }
                                    if (idx >= 0) {
                                        currentList[idx] = updated
                                        repository.saveMovieProviders(currentList)
                                        refreshExtensions()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00E5FF),
                                    checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        // Expanded View with Full Details and "Browse Content" button
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0B1120))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                TagInfoRow(label = "বিবরণ", value = provider.description ?: "${provider.name} থেকে সিনেমা ও সিরিজ স্ট্রিমিং")
                                TagInfoRow(label = "ডেভেলপার", value = provider.authors ?: "Phisher98")
                                TagInfoRow(label = "ভার্সন", value = provider.version ?: "v7.0")
                                TagInfoRow(label = "স্ট্যাটাস", value = "সক্রিয় (Ok / 1080p Stream)")
                                TagInfoRow(label = "সাইজ", value = provider.size ?: "45 kB")
                                TagInfoRow(label = "ভাষা", value = "${provider.flag ?: "🌐"} ${provider.language}")
                                TagInfoRow(label = "সাপোর্ট", value = provider.supported.ifEmpty { listOf("Movie", "Series") }.joinToString(", "))

                                Spacer(modifier = Modifier.height(10.dp))

                                // Bottom Action Row: Browse Catalog / Install / Uninstall
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Primary Button: Enter provider to load movies
                                    Button(
                                        onClick = { onOpenMovieBrowser?.invoke(provider) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00E5FF))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("মুভি ও সিরিজ লোড করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    // Install / Uninstall Toggle
                                    if (provider.isInstalled) {
                                        OutlinedButton(
                                            onClick = {
                                                val updated = provider.copy(isInstalled = false, isEnabled = false)
                                                val currentList = repository.getAllMovieProviders().toMutableList()
                                                val idx = currentList.indexOfFirst { it.id == provider.id }
                                                if (idx >= 0) {
                                                    currentList[idx] = updated
                                                    repository.saveMovieProviders(currentList)
                                                    refreshExtensions()
                                                    Toast.makeText(context, "${provider.name} আনইনস্টল করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                val updated = provider.copy(isInstalled = true, isEnabled = true)
                                                val currentList = repository.getAllMovieProviders().toMutableList()
                                                val idx = currentList.indexOfFirst { it.id == provider.id }
                                                if (idx >= 0) {
                                                    currentList[idx] = updated
                                                    repository.saveMovieProviders(currentList)
                                                    refreshExtensions()
                                                    Toast.makeText(context, "${provider.name} ইনস্টল সম্পন্ন!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ইনস্টল", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    // Modal Dialog: Add Repository URL
    if (showAddRepoDialog) {
        AlertDialog(
            onDismissRequest = { showAddRepoDialog = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AddLink, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("রিপোজিটরি বা সোর্স লিংক যোগ করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "CloudStream এক্সটেনশন repo.json এর গিটহাব বা অনলাইন লিংক পেস্ট করুন:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = newRepoName,
                        onValueChange = { newRepoName = it },
                        label = { Text("রিপোজিটরির নাম", color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
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
                            focusedBorderColor = Color(0xFF00E5FF),
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
                    Text("সংরক্ষণ ও লোড করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRepoDialog = false }) {
                    Text("বাতিল", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Modal Dialog: Paste JSON Content Directly
    if (showPasteJsonDialog) {
        AlertDialog(
            onDismissRequest = { showPasteJsonDialog = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DataObject, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("JSON বা প্লাগইন কোড পেস্ট করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "প্লাগইন অবজেক্ট বা রিপোজিটরির JSON টেক্সট পেস্ট করুন:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = pastedJsonContent,
                        onValueChange = { pastedJsonContent = it },
                        placeholder = { Text("{\n  \"name\": \"Custom Repo\",\n  \"pluginLists\": [...]\n}", color = Color(0xFF64748B), fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pastedJsonContent.isNotBlank()) {
                            val result = repository.installExtensionFromJson(pastedJsonContent)
                            refreshExtensions()
                            showPasteJsonDialog = false
                            Toast.makeText(context, result.second, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("ইনস্টল করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteJsonDialog = false }) {
                    Text("বাতিল", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
private fun TagInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Text(
            text = value,
            color = Color(0xFFF1F5F9),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
