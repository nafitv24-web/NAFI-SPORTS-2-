package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.MediaItem
import com.example.model.StreamServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ChannelStatusManager: Tracks working / inactive channels dynamically.
 * Remembers validated channels and auto-verifies channels smoothly in the background
 * without freezing, lagging, or crashing devices of any specification.
 */
object ChannelStatusManager {

    private const val PREFS_NAME = "nafitv_channel_status"
    private const val KEY_FAILED_CHANNELS = "failed_channel_ids"
    private const val KEY_VERIFIED_ACTIVE = "verified_channel_ids"
    private const val KEY_FAILED_SERVERS = "failed_server_urls"
    private const val KEY_ONLY_ACTIVE_ENABLED = "only_active_enabled"

    private val workingStatusMap = ConcurrentHashMap<String, Boolean>()
    private val failedServerUrls = ConcurrentHashMap.newKeySet<String>()
    private val verifiedTitles = ConcurrentHashMap.newKeySet<String>()
    private val failedTitles = ConcurrentHashMap.newKeySet<String>()
    private val probingIds = ConcurrentHashMap.newKeySet<String>()
    private val probedIds = ConcurrentHashMap.newKeySet<String>()
    private val probeQueue = ConcurrentLinkedQueue<MediaItem>()
    private val isWorkerRunning = AtomicBoolean(false)
    private var lastTickTime = 0L

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _statusUpdateTick = MutableStateFlow(0L)
    val statusUpdateTick: StateFlow<Long> = _statusUpdateTick.asStateFlow()

    private var prefs: SharedPreferences? = null

