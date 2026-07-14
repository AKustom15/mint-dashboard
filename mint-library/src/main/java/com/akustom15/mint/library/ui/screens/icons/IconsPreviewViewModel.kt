package com.akustom15.mint.library.ui.screens.icons

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akustom15.mint.library.data.IconPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IconPreviewItem(
    val name: String,
    val resourceId: Int
)

data class IconsPreviewUiState(
    val allIcons: List<IconPreviewItem> = emptyList(),
    val categoryMap: Map<String, Set<String>> = emptyMap(),
    val searchQuery: String = "",
    val favorites: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val selectedIcon: IconPreviewItem? = null
)

class IconsPreviewViewModel : ViewModel() {

    companion object {
        private val EXCLUDED_ICONS = setOf(
            "icon_back", "icon_mask", "icon_upon", "icon_base"
        )
    }

    private val _uiState = MutableStateFlow(IconsPreviewUiState())
    val uiState: StateFlow<IconsPreviewUiState> = _uiState.asStateFlow()

    fun loadIcons(context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val packageName = context.packageName

            // Load ALL drawable icons (like GlassWave), excluding mask icons
            val icons = loadAllDrawableIcons(packageName)

            val catMap = IconPackManager.loadCategoriesFromXml(context)

            val prefs = context.getSharedPreferences("mint_favorites", Context.MODE_PRIVATE)
            val savedFavorites = prefs.getStringSet("favorite_icons", emptySet()) ?: emptySet()

            Log.d("IconsPreview", "Total icons loaded: ${icons.size}, categories: ${catMap.keys}")

            _uiState.value = _uiState.value.copy(
                allIcons = icons,
                categoryMap = catMap,
                favorites = savedFavorites,
                isLoading = false
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleFavorite(context: Context, iconName: String) {
        val currentFavorites = _uiState.value.favorites.toMutableSet()
        if (currentFavorites.contains(iconName)) {
            currentFavorites.remove(iconName)
        } else {
            currentFavorites.add(iconName)
        }
        _uiState.value = _uiState.value.copy(favorites = currentFavorites)

        context.getSharedPreferences("mint_favorites", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("favorite_icons", currentFavorites)
            .apply()
    }

    fun selectIcon(icon: IconPreviewItem?) {
        _uiState.value = _uiState.value.copy(selectedIcon = icon)
    }

    private fun loadAllDrawableIcons(packageName: String): List<IconPreviewItem> {
        return try {
            val rClass = Class.forName("$packageName.R\$drawable")
            rClass.fields
                .filter { field ->
                    field.type == Int::class.javaPrimitiveType &&
                    field.name.startsWith("icon_") &&
                    !EXCLUDED_ICONS.contains(field.name)
                }
                .map { field ->
                    IconPreviewItem(
                        name = field.name,
                        resourceId = field.getInt(null)
                    )
                }
                .sortedBy { it.name }
        } catch (e: Exception) {
            Log.e("IconsPreview", "Error loading drawable icons: ${e.message}", e)
            emptyList()
        }
    }
}
