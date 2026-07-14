package com.akustom15.mint.library.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Simple manual update checker.
 *
 * Fetches a small JSON descriptor from [MintConfig.updateJsonUrl] with the shape:
 * ```json
 * { "versionCode": 12, "versionName": "1.2.0", "url": "https://play.google.com/..." }
 * ```
 * and compares `versionCode` against the currently installed one.
 */
object MintUpdateChecker {

    private const val TAG = "MintUpdateChecker"

    data class UpdateResult(
        val updateAvailable: Boolean,
        val latestVersionName: String = "",
        val downloadUrl: String = ""
    )

    /**
     * @param jsonUrl remote descriptor URL (from MintConfig.updateJsonUrl)
     * @param currentVersionCode the installed app's versionCode
     * @return [UpdateResult], or null if the check failed (network/parse error).
     */
    suspend fun check(jsonUrl: String, currentVersionCode: Int): UpdateResult? =
        withContext(Dispatchers.IO) {
            if (jsonUrl.isBlank()) return@withContext null
            try {
                val connection = (URL(jsonUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    requestMethod = "GET"
                }
                val raw = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val json = JSONObject(raw)
                val latestCode = json.optInt("versionCode", 0)
                UpdateResult(
                    updateAvailable = latestCode > currentVersionCode,
                    latestVersionName = json.optString("versionName", ""),
                    downloadUrl = json.optString("url", "")
                )
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
                null
            }
        }
}
