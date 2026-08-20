package com.example.player

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object StreamExtractor {

    private const val TAG = "StreamExtractor"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    /**
     * Checks whether the given URL is an embed player URL that should be resolved to a direct stream.
     */
    fun isEmbedUrl(url: String): Boolean {
        val clean = url.lowercase().trim()
        return clean.contains("2embed") ||
                clean.contains("vidsrc") ||
                clean.contains("superstream") ||
                clean.contains("smashystream") ||
                clean.contains("autoembed") ||
                clean.contains("embed") ||
                clean.contains("streamtape") ||
                clean.contains("mixdrop") ||
                clean.contains("dood") ||
                clean.contains("filemoon") ||
                clean.contains("rabbitstream") ||
                clean.contains("megacloud") ||
                clean.contains("dokicloud")
    }

    /**
     * Resolves an embed/web URL to a direct playable stream (m3u8, mpd, mp4) with necessary request headers.
     */
    suspend fun extractDirectStream(url: String): ExtractedStreamResult? = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = url.trim()
            val lower = cleanUrl.lowercase()

            // 1. If it's already a direct video stream
            if (lower.contains(".m3u8") || lower.contains(".mpd") || lower.contains(".mp4")) {
                return@withContext ExtractedStreamResult(
                    streamUrl = cleanUrl,
                    headers = mapOf("User-Agent" to DEFAULT_UA)
                )
            }

            // 2. 2Embed resolver (2embed.cc, 2embed.to, 2embed.skin, 2embed.stream)
            if (lower.contains("2embed")) {
                extractFrom2Embed(cleanUrl)?.let { return@withContext it }
            }

            // 3. VidSrc resolver (vidsrc.to, vidsrc.me, vidsrc.net, vidsrc.xyz, vidsrc.in)
            if (lower.contains("vidsrc")) {
                extractFromVidSrc(cleanUrl)?.let { return@withContext it }
            }

            // 4. SuperStream / SmashyStream resolver
            if (lower.contains("superstream") || lower.contains("smashystream")) {
                extractFromGenericEmbed(cleanUrl)?.let { return@withContext it }
            }

            // 5. Generic page scraping for embedded m3u8 / mpd / mp4 / hls sources
            extractFromGenericEmbed(cleanUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed for $url: ${e.message}")
            null
        }
    }

    private suspend fun extractFrom2Embed(url: String): ExtractedStreamResult? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_UA)
                .header("Referer", "https://2embed.cc/")
                .build()

            val resp = client.newCall(req).execute()
            val html = resp.body?.string() ?: return@withContext null

            // Search for direct stream URLs or player iframes
            findStreamInHtml(html, url)?.let { return@withContext it }

            // Look for iframe src
            val iframeMatcher = Pattern.compile("<iframe[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
            while (iframeMatcher.find()) {
                var iframeSrc = iframeMatcher.group(1) ?: continue
                if (iframeSrc.startsWith("//")) iframeSrc = "https:$iframeSrc"
                if (iframeSrc.startsWith("/")) {
                    val base = getBaseUrl(url)
                    iframeSrc = "$base$iframeSrc"
                }

                // If iframe is a stream
                if (iframeSrc.contains(".m3u8") || iframeSrc.contains(".mpd") || iframeSrc.contains(".mp4")) {
                    return@withContext ExtractedStreamResult(
                        streamUrl = iframeSrc,
                        headers = mapOf(
                            "User-Agent" to DEFAULT_UA,
                            "Referer" to url
                        )
                    )
                }

                // Recursive check 1 level
                extractFromGenericEmbed(iframeSrc)?.let { return@withContext it }
            }
        } catch (e: Exception) {
            Log.e(TAG, "2Embed extraction error: ${e.message}")
        }
        null
    }

    private suspend fun extractFromVidSrc(url: String): ExtractedStreamResult? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_UA)
                .header("Referer", url)
                .build()

            val resp = client.newCall(req).execute()
            val html = resp.body?.string() ?: return@withContext null

            findStreamInHtml(html, url)?.let { return@withContext it }

            // Match rcp / prourl / player endpoints
            val rcpMatcher = Pattern.compile("src: [\"']([^\"']*(?:rcp|prorpm|player)[^\"']*)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
            if (rcpMatcher.find()) {
                var rcpUrl = rcpMatcher.group(1) ?: ""
                if (rcpUrl.startsWith("//")) rcpUrl = "https:$rcpUrl"
                extractFromGenericEmbed(rcpUrl)?.let { return@withContext it }
            }
        } catch (e: Exception) {
            Log.e(TAG, "VidSrc extraction error: ${e.message}")
        }
        null
    }

    private suspend fun extractFromGenericEmbed(url: String): ExtractedStreamResult? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_UA)
                .header("Referer", url)
                .build()

            val resp = client.newCall(req).execute()
            val html = resp.body?.string() ?: return@withContext null

            findStreamInHtml(html, url)
        } catch (e: Exception) {
            Log.e(TAG, "Generic embed error: ${e.message}")
            null
        }
    }

    private fun findStreamInHtml(html: String, pageUrl: String): ExtractedStreamResult? {
        // Pattern 1: Direct .m3u8, .mpd, .mp4 inside sources / file / url regex
        val patterns = listOf(
            Pattern.compile("file[\"']?\\s*:\\s*[\"']([^\"']+\\.(?:m3u8|mpd|mp4)[^\"']*)[\"']", Pattern.CASE_INSENSITIVE),
            Pattern.compile("source[\"']?\\s*:\\s*[\"']([^\"']+\\.(?:m3u8|mpd|mp4)[^\"']*)[\"']", Pattern.CASE_INSENSITIVE),
            Pattern.compile("src[\"']?\\s*:\\s*[\"']([^\"']+\\.(?:m3u8|mpd|mp4)[^\"']*)[\"']", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[\"'](https?://[^\"']+\\.(?:m3u8|mpd)[^\"']*)[\"']", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[\"'](https?://[^\"']+/playlist\\.m3u8[^\"']*)[\"']", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[\"'](https?://[^\"']+/manifest\\.mpd[^\"']*)[\"']", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[\"'](https?://[^\"']+/index\\.m3u8[^\"']*)[\"']", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[\"'](https?://[^\"']+/index_web\\.mpd[^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(html)
            while (matcher.find()) {
                val found = matcher.group(1) ?: continue
                if (!isAdOrTrackerUrl(found)) {
                    val base = getBaseUrl(pageUrl)
                    return ExtractedStreamResult(
                        streamUrl = found,
                        headers = mapOf(
                            "User-Agent" to DEFAULT_UA,
                            "Referer" to pageUrl,
                            "Origin" to base
                        )
                    )
                }
            }
        }

        return null
    }

    private fun isAdOrTrackerUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("google") ||
                lower.contains("adsterra") ||
                lower.contains("doubleclick") ||
                lower.contains("analytics") ||
                lower.contains("popcash") ||
                lower.contains("syndication") ||
                lower.contains("trailer") ||
                lower.contains("preview")
    }

    private fun getBaseUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (_: Exception) {
            url
        }
    }
}

data class ExtractedStreamResult(
    val streamUrl: String,
    val headers: Map<String, String> = emptyMap()
)
