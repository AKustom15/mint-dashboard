package com.akustom15.mint.library.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.akustom15.mint.library.data.MintAppLanguage
import com.akustom15.mint.library.data.MintPreferences
import java.util.Locale

/**
 * Wrapper composable that applies the selected language from MintPreferences.
 * Replicates PUM's LocalizedContent behavior for Mint library.
 */
@Composable
fun MintLocalizedContent(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { MintPreferences.getInstance(context) }
    val appLanguage = preferences.appLanguage.collectAsState().value

    val targetLocale = when (appLanguage) {
        MintAppLanguage.SYSTEM -> Locale.getDefault()
        MintAppLanguage.ENGLISH -> Locale("en")
        MintAppLanguage.SPANISH -> Locale("es")
        MintAppLanguage.FRENCH -> Locale("fr")
        MintAppLanguage.GERMAN -> Locale("de")
        MintAppLanguage.PORTUGUESE -> Locale("pt", "BR")
        MintAppLanguage.ARABIC -> Locale("ar")
        MintAppLanguage.ITALIAN -> Locale("it")
        MintAppLanguage.HINDI -> Locale("hi")
        MintAppLanguage.INDONESIAN -> Locale("in")
        MintAppLanguage.CHINESE -> Locale("zh", "CN")
        MintAppLanguage.JAPANESE -> Locale("ja")
        MintAppLanguage.KOREAN -> Locale("ko")
        MintAppLanguage.RUSSIAN -> Locale("ru")
    }

    key(appLanguage) {
        val localizedContext = remember(targetLocale) {
            createLocalizedContext(context, targetLocale)
        }

        val localizedConfiguration = remember(targetLocale) {
            Configuration(context.resources.configuration).apply {
                setLocale(targetLocale)
            }
        }

        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfiguration
        ) {
            content()
        }
    }
}

private fun createLocalizedContext(context: Context, locale: Locale): Context {
    val configuration = Configuration(context.resources.configuration)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
    } else {
        @Suppress("DEPRECATION")
        configuration.locale = locale
        configuration.setLayoutDirection(locale)
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createConfigurationContext(configuration)
    } else {
        @Suppress("DEPRECATION")
        val resources = context.resources
        resources.updateConfiguration(configuration, resources.displayMetrics)
        context
    }
}
