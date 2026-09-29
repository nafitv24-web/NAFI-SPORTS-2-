package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class SportComment(
    val id: String,
    val userName: String,
    val text: String,
    val timestamp: String,
    val avatarBgColorHex: String = "#0284C7"
)

object SportsInteractionManager {
    private const val PREFS_NAME = "nafi_sports_interactions"
    private var prefs: SharedPreferences? = null

    private val _updateTick = MutableStateFlow(0L)
    val updateTick: StateFlow<Long> = _updateTick.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    val EMOJI_LIST = listOf("🔥", "🏏", "⚽", "👏", "❤️", "🏆")

    fun getPlaylistSource(category: String?, tournament: String?, isAdminAdded: Boolean, id: String = ""): String {
        val cat = category ?: ""
        val trn = tournament ?: ""
        return when {
            isAdminAdded || id.startsWith("sport_") || id.startsWith("admin_") || id.startsWith("event_") -> "অ্যাডমিন স্পোর্টস প্লেলিস্ট"
            id.startsWith("tapmad_") || cat.contains("tapmad", ignoreCase = true) || trn.contains("tapmad", ignoreCase = true) -> "Tapmad Sports"
            cat.contains("Toffee", ignoreCase = true) || trn.contains("Toffee", ignoreCase = true) -> "Toffee Sports"
            cat.contains("TSports", ignoreCase = true) || cat.contains("T Sports", ignoreCase = true) -> "T Sports Live"
            cat.contains("Sony", ignoreCase = true) || cat.contains("Ten", ignoreCase = true) -> "Sony Sports"
            cat.contains("Star", ignoreCase = true) -> "Star Sports"
            cat.isNotBlank() && !cat.equals("Sports", ignoreCase = true) && !cat.equals("All", ignoreCase = true) -> cat
            trn.isNotBlank() && !trn.equals("Sports Event", ignoreCase = true) -> trn
            else -> "স্পোর্টস লাইভ স্ট্রিম"
        }
    }

    fun getReactions(matchId: String): Map<String, Int> {
        val p = prefs ?: return EMOJI_LIST.associateWith { getInitialReactionCount(matchId, it) }
        val rawJson = p.getString("rx_$matchId", null)
        val map = mutableMapOf<String, Int>()
        if (rawJson != null) {
            try {
                val json = JSONObject(rawJson)
                EMOJI_LIST.forEach { emoji ->
                    map[emoji] = json.optInt(emoji, getInitialReactionCount(matchId, emoji))
                }
                return map
            } catch (_: Exception) {}
        }
        EMOJI_LIST.forEach { emoji ->
            map[emoji] = getInitialReactionCount(matchId, emoji)
        }
        return map
    }

    private fun getInitialReactionCount(matchId: String, emoji: String): Int {
        val base = Math.abs((matchId + emoji).hashCode() % 35) + 6
        return base
    }

    fun addReaction(matchId: String, emoji: String) {
        val p = prefs ?: return
        val current = getReactions(matchId).toMutableMap()
        current[emoji] = (current[emoji] ?: 0) + 1
        val json = JSONObject()
        current.forEach { (k, v) -> json.put(k, v) }
        p.edit().putString("rx_$matchId", json.toString()).apply()
        _updateTick.value = System.currentTimeMillis()
    }

    fun getComments(matchId: String): List<SportComment> {
        val p = prefs ?: return getDefaultComments(matchId)
        val rawJson = p.getString("cm_$matchId", null)
        if (rawJson != null) {
            try {
                val array = JSONArray(rawJson)
                val list = mutableListOf<SportComment>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SportComment(
                            id = obj.optString("id", "c_$i"),
                            userName = obj.optString("user", "ফ্যান"),
                            text = obj.optString("text", ""),
                            timestamp = obj.optString("time", "এখনই"),
                            avatarBgColorHex = obj.optString("color", "#0284C7")
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {}
        }
        val defaultList = getDefaultComments(matchId)
        saveComments(matchId, defaultList)
        return defaultList
    }

    fun addComment(matchId: String, userName: String, text: String): SportComment {
        val cleanUser = userName.trim().ifBlank { "সরাসরি ফ্যান" }
        val cleanText = text.trim()
        val colors = listOf("#0284C7", "#10B981", "#8B5CF6", "#F59E0B", "#EC4899", "#3B82F6")
        val color = colors[Math.abs(cleanUser.hashCode()) % colors.size]
        val newComment = SportComment(
            id = "c_${System.currentTimeMillis()}",
            userName = cleanUser,
            text = cleanText,
            timestamp = "এখনই",
            avatarBgColorHex = color
        )
        val current = getComments(matchId).toMutableList()
        current.add(0, newComment)
        saveComments(matchId, current)
        _updateTick.value = System.currentTimeMillis()
        return newComment
    }

    private fun saveComments(matchId: String, list: List<SportComment>) {
        val p = prefs ?: return
        val array = JSONArray()
        list.take(60).forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("user", c.userName)
            obj.put("text", c.text)
            obj.put("time", c.timestamp)
            obj.put("color", c.avatarBgColorHex)
            array.put(obj)
        }
        p.edit().putString("cm_$matchId", array.toString()).apply()
    }

    private fun getDefaultComments(matchId: String): List<SportComment> {
        return listOf(
            SportComment("d1", "নাফি স্পোর্টস ফ্যান", "লাইভ স্ট্রিমিং অনেক স্মুথ চলছে, ধন্যবাদ নাফি টিভি 🔥", "১ মিনিট আগে", "#10B981"),
            SportComment("d2", "আরিফ হোসেন", "আজকের খেলাটা সেই জমে উঠেছে! কী অসাধারণ পারফরম্যান্স! 🏏", "৩ মিনিট আগে", "#0284C7"),
            SportComment("d3", "তানভীর", "আমাদের দলই জিতবে ইনশাআল্লাহ! সাবাশ টাইগার্স 👏", "৫ মিনিট আগে", "#8B5CF6"),
            SportComment("d4", "সাকিবুর", "এইচডি কোয়ালিটিতে কোনো বাফারিং ছাড়াই খেলা দেখতে পারছি ❤️", "৮ মিনিট আগে", "#F59E0B")
        )
    }
}
