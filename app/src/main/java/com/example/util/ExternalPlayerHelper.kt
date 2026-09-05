package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast

object ExternalPlayerHelper {

    /**
     * Launches external media players (e.g. VLC, MX Player, System Video Player)
     * for a given video URL with title metadata.
     */
    fun launchExternalPlayer(context: Context, url: String, title: String? = null): Boolean {
        return try {
            val safeUri = UrlSanitizer.toSafeUri(url)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(safeUri, "video/*")
                if (!title.isNullOrBlank()) {
                    putExtra("title", title)
                    putExtra("android.media.intent.extra.TITLE", title)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "ভিডিও প্লেয়ার বেছে নিন (VLC / MX Player)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "এক্সটার্নাল প্লেয়ার খোলা যায়নি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
