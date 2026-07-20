package com.akustom15.mint.library.ui.screens.icons

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akustom15.mint.library.data.AppFilterCache
import com.akustom15.mint.library.data.IconPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser

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

            // Load ALL drawable icons via XML parsing + getIdentifier (R8-safe)
            val icons = loadAllDrawableIcons(context)

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

    /**
     * Load all icon drawables by parsing res/xml/drawable.xml and resolving each
     * name via resources.getIdentifier(). This is R8-safe — it does NOT rely on
     * R$drawable reflection which breaks when isMinifyEnabled=true (R8 inlines
     * and removes the R class fields in AGP 8+).
     */
    private fun loadAllDrawableIcons(context: Context): List<IconPreviewItem> {
        val packageName = context.packageName
        val resources = context.resources
        val iconNames = mutableSetOf<String>()

        // 1) Parse res/xml/drawable.xml — the canonical icon catalogue
        try {
            val resId = resources.getIdentifier("drawable", "xml", packageName)
            if (resId != 0) {
                val parser = resources.getXml(resId)
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (drawable != null && drawable.startsWith("icon_") &&
                            !EXCLUDED_ICONS.contains(drawable)
                        ) {
                            iconNames.add(drawable)
                        }
                    }
                    eventType = parser.next()
                }
                parser.close()
            }
        } catch (e: Exception) {
            Log.e("IconsPreview", "Error parsing res/xml/drawable.xml", e)
        }

        // 2) Also parse appfilter_new.xml to include new/recent icons
        try {
            val newResId = resources.getIdentifier("appfilter_new", "xml", packageName)
            if (newResId != 0) {
                val parser = resources.getXml(newResId)
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (drawable != null && drawable.startsWith("icon_") &&
                            !EXCLUDED_ICONS.contains(drawable)
                        ) {
                            iconNames.add(drawable)
                        }
                    }
                    eventType = parser.next()
                }
                parser.close()
            }
        } catch (e: Exception) {
            Log.e("IconsPreview", "Error parsing res/xml/appfilter_new.xml", e)
        }

        // 3) Fallback: parse from appfilter.xml if nothing loaded yet
        if (iconNames.isEmpty()) {
            try {
                AppFilterCache.getIconNames(context)
                    .filter { it.startsWith("icon_") && !EXCLUDED_ICONS.contains(it) }
                    .forEach { iconNames.add(it) }
            } catch (e: Exception) {
                Log.e("IconsPreview", "Error loading from appfilter fallback", e)
            }
        }

        // 4) Resolve each name → resource ID (R8-safe)
        return iconNames
            .mapNotNull { name ->
                val id = resources.getIdentifier(name, "drawable", packageName)
                if (id != 0) IconPreviewItem(name = name, resourceId = id)
                else null
            }
            .sortedBy { it.name }
    }
}
