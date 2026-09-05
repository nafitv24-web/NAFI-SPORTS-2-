package com.example.util

import android.net.Uri

/**
 * Utility for sanitizing and encoding media stream URLs.
 * Ensures proper URL encoding for spaces, special characters, and brackets in HLS/DASH/VOD links,
 * while preserving query parameters and custom header pipes.
 */
object UrlSanitizer {

    /**
     * Sanitizes a stream URL by:
     * - Trimming whitespace
     * - Replacing unencoded spaces (' ') with '%20'
     * - Replacing unencoded brackets ('[' and ']') in paths with '%5B' and '%5D'
     * - Preserving pipe parameters (e.g. |User-Agent=...)
     */
    fun sanitizeStreamUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return trimmed

        // Extract pipe parameters if present (e.g. url|User-Agent=... or url?|...)
        val pipeIndex = when {
            trimmed.contains("?%7C", ignoreCase = true) -> trimmed.indexOf("?%7C", ignoreCase = true)
            trimmed.contains("%7C", ignoreCase = true) -> trimmed.indexOf("%7C", ignoreCase = true)
            trimmed.contains("?|") -> trimmed.indexOf("?|")
            trimmed.contains("|") -> trimmed.indexOf("|")
            else -> -1
        }

        val basePart: String
        val pipePart: String
        if (pipeIndex != -1) {
            basePart = trimmed.substring(0, pipeIndex).trim()
            pipePart = trimmed.substring(pipeIndex)
        } else {
            basePart = trimmed
            pipePart = ""
        }

        // Clean up base URL
        var sanitizedBase = basePart
            // Normalize raw spaces in path/query to %20
            .replace(" ", "%20")
            // Normalize brackets in path
            .replace("[", "%5B")
            .replace("]", "%5D")

        return sanitizedBase + pipePart
    }

    /**
     * Converts a raw or sanitized URL into a safe Android Uri.
     */
    fun toSafeUri(rawUrl: String): Uri {
        val sanitized = sanitizeStreamUrl(rawUrl)
        return when {
            sanitized.startsWith("file://", ignoreCase = true) -> Uri.parse(sanitized)
            sanitized.startsWith("content://", ignoreCase = true) -> Uri.parse(sanitized)
            sanitized.startsWith("/") -> Uri.fromFile(java.io.File(sanitized))
            else -> Uri.parse(sanitized)
        }
    }
}
