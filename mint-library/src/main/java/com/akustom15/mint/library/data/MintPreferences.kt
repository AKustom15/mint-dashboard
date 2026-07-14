package com.akustom15.mint.library.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Enum for the application language.
 * Matches PUM's AppLanguage for consistency across icon packs.
 */
enum class MintAppLanguage(val code: String, val displayName: String) {
    SYSTEM("system", "Automático / Auto"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    PORTUGUESE("pt-BR", "Português (Brasil)"),
    ARABIC("ar", "العربية"),
    ITALIAN("it", "Italiano"),
    HINDI("hi", "हिन्दी"),
    INDONESIAN("in", "Bahasa Indonesia"),
    CHINESE("zh-CN", "简体中文"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    RUSSIAN("ru", "Русский")
}

/**
 * Enum for theme mode
 */
enum class MintThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Enum for widget grid column count
 */
enum class MintGridColumns(val count: Int) {
    ONE(1),
    TWO(2)
}

/**
 * Centralized preferences manager for Mint library.
 * Handles language, theme and other settings with reactive StateFlows.
 * Uses AppCompatDelegate.setApplicationLocales for proper language switching.
 */
class MintPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(getAppLanguage())
    val appLanguage: StateFlow<MintAppLanguage> = _appLanguage.asStateFlow()

    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<MintThemeMode> = _themeMode.asStateFlow()

    private val _gridColumns = MutableStateFlow(getGridColumns())
    val gridColumns: StateFlow<MintGridColumns> = _gridColumns.asStateFlow()

    // Language
    fun getAppLanguage(): MintAppLanguage {
        val value = prefs.getString(KEY_APP_LANGUAGE, MintAppLanguage.SYSTEM.name)
            ?: MintAppLanguage.SYSTEM.name
        return try {
            MintAppLanguage.valueOf(value)
        } catch (e: Exception) {
            MintAppLanguage.SYSTEM
        }
    }

    fun setAppLanguage(language: MintAppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
        _appLanguage.value = language
        applyLanguage(language)
    }

    fun applyStoredLanguage() {
        val language = getAppLanguage()
        applyLanguage(language)
    }

    private fun applyLanguage(language: MintAppLanguage) {
        val localeList = when (language) {
            MintAppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(language.code)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    // Theme
    fun getThemeMode(): MintThemeMode {
        val value = prefs.getString(KEY_THEME_MODE, MintThemeMode.SYSTEM.name)
            ?: MintThemeMode.SYSTEM.name
        return try {
            MintThemeMode.valueOf(value)
        } catch (e: Exception) {
            MintThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: MintThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    // Grid columns
    fun getGridColumns(): MintGridColumns {
        val value = prefs.getString(KEY_GRID_COLUMNS, MintGridColumns.ONE.name)
            ?: MintGridColumns.ONE.name
        return try {
            MintGridColumns.valueOf(value)
        } catch (e: Exception) {
            MintGridColumns.ONE
        }
    }

    fun setGridColumns(columns: MintGridColumns) {
        prefs.edit().putString(KEY_GRID_COLUMNS, columns.name).apply()
        _gridColumns.value = columns
    }

    // Cache clearing
    /**
     * Clears all image caches:
     *  - Coil memory cache (in-RAM bitmaps for wallpapers/widgets/more-apps)
     *  - Coil disk cache (downloaded wallpaper/widget images stored on device)
     *  - AppFilterCache (parsed icon filter in-memory cache)
     *  - icon_request folder (locally cached icon request thumbnails)
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearImageCache(context: Context): Boolean {
        return try {
            // 1. Coil memory + disk cache (wallpapers, widgets, more-apps carousel images)
            val imageLoader = coil.Coil.imageLoader(context)
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()

            // 2. In-memory icon filter cache
            AppFilterCache.clearCache()

            // 3. icon_request thumbnail folder
            val cacheDir = java.io.File(context.cacheDir, "icon_request")
            if (cacheDir.exists()) cacheDir.deleteRecursively()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        private const val PREFS_NAME = "mint_preferences"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_GRID_COLUMNS = "grid_columns"

        @Volatile
        private var instance: MintPreferences? = null

        fun getInstance(context: Context): MintPreferences {
            return instance ?: synchronized(this) {
                instance ?: MintPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
