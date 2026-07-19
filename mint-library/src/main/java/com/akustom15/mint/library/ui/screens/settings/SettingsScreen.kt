package com.akustom15.mint.library.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.akustom15.mint.library.R
import com.akustom15.mint.library.config.MintConfig
import com.akustom15.mint.library.data.MintAppLanguage
import com.akustom15.mint.library.data.MintPreferences
import com.akustom15.mint.library.data.MintThemeMode
import com.akustom15.mint.library.data.MintUpdateChecker
import com.akustom15.mint.library.notifications.MintNotificationHelper
import com.akustom15.mint.library.notifications.MintNotificationPreferences
import com.akustom15.mint.library.ui.composables.FrostedGlassDialogCard
import com.akustom15.mint.library.ui.composables.GradientBackground
import com.akustom15.mint.library.ui.composables.LiquidGlassCard
import com.akustom15.mint.library.config.MoreApp
import com.akustom15.mint.library.config.MoreAppsLoader
import com.akustom15.mint.library.ui.composables.MoreAppsCarousel
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState


@Composable
fun SettingsScreen(
    config: MintConfig,
    onNavigateBack: () -> Unit,
    onNavigateToNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liquidColors = LocalLiquidGlassColors.current
    val preferences = remember { MintPreferences.getInstance(context) }
    val notificationPrefs = remember { MintNotificationPreferences.getInstance(context) }

    val appLanguage by preferences.appLanguage.collectAsState()
    val themeMode by preferences.themeMode.collectAsState()

    var notificationsEnabled by remember { mutableStateOf(notificationPrefs.areNotificationsEnabled()) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    // More Apps: load from remote JSON URL if configured, otherwise fall back to static list
    var remoteMoreApps by remember { mutableStateOf<List<MoreApp>?>(null) }
    LaunchedEffect(config.moreAppsJsonUrl) {
        if (config.moreAppsJsonUrl.isNotBlank()) {
            scope.launch {
                val loaded = MoreAppsLoader.loadFromUrl(config.moreAppsJsonUrl)
                if (loaded.isNotEmpty()) remoteMoreApps = loaded
            }
        }
    }
    val effectiveMoreApps = remoteMoreApps ?: config.moreApps

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheCleared by remember { mutableStateOf(false) }

    val isDialogOpen = showThemeDialog || showLanguageDialog || showClearCacheDialog

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isDialogOpen) Modifier.blur(20.dp) else Modifier)
        ) {
            // Top Bar
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = liquidColors.textPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.mint_settings_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = liquidColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                // Bell → notification history (with unread badge)
                val unreadCount by notificationPrefs.unreadCountFlow.collectAsState()
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.mint_notifications_history),
                            tint = liquidColors.textPrimary
                        )
                    }
                    if (unreadCount > 0) {
                        val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "badge_scale"
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 6.dp)
                                .scale(pulseScale)
                                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                .background(androidx.compose.ui.graphics.Color(0xFFFF3B30), CircleShape)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = androidx.compose.ui.graphics.Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    // navigationBarsPadding = system gesture bar
                    // Extra 100.dp = app bottom nav bar (62dp height + 8dp padding + safety)
                    .padding(bottom = 100.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Appearance Section
                Text(
                    text = stringResource(R.string.mint_settings_appearance),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintColors.Primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                // Theme
                SettingsItem(
                    title = stringResource(R.string.mint_settings_theme),
                    subtitle = when (themeMode) {
                        MintThemeMode.LIGHT -> stringResource(R.string.mint_settings_theme_light)
                        MintThemeMode.DARK -> stringResource(R.string.mint_settings_theme_dark)
                        MintThemeMode.SYSTEM -> stringResource(R.string.mint_settings_theme_system)
                    },
                    onClick = { showThemeDialog = true }
                )

                // Language
                SettingsItem(
                    title = stringResource(R.string.mint_settings_language),
                    subtitle = appLanguage.displayName,
                    onClick = { showLanguageDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Data Section
                Text(
                    text = stringResource(R.string.mint_settings_data),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintColors.Primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                // Clear Cache
                SettingsItem(
                    title = stringResource(R.string.mint_settings_clear_cache),
                    subtitle = if (cacheCleared) stringResource(R.string.mint_settings_cache_cleared)
                    else stringResource(R.string.mint_settings_clear_cache_desc),
                    onClick = { showClearCacheDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notifications Section
                Text(
                    text = stringResource(R.string.mint_settings_notifications_section),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintColors.Primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                // Push notifications toggle
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.mint_settings_notifications),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = liquidColors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.mint_settings_notifications_desc),
                                fontSize = 13.sp,
                                color = liquidColors.textSecondary
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                notificationsEnabled = enabled
                                notificationPrefs.setNotificationsEnabled(enabled)
                                MintNotificationHelper.syncSubscription(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MintColors.OnPrimary,
                                checkedTrackColor = MintColors.Primary
                            )
                        )
                    }
                }

                // Check for updates (only if an update URL or firestore document is configured)
                if (config.updateJsonUrl.isNotEmpty() || config.firestoreUpdateDocument.isNotEmpty()) {
                    SettingsItem(
                        title = stringResource(R.string.mint_settings_check_update),
                        subtitle = if (isCheckingUpdate)
                            stringResource(R.string.mint_settings_checking_update)
                        else stringResource(R.string.mint_settings_check_update_desc),
                        onClick = {
                            if (!isCheckingUpdate) {
                                isCheckingUpdate = true
                                scope.launch {
                                    if (config.firestoreUpdateDocument.isNotEmpty()) {
                                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        db.collection("app_versions").document(config.firestoreUpdateDocument)
                                            .get()
                                            .addOnSuccessListener { doc ->
                                                isCheckingUpdate = false
                                                if (doc.exists()) {
                                                    val vCode = doc.getLong("versionCode")?.toInt() ?: 0
                                                    if (vCode > config.versionCode) {
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.mint_settings_update_available),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                            data = android.net.Uri.parse("market://details?id=${context.packageName}")
                                                            setPackage("com.android.vending")
                                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        try {
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                                                android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                                            ).apply {
                                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(webIntent)
                                                        }
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.mint_settings_up_to_date),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.mint_settings_update_failed),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            .addOnFailureListener {
                                                isCheckingUpdate = false
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.mint_settings_update_failed),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    } else {
                                        val result = MintUpdateChecker.check(
                                            config.updateJsonUrl,
                                            config.versionCode
                                        )
                                        isCheckingUpdate = false
                                        when {
                                            result == null -> Toast.makeText(
                                                context,
                                                context.getString(R.string.mint_settings_update_failed),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            result.updateAvailable -> {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.mint_settings_update_available),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                val url = result.downloadUrl.ifEmpty { config.moreAppsUrl }
                                                if (url.isNotEmpty()) {
                                                    val webIntent = android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        android.net.Uri.parse(url)
                                                    ).apply {
                                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(webIntent)
                                                }
                                            }
                                            else -> Toast.makeText(
                                                context,
                                                context.getString(R.string.mint_settings_up_to_date),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Info Section
                Text(
                    text = stringResource(R.string.mint_settings_info),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintColors.Primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                if (config.updateInfo.isNotEmpty()) {
                    SettingsItem(
                        title = stringResource(R.string.mint_settings_whats_new),
                        subtitle = config.updateInfo,
                        onClick = {}
                    )
                }

                SettingsItem(
                    title = stringResource(R.string.mint_settings_version),
                    subtitle = "${config.versionName} (${config.versionCode})",
                    onClick = {}
                )

                SettingsItem(
                    title = stringResource(R.string.mint_settings_package),
                    subtitle = config.packageName,
                    onClick = {}
                )

                if (config.privacyPolicyUrl.isNotEmpty()) {
                    SettingsItem(
                        title = stringResource(R.string.mint_settings_privacy),
                        subtitle = stringResource(R.string.mint_settings_privacy_desc),
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(config.privacyPolicyUrl)
                            )
                            context.startActivity(intent)
                        }
                    )
                }

                // More Apps carousel (Lunex/Pump style) — shows when static list or remote URL is set
                if (effectiveMoreApps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MoreAppsCarousel(
                        apps = effectiveMoreApps,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Theme dialog — frosted glass card
        if (showThemeDialog) {
            Dialog(onDismissRequest = { showThemeDialog = false }) {
                com.akustom15.mint.library.ui.MintLocalizedContent {

                FrostedGlassDialogCard {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.mint_settings_select_theme),
                            color = liquidColors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        MintThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                MintThemeMode.LIGHT -> stringResource(R.string.mint_settings_theme_light)
                                MintThemeMode.DARK -> stringResource(R.string.mint_settings_theme_dark)
                                MintThemeMode.SYSTEM -> stringResource(R.string.mint_settings_theme_system)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        preferences.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = themeMode == mode,
                                    onClick = {
                                        preferences.setThemeMode(mode)
                                        showThemeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MintColors.Primary,
                                        unselectedColor = liquidColors.textSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    color = liquidColors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showThemeDialog = false }) {
                            Text(stringResource(R.string.mint_settings_cancel), color = MintColors.Primary)
                        }
                    }
                }
            
                }
            }
        }

        // Language dialog — frosted glass card
        if (showLanguageDialog) {
            Dialog(onDismissRequest = { showLanguageDialog = false }) {
                com.akustom15.mint.library.ui.MintLocalizedContent {

                FrostedGlassDialogCard {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.mint_settings_select_language),
                            color = liquidColors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                            MintAppLanguage.entries.forEach { lang ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            preferences.setAppLanguage(lang)
                                            showLanguageDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = appLanguage == lang,
                                        onClick = {
                                            preferences.setAppLanguage(lang)
                                            showLanguageDialog = false
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MintColors.Primary,
                                            unselectedColor = liquidColors.textSecondary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lang.displayName,
                                        color = liquidColors.textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text(stringResource(R.string.mint_settings_cancel), color = MintColors.Primary)
                        }
                    }
                }
            
                }
            }
        }

        // Clear Cache dialog — frosted glass card
        if (showClearCacheDialog) {
            Dialog(onDismissRequest = { showClearCacheDialog = false }) {
                com.akustom15.mint.library.ui.MintLocalizedContent {

                FrostedGlassDialogCard {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.mint_settings_clear_cache),
                            color = liquidColors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            stringResource(R.string.mint_settings_clear_cache_confirm),
                            color = liquidColors.textPrimary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showClearCacheDialog = false }) {
                                Text(stringResource(R.string.mint_settings_cancel), color = liquidColors.textPrimary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                preferences.clearImageCache(context)
                                cacheCleared = true
                                showClearCacheDialog = false
                            }) {
                                Text(stringResource(R.string.mint_settings_clear), color = MintColors.Primary)
                            }
                        }
                    }
                }
            
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val liquidColors = LocalLiquidGlassColors.current

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = liquidColors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = liquidColors.textSecondary
            )
        }
    }
}
