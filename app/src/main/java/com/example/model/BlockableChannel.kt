package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Status of an M3U/M3U8 streaming channel
 */
enum class ChannelStatus(val value: String) {
    ONLINE("online"),
    OFFLINE("offline"),
    BLOCKED("blocked"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(str: String?): ChannelStatus {
            return when (str?.trim()?.lowercase()) {
                "online" -> ONLINE
                "offline", "dead" -> OFFLINE
                "blocked" -> BLOCKED
                else -> UNKNOWN
            }
        }
    }
}

/**
 * M3U/M3U8 Channel object for Auto-Block management
 */
data class BlockableChannel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val status: ChannelStatus = ChannelStatus.UNKNOWN,
    val isBlocked: Boolean = false,
    val blockReason: String? = null,
    val blockedAt: String? = null, // ISO-8601 timestamp string
    val lastChecked: String? = null, // ISO-8601 timestamp string
    val responseTime: Long = 0L, // in milliseconds
    val httpCode: Int = 0,
    val backupUrls: List<String> = emptyList(),
    val userAgent: String? = null,
    val referrer: String? = null,
    val origin: String? = null,
    val cookie: String? = null,
    val customHeaders: Map<String, String>? = null
) {
    companion object {
        fun currentIsoDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date())
        }

        fun fromMediaItem(item: MediaItem, isBlocked: Boolean = false): BlockableChannel {
            val streamUrl = item.streamUrl.ifBlank {
                item.servers.firstOrNull()?.url.orEmpty()
            }
            val allServers = item.getAllServers()
            val extraUrls = allServers
                .map { it.url.trim() }
                .filter { it.isNotBlank() && !it.equals(streamUrl.trim(), ignoreCase = true) }
                .distinct()

            return BlockableChannel(
                id = item.id,
                name = item.title,
                url = streamUrl,
                logo = item.logoUrl,
                group = item.category.ifBlank { "General" },
                status = if (isBlocked) ChannelStatus.BLOCKED else ChannelStatus.UNKNOWN,
                isBlocked = isBlocked,
                backupUrls = extraUrls,
                userAgent = item.userAgent,
                referrer = item.referrer,
                origin = item.origin,
                cookie = item.cookie,
                customHeaders = item.customHeaders
            )
        }
    }

    fun getAllCandidateUrls(): List<String> {
        val list = mutableListOf<String>()
        val main = url.trim()
        if (main.isNotBlank()) list.add(main)
        for (u in backupUrls) {
            val t = u.trim()
            if (t.isNotBlank() && !list.contains(t)) {
                list.add(t)
            }
        }
        return list
    }

    fun getEffectiveHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        userAgent?.takeIf { it.isNotBlank() }?.let { headers["User-Agent"] = it }
        referrer?.takeIf { it.isNotBlank() }?.let { headers["Referer"] = it }
        origin?.takeIf { it.isNotBlank() }?.let { headers["Origin"] = it }
        cookie?.takeIf { it.isNotBlank() }?.let { headers["Cookie"] = it }
        customHeaders?.let { headers.putAll(it) }
        return headers
    }

    fun toMediaItem(): MediaItem {
        val serversList = mutableListOf<StreamServer>()
        if (url.isNotBlank()) {
            serversList.add(StreamServer("সার্ভার ১ (Main)", url))
        }
        backupUrls.forEachIndexed { index, bUrl ->
            if (bUrl.isNotBlank() && !bUrl.equals(url, ignoreCase = true)) {
                serversList.add(StreamServer("সার্ভার ${index + 2}", bUrl))
            }
        }

        return MediaItem(
            id = id,
            title = name,
            category = group ?: "General",
            type = MediaType.LIVE_TV,
            streamUrl = url,
            servers = serversList,
            logoUrl = logo,
            userAgent = userAgent,
            referrer = referrer,
            origin = origin,
            cookie = cookie,
            customHeaders = customHeaders,
            isLive = true
        )
    }
}

/**
 * Result of a single stream channel status check
 */
data class ChannelCheckResult(
    val url: String,
    val status: ChannelStatus,
    val code: Int,
    val reason: String,
    val responseTime: Long = 0L
)

/**
 * Statistics breakdown of all channels
 */
data class ChannelStatistics(
    val total: Int = 0,
    val online: Int = 0,
    val offline: Int = 0,
    val blocked: Int = 0,
    val unknown: Int = 0,
    val onlinePercentage: Float = 0f,
    val offlinePercentage: Float = 0f,
    val blockedPercentage: Float = 0f,
    val unknownPercentage: Float = 0f
)
