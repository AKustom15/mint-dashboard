package com.akustom15.mint.library.notifications

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Stores notification preferences (enabled/disabled) and notification history
 * (last 100 notifications) using SharedPreferences.
 */
class MintNotificationPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "mint_notifications"
        private const val KEY_ENABLED = "notifications_enabled"
        private const val KEY_HISTORY = "notification_history"
        private const val MAX_HISTORY = 100

        @Volatile
        private var instance: MintNotificationPreferences? = null

        fun getInstance(context: Context): MintNotificationPreferences {
            return instance ?: synchronized(this) {
                instance ?: MintNotificationPreferences(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    fun areNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * Save a notification to history. Keeps the last [MAX_HISTORY] items.
     */
    fun saveNotification(title: String, body: String, imageUrl: String? = null) {
        val history = getHistoryJson()
        val entry = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("title", title)
            put("body", body)
            put("timestamp", System.currentTimeMillis())
            put("isRead", false)
            if (!imageUrl.isNullOrBlank()) {
                put("imageUrl", imageUrl)
            }
        }
        history.put(entry)

        // Trim to last MAX_HISTORY
        while (history.length() > MAX_HISTORY) {
            history.remove(0)
        }

        prefs.edit().putString(KEY_HISTORY, history.toString()).apply()
    }

    /**
     * Get all stored notifications, newest first.
     */
    fun getNotificationHistory(): List<NotificationItem> {
        val history = getHistoryJson()
        val list = mutableListOf<NotificationItem>()
        var needsSave = false
        
        for (i in 0 until history.length()) {
            val obj = history.getJSONObject(i)
            
            // Fix legacy items missing an ID
            var id = obj.optString("id", "")
            if (id.isBlank()) {
                id = UUID.randomUUID().toString()
                obj.put("id", id)
                needsSave = true
            }
            
            list.add(
                NotificationItem(
                    id = id,
                    title = obj.optString("title", ""),
                    body = obj.optString("body", ""),
                    timestamp = obj.optLong("timestamp", 0L),
                    isRead = obj.optBoolean("isRead", false),
                    imageUrl = if (obj.has("imageUrl")) obj.optString("imageUrl") else null
                )
            )
        }
        
        if (needsSave) {
            prefs.edit().putString(KEY_HISTORY, history.toString()).apply()
        }
        
        return list.reversed() // newest first
    }

    fun markAsRead(id: String) {
        val history = getHistoryJson()
        for (i in 0 until history.length()) {
            val obj = history.getJSONObject(i)
            if (obj.optString("id") == id) {
                obj.put("isRead", true)
                break
            }
        }
        prefs.edit().putString(KEY_HISTORY, history.toString()).apply()
    }

    fun deleteNotification(id: String) {
        val history = getHistoryJson()
        val newHistory = JSONArray()
        for (i in 0 until history.length()) {
            val obj = history.getJSONObject(i)
            if (obj.optString("id") != id) {
                newHistory.put(obj)
            }
        }
        prefs.edit().putString(KEY_HISTORY, newHistory.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().putString(KEY_HISTORY, "[]").apply()
    }

    private fun getHistoryJson(): JSONArray {
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            JSONArray(raw)
        } catch (e: Exception) {
            JSONArray()
        }
    }
}

/**
 * Represents a single stored notification.
 */
data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    var isRead: Boolean = false,
    val imageUrl: String? = null
)
