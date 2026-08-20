package com.example.tmdb

import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.MovieProvider
import com.example.model.StreamServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * TMDB (The Movie Database) Movie Engine
 * কনফিগারেশন:
 * const CFG = {
 *     API_KEY: '05902896074695709d7763505bb88b4d', // TMDB API Key
 *     BASE: 'https://api.themoviedb.org/3'
 * }
 * শুধুমাত্র মুভি দেখার জন্য (Dedicated strictly for Movie Streaming & Browsing)
 */
object TmdbConfig {
    const val API_KEY = "05902896074695709d7763505bb88b4d"
    const val BASE = "https://api.themoviedb.org/3"
    const val IMAGE_BASE = "https://image.tmdb.org/t/p/w500"
    const val BACKDROP_BASE = "https://image.tmdb.org/t/p/original"
}

class TmdbMovieEngine(private val client: OkHttpClient) {

    companion object {
        val TMDB_PROVIDER = MovieProvider(
            id = "tmdb_movie_engine",
            name = "TMDB Cinema Hub",
            siteUrl = "https://www.themoviedb.org",
            iconUrl = "https://www.themoviedb.org/assets/2/v4/logos/v2/blue_square_2-d537fb228cf3ded904ef09b136fe3fec72548ebc1fea3fbbd1ad9e36364db38b.png",
            description = "The Movie Database (TMDB) • শুধুমাত্র মুভি দেখার জন্য",
            types = listOf("Movie"),
            isInstalled = true,
            isEnabled = true,
            repoName = "TMDB Movie Hub"
        )

        // TMDB Movie Genres (Only Movies)
        val MOVIE_GENRES = mapOf(
            "Action" to 28,
            "Adventure" to 12,
            "Animation" to 16,
            "Comedy" to 35,
            "Crime" to 80,
            "Documentary" to 99,
            "Drama" to 18,
            "Family" to 10751,
            "Fantasy" to 14,
            "History" to 36,
            "Horror" to 27,
            "Music" to 10402,
            "Mystery" to 9648,
            "Romance" to 10749,
            "Sci-Fi" to 878,
            "Thriller" to 53,
            "War" to 10752,
            "Western" to 37
        )
    }

