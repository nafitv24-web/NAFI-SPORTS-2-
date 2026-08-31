package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
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
import com.example.model.MediaType

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
        listOf("All", "❤️ Favorites") + unique
    }

    val filteredMovies = remember(movies, searchQuery, selectedCategory, favoriteIds) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {
        // Top Search Bar & Offline Button
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
                        "মুভি অথবা সিরিজ খুঁজুন...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
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
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A)
                ),
                singleLine = true
            )

            // Offline Downloads Quick Access Button
            Button(
                onClick = onOpenOfflineDownloads,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Downloads",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ডাউনলোডস",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                var isCatFocused by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF0F172A),
                    border = BorderStroke(
                        1.dp,
                        if (isCatFocused) Color(0xFFFFD600) else if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .onFocusChanged { isCatFocused = it.isFocused }
                        .focusable()
                        .clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Movies Grid
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
                    val isFav = favoriteIds.contains(movie.id)
                    var isCardFocused by remember { mutableStateOf(false) }
                    val cardScale by animateFloatAsState(
                        targetValue = if (isCardFocused) (if (isTvMode) 1.08f else 1.05f) else 1.0f,
                        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
                        label = "movieCardScale"
                    )

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
                            .fillMaxWidth()
                            .height(if (isTvMode) 220.dp else 180.dp)
                            .onFocusChanged { isCardFocused = it.isFocused }
                            .focusable()
                            .clickable { onSelectMedia(movie) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Movie Poster Image
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
                                                Color.Black.copy(alpha = 0.6f),
                                                Color.Black.copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                            )

                            // Top Left Tag (Category or Rating)
                            if (movie.category.isNotBlank() && !movie.category.equals("Unknown", ignoreCase = true)) {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 8.dp),
                                    color = Color(0xFF6366F1).copy(alpha = 0.9f),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = movie.category,
                                        color = Color.White,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Top Right Favorite Button
                            IconButton(
                                onClick = { onToggleFavorite(movie.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFav) Color(0xFFEF4444) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Bottom Info
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = movie.title,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val servers = movie.getAllServers()
                                if (servers.size > 1) {
                                    Text(
                                        text = "${servers.size} সার্ভার উপলভ্য",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
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
