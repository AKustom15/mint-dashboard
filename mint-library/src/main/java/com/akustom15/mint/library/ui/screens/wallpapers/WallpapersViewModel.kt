package com.akustom15.mint.library.ui.screens.wallpapers

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class WallpaperItem(
    @SerializedName("name") val name: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("collections") val collections: String = "",
    @SerializedName("downloadable") val downloadable: Boolean = true,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("dimensions") val dimensions: String? = null,
    @SerializedName("copyright") val copyright: String = ""
)

sealed class WallpapersUiState {
    data object Loading : WallpapersUiState()
    data class Success(val wallpapers: List<WallpaperItem>) : WallpapersUiState()
    data class Error(val message: String) : WallpapersUiState()
}

class WallpapersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<WallpapersUiState>(WallpapersUiState.Loading)
    val uiState: StateFlow<WallpapersUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun loadWallpapers(url: String) {
        if (url.isBlank()) {
            _uiState.value = WallpapersUiState.Error("No wallpapers URL configured")
            return
        }

        viewModelScope.launch {
            _uiState.value = WallpapersUiState.Loading
            try {
                val wallpapers = withContext(Dispatchers.IO) { fetchWallpapers(url) }
                _uiState.value = WallpapersUiState.Success(wallpapers)
            } catch (e: Exception) {
                Log.e("WallpapersVM", "Error loading wallpapers", e)
                _uiState.value = WallpapersUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun fetchWallpapers(url: String): List<WallpaperItem> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mint/1.0 OkHttp")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
        val body = response.body?.string() ?: throw Exception("Empty response")
        val type = object : TypeToken<List<WallpaperItem>>() {}.type
        return Gson().fromJson(body, type)
    }
}