    /**
     * Fetch complete Home Movie Catalog combining multiple categories in parallel
     */
    suspend fun fetchAllHomeMovies(): List<MediaItem> = withContext(Dispatchers.IO) {
        val combined = mutableListOf<MediaItem>()
        try {
            coroutineScope {
                val trending = async { fetchMovies("Trending", page = 1) }
                val popular = async { fetchMovies("Popular", page = 1) }
                val topRated = async { fetchMovies("Top Rated", page = 1) }
                val nowPlaying = async { fetchMovies("Now Playing", page = 1) }
                val bollywood = async { fetchMovies("Bollywood", page = 1) }
                val bangla = async { fetchMovies("Bangla", page = 1) }
                val action = async { fetchMovies("Action", page = 1) }
                val scifi = async { fetchMovies("Sci-Fi", page = 1) }
                val horror = async { fetchMovies("Horror", page = 1) }
                val animation = async { fetchMovies("Animation", page = 1) }
                val comedy = async { fetchMovies("Comedy", page = 1) }

                val allLists = awaitAll(
                    trending, popular, topRated, nowPlaying,
                    bollywood, bangla, action, scifi, horror, animation, comedy
                )
                for (list in allLists) {
                    combined.addAll(list)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        combined.distinctBy { it.id }
    }

    /**
     * Fetch TMDB Movies based on Category or Search Query
     * @param category: e.g. "Trending", "Popular", "Top Rated", "Now Playing", "Upcoming", "Bollywood", "Bangla", "Action", "Horror", etc.
     * @param query: optional search keyword
     */
    suspend fun fetchMovies(
        category: String = "Trending",
        query: String = "",
        page: Int = 1
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MediaItem>()
        try {
            val url = when {
                query.isNotBlank() -> {
                    val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                    "${TmdbConfig.BASE}/search/movie?api_key=${TmdbConfig.API_KEY}&query=$encoded&page=$page&include_adult=false"
                }
                category.equals("Trending", ignoreCase = true) || category.equals("All", ignoreCase = true) -> {
                    "${TmdbConfig.BASE}/trending/movie/day?api_key=${TmdbConfig.API_KEY}&page=$page"
                }
                category.equals("Popular", ignoreCase = true) || category.equals("Movies", ignoreCase = true) -> {
                    "${TmdbConfig.BASE}/movie/popular?api_key=${TmdbConfig.API_KEY}&page=$page"
                }
                category.equals("Top Rated", ignoreCase = true) -> {
                    "${TmdbConfig.BASE}/movie/top_rated?api_key=${TmdbConfig.API_KEY}&page=$page"
                }
                category.equals("Now Playing", ignoreCase = true) -> {
                    "${TmdbConfig.BASE}/movie/now_playing?api_key=${TmdbConfig.API_KEY}&page=$page"
                }
                category.equals("Upcoming", ignoreCase = true) -> {
                    "${TmdbConfig.BASE}/movie/upcoming?api_key=${TmdbConfig.API_KEY}&page=$page"
                }
                category.contains("Bollywood", ignoreCase = true) || category.contains("Hindi", ignoreCase = true) -> {
                    "${TmdbConfig.BASE}/discover/movie?api_key=${TmdbConfig.API_KEY}&with_original_language=hi&sort_by=popularity.desc&page=$page"
                }
                category.contains("Bangla", ignoreCase = true) || category.contains("Bengali", ignoreCase = true) -> {
                    "${TmdbConfig.BASE}/discover/movie?api_key=${TmdbConfig.API_KEY}&with_original_language=bn&sort_by=popularity.desc&page=$page"
                }
                category.contains("South", ignoreCase = true) || category.contains("Tamil", ignoreCase = true) || category.contains("Telugu", ignoreCase = true) -> {
                    "${TmdbConfig.BASE}/discover/movie?api_key=${TmdbConfig.API_KEY}&with_original_language=te|ta|ml|kn&sort_by=popularity.desc&page=$page"
                }
                category.contains("Anime", ignoreCase = true) || category.contains("Japanese", ignoreCase = true) -> {
                    "${TmdbConfig.BASE}/discover/movie?api_key=${TmdbConfig.API_KEY}&with_genres=16&with_original_language=ja&sort_by=popularity.desc&page=$page"
                }
                category.contains("Korean", ignoreCase = true) || category.contains("Asian", ignoreCase = true) -> {
                    "${TmdbConfig.BASE}/discover/movie?api_key=${TmdbConfig.API_KEY}&with_original_language=ko&sort_by=popularity.desc&page=$page"
                }
                else -> {
                    val genreId = MOVIE_GENRES[category] ?: 28 // Default to Action if not mapped
                    "${TmdbConfig.BASE}/discover/movie?api_key=${TmdbConfig.API_KEY}&with_genres=$genreId&sort_by=popularity.desc&page=$page"
                }
            }

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) TMDB-Engine/3.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: ""
                results.addAll(parseTmdbMovieResults(jsonString, category))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }

    /**
     * Parse TMDB JSON array of movie objects into MediaItem with Multi-Server Stream links
     */
    private fun parseTmdbMovieResults(jsonStr: String, fallbackCategory: String): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
        try {
            val root = JSONObject(jsonStr)
            val resultsArr = root.optJSONArray("results") ?: JSONArray()
            for (i in 0 until resultsArr.length()) {
                val itemObj = resultsArr.optJSONObject(i) ?: continue
                val id = itemObj.optInt("id", 0)
                if (id == 0) continue

                val title = itemObj.optString("title", itemObj.optString("original_title", "Untitled Movie")).trim()
                val overview = itemObj.optString("overview", "").trim()
                val posterPath = itemObj.optString("poster_path", "")
                val backdropPath = itemObj.optString("backdrop_path", "")
                val voteAverage = itemObj.optDouble("vote_average", 0.0)
                val releaseDate = itemObj.optString("release_date", "")
                val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "2026"
                val originalLanguage = itemObj.optString("original_language", "en").uppercase()

                val posterUrl = if (posterPath.isNotBlank() && posterPath != "null") {
                    "${TmdbConfig.IMAGE_BASE}$posterPath"
                } else if (backdropPath.isNotBlank() && backdropPath != "null") {
                    "${TmdbConfig.IMAGE_BASE}$backdropPath"
                } else {
                    "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500"
                }

                val scoreFormatted = String.format(java.util.Locale.US, "%.1f", voteAverage)

                // Multi-Server Fast Streaming URLs for this specific TMDB Movie ID
                val servers = buildTmdbStreamServers(id, title)
                val primaryStreamUrl = servers.firstOrNull()?.url ?: "https://vidsrc.to/embed/movie/$id"

                val categoryLabel = when {
                    fallbackCategory.isNotBlank() && fallbackCategory != "All" -> fallbackCategory
                    originalLanguage == "HI" -> "Bollywood"
                    originalLanguage == "BN" -> "Bangla"
                    originalLanguage == "JA" -> "Anime"
                    originalLanguage == "KO" -> "Korean"
                    voteAverage >= 8.0 -> "Top Rated"
                    else -> "Trending Movies"
                }

                list.add(
                    MediaItem(
                        id = "tmdb_$id",
                        title = title,
                        streamUrl = primaryStreamUrl,
                        logoUrl = posterUrl,
                        type = MediaType.MOVIE,
                        category = categoryLabel,
                        tournament = "TMDB_CINEMA",
                        description = if (overview.isNotBlank()) overview else "$title ($year) - TMDB Rating: $scoreFormatted/10 [$originalLanguage]",
                        score1 = "★ $scoreFormatted",
                        score2 = year,
                        rating = scoreFormatted,
                        year = year,
                        quality = "1080p",
                        servers = servers
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Build 10+ high-speed fast streaming servers specifically optimized for TMDB Movie IDs
     */
    private fun buildTmdbStreamServers(tmdbId: Int, movieTitle: String): List<StreamServer> {
        return listOf(
            StreamServer(
                name = "⚡ VidSrc V2 (Primary HD)",
                url = "https://vidsrc.cc/v2/embed/movie/$tmdbId"
            ),
            StreamServer(
                name = "🚀 Embed.su HighSpeed",
                url = "https://embed.su/embed/movie/$tmdbId"
            ),
            StreamServer(
                name = "🎬 AutoEmbed 1080p",
                url = "https://autoembed.to/movie/tmdb/$tmdbId"
            ),
            StreamServer(
                name = "💎 MultiEmbed Cloud",
                url = "https://multiembed.mov/?video_id=$tmdbId&tmdb=1"
            ),
            StreamServer(
                name = "🍿 VidSrc.to Fast",
                url = "https://vidsrc.to/embed/movie/$tmdbId"
            ),
            StreamServer(
                name = "🌟 2Embed Multi-Sub",
                url = "https://2embed.cc/embed/$tmdbId"
            ),
            StreamServer(
                name = "📡 SmashyStream Direct",
                url = "https://player.smashy.stream/movie/$tmdbId"
            ),
            StreamServer(
                name = "🌐 VidSrc.xyz Mirror",
                url = "https://vidsrc.xyz/embed/movie?tmdb=$tmdbId"
            ),
            StreamServer(
                name = "✨ SuperEmbed Stream",
                url = "https://superembed.stream/movie/$tmdbId"
            ),
            StreamServer(
                name = "🔥 MoviesAPI Club",
                url = "https://moviesapi.club/movie/$tmdbId"
            )
        )
    }

    /**
     * Fetch complete Movie Details including synopsis, runtime, backdrop, genres, rating, and cast
     */
    suspend fun fetchMovieDetails(tmdbId: Int): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = "${TmdbConfig.BASE}/movie/$tmdbId?api_key=${TmdbConfig.API_KEY}&append_to_response=credits,videos,similar"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: ""
                return@withContext JSONObject(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}
