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

    // Dedicated OkHttpClient with 10s connect, read, and call timeouts
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
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
    // ১. checkChannelStatus(url)
    // =========================================================================
    /**
     * Checks a stream URL to verify if it is online or offline.
     * - Uses HEAD request with 10-second timeout
     * - Measures response time in milliseconds
     * - Maps errors cleanly:
     *   * Network error -> 'Connection failed'
     *   * Timeout -> 'Request timeout'
     *   * HTTP error -> 'HTTP 404', 'HTTP 500', etc.
     *   * Invalid URL -> 'Invalid URL'
     */
    suspend fun checkChannelStatus(url: String): ChannelCheckResult = withContext(Dispatchers.IO) {
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
        try {
            val request = Request.Builder()
                .url(trimmed)
                .head()
                .header("User-Agent", getBrowserUserAgent(trimmed))
                .header("Accept", "*/*")
                .build()

            val response = httpClient.newCall(request).execute()
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
            } else if (code in listOf(400, 401, 403, 405, 416, 500, 503)) {
                // Many HLS/M3U8 servers reject HEAD requests with 405 Method Not Allowed or 403.
                // Fallback to minimal range GET (first 1KB) to confirm stream reachability
                checkWithRangeGet(trimmed, startTime, code)
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

    private fun checkWithRangeGet(url: String, originalStartTime: Long, originalCode: Int): ChannelCheckResult {
        return try {
            val getRequest = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-1024")
                .header("User-Agent", getBrowserUserAgent(url))
                .header("Accept", "*/*")
                .build()

            val getResponse = httpClient.newCall(getRequest).execute()
            val getElapsed = SystemClock.elapsedRealtime() - originalStartTime
            val getCode = getResponse.code
            val isSuccessful = getResponse.isSuccessful || getCode in 200..399
            val contentType = getResponse.header("Content-Type")?.lowercase().orEmpty()
            val body = getResponse.body
            val peekBytes = try { body?.source()?.peek()?.readByteArray(256) ?: ByteArray(0) } catch (_: Exception) { ByteArray(0) }
            getResponse.close()

            val isMedia = contentType.contains("mpegurl") ||
                    contentType.contains("video") ||
                    contentType.contains("audio") ||
                    contentType.contains("octet-stream") ||
                    contentType.contains("apple") ||
                    String(peekBytes).contains("#EXTM3U", ignoreCase = true)

            if (isSuccessful || isMedia) {
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
                    // Force refresh without memoized cache for re-checking
                    checkChannelStatus(channel.url)
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
                    checkChannelStatus(channel.url)
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
                        httpCode = obj.optInt("httpCode", 0)
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
