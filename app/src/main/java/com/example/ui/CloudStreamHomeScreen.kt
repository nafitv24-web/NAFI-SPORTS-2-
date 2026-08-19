package com.example.ui

import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.MediaRepository
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.MovieProvider
import com.example.model.StreamServer

data class MovieCatalogItem(
    val id: String,
    val title: String,
    val rating: String = "8.5",
    val year: String = "2026",
    val category: String = "Movie",
    val posterUrl: String,
    val backdropUrl: String,
    val description: String = "An exciting blockbuster streaming experience with full multi-server playback.",
    val streamUrl: String = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
    val servers: List<StreamServer> = listOf(
        StreamServer("Server 1 (Ultra HD Fast)", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
        StreamServer("Server 2 (1080p Stream)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
        StreamServer("Server 3 (HLS Backup)", "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8")
    ),
    val episodes: List<Pair<String, String>> = emptyList()
)

@Composable
fun CloudStreamHomeScreen(
    repository: MediaRepository,
    movieProviders: List<MovieProvider>,
    activeProvider: MovieProvider?,
    onSelectProvider: (MovieProvider?) -> Unit,
    onOpenExtensions: () -> Unit,
    onPlayMovie: (MediaItem) -> Unit,
    onOpenMovieBrowser: (MovieProvider) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedMovieForDetail by remember { mutableStateOf<MovieCatalogItem?>(null) }
    var showProviderFilterMenu by remember { mutableStateOf(false) }
    var myWatchlistIds by remember { mutableStateOf(setOf<String>()) }

    // Curated catalog matching exact Screenshot 3
    val heroMovie = MovieCatalogItem(
        id = "hero_blind_girl",
        title = "Helpless Blind Girl",
        category = "Nollywood",
        rating = "7.8",
        year = "2026",
        posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&q=80",
        backdropUrl = "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=1000&q=80",
        description = "A gripping emotional drama about resilience, love, and finding hope in the darkest situations against all odds.",
        streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        servers = listOf(
            StreamServer("Server 1 (MovieBox 4K)", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            StreamServer("Server 2 (1080p HD)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            StreamServer("Server 3 (Fast HLS)", "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8")
        )
    )

    val trendingMovies = listOf(
        MovieCatalogItem(
            id = "trend_1",
            title = "Awarapan 2",
            rating = "6.8★",
            year = "2026",
            category = "Bollywood",
            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1000&q=80",
            description = "High octane Indian action romance saga returning to screens with unmatched intensity.",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        ),
        MovieCatalogItem(
            id = "trend_2",
            title = "The Death of Robin Hood",
            rating = "6.1★",
            year = "2026",
            category = "Hollywood",
            posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1000&q=80",
            description = "He was no hero. A dark reimagining of the legendary outlaw fighting his final battle.",
            streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"
        ),
        MovieCatalogItem(
            id = "trend_3",
            title = "The End of Oak Street",
            rating = "6.6★",
            year = "2026",
            category = "Thriller",
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1000&q=80",
            description = "A quiet suburban neighborhood unravels when secrets buried for twenty years come to light.",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        ),
        MovieCatalogItem(
            id = "trend_4",
            title = "Batwara 1947",
            rating = "4.7★",
            year = "2026",
            category = "Historical Drama",
            posterUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1000&q=80",
            description = "The partition day. An epic saga depicting the struggles and triumphs during 1947.",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        ),
        MovieCatalogItem(
            id = "trend_5",
            title = "Deadpool & Wolverine",
            rating = "8.2★",
            year = "2026",
            category = "Marvel Action",
            posterUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=1000&q=80",
            description = "The iconic Marvel duo unites to save the multiverse with comedy, chaos, and action.",
            streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"
        )
    )

    val trendingCinema = listOf(
        MovieCatalogItem(
            id = "cinema_1",
            title = "Awarapan 2",
            rating = "6.8★",
            year = "2026",
            category = "In Cinemas",
            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1000&q=80",
            description = "High octane action romance returning to cinema.",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        ),
        MovieCatalogItem(
            id = "cinema_2",
            title = "Batwara 1947",
            rating = "4.7★",
            year = "2026",
            category = "In Cinemas",
            posterUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1000&q=80",
            description = "Partition day epic cinematic journey.",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        ),
        MovieCatalogItem(
            id = "cinema_3",
            title = "The End of Oak Street",
            rating = "6.6★",
            year = "2026",
            category = "In Cinemas",
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1000&q=80",
            description = "Mysterious thriller in theater screenings.",
            streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"
        ),
        MovieCatalogItem(
            id = "cinema_4",
            title = "Ohh My Dog",
            rating = "9.1★",
            year = "2026",
            category = "Family Comedy",
            posterUrl = "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=1000&q=80",
            description = "A heartwarming tale of a boy and a blind pup who conquer every hurdle.",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        )
    )

    val bollywoodSeries = listOf(
        MovieCatalogItem(
            id = "bolly_1",
            title = "Toofan (তুফান)",
            rating = "8.9★",
            year = "2026",
            category = "Bangla & Hindi Dub",
            posterUrl = "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=1000&q=80",
            description = "Shakib Khan starrer blockbuster 90s gangster drama that took box offices by storm.",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        ),
        MovieCatalogItem(
            id = "bolly_2",
            title = "Panchayat Season 3",
            rating = "9.2★",
            year = "2026",
            category = "Hindi Comedy Series",
            posterUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1000&q=80",
            description = "The beloved Phulera panchayat returns with fresh elections, comedy, and village charm.",
            streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
            episodes = listOf(
                "Episode 1 - Rangbaazi" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                "Episode 2 - Gaddha" to "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                "Episode 3 - Ghar Ka Bhedi" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            )
        ),
        MovieCatalogItem(
            id = "bolly_3",
            title = "Mirzapur Season 3",
            rating = "8.7★",
            year = "2026",
            category = "Crime Series",
            posterUrl = "https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=1000&q=80",
            description = "The throne of Purvanchal is contested in a violent power struggle.",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        ),
        MovieCatalogItem(
            id = "bolly_4",
            title = "Jawan Extended Cut",
            rating = "8.4★",
            year = "2026",
            category = "Bollywood 4K",
            posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1000&q=80",
            description = "A high-voltage emotional action thriller driven by a man on a mission to rectify societal wrongs.",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        )
    )

    fun playItem(item: MovieCatalogItem) {
        val mediaItem = MediaItem(
            id = item.id,
            title = item.title,
            category = item.category,
            type = if (item.episodes.isNotEmpty()) MediaType.SERIES else MediaType.MOVIE,
            streamUrl = item.streamUrl,
            servers = item.servers,
            logoUrl = item.posterUrl,
            description = item.description,
            rating = item.rating,
            year = item.year
        )
        onPlayMovie(mediaItem)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // HERO BANNER (Exact UI from Screenshot 3)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                ) {
                    // Backdrop Poster
                    AsyncImage(
                        model = heroMovie.backdropUrl,
                        contentDescription = heroMovie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient Overlay on bottom & top
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.6f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f),
                                        Color.Black
                                    )
                                )
                            )
                    )

                    // Top Action Bar Header (Search icon on left, Profile Avatar on right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Provider Pill Indicator (e.g. MovieBox / Phisher Repo)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                            modifier = Modifier.clickable { onOpenExtensions() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
                                    text = activeProvider?.name ?: "Phisher • MovieBox",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                                .clickable { onOpenExtensions() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Hero Content Text & Action Buttons
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = heroMovie.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = heroMovie.category,
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3 Action Buttons matching Screenshot 3: [+ None / My List], [▶ Play], [ⓘ Info]
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Button 1: [+ None / My List]
                            val inList = myWatchlistIds.contains(heroMovie.id)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        myWatchlistIds = if (inList) myWatchlistIds - heroMovie.id else myWatchlistIds + heroMovie.id
                                        Toast.makeText(context, if (inList) "Removed from My List" else "Added to My List!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (inList) Icons.Rounded.Check else Icons.Rounded.Add,
                                    contentDescription = "My List",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (inList) "Added" else "None",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Button 2: [▶ Play] (Pill White Button)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .clickable { playItem(heroMovie) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Play",
                                        color = Color.Black,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Button 3: [ⓘ Info]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { selectedMovieForDetail = heroMovie }
                                    .padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "Info",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Info",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar (Shown when search toggled)
            if (isSearchActive) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search movies, anime, or series...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF38BDF8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // SECTION 1: Trending (Exact row from Screenshot 3)
            item {
                MovieCatalogRow(
                    sectionTitle = "Trending",
                    movies = trendingMovies.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) },
                    onMovieClick = { selectedMovieForDetail = it },
                    onPlayClick = { playItem(it) }
                )
            }

            // SECTION 2: Trending in Cinema (Exact row from Screenshot 3)
            item {
                MovieCatalogRow(
                    sectionTitle = "Trending in Cinema",
                    movies = trendingCinema.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) },
                    onMovieClick = { selectedMovieForDetail = it },
                    onPlayClick = { playItem(it) }
                )
            }

            // SECTION 3: Latest Bollywood & Web Series
            item {
                MovieCatalogRow(
                    sectionTitle = "Latest Bollywood & Series",
                    movies = bollywoodSeries.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) },
                    onMovieClick = { selectedMovieForDetail = it },
                    onPlayClick = { playItem(it) }
                )
            }
        }

        // Floating Filter Button (Matching Screenshot 3 bottom right ☰ icon)
        FloatingActionButton(
            onClick = { showProviderFilterMenu = true },
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp)
                .size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MenuOpen,
                contentDescription = "Providers Filter",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // Provider Selector Modal Sheet / Dialog
    if (showProviderFilterMenu) {
        AlertDialog(
            onDismissRequest = { showProviderFilterMenu = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Select Movie Provider", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { onOpenExtensions(); showProviderFilterMenu = false }) {
                        Icon(Icons.Rounded.Extension, contentDescription = "Extensions", tint = Color(0xFF38BDF8))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Choose which Phisher extension to source movies & series from:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val installedProviders = movieProviders.filter { it.isInstalled }
                    installedProviders.take(8).forEach { prov ->
                        val isSelected = activeProvider?.id == prov.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectProvider(prov)
                                    showProviderFilterMenu = false
                                    Toast.makeText(context, "Sourced from ${prov.name}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(prov.flag ?: "🎬", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(prov.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text(prov.language, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onOpenExtensions()
                            showProviderFilterMenu = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manage Extensions & Repos", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProviderFilterMenu = false }) {
                    Text("Close", color = Color(0xFF38BDF8))
                }
            }
        )
    }

    // Movie Detail Sheet / Modal
    if (selectedMovieForDetail != null) {
        val movie = selectedMovieForDetail!!
        AlertDialog(
            onDismissRequest = { selectedMovieForDetail = null },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = movie.backdropUrl,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = movie.rating,
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = movie.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${movie.category} • ${movie.year} • Multi-Server Fast Streaming",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = movie.description,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    // Episode selector if series
                    if (movie.episodes.isNotEmpty()) {
                        Text("Episodes:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        movie.episodes.forEach { (epName, epUrl) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playItem(movie.copy(streamUrl = epUrl))
                                        selectedMovieForDetail = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(epName, color = Color.White, fontSize = 12.sp)
                                    Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        playItem(movie)
                        selectedMovieForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Watch Now (এখনই দেখুন)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMovieForDetail = null }) {
                    Text("Close", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
private fun MovieCatalogRow(
    sectionTitle: String,
    movies: List<MovieCatalogItem>,
    onMovieClick: (MovieCatalogItem) -> Unit,
    onPlayClick: (MovieCatalogItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Section Header with Title & Arrow (Exact UI from Screenshot 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = sectionTitle,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = "See all",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Horizontal Card Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(movies, key = { it.id }) { item ->
                var isFocused by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .width(115.dp)
                        .scale(if (isFocused) 1.06f else 1.0f)
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .clickable { onMovieClick(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Rating Badge on top right (e.g. 6.8★)
                        Surface(
                            shape = RoundedCornerShape(bottomStart = 6.dp),
                            color = Color.Black.copy(alpha = 0.85f),
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = item.rating,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
