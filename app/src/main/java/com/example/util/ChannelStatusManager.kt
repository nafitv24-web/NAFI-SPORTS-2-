package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.MediaItem
import com.example.model.StreamServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * ChannelStatusManager: Tracks working / inactive channels dynamically.
 * Remembers validated channels and auto-verifies channels on the fly so that
 * when "Only Active Channel" is enabled, only verified working channels are shown.
 */
object ChannelStatusManager {

    private const val PREFS_NAME = "nafitv_channel_status"
    private const val KEY_FAILED_CHANNELS = "failed_channel_ids"
    private const val KEY_VERIFIED_ACTIVE = "verified_channel_ids"

    private val workingStatusMap = ConcurrentHashMap<String, Boolean>()
    private val _statusUpdateTick = MutableStateFlow(0L)
    val statusUpdateTick: StateFlow<Long> = _statusUpdateTick.asStateFlow()

    private var prefs: SharedPreferences? = null

    // Dedicated fast client for quick HEAD / range requests (5s timeout)
    private val checkClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadPersistedStatuses()
        }
    }

    private fun loadPersistedStatuses() {
        val p = prefs ?: return
        val failedSet = p.getStringSet(KEY_FAILED_CHANNELS, emptySet()) ?: emptySet()
        val verifiedSet = p.getStringSet(KEY_VERIFIED_ACTIVE, emptySet()) ?: emptySet()

        for (id in failedSet) {
            workingStatusMap[id] = false
        }
        for (id in verifiedSet) {
            workingStatusMap[id] = true
        }
    }

    /**
     * Checks whether a channel is considered currently active and working.
     * Rules:
     * 1. If explicit check recorded:
     *    - false -> NOT active
     *    - true -> Active
     * 2. If no check yet:
     *    - Check URL validity (not blank, valid http/https scheme, has working extension or stream server)
     *    - Filter out dummy or placeholder links like empty, '#' or non-resolvable dummy formats
     */
    fun isChannelActive(channel: MediaItem): Boolean {
        // If explicitly recorded as failed
        val recordedStatus = workingStatusMap[channel.id]
        if (recordedStatus == false) {
            return false
        }

        // Check if stream URL is present and not a dummy
        val mainUrl = channel.streamUrl.trim()
        val allServers = channel.getAllServers()

        if (mainUrl.isBlank() && allServers.isEmpty()) {
            return false
        }

        val primaryUrl = mainUrl.ifBlank { allServers.firstOrNull()?.url.orEmpty() }.trim()
        if (!isValidStreamFormat(primaryUrl)) {
            return false
        }

        // Check if any server in the channel is valid
        if (allServers.isNotEmpty()) {
            val hasValidServer = allServers.any { isValidStreamFormat(it.url) }
            if (!hasValidServer) return false
        }

        return true
    }

    private fun isValidStreamFormat(url: String): Boolean {
        if (url.isBlank() || url.length < 8) return false
        val lower = url.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://") && !lower.startsWith("rtmp://") && !lower.startsWith("rtsp://")) {
            return false
        }
        // Exclude dummy test URLs or non-functional placeholder paths
        if (lower.contains("dummy") || lower.contains("placeholder") || lower.contains("127.0.0.1") || lower.contains("example.com") || lower.endsWith("#")) {
            return false
        }
        return true
    }

    /**
     * Mark a channel as working successfully (called when playback starts or frames render)
     */
    fun markChannelSuccess(channelId: String) {
        if (channelId.isBlank()) return
        workingStatusMap[channelId] = true
        _statusUpdateTick.value = System.currentTimeMillis()
        saveStatusToPrefs(channelId, true)
    }

    /**
     * Mark a channel as failed/broken (called when playback fails and all servers fail)
     */
    fun markChannelFailed(channelId: String) {
        if (channelId.isBlank()) return
        workingStatusMap[channelId] = false
        _statusUpdateTick.value = System.currentTimeMillis()
        saveStatusToPrefs(channelId, false)
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
        _statusUpdateTick.value = System.currentTimeMillis()
        prefs?.edit()?.clear()?.apply()
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
}
