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
    val httpCode: Int = 0
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
            return BlockableChannel(
                id = item.id,
                name = item.title,
                url = streamUrl,
                logo = item.logoUrl,
                group = item.category.ifBlank { "General" },
                status = if (isBlocked) ChannelStatus.BLOCKED else ChannelStatus.UNKNOWN,
                isBlocked = isBlocked
            )
        }
    }

    fun toMediaItem(): MediaItem {
        return MediaItem(
            id = id,
            title = name,
            category = group ?: "General",
            type = MediaType.LIVE_TV,
            streamUrl = url,
            logoUrl = logo,
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