    // Dedicated lightweight, zero-leak client for stream reachability checks
    // Lenient SSL and generous timeouts to prevent false offline detection on slow/custom stream servers
    private val checkClient by lazy {
        val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
            object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            }
        )
        val sslContext = javax.net.ssl.SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .callTimeout(8000, TimeUnit.MILLISECONDS)
            .connectTimeout(5000, TimeUnit.MILLISECONDS)
            .readTimeout(5000, TimeUnit.MILLISECONDS)
            .connectionPool(okhttp3.ConnectionPool(4, 15, TimeUnit.SECONDS))
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadPersistedStatuses()
        }
    }

    /**
     * Get persisted "Only Active" toggle state.
     */
    fun isOnlyActiveEnabled(): Boolean {
        return prefs?.getBoolean(KEY_ONLY_ACTIVE_ENABLED, false) ?: false
    }

    /**
     * Persist user's choice for "Only Active" toggle.
     */
    fun setOnlyActiveEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_ONLY_ACTIVE_ENABLED, enabled)?.apply()
        _statusUpdateTick.value = System.currentTimeMillis()
    }

    private fun loadPersistedStatuses() {
        val p = prefs ?: return
        // Reset old speculative failed caches completely so no playable channel is falsely marked offline
        p.edit()
            .remove(KEY_FAILED_CHANNELS)
            .remove(KEY_FAILED_SERVERS)
            .apply()

        workingStatusMap.clear()
        failedServerUrls.clear()
        failedTitles.clear()

        val verifiedSet = p.getStringSet(KEY_VERIFIED_ACTIVE, emptySet()) ?: emptySet()
        for (id in verifiedSet) {
            workingStatusMap[id] = true
        }
    }

    /**
     * Checks whether a specific server URL is active and not marked broken.
     */
    fun isServerActive(serverUrl: String): Boolean {
        val trimmed = serverUrl.trim()
        if (trimmed.isBlank()) return false
        val clean = try { DrmHelper.extractStreamInfo(trimmed).cleanUrl } catch (_: Exception) { trimmed }
        if (failedServerUrls.contains(clean)) return false
        return isValidStreamFormat(clean)
    }

    /**
     * Mark a specific server URL as failed/inactive.
     */
    fun markServerFailed(serverUrl: String) {
        val trimmed = serverUrl.trim()
        if (trimmed.isBlank()) return
        val clean = try { DrmHelper.extractStreamInfo(trimmed).cleanUrl } catch (_: Exception) { trimmed }
        failedServerUrls.add(clean)
        saveFailedServerToPrefs(clean)
    }

    /**
     * Mark a specific server URL as active/working (re-enable).
     */
    fun markServerSuccess(serverUrl: String) {
        val trimmed = serverUrl.trim()
        if (trimmed.isBlank()) return
        val clean = try { DrmHelper.extractStreamInfo(trimmed).cleanUrl } catch (_: Exception) { trimmed }
        if (failedServerUrls.remove(clean)) {
            removeFailedServerFromPrefs(clean)
        }
    }

    /**
     * Returns the active servers for a media item, filtering out inactive/broken servers
     * unless all servers would be filtered out (in which case it keeps valid format ones as fallback).
     */
    fun getActiveServers(mediaItem: MediaItem): List<StreamServer> {
        val allServers = mediaItem.getAllServers()
        if (allServers.isEmpty()) return emptyList()

        val active = allServers.filter { isServerActive(it.url) }
        return if (active.isNotEmpty()) {
            active
        } else {
            allServers.filter { isValidStreamFormat(it.url) }.ifEmpty { allServers }
        }
    }

    /**
     * Normalize channel title for matching across different sources (e.g. "[BD] Somoy TV" vs "Somoy TV Live")
     */
    fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("""\[.*?\]"""), "") // Remove [BD], [HD], etc.
            .replace(Regex("""\b(live|tv|news|hd|sd|bd)\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[^a-z0-9\u0980-\u09FF]"""), "") // keep alphanumeric and Bengali
            .trim()
    }

    /**
     * Checks whether a channel is considered currently active and working.
     * Only channels that explicitly fail during actual playback or have invalid URLs are offline.
     * Playable channels are NEVER shown offline.
     */
    fun isChannelActive(channel: MediaItem): Boolean {
        // 1. Explicitly recorded status (true = active, false = failed in player)
        val recordedStatus = workingStatusMap[channel.id]
        if (recordedStatus == false) {
            return false
        }
        val normTitle = normalizeTitle(channel.title)
        if (normTitle.isNotBlank() && failedTitles.contains(normTitle)) {
            return false
        }
        if (recordedStatus == true || (normTitle.isNotBlank() && verifiedTitles.contains(normTitle))) {
            return true
        }

        // 2. Validate stream URL format
        val mainUrl = channel.streamUrl.trim()
        val allServers = channel.getAllServers()

        if (mainUrl.isBlank() && allServers.isEmpty()) {
            return false
        }

        val primaryUrl = mainUrl.ifBlank { allServers.firstOrNull()?.url.orEmpty() }.trim()
        val cleanPrimary = try { DrmHelper.extractStreamInfo(primaryUrl).cleanUrl } catch (_: Exception) { primaryUrl }
        if (!isValidStreamFormat(cleanPrimary)) {
            return false
        }

        // 3. If all servers in the channel have failed during playback
        if (allServers.isNotEmpty()) {
            val hasValidServer = allServers.any {
                val clean = try { DrmHelper.extractStreamInfo(it.url).cleanUrl } catch (_: Exception) { it.url.trim() }
                isValidStreamFormat(clean) && !failedServerUrls.contains(clean)
            }
            if (!hasValidServer) return false
        } else if (failedServerUrls.contains(cleanPrimary)) {
            return false
        }

        // By default, every valid channel is considered ACTIVE so it is NEVER falsely shown as offline
        return true
    }

    /**
     * Enqueues channels for lightweight, non-blocking background health check.
     * Opportunistically discovers verified working channels.
     * CRITICAL: Never marks channels offline from background probe failure!
     */
    fun enqueueChannelsForProbing(items: List<MediaItem>) {
        if (items.isEmpty()) return
        val unverified = items.filter { item ->
            item.id.isNotBlank() && !workingStatusMap.containsKey(item.id) && probedIds.add(item.id)
        }
        if (unverified.isEmpty()) return

        probeQueue.addAll(unverified)

        if (isWorkerRunning.compareAndSet(false, true)) {
            managerScope.launch {
                try {
                    val batchResults = mutableMapOf<String, Boolean>()
                    var processedInBatch = 0

                    while (true) {
                        val nextItem = probeQueue.poll() ?: break
                        probingIds.add(nextItem.id)
                        try {
                            val isWorking = checkChannelReachability(nextItem)
                            if (isWorking) {
                                workingStatusMap[nextItem.id] = true
                                val norm = normalizeTitle(nextItem.title)
                                if (norm.isNotBlank()) {
                                    verifiedTitles.add(norm)
                                    failedTitles.remove(norm)
                                }
                                batchResults[nextItem.id] = true
                                processedInBatch++
                            }
                            // NOTE: If isWorking == false, we DO NOT mark it failed!
                            // Speculative background probes often fail on protected/Akamai/tokenized streams
                            // that ExoPlayer plays with ease. Only actual playback failure marks channels offline.

                            val now = System.currentTimeMillis()
                            // Debounce UI update tick to at least 1.5 seconds to prevent recomposition stutter
                            if (processedInBatch >= 6 || (now - lastTickTime) >= 1500L) {
                                if (batchResults.isNotEmpty()) {
                                    batchSaveVerifiedToPrefs(batchResults.keys)
                                    batchResults.clear()
                                }
                                processedInBatch = 0
                                lastTickTime = now
                                _statusUpdateTick.value = now
                            }
                        } catch (e: Exception) {
                            Log.w("ChannelStatusManager", "Probe error for ${nextItem.title}", e)
                        } finally {
                            probingIds.remove(nextItem.id)
                        }
                        kotlinx.coroutines.delay(100) // 100ms throttle keeps device cool and network free
                    }

                    if (batchResults.isNotEmpty()) {
                        batchSaveVerifiedToPrefs(batchResults.keys)
                        lastTickTime = System.currentTimeMillis()
                        _statusUpdateTick.value = lastTickTime
                    }
                } finally {
                    isWorkerRunning.set(false)
                }
            }
        }
    }

    /**
     * Backwards-compatible caller for probing unverified channels.
     */
    fun probeChannelsAsync(scope: CoroutineScope, items: List<MediaItem>) {
        enqueueChannelsForProbing(items)
    }

    private fun checkChannelReachability(item: MediaItem): Boolean {
        val allServers = item.getAllServers()
        val mainUrl = item.streamUrl.trim()

        if (mainUrl.isBlank() && allServers.isEmpty()) {
            return false
        }

        // Collect all distinct stream URLs
        val urlsToTest = mutableListOf<String>()
        if (mainUrl.isNotBlank()) {
            val clean = try { DrmHelper.extractStreamInfo(mainUrl).cleanUrl } catch (_: Exception) { mainUrl }
            if (isValidStreamFormat(clean)) {
                urlsToTest.add(mainUrl)
            }
        }
        for (server in allServers) {
            val sUrl = server.url.trim()
            if (sUrl.isNotBlank()) {
                val clean = try { DrmHelper.extractStreamInfo(sUrl).cleanUrl } catch (_: Exception) { sUrl }
                if (isValidStreamFormat(clean) && !urlsToTest.contains(sUrl)) {
                    urlsToTest.add(sUrl)
                }
            }
        }

        if (urlsToTest.isEmpty()) {
            return false
        }

        // Extract channel-specific headers to avoid false 403 Forbidden on token/referer-protected channels
        val channelHeaders = mutableMapOf<String, String>()
        item.userAgent?.takeIf { it.isNotBlank() }?.let { channelHeaders["User-Agent"] = it }
        item.referrer?.takeIf { it.isNotBlank() }?.let { channelHeaders["Referer"] = it }
        item.origin?.takeIf { it.isNotBlank() }?.let { channelHeaders["Origin"] = it }
        item.cookie?.takeIf { it.isNotBlank() }?.let { channelHeaders["Cookie"] = it }
        item.customHeaders?.let { channelHeaders.putAll(it) }

        var anyServerWorking = false
        for (url in urlsToTest) {
            val ok = testSingleStreamUrl(url, channelHeaders)
            if (ok) {
                anyServerWorking = true
                markServerSuccess(url)
                break // If any server works, channel is active
            }
            // CRITICAL: NEVER markServerFailed here! Background probes can fail on CDNs
            // that ExoPlayer plays without issue. Only player playback failure marks servers failed.
        }

        return anyServerWorking
    }

    private fun testSingleStreamUrl(url: String, extraHeaders: Map<String, String> = emptyMap()): Boolean {
        val streamInfo = try { DrmHelper.extractStreamInfo(url) } catch (_: Exception) { null }
        val cleanUrl = streamInfo?.cleanUrl ?: url.trim()
        if (!isValidStreamFormat(cleanUrl)) return false

        val combinedHeaders = mutableMapOf<String, String>()
        combinedHeaders.putAll(extraHeaders)
        if (streamInfo != null) {
            combinedHeaders.putAll(streamInfo.headers)
        }

        val lowerUrl = cleanUrl.lowercase()
        val isToffee = lowerUrl.contains("toffee") || lowerUrl.contains("bldcmprod-cdn")
        val isHakuna = lowerUrl.contains("hakunaymatata") || lowerUrl.contains("sacdn")

        // Determine appropriate User-Agent & Referer for known streaming networks
        val effectiveUa = combinedHeaders["User-Agent"]
            ?: when {
                isToffee -> "Toffee (Linux;Android 14)"
                lowerUrl.contains("nagorik") -> "Nagorik/1.0 (Android)"
                else -> "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            }
        val effectiveReferer = combinedHeaders["Referer"]
            ?: when {
                isToffee -> "https://toffeelive.com/"
                isHakuna -> "https://hakunaymatata.com/"
                else -> null
            }
        val effectiveOrigin = combinedHeaders["Origin"]
            ?: when {
                isToffee -> "https://toffeelive.com"
                isHakuna -> "https://hakunaymatata.com"
                else -> null
            }

        return try {
            val getBuilder = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", effectiveUa)
                .header("Accept", "*/*")
                .header("Connection", "close")

            if (effectiveReferer != null) getBuilder.header("Referer", effectiveReferer)
            if (effectiveOrigin != null) getBuilder.header("Origin", effectiveOrigin)
            combinedHeaders.forEach { (k, v) ->
                if (!k.equals("User-Agent", ignoreCase = true) &&
                    !k.equals("Referer", ignoreCase = true) &&
                    !k.equals("Origin", ignoreCase = true) &&
                    !k.equals("Connection", ignoreCase = true) &&
                    !k.equals("Range", ignoreCase = true)
                ) {
                    getBuilder.header(k, v)
                }
            }

            val getCall = checkClient.newCall(getBuilder.build())
            val getResp = getCall.execute()
            getResp.use { resp ->
                val getCode = resp.code
                val getContentType = resp.header("Content-Type")?.lowercase() ?: ""

                // 200 OK, 206 Partial Content, 3xx Redirects, 416 Range Not Satisfiable (stream resource exists)
                if (getCode in 200..399 || getCode == 416) {
                    return true
                }

                // 401/403: Verify if it contains media content-type or EXTM3U
                if (getCode == 401 || getCode == 403) {
                    val body = resp.body
                    val bodyBytes = try { body?.source()?.peek()?.readByteArray(256) ?: ByteArray(0) } catch (_: Exception) { ByteArray(0) }
                    val isMediaContent = getContentType.contains("mpegurl") ||
                            getContentType.contains("video") ||
                            getContentType.contains("audio") ||
                            getContentType.contains("octet-stream") ||
                            getContentType.contains("vnd.apple") ||
                            String(bodyBytes).contains("#EXTM3U", ignoreCase = true)
                    if (isMediaContent) {
                        return true
                    }
                }

                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidStreamFormat(url: String): Boolean {
        if (url.isBlank() || url.length < 8) return false
        val lower = url.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://") && !lower.startsWith("rtmp://") && !lower.startsWith("rtsp://")) {
            return false
        }
        if (lower.contains("dummy") || lower.contains("placeholder") || lower.contains("127.0.0.1") || lower.contains("example.com") || lower.endsWith("#")) {
            return false
        }
        return true
    }

    /**
     * Mark a channel as working successfully (called when playback starts or frames render)
     */
    fun markChannelSuccess(channelId: String, channelTitle: String? = null, streamUrl: String? = null) {
        if (channelId.isNotBlank()) {
            workingStatusMap[channelId] = true
            saveStatusToPrefs(channelId, true)
        }
        if (!channelTitle.isNullOrBlank()) {
            val norm = normalizeTitle(channelTitle)
            if (norm.isNotBlank()) {
                verifiedTitles.add(norm)
                failedTitles.remove(norm)
            }
        }
        if (!streamUrl.isNullOrBlank()) {
            markServerSuccess(streamUrl)
        }
        _statusUpdateTick.value = System.currentTimeMillis()
    }

    /**
     * Mark a channel as failed/broken (called when playback fails and all servers fail)
     */
    fun markChannelFailed(channelId: String, channelTitle: String? = null, streamUrl: String? = null) {
        if (channelId.isNotBlank()) {
            workingStatusMap[channelId] = false
            saveStatusToPrefs(channelId, false)
        }
        if (!channelTitle.isNullOrBlank()) {
            val norm = normalizeTitle(channelTitle)
            if (norm.isNotBlank()) {
                failedTitles.add(norm)
                verifiedTitles.remove(norm)
            }
        }
        if (!streamUrl.isNullOrBlank()) {
            markServerFailed(streamUrl)
        }
        _statusUpdateTick.value = System.currentTimeMillis()
    }

    /**
     * Reset a channel's failed status so it can be re-tested
     */
    fun resetChannelStatus(channelId: String) {
        workingStatusMap.remove(channelId)
        _statusUpdateTick.value = System.currentTimeMillis()
        prefs?.let { p ->
            val failedSet = p.getStringSet(KEY_FAILED_CHANNELS, emptySet())?.toMutableSet() ?: mutableSetOf()
            if (failedSet.remove(channelId)) {
                p.edit().putStringSet(KEY_FAILED_CHANNELS, failedSet).apply()
            }
        }
    }

    /**
     * Reset all status marks
     */
    fun resetAllStatuses() {
        workingStatusMap.clear()
        verifiedTitles.clear()
        failedTitles.clear()
        probedIds.clear()
        probeQueue.clear()
        _statusUpdateTick.value = System.currentTimeMillis()
        prefs?.edit()?.clear()?.apply()
    }

    private fun saveFailedServerToPrefs(serverUrl: String) {
        val p = prefs ?: return
        try {
            val curSet = p.getStringSet(KEY_FAILED_SERVERS, emptySet())?.toMutableSet() ?: mutableSetOf()
            curSet.add(serverUrl)
            p.edit().putStringSet(KEY_FAILED_SERVERS, curSet).apply()
        } catch (e: Exception) {
            Log.w("ChannelStatusManager", "Failed to save failed server status", e)
        }
    }

    private fun removeFailedServerFromPrefs(serverUrl: String) {
        val p = prefs ?: return
        try {
            val curSet = p.getStringSet(KEY_FAILED_SERVERS, emptySet())?.toMutableSet() ?: mutableSetOf()
            if (curSet.remove(serverUrl)) {
                p.edit().putStringSet(KEY_FAILED_SERVERS, curSet).apply()
            }
        } catch (e: Exception) {
            Log.w("ChannelStatusManager", "Failed to remove failed server status", e)
        }
    }

    private fun saveStatusToPrefs(channelId: String, isSuccess: Boolean) {
        val p = prefs ?: return
        try {
            val key = if (isSuccess) KEY_VERIFIED_ACTIVE else KEY_FAILED_CHANNELS
            val otherKey = if (isSuccess) KEY_FAILED_CHANNELS else KEY_VERIFIED_ACTIVE
            val curSet = p.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
            val otherSet = p.getStringSet(otherKey, emptySet())?.toMutableSet() ?: mutableSetOf()

            otherSet.remove(channelId)
            curSet.add(channelId)

            p.edit()
                .putStringSet(key, curSet)
                .putStringSet(otherKey, otherSet)
                .apply()
        } catch (e: Exception) {
            Log.w("ChannelStatusManager", "Failed to save channel status", e)
        }
    }

    private fun batchSaveVerifiedToPrefs(verifiedIds: Collection<String>) {
        if (verifiedIds.isEmpty()) return
        val p = prefs ?: return
        try {
            val verifiedSet = p.getStringSet(KEY_VERIFIED_ACTIVE, emptySet())?.toMutableSet() ?: mutableSetOf()
            val failedSet = p.getStringSet(KEY_FAILED_CHANNELS, emptySet())?.toMutableSet() ?: mutableSetOf()

            verifiedIds.forEach { id ->
                failedSet.remove(id)
                verifiedSet.add(id)
            }

            p.edit()
                .putStringSet(KEY_VERIFIED_ACTIVE, verifiedSet)
                .putStringSet(KEY_FAILED_CHANNELS, failedSet)
                .apply()
        } catch (e: Exception) {
            Log.w("ChannelStatusManager", "Failed to batch save verified statuses", e)
        }
    }
}

