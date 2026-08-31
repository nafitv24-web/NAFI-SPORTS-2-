package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem

@Composable
fun MoviesTabScreen(
    movies: List<MediaItem>,
    favoriteIds: Set<String>,
    isTvMode: Boolean = false,
    onSelectMedia: (MediaItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenOfflineDownloads: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(movies) {
        val unique = movies.map { it.category.trim() }
            .filter { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
            .distinct()
            .sorted()
        listOf("All", "ডাউনলোডসমূহ") + unique
    }

    // Identify Featured Spotlight Movie (either specifically Sultan Salahuddin or the first movie)
    val featuredMovie = remember(movies) {
        movies.firstOrNull { it.title.contains("Sultan", ignoreCase = true) || it.title.contains("Salahuddin", ignoreCase = true) }
            ?: movies.firstOrNull { !it.logoUrl.isNullOrBlank() }
            ?: movies.firstOrNull()
    }

    // Group movies by category for the categorized carousels view
    val categorizedMovies = remember(movies) {
        val uniqueCats = movies.map { it.category.trim() }
            .filter { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
            .distinct()
        uniqueCats.map { cat ->
            cat to movies.filter { it.category.trim().equals(cat, ignoreCase = true) }
        }
    }

    // Filtered movies when search query is active or a single category is selected
    val filteredMovies = remember(movies, searchQuery, selectedCategory, favoriteIds) {
        if (selectedCategory == "ডাউনলোডসমূহ") {
            emptyList()
        } else {
            movies.filter { movie ->
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    movie.title.contains(searchQuery, ignoreCase = true) ||
                            movie.category.contains(searchQuery, ignoreCase = true) ||
                            (movie.description != null && movie.description.contains(searchQuery, ignoreCase = true))
                }
                val matchesCategory = when (selectedCategory) {
                    "All" -> true
                    "❤️ Favorites" -> favoriteIds.contains(movie.id)
                    else -> movie.category.trim().equals(selectedCategory.trim(), ignoreCase = true)
                }
                matchesSearch && matchesCategory
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {
        // TOP SEARCH BAR & DOWNLOAD BUTTON (Matching Screenshot 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "মুভি ও ওয়েব সিরিজ খুঁজুন (যেমন: Jawan, Toofan, Leo)",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = Color.White
                            )
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A)
                ),
                singleLine = true
            )

            // Right Download Square Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                modifier = Modifier
                    .size(50.dp)
                    .clickable { onOpenOfflineDownloads() }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Downloads",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // CATEGORY FILTER CHIPS ROW (Horizontal Scroll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                var isCatFocused by remember { mutableStateOf(false) }

                val isDownloadTab = cat == "ডাউনলোডসমূহ"

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        isDownloadTab -> Color(0xFF0F766E)
                        isSelected -> Color(0xFF2563EB)
                        else -> Color(0xFF1E293B)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isCatFocused -> Color(0xFFFFD600)
                            isDownloadTab -> Color(0xFF14B8A6)
                            isSelected -> Color(0xFF3B82F6)
                            else -> Color(0xFF334155)
                        }
                    ),
                    modifier = Modifier
                        .onFocusChanged { isCatFocused = it.isFocused }
                        .focusable()
                        .clickable {
                            if (isDownloadTab) {
                                onOpenOfflineDownloads()
                            } else {
                                selectedCategory = cat
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        if (isDownloadTab) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = cat,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected || isDownloadTab) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // MAIN CONTENT AREA
        if (searchQuery.isBlank() && selectedCategory == "All") {
            // HOME / ALL VIEW: FEATURED BANNER + CATEGORY CAROUSELS (Matching Screenshot 1)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. FEATURED SPOTLIGHT BANNER
                if (featuredMovie != null) {
                    item {
                        var isBannerFocused by remember { mutableStateOf(false) }
                        val bannerScale by animateFloatAsState(
                            targetValue = if (isBannerFocused) 1.02f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
                            label = "bannerScale"
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(
                                1.dp,
                                if (isBannerFocused) Color(0xFFFFD600) else Color(0xFF1E293B)
                            ),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .scale(bannerScale)
                                .fillMaxWidth()
                                .height(210.dp)
                                .padding(horizontal = 14.dp)
                                .onFocusChanged { isBannerFocused = it.isFocused }
                                .focusable()
                                .clickable { onSelectMedia(featuredMovie) }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Background Banner Image
                                if (!featuredMovie.logoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = featuredMovie.logoUrl,
                                        contentDescription = featuredMovie.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                                                )
                                            )
                                    )
                                }

                                // Dark Gradient Overlays for High Contrast
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.2f),
                                                    Color.Black.copy(alpha = 0.5f),
                                                    Color.Black.copy(alpha = 0.95f)
                                                )
                                            )
                                        )
                                )

                                // Banner Content
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    // Badges Row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Red Spotlight Badge
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE11D48)
                                        ) {
                                            Text(
                                                text = "FEATURED SPOTLIGHT",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }

                                        // Teal Download Support Badge
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF0D9488)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Download,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "ডাউনলোড সাপোর্ট",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Main Title
                                    Text(
                                        text = featuredMovie.title,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Subtitle / Description
                                    Text(
                                        text = featuredMovie.description?.takeIf { it.isNotBlank() }
                                            ?: "ঐতিহাসিক অ্যাকশন ও ড্রামা সিরিজ/মুভি।",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. CATEGORIZED HORIZONTAL CAROUSELS
                items(categorizedMovies, key = { it.first }) { (categoryName, catMovies) ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Section Header: "🎬 NAFI OTT • BANGLA" ----- "সব (15)"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🎬 $categoryName",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                )
                            }

                            Text(
                                text = "সব (${catMovies.size})",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { selectedCategory = categoryName }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            )
                        }

                        // Horizontal Poster Row
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(catMovies, key = { it.id }) { movie ->
                                MoviePosterCard(
                                    movie = movie,
                                    isFav = favoriteIds.contains(movie.id),
                                    isTvMode = isTvMode,
                                    onSelect = { onSelectMedia(movie) },
                                    onToggleFav = { onToggleFavorite(movie.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // FILTERED OR SEARCH VIEW (Grid Layout)
            if (filteredMovies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Movie,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "কোনো মুভি বা সিরিজ পাওয়া যায়নি",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "অন্য কি-ওয়ার্ড দিয়ে সার্চ করুন অথবা ক্যাটাগরি পরিবর্তন করুন।",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isTvMode) 5 else 3),
                    contentPadding = PaddingValues(horizontal = if (isTvMode) 14.dp else 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 12.dp else 8.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isTvMode) 14.dp else 10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredMovies, key = { it.id }) { movie ->
                        MoviePosterCard(
                            movie = movie,
                            isFav = favoriteIds.contains(movie.id),
                            isTvMode = isTvMode,
                            onSelect = { onSelectMedia(movie) },
                            onToggleFav = { onToggleFavorite(movie.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper to determine the movie language for badge display
 * (e.g., "বাংলা", "হিন্দি", "হিন্দি ডাবড", "তামিল", "তেলেগু", "ইংরেজি", etc.)
 * Returns "NFT" when no specific language is confirmed.
 */
fun getMovieLanguageBadge(movie: MediaItem): String {
    // Remove network/playlist tags like "BDIX", "NAFI OTT" before scanning language
    val cleanCategory = movie.category.replace("BDIX", "", ignoreCase = true)
        .replace("NAFI OTT", "", ignoreCase = true)
        .replace("NAFI_OTT", "", ignoreCase = true)
    val cleanTitle = movie.title.replace("BDIX", "", ignoreCase = true)
    val cleanDesc = (movie.description ?: "").replace("BDIX", "", ignoreCase = true)

    val textToScan = "$cleanCategory $cleanTitle $cleanDesc".lowercase()

    return when {
        // Specific Dubbed Tags
        textToScan.contains("bangla dubbed") || textToScan.contains("bangla-dubbed") || textToScan.contains("বাংলা ডাবড") || textToScan.contains("বাংলা ডাবিং") -> "বাংলা ডাবড"
        textToScan.contains("hindi dubbed") || textToScan.contains("hindi-dubbed") || textToScan.contains("dubbed in hindi") || textToScan.contains("হিন্দি ডাবড") || textToScan.contains("হিন্দি ডাবিং") -> "হিন্দি ডাবড"
        textToScan.contains("tamil dubbed") || textToScan.contains("তামিল ডাবড") -> "তামিল ডাবড"
        textToScan.contains("telugu dubbed") || textToScan.contains("তেলেগু ডাবড") -> "তেলেগু ডাবড"
        textToScan.contains("english dubbed") || textToScan.contains("ইংরেজি ডাবড") -> "ইংরেজি ডাবড"

        // Tamil / Telugu / Malayalam / Kannada / South
        textToScan.contains("tamil") || textToScan.contains("তামিল") || textToScan.contains("kollywood") -> "তামিল"
        textToScan.contains("telugu") || textToScan.contains("তেলেগু") || textToScan.contains("tollywood") -> "তেলেগু"
        textToScan.contains("malayalam") || textToScan.contains("মালায়ালাম") || textToScan.contains("mollywood") -> "মালায়ালাম"
        textToScan.contains("kannada") || textToScan.contains("কন্নড়") || textToScan.contains("sandalwood") -> "কন্নড়"
        textToScan.contains("south movie") || textToScan.contains("south indian") || textToScan.contains("সাউথ") -> "সাউথ"

        // Hindi / Bollywood / Hindi Shows
        textToScan.contains("hindi") || textToScan.contains("হিন্দি") || textToScan.contains("bollywood") ||
                textToScan.contains("khatron ke khiladi") || textToScan.contains("kapil show") ||
                textToScan.contains("bigg boss") || textToScan.contains("indian idol") ||
                textToScan.contains("dance deewane") || textToScan.contains("roadies") ||
                textToScan.contains("splitsvilla") || textToScan.contains("anupamaa") ||
                textToScan.contains("tmkoc") || textToScan.contains("taarak mehta") -> "হিন্দি"

        // Bangla / Dhallywood / Tollywood Bangla
        textToScan.contains("bangla") || textToScan.contains("bengali") || textToScan.contains("বাংলা") ||
                textToScan.contains("dhallywood") || textToScan.contains("bangladesh") ||
                textToScan.contains("hoichoi") || textToScan.contains("chorki") ||
                textToScan.contains("kolkata") || textToScan.contains("নাটক") || textToScan.contains("natok") -> "বাংলা"

        // Korean / K-Drama
        textToScan.contains("korean") || textToScan.contains("k-drama") || textToScan.contains("kdrama") ||
                textToScan.contains("k drama") || textToScan.contains("কোরিয়ান") || textToScan.contains("korea") -> "কোরিয়ান"

        // Anime / Japanese
        textToScan.contains("anime") || textToScan.contains("japanese") || textToScan.contains("অ্যানিমে") ||
                textToScan.contains("japan") -> "অ্যানিমে"

        // Turkish
        textToScan.contains("turkish") || textToScan.contains("তুর্কি") || textToScan.contains("turkey") ||
                textToScan.contains("kurulus") || textToScan.contains("ertugrul") || textToScan.contains("osman") -> "তুর্কি"

        // English / Hollywood
        textToScan.contains("english") || textToScan.contains("ইংরেজি") || textToScan.contains("hollywood") ||
                textToScan.contains("marvel") || textToScan.contains("dc comics") || textToScan.contains("hbo") ||
                textToScan.contains("netflix original") -> "ইংরেজি"

        // If no explicit tag or language pattern is matched, show NFT
        else -> "NFT"
    }
}

@Composable
fun MoviePosterCard(
    movie: MediaItem,
    isFav: Boolean,
    isTvMode: Boolean,
    onSelect: () -> Unit,
    onToggleFav: () -> Unit
) {
    var isCardFocused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isCardFocused) (if (isTvMode) 1.08f else 1.05f) else 1.0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label = "movieCardScale"
    )

    val langBadge = remember(movie.id, movie.title, movie.category, movie.description) {
        getMovieLanguageBadge(movie)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCardFocused) Color(0xFF1E293B) else Color(0xFF0F172A),
        border = when {
            isCardFocused -> BorderStroke(2.5.dp, Color(0xFFFFD600))
            isFav -> BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.7f))
            else -> BorderStroke(1.dp, Color(0xFF1E293B))
        },
        shadowElevation = if (isCardFocused) 12.dp else 4.dp,
        modifier = Modifier
            .scale(cardScale)
            .width(if (isTvMode) 140.dp else 115.dp)
            .height(if (isTvMode) 210.dp else 170.dp)
            .onFocusChanged { isCardFocused = it.isFocused }
            .focusable()
            .clickable { onSelect() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster Image
            if (!movie.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = movie.logoUrl,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Dark Gradient Overlay at Bottom for Readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Top Left Language Badge (বাংলা / হিন্দি / ইংরেজি etc., replacing "OTT")
            Surface(
                shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 8.dp),
                color = when (langBadge) {
                    "বাংলা", "বাংলা ডাবড" -> Color(0xFF059669)
                    "হিন্দি", "হিন্দি ডাবড" -> Color(0xFFD97706)
                    "ইংরেজি" -> Color(0xFF2563EB)
                    "তামিল", "তেলেগু", "মালায়ালাম", "সাউথ" -> Color(0xFFEA580C)
                    "কোরিয়ান" -> Color(0xFF7C3AED)
                    "অ্যানিমে" -> Color(0xFFDB2777)
                    "তুর্কি" -> Color(0xFF0D9488)
                    "NFT" -> Color(0xFF6366F1)
                    else -> Color(0xFF6366F1)
                },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = langBadge,
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Top Right Favorite Button
            IconButton(
                onClick = onToggleFav,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFav) Color(0xFFEF4444) else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Bottom Movie Name inside the movie card (মুভির নাম নিচের দিকে থাকবে মুভির ভিতরে থাকবে)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 7.dp, vertical = 6.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
