package com.akustom15.mint.library.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads the "More Apps" list from a remote JSON URL.
 *
 * Expected JSON format:
 * {
 *   "apps": [
 *     {
 *       "name": "App Name",
 *       "description": "Short description",
 *       "iconUrl": "https://...",
 *       "screenshotUrls": ["https://..."],
 *       "playStoreUrl": "https://play.google.com/store/apps/details?id=..."
 *     }
 *   ]
 * }
 */
object MoreAppsLoader {

    /**
     * Fetches and parses the More Apps JSON from the given URL.
     * @param jsonUrl URL to the JSON file
     * @return List of MoreApp objects, or empty list on failure
     */
    suspend fun loadFromUrl(jsonUrl: String): List<MoreApp> = withContext(Dispatchers.IO) {
        if (jsonUrl.isBlank()) return@withContext emptyList()
        try {
            val url = URL(jsonUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseJson(response)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseJson(jsonString: String): List<MoreApp> {
        val result = mutableListOf<MoreApp>()
        try {
            val root = JSONObject(jsonString)
            val appsArray = root.getJSONArray("apps")
            for (i in 0 until appsArray.length()) {
                val obj = appsArray.getJSONObject(i)
                val screenshotUrls = mutableListOf<String>()
                val screenshotsArray = obj.optJSONArray("screenshotUrls")
                if (screenshotsArray != null) {
                    for (j in 0 until screenshotsArray.length()) {
                        screenshotUrls.add(screenshotsArray.getString(j))
                    }
                }
                result.add(
                    MoreApp(
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        iconUrl = obj.optString("iconUrl", ""),
                        screenshotUrls = screenshotUrls,
                        playStoreUrl = obj.optString("playStoreUrl", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
