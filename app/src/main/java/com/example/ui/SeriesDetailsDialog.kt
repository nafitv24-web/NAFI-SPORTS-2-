package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.model.EpisodeItem
import com.example.model.MediaItem
import com.example.model.SeasonInfo
import com.example.util.DownloadState
import com.example.util.MovieDownloadManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailsDialog(
    series: MediaItem,
    repository: MediaRepository,
    isTvMode: Boolean = false,
    onPlayEpisode: (MediaItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loadedSeries by remember { mutableStateOf(series) }
    var isLoadingDetails by remember { mutableStateOf(series.seasons.isEmpty() && series.episodes.isEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSeasonNum by remember { mutableIntStateOf(1) }
    var isOverviewExpanded by remember { mutableStateOf(false) }

    // Fetch complete season & episode list from Xtream API if not already populated
    LaunchedEffect(series.id) {
        if (series.seasons.isEmpty() && series.episodes.isEmpty()) {
            isLoadingDetails = true
            errorMessage = null
            try {
                val detailed = repository.fetchSeriesSeasonsAndEpisodes(series)
                loadedSeries = detailed
                if (detailed.seasons.isNotEmpty()) {
                    selectedSeasonNum = detailed.seasons.first().seasonNumber
                } else if (detailed.episodes.isNotEmpty()) {
                    selectedSeasonNum = detailed.episodes.first().seasonNum
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "সিজন ও পর্ব লোড করতে ব্যর্থ হয়েছে: ${e.localizedMessage ?: "সার্ভার রেসপন্স করছে না"}"
            } finally {
                isLoadingDetails = false
            }
        } else {
            if (series.seasons.isNotEmpty()) {
                selectedSeasonNum = series.seasons.first().seasonNumber
            }
        }
    }

    val availableSeasons: List<SeasonInfo> = remember(loadedSeries) {
        if (loadedSeries.seasons.isNotEmpty()) {
            loadedSeries.seasons
        } else if (loadedSeries.episodes.isNotEmpty()) {
            // Group episodes into synthetic seasons if seasons list was omitted
            val grouped = loadedSeries.episodes.groupBy { it.seasonNum }
            grouped.map { (sNum, eps) ->
                SeasonInfo(
                    seasonNumber = sNum,
                    name = "Season $sNum",
                    episodeCount = eps.size,
                    episodes = eps
                )
            }.sortedBy { it.seasonNumber }
        } else {
            emptyList()
        }
    }

    val episodesForCurrentSeason: List<EpisodeItem> = remember(loadedSeries, selectedSeasonNum, availableSeasons) {
        val seasonObj = availableSeasons.firstOrNull { it.seasonNumber == selectedSeasonNum }
        if (seasonObj != null && seasonObj.episodes.isNotEmpty()) {
            seasonObj.episodes
        } else {
            loadedSeries.episodes.filter { it.seasonNum == selectedSeasonNum }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B132B),
        scrimColor = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFF475569))
        },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Top Bar with Close button and Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = loadedSeries.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = loadedSeries.category.ifBlank { "ওয়েব সিরিজ" },
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Series Poster, Details, and Genres Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Series Poster
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 145.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    AsyncImage(
                        model = loadedSeries.logoUrl
                            ?: "https://images.unsplash.com/photo-1578022761797-b8636ac1773c?w=400&fit=crop",
                        contentDescription = loadedSeries.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Quality / Series tag
                    Surface(
                        color = Color(0xFFE11D48),
                        shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "SERIES",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Rating and Year Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!loadedSeries.rating.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = loadedSeries.rating ?: "8.5",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (!loadedSeries.year.isNullOrBlank()) {
                            Text(
                                text = loadedSeries.year ?: "",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (availableSeasons.isNotEmpty()) {
                            Text(
                                text = "•  ${availableSeasons.size}টি সিজন",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Genre / Cast
                    if (!loadedSeries.genre.isNullOrBlank()) {
                        Text(
                            text = "জনরা: ${loadedSeries.genre}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!loadedSeries.cast.isNullOrBlank()) {
                        Text(
                            text = "অভিনয়ে: ${loadedSeries.cast}",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Synopsis / Overview
                    val overview = loadedSeries.description?.takeIf { it.isNotBlank() }
                        ?: "রোমাঞ্চকর ও জনপ্রিয় ওয়েব সিরিজটি উপভোগ করুন সম্পূর্ণ এইচডি কোয়ালিটিতে।"

                    Text(
                        text = overview,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = if (isOverviewExpanded) 6 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable { isOverviewExpanded = !isOverviewExpanded }
                            .animateContentSize()
                    )
                }
            }

            Divider(color = Color(0xFF1E293B), thickness = 1.dp)

            // Season Selection Tabs / Chips
            if (availableSeasons.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    Text(
                        text = "সিজন নির্বাচন করুন (Seasons):",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(availableSeasons) { season ->
                            val isSelected = selectedSeasonNum == season.seasonNumber
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155)
                                ),
                                modifier = Modifier.clickable {
                                    selectedSeasonNum = season.seasonNumber
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Layers,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = season.name.ifBlank { "সিজন ${season.seasonNumber}" },
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (season.episodeCount > 0) {
                                        Text(
                                            text = "(${season.episodeCount})",
                                            color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFF64748B),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Episodes List Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoadingDetails) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "সিজন ও পর্বসমূহ লোড হচ্ছে...",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "সার্ভার থেকে পর্বের তালিকা আনা হচ্ছে, অপেক্ষা করুন...",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "ত্রুটি ঘটেছে",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoadingDetails = true
                                    errorMessage = null
                                    try {
                                        val detailed = repository.fetchSeriesSeasonsAndEpisodes(series)
                                        loadedSeries = detailed
                                        if (detailed.seasons.isNotEmpty()) {
                                            selectedSeasonNum = detailed.seasons.first().seasonNumber
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage
                                    } finally {
                                        isLoadingDetails = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("পুনরায় চেষ্টা করুন (Retry)")
                        }
                    }
                } else if (episodesForCurrentSeason.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MovieFilter,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "এই সিজনে কোনো পর্ব পাওয়া যায়নি",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(episodesForCurrentSeason, key = { _, ep -> ep.id }) { index, episode ->
                            EpisodeCardItem(
                                episode = episode,
                                parentSeries = loadedSeries,
                                onPlay = {
                                    val playableMedia = episode.toMediaItem(loadedSeries)
                                    onPlayEpisode(playableMedia)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeCardItem(
    episode: EpisodeItem,
    parentSeries: MediaItem,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    val activeDownloadsMap by MovieDownloadManager.downloadsState.collectAsState()
    val downloadedMovies by MovieDownloadManager.downloadedMoviesFlow.collectAsState()

    val epId = "${parentSeries.id}_s${episode.seasonNum}e${episode.episodeNum}"
    val dlProg = activeDownloadsMap[epId]
    val isDownloading = dlProg?.state == DownloadState.DOWNLOADING || dlProg?.state == DownloadState.PENDING
    val isDownloaded = downloadedMovies.any { it.id == epId && it.fileExists }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF141F38),
        border = BorderStroke(1.dp, Color(0xFF223456)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode Thumbnail / Number Box
            Box(
                modifier = Modifier
                    .size(width = 85.dp, height = 55.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
            ) {
                if (!episode.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = episode.logoUrl,
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF).copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Episode Title, Number & Duration
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "পর্ব ${episode.episodeNum}: ${episode.title.ifBlank { "Episode ${episode.episodeNum}" }}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!episode.duration.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = episode.duration ?: "",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    if (!episode.releaseDate.isNullOrBlank()) {
                        Text(
                            text = episode.releaseDate ?: "",
                            color = Color(0xFF64748B),
                            fontSize = 10.5.sp
                        )
                    }
                }

                if (!episode.overview.isNullOrBlank()) {
                    Text(
                        text = episode.overview ?: "",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action: Download Episode button
            IconButton(
                onClick = {
                    if (isDownloaded) {
                        Toast.makeText(context, "পর্বটি ইতিমধ্যে ডাউনলোড করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    } else if (isDownloading) {
                        Toast.makeText(context, "ডাউনলোড চলছে (${dlProg?.progressPercent ?: 0}%)...", Toast.LENGTH_SHORT).show()
                    } else {
                        val playable = episode.toMediaItem(parentSeries)
                        MovieDownloadManager.startDownload(
                            context = context,
                            mediaItem = playable,
                            preferredUrl = episode.streamUrl
                        )
                        Toast.makeText(context, "📥 পর্ব ডাউনলোড শুরু হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF1E293B), CircleShape)
            ) {
                Icon(
                    imageVector = when {
                        isDownloaded -> Icons.Rounded.CheckCircle
                        isDownloading -> Icons.Rounded.Downloading
                        else -> Icons.Rounded.FileDownload
                    },
                    contentDescription = "Download Episode",
                    tint = when {
                        isDownloaded -> Color(0xFF10B981)
                        isDownloading -> Color(0xFF00E5FF)
                        else -> Color(0xFF94A3B8)
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
