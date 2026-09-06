package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import com.example.model.BlockableChannel
import com.example.model.ChannelCheckResult
import com.example.model.ChannelStatistics
import com.example.model.ChannelStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * OfflineChannelAutoBlockManager
 *
 * Implements "অফলাইন চ্যানেল অটো-ব্লক" (Offline Channel Auto-Block):
 * - checkChannelStatus(url): Fast HEAD check with 10s timeout & error handling
 * - checkMultipleChannels(urls, onProgress): Concurrency limit 5 with memoization
 * - blockOfflineChannels(channels): Automatically flags offline/dead channels as blocked
 * - filterBlockedChannels(channels): Filters isBlocked == true
 * - unblockChannel(channelId): Restores channel to unblocked state
 * - recheckBlockedChannels(channels): Re-verifies blocked channels and unblocks if online
 * - getStatistics(channels): Generates counts and percentages
 * - LocalStorage persistence with autoSave (every 5 seconds)
 */
class OfflineChannelAutoBlockManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Dedicated OkHttpClient with 8s connect and read timeouts, following redirects
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    // In-memory memoization cache for tested URLs to avoid redundant network pings
    private val memoizedResults = ConcurrentHashMap<String, ChannelCheckResult>()

    // Blocked channel IDs set for instant cross-app lookup
    private val blockedChannelIds = ConcurrentHashMap.newKeySet<String>()

    // Auto-save management
    private val autoSaveScope = CoroutineScope(Dispatchers.IO + Job())
    private var pendingAutoSaveJob: Job? = null
    private var latestChannelsToSave: List<BlockableChannel>? = null

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow("")
    val scanStatusMessage: StateFlow<String> = _scanStatusMessage.asStateFlow()

    companion object {
        private const val TAG = "AutoBlockManager"
        private const val PREFS_NAME = "nafitv_auto_block_prefs"
        private const val KEY_BLOCKED_CHANNELS = "blocked_channels_json"
        private const val KEY_BLOCKED_IDS = "blocked_ids_set"
        private const val KEY_AUTO_BLOCK_ENABLED = "auto_block_enabled"
        private const val CONCURRENT_LIMIT = 5
        private const val AUTO_SAVE_INTERVAL_MS = 5000L

        @Volatile
        private var instance: OfflineChannelAutoBlockManager? = null

        fun getInstance(context: Context): OfflineChannelAutoBlockManager {
            return instance ?: synchronized(this) {
                instance ?: OfflineChannelAutoBlockManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        loadPersistedBlockedIds()
        startAutoSaveWorker()
    }

    private fun loadPersistedBlockedIds() {
        val savedIds = prefs.getStringSet(KEY_BLOCKED_IDS, emptySet()) ?: emptySet()
        blockedChannelIds.addAll(savedIds)
    }

    /**
     * Checks whether auto-block toggle is enabled globally
     */
    fun isAutoBlockEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_BLOCK_ENABLED, true)
    }

    fun setAutoBlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BLOCK_ENABLED, enabled).apply()
    }

    /**
     * Checks if a channel ID is marked blocked
     */
    fun isChannelBlocked(channelId: String): Boolean {
        return blockedChannelIds.contains(channelId)
    }

    // =========================================================================
    // ১. checkChannelStatus(url) & checkChannelWithFallbacks(channel)
    // =========================================================================
    /**
     * Checks a stream URL to verify if it is online or offline.
     * - Uses HEAD request with 8-second timeout
     * - Falls back to Range GET (0-4096 bytes) if HEAD returns an error code (400-503)
     * - Supports custom headers, stream-specific User-Agent, Referer, and Origin
     * - Correctly detects HLS / media stream signatures (#EXTM3U, octet-stream, etc.)
     * - Measures response time in milliseconds
     */
    suspend fun checkChannelStatus(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ChannelCheckResult = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        if (trimmed.isBlank() || !isValidStreamUrl(trimmed)) {
            return@withContext ChannelCheckResult(
                url = trimmed,
                status = ChannelStatus.OFFLINE,
                code = 0,
                reason = "Invalid URL",
                responseTime = 0L
            )
        }

        val startTime = SystemClock.elapsedRealtime()
        val lowerUrl = trimmed.lowercase()

        // Match the player's User-Agent / Referer / Origin conventions
        val effectiveUa = headers["User-Agent"]
            ?: when {
                lowerUrl.contains("toffee") || lowerUrl.contains("bldcmprod-cdn") -> "Toffee (Linux;Android 14)"
                else -> "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
            }
        val effectiveReferer = headers["Referer"]
            ?: when {
                lowerUrl.contains("toffee") || lowerUrl.contains("bldcmprod-cdn") -> "https://toffeelive.com/"
                lowerUrl.contains("hakunaymatata") || lowerUrl.contains("sacdn") -> "https://hakunaymatata.com/"
                else -> null
            }
        val effectiveOrigin = headers["Origin"]
            ?: when {
                lowerUrl.contains("toffee") || lowerUrl.contains("bldcmprod-cdn") -> "https://toffeelive.com"
                lowerUrl.contains("hakunaymatata") || lowerUrl.contains("sacdn") -> "https://hakunaymatata.com"
                else -> null
            }

        try {
            val headBuilder = Request.Builder()
                .url(trimmed)
                .head()
                .header("User-Agent", effectiveUa)
                .header("Accept", "*/*")

            if (effectiveReferer != null) headBuilder.header("Referer", effectiveReferer)
            if (effectiveOrigin != null) headBuilder.header("Origin", effectiveOrigin)
            headers.forEach { (k, v) ->
                if (!k.equals("User-Agent", ignoreCase = true) &&
                    !k.equals("Referer", ignoreCase = true) &&
                    !k.equals("Origin", ignoreCase = true)
                ) {
                    headBuilder.header(k, v)
                }
            }

            val response = httpClient.newCall(headBuilder.build()).execute()
            val elapsed = SystemClock.elapsedRealtime() - startTime
            val code = response.code
            val isOk = response.isSuccessful || code in 200..399
            response.close()

            if (isOk) {
                ChannelCheckResult(
                    url = trimmed,
                    status = ChannelStatus.ONLINE,
                    code = code,
                    reason = "OK (HTTP $code)",
                    responseTime = elapsed
                )
            } else if (code in listOf(400, 401, 403, 404, 405, 406, 416, 500, 501, 503)) {
                // Many HLS/M3U8 servers reject HEAD requests with 405 Method Not Allowed, 403, or 401.
                // Fallback to minimal range GET (first 4KB) with deep media inspection
                checkWithRangeGet(
                    url = trimmed,
                    originalStartTime = startTime,
                    originalCode = code,
                    effectiveUa = effectiveUa,
                    effectiveReferer = effectiveReferer,
                    effectiveOrigin = effectiveOrigin,
                    customHeaders = headers
                )
            } else {
                ChannelCheckResult(
                    url = trimmed,
                    status = ChannelStatus.OFFLINE,
                    code = code,
                    reason = "HTTP $code",
                    responseTime = elapsed
                )
            }
        } catch (e: Exception) {
            val elapsed = SystemClock.elapsedRealtime() - startTime
            val reason = when (e) {
                is SocketTimeoutException, is InterruptedIOException -> "Request timeout"
                is UnknownHostException, is ConnectException -> "Connection failed"
                else -> {
                    val msg = e.message.orEmpty()
                    if (msg.contains("timeout", ignoreCase = true)) "Request timeout"
                    else if (msg.contains("Unable to resolve host", ignoreCase = true)) "Connection failed"
                    else "Connection failed"
                }
            }
            ChannelCheckResult(
                url = trimmed,
                status = ChannelStatus.OFFLINE,
                code = if (reason == "Request timeout") 408 else 0,
                reason = reason,
                responseTime = elapsed
            )
        }
    }

    private fun checkWithRangeGet(
        url: String,
        originalStartTime: Long,
        originalCode: Int,
        effectiveUa: String,
        effectiveReferer: String?,
        effectiveOrigin: String?,
        customHeaders: Map<String, String>
    ): ChannelCheckResult {
        return try {
            val getBuilder = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-4096")
                .header("User-Agent", effectiveUa)
                .header("Accept", "*/*")

            if (effectiveReferer != null) getBuilder.header("Referer", effectiveReferer)
            if (effectiveOrigin != null) getBuilder.header("Origin", effectiveOrigin)
            customHeaders.forEach { (k, v) ->
                if (!k.equals("User-Agent", ignoreCase = true) &&
                    !k.equals("Referer", ignoreCase = true) &&
                    !k.equals("Origin", ignoreCase = true)
                ) {
                    getBuilder.header(k, v)
                }
            }

            val getResponse = httpClient.newCall(getBuilder.build()).execute()
            val getElapsed = SystemClock.elapsedRealtime() - originalStartTime
            val getCode = getResponse.code
            val isSuccessful = getResponse.isSuccessful || getCode in 200..399
            val contentType = getResponse.header("Content-Type")?.lowercase().orEmpty()
            val body = getResponse.body
            val bodyLength = body?.contentLength() ?: 0L
            val peekBytes = try { body?.source()?.peek()?.readByteArray(512) ?: ByteArray(0) } catch (_: Exception) { ByteArray(0) }
            val peekString = try { String(peekBytes) } catch (_: Exception) { "" }
            getResponse.close()

            val isMedia = contentType.contains("mpegurl") ||
                    contentType.contains("video") ||
                    contentType.contains("audio") ||
                    contentType.contains("octet-stream") ||
                    contentType.contains("apple") ||
                    peekString.contains("#EXTM3U", ignoreCase = true) ||
                    peekString.contains("#EXT-X", ignoreCase = true)

            // If 200-399 -> definitely online
            // If 401/403 with active media response -> server is alive and media is reachable by player
            val isChannelOnline = isSuccessful || isMedia || ((getCode == 401 || getCode == 403) && (bodyLength > 0 || peekBytes.isNotEmpty()))

            if (isChannelOnline) {
                ChannelCheckResult(
                    url = url,
                    status = ChannelStatus.ONLINE,
                    code = getCode,
                    reason = "OK (HTTP $getCode)",
                    responseTime = getElapsed
                )
            } else {
                ChannelCheckResult(
                    url = url,
                    status = ChannelStatus.OFFLINE,
                    code = getCode,
                    reason = "HTTP $getCode",
                    responseTime = getElapsed
                )
            }
        } catch (e: Exception) {
            val elapsed = SystemClock.elapsedRealtime() - originalStartTime
            val reason = when (e) {
                is SocketTimeoutException, is InterruptedIOException -> "Request timeout"
                is UnknownHostException, is ConnectException -> "Connection failed"
                else -> "HTTP $originalCode"
            }
            ChannelCheckResult(
                url = url,
                status = ChannelStatus.OFFLINE,
                code = originalCode,
                reason = reason,
                responseTime = elapsed
            )
        }
    }

    /**
     * Checks a BlockableChannel across its main stream URL and any candidate backup servers,
     * applying channel-specific headers (User-Agent, Referer, Cookies, etc.).
     * If ANY server is online, the channel is considered ONLINE!
     */
    suspend fun checkChannelWithFallbacks(channel: BlockableChannel): ChannelCheckResult = withContext(Dispatchers.IO) {
        val candidates = channel.getAllCandidateUrls()
        if (candidates.isEmpty()) {
            return@withContext ChannelCheckResult(
                url = channel.url,
                status = ChannelStatus.OFFLINE,
                code = 0,
                reason = "No stream URL",
                responseTime = 0L
            )
        }

        val effectiveHeaders = channel.getEffectiveHeaders()
        var lastOfflineResult: ChannelCheckResult? = null

        for (candUrl in candidates) {
            val res = checkChannelStatus(candUrl, effectiveHeaders)
            if (res.status == ChannelStatus.ONLINE) {
                return@withContext res
            }
            lastOfflineResult = res
        }

        lastOfflineResult ?: ChannelCheckResult(
            url = channel.url,
            status = ChannelStatus.OFFLINE,
            code = 0,
            reason = "Stream offline",
            responseTime = 0L
        )
    }

    private fun isValidStreamUrl(url: String): Boolean {
        if (url.length < 8) return false
        val lower = url.lowercase()
        return (lower.startsWith("http://") || lower.startsWith("https://") ||
                lower.startsWith("rtmp://") || lower.startsWith("rtsp://")) &&
                !lower.contains("example.com") && !lower.contains("127.0.0.1")
    }

    private fun getBrowserUserAgent(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains("toffee") || lower.contains("bldcmprod-cdn") -> "Toffee (Linux;Android 14)"
            else -> "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        }
    }

    // =========================================================================
    // ২. checkMultipleChannels(urls, onProgress)
    // =========================================================================
    /**
     * Checks multiple URLs concurrently with:
     * - Concurrency limit: 5 (Semaphore(5))
     * - Parallel execution via coroutines async/awaitAll
     * - Memoization to skip redundant checks
     * - onProgress callback reporting (current, total, result)
     */
    suspend fun checkMultipleChannels(
        urls: List<String>,
        onProgress: ((current: Int, total: Int, result: ChannelCheckResult) -> Unit)? = null
    ): List<ChannelCheckResult> = withContext(Dispatchers.IO) {
        if (urls.isEmpty()) return@withContext emptyList()

        val semaphore = Semaphore(CONCURRENT_LIMIT)
        val progressCounter = AtomicInteger(0)
        val totalCount = urls.size

        val deferredResults = urls.map { url ->
            async {
                val trimmed = url.trim()
                // Check in-memory memoization cache first
                val cached = memoizedResults[trimmed]
                if (cached != null) {
                    val current = progressCounter.incrementAndGet()
                    onProgress?.invoke(current, totalCount, cached)
                    return@async cached
                }

                // Acquire semaphore permit (max 5 concurrent checks)
                val checkResult = semaphore.withPermit {
                    checkChannelStatus(trimmed)
                }

                // Memoize result
                memoizedResults[trimmed] = checkResult

                val current = progressCounter.incrementAndGet()
                onProgress?.invoke(current, totalCount, checkResult)
                checkResult
            }
        }

        deferredResults.awaitAll()
    }

    // =========================================================================
    // ৩. blockOfflineChannels(channels)
    // =========================================================================
    /**
     * Iterates through channel list:
     * If status is 'offline' or 'dead', marks channel as:
     * - isBlocked: true
     * - status: ChannelStatus.BLOCKED
     * - blockReason: reason from check or default offline reason
     * - blockedAt: current ISO timestamp
     */
    fun blockOfflineChannels(channels: List<BlockableChannel>): List<BlockableChannel> {
        val nowIso = BlockableChannel.currentIsoDate()
        val updated = channels.map { channel ->
            if (channel.status == ChannelStatus.OFFLINE || channel.status.value.equals("dead", ignoreCase = true)) {
                val reason = channel.blockReason?.ifBlank { null }
                    ?: (if (channel.httpCode > 0) "HTTP ${channel.httpCode}" else "Stream offline")
                blockedChannelIds.add(channel.id)
                channel.copy(
                    isBlocked = true,
                    status = ChannelStatus.BLOCKED,
                    blockReason = reason,
                    blockedAt = channel.blockedAt ?: nowIso
                )
            } else {
                channel
            }
        }
        persistBlockedIds()
        triggerAutoSave(updated)
        return updated
    }

    // =========================================================================
    // ৪. filterBlockedChannels(channels)
    // =========================================================================
    /**
     * Filters and returns only the channels where isBlocked == true
     */
    fun filterBlockedChannels(channels: List<BlockableChannel>): List<BlockableChannel> {
        return channels.filter { it.isBlocked }
    }

    // =========================================================================
    // ৫. unblockChannel(channels, channelId) & unblockChannel(channelId)
    // =========================================================================
    /**
     * Unblocks a specific channel by ID:
     * - isBlocked: false
     * - status: ChannelStatus.UNKNOWN
     * - blockReason: null
     */
    fun unblockChannel(channels: List<BlockableChannel>, channelId: String): List<BlockableChannel> {
        blockedChannelIds.remove(channelId)
        persistBlockedIds()

        val updated = channels.map { channel ->
            if (channel.id == channelId) {
                channel.copy(
                    isBlocked = false,
                    status = ChannelStatus.UNKNOWN,
                    blockReason = null
                )
            } else {
                channel
            }
        }
        triggerAutoSave(updated)
        return updated
    }

    fun unblockChannelId(channelId: String) {
        blockedChannelIds.remove(channelId)
        persistBlockedIds()
    }

    fun blockChannelDirect(channel: BlockableChannel, reason: String = "Manually blocked"): BlockableChannel {
        blockedChannelIds.add(channel.id)
        persistBlockedIds()
        return channel.copy(
            isBlocked = true,
            status = ChannelStatus.BLOCKED,
            blockReason = reason,
            blockedAt = BlockableChannel.currentIsoDate()
        )
    }

    // =========================================================================
    // ৬. recheckBlockedChannels(channels, onProgress)
    // =========================================================================
    /**
     * Re-checks only channels that are currently blocked (isBlocked == true):
     * - Calls checkChannelStatus for each
     * - If online -> unblocks the channel! (isBlocked = false, status = ONLINE)
     * - If offline -> keeps blocked and updates lastChecked / reason
     */
    suspend fun recheckBlockedChannels(
        channels: List<BlockableChannel>,
        onProgress: ((current: Int, total: Int, result: ChannelCheckResult) -> Unit)? = null
    ): List<BlockableChannel> = withContext(Dispatchers.IO) {
        val blockedOnly = channels.filter { it.isBlocked }
        if (blockedOnly.isEmpty()) return@withContext channels

        val semaphore = Semaphore(CONCURRENT_LIMIT)
        val progressCounter = AtomicInteger(0)
        val totalCount = blockedOnly.size

        val resultMap = ConcurrentHashMap<String, ChannelCheckResult>()

        val jobs = blockedOnly.map { channel ->
            async {
                val result = semaphore.withPermit {
                    // Force refresh without memoized cache for re-checking candidate streams
                    checkChannelWithFallbacks(channel)
                }
                resultMap[channel.id] = result
                val cur = progressCounter.incrementAndGet()
                onProgress?.invoke(cur, totalCount, result)
            }
        }
        jobs.awaitAll()

        val nowIso = BlockableChannel.currentIsoDate()
        val updatedList = channels.map { channel ->
            val res = resultMap[channel.id]
            if (res != null) {
                if (res.status == ChannelStatus.ONLINE) {
                    blockedChannelIds.remove(channel.id)
                    channel.copy(
                        isBlocked = false,
                        status = ChannelStatus.ONLINE,
                        blockReason = null,
                        lastChecked = nowIso,
                        responseTime = res.responseTime,
                        httpCode = res.code
                    )
                } else {
                    blockedChannelIds.add(channel.id)
                    channel.copy(
                        isBlocked = true,
                        status = ChannelStatus.BLOCKED,
                        blockReason = res.reason,
                        lastChecked = nowIso,
                        responseTime = res.responseTime,
                        httpCode = res.code
                    )
                }
            } else {
                channel
            }
        }

        persistBlockedIds()
        triggerAutoSave(updatedList)
        updatedList
    }

    // =========================================================================
    // ৭. getStatistics(channels)
    // =========================================================================
    /**
     * Calculates statistics: total, online, offline, blocked, unknown, and percentages
     */
    fun getStatistics(channels: List<BlockableChannel>): ChannelStatistics {
        val total = channels.size
        if (total == 0) return ChannelStatistics()

        var onlineCount = 0
        var offlineCount = 0
        var blockedCount = 0
        var unknownCount = 0

        for (ch in channels) {
            when {
                ch.isBlocked -> blockedCount++
                ch.status == ChannelStatus.ONLINE -> onlineCount++
                ch.status == ChannelStatus.OFFLINE -> offlineCount++
                else -> unknownCount++
            }
        }

        val totalF = total.toFloat()
        return ChannelStatistics(
            total = total,
            online = onlineCount,
            offline = offlineCount,
            blocked = blockedCount,
            unknown = unknownCount,
            onlinePercentage = (onlineCount / totalF) * 100f,
            offlinePercentage = (offlineCount / totalF) * 100f,
            blockedPercentage = (blockedCount / totalF) * 100f,
            unknownPercentage = (unknownCount / totalF) * 100f
        )
    }

    // =========================================================================
    // Scan & Process Pipeline
    // =========================================================================
    /**
     * Full scan workflow:
     * 1. Checks all channels with 5-limit concurrency
     * 2. Updates statuses (ONLINE / OFFLINE)
     * 3. Automatically blocks offline channels if autoBlock is enabled
     * 4. Auto-saves results
     */
    suspend fun scanAndAutoBlock(
        channels: List<BlockableChannel>,
        autoBlockOffline: Boolean = true,
        onProgress: ((current: Int, total: Int, currentChannel: BlockableChannel) -> Unit)? = null
    ): List<BlockableChannel> = withContext(Dispatchers.IO) {
        if (channels.isEmpty()) return@withContext emptyList()

        _isScanning.value = true
        _scanProgress.value = 0f
        _scanStatusMessage.value = "স্ক্যান শুরু হচ্ছে (মোট ${channels.size} চ্যানেল)..."

        val total = channels.size
        val semaphore = Semaphore(CONCURRENT_LIMIT)
        val progress = AtomicInteger(0)
        val updatedChannelList = ConcurrentHashMap<Int, BlockableChannel>()
        val nowIso = BlockableChannel.currentIsoDate()

        val jobs = channels.mapIndexed { index, channel ->
            async {
                val res = semaphore.withPermit {
                    checkChannelWithFallbacks(channel)
                }

                val cur = progress.incrementAndGet()
                val isOffline = res.status == ChannelStatus.OFFLINE
                val shouldBlock = isOffline && autoBlockOffline

                val updated = if (shouldBlock) {
                    blockedChannelIds.add(channel.id)
                    channel.copy(
                        status = ChannelStatus.BLOCKED,
                        isBlocked = true,
                        blockReason = res.reason,
                        blockedAt = channel.blockedAt ?: nowIso,
                        lastChecked = nowIso,
                        responseTime = res.responseTime,
                        httpCode = res.code
                    )
                } else {
                    if (channel.isBlocked && res.status == ChannelStatus.ONLINE) {
                        blockedChannelIds.remove(channel.id)
                    }
                    channel.copy(
                        status = res.status,
                        isBlocked = if (res.status == ChannelStatus.ONLINE) false else channel.isBlocked,
                        blockReason = if (res.status == ChannelStatus.ONLINE) null else channel.blockReason ?: res.reason,
                        lastChecked = nowIso,
                        responseTime = res.responseTime,
                        httpCode = res.code
                    )
                }

                updatedChannelList[index] = updated
                _scanProgress.value = cur.toFloat() / total
                _scanStatusMessage.value = "চেক করা হচ্ছে: $cur/$total - ${channel.name}"
                onProgress?.invoke(cur, total, updated)
            }
        }

        jobs.awaitAll()

        val resultList = (0 until total).mapNotNull { updatedChannelList[it] }

        persistBlockedIds()
        triggerAutoSave(resultList)

        _isScanning.value = false
        _scanStatusMessage.value = "স্ক্যান সম্পন্ন! মোট $total চ্যানেল যাচাই করা হয়েছে।"
        resultList
    }

    // =========================================================================
    // স্টোরেজ ম্যানেজমেন্ট (Storage Management)
    // =========================================================================
    /**
     * saveToLocalStorage(channels): Persists channels and their blocked states to SharedPreferences
     */
    fun saveToLocalStorage(channels: List<BlockableChannel>) {
        try {
            val jsonArray = JSONArray()
            for (ch in channels) {
                val obj = JSONObject().apply {
                    put("id", ch.id)
                    put("name", ch.name)
                    put("url", ch.url)
                    put("logo", ch.logo ?: "")
                    put("group", ch.group ?: "")
                    put("status", ch.status.value)
                    put("isBlocked", ch.isBlocked)
                    put("blockReason", ch.blockReason ?: "")
                    put("blockedAt", ch.blockedAt ?: "")
                    put("lastChecked", ch.lastChecked ?: "")
                    put("responseTime", ch.responseTime)
                    put("httpCode", ch.httpCode)
                    if (ch.backupUrls.isNotEmpty()) {
                        val backupsArr = JSONArray()
                        ch.backupUrls.forEach { backupsArr.put(it) }
                        put("backupUrls", backupsArr)
                    }
                    ch.userAgent?.let { put("userAgent", it) }
                    ch.referrer?.let { put("referrer", it) }
                    ch.origin?.let { put("origin", it) }
                    ch.cookie?.let { put("cookie", it) }
                }
                jsonArray.put(obj)
            }
            prefs.edit()
                .putString(KEY_BLOCKED_CHANNELS, jsonArray.toString())
                .apply()
            Log.d(TAG, "Saved ${channels.size} channels to local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving channels to local storage", e)
        }
    }

    /**
     * loadFromLocalStorage(): Loads saved channels and blocked statuses from SharedPreferences
     */
    fun loadFromLocalStorage(): List<BlockableChannel> {
        val jsonStr = prefs.getString(KEY_BLOCKED_CHANNELS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<BlockableChannel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "")
                val isBlocked = obj.optBoolean("isBlocked", false) || blockedChannelIds.contains(id)

                val backupsList = mutableListOf<String>()
                val backupsArr = obj.optJSONArray("backupUrls")
                if (backupsArr != null) {
                    for (bIdx in 0 until backupsArr.length()) {
                        val bUrl = backupsArr.optString(bIdx, "")
                        if (bUrl.isNotBlank()) backupsList.add(bUrl)
                    }
                }

                list.add(
                    BlockableChannel(
                        id = id,
                        name = obj.optString("name", "Channel"),
                        url = obj.optString("url", ""),
                        logo = obj.optString("logo").takeIf { it.isNotBlank() },
                        group = obj.optString("group").takeIf { it.isNotBlank() },
                        status = if (isBlocked) ChannelStatus.BLOCKED else ChannelStatus.fromString(obj.optString("status")),
                        isBlocked = isBlocked,
                        blockReason = obj.optString("blockReason").takeIf { it.isNotBlank() },
                        blockedAt = obj.optString("blockedAt").takeIf { it.isNotBlank() },
                        lastChecked = obj.optString("lastChecked").takeIf { it.isNotBlank() },
                        responseTime = obj.optLong("responseTime", 0L),
                        httpCode = obj.optInt("httpCode", 0),
                        backupUrls = backupsList,
                        userAgent = obj.optString("userAgent").takeIf { it.isNotBlank() },
                        referrer = obj.optString("referrer").takeIf { it.isNotBlank() },
                        origin = obj.optString("origin").takeIf { it.isNotBlank() },
                        cookie = obj.optString("cookie").takeIf { it.isNotBlank() }
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading channels from local storage", e)
            emptyList()
        }
    }

    /**
     * autoSave(): Auto-saves every 5 seconds when updates are triggered
     */
    fun triggerAutoSave(channels: List<BlockableChannel>) {
        latestChannelsToSave = channels
        if (pendingAutoSaveJob?.isActive != true) {
            pendingAutoSaveJob = autoSaveScope.launch {
                delay(AUTO_SAVE_INTERVAL_MS)
                latestChannelsToSave?.let {
                    saveToLocalStorage(it)
                }
            }
        }
    }

    private fun startAutoSaveWorker() {
        autoSaveScope.launch {
            while (isActive) {
                delay(AUTO_SAVE_INTERVAL_MS)
                latestChannelsToSave?.let {
                    saveToLocalStorage(it)
                    latestChannelsToSave = null
                }
            }
        }
    }

    private fun persistBlockedIds() {
        prefs.edit().putStringSet(KEY_BLOCKED_IDS, blockedChannelIds).apply()
    }

    fun clearMemoizedCache() {
        memoizedResults.clear()
    }

    // =========================================================================
    // এক্সপোর্ট বাটন ফাংশনালিটি (Export Formats)
    // =========================================================================
    /**
     * Exports blocked channels to formatted JSON string
     */
    fun exportBlockedChannelsJson(channels: List<BlockableChannel>): String {
        val blocked = filterBlockedChannels(channels)
        val array = JSONArray()
        for (ch in blocked) {
            val obj = JSONObject().apply {
                put("id", ch.id)
                put("name", ch.name)
                put("url", ch.url)
                put("group", ch.group ?: "Default")
                put("status", ch.status.value)
                put("isBlocked", ch.isBlocked)
                put("blockReason", ch.blockReason ?: "Offline")
                put("blockedAt", ch.blockedAt ?: "")
                put("lastChecked", ch.lastChecked ?: "")
                put("httpCode", ch.httpCode)
                put("responseTimeMs", ch.responseTime)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    /**
     * Exports blocked channels to CSV string
     */
    fun exportBlockedChannelsCsv(channels: List<BlockableChannel>): String {
        val blocked = filterBlockedChannels(channels)
        val sb = StringBuilder()
        sb.append("ID,Name,URL,Group,Status,BlockReason,BlockedAt,LastChecked,HttpCode,ResponseTimeMs\n")
        for (ch in blocked) {
            val safeName = "\"${ch.name.replace("\"", "\"\"")}\""
            val safeUrl = "\"${ch.url.replace("\"", "\"\"")}\""
            val safeGroup = "\"${(ch.group ?: "").replace("\"", "\"\"")}\""
            val safeReason = "\"${(ch.blockReason ?: "Offline").replace("\"", "\"\"")}\""
            sb.append("${ch.id},$safeName,$safeUrl,$safeGroup,${ch.status.value},$safeReason,${ch.blockedAt.orEmpty()},${ch.lastChecked.orEmpty()},${ch.httpCode},${ch.responseTime}\n")
        }
        return sb.toString()
    }
}
