package com.akustom15.mint.library.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.akustom15.mint.library.config.MintConfig
import com.akustom15.mint.library.ui.screens.dashboard.DashboardScreen
import com.akustom15.mint.library.ui.screens.icons.IconsPreviewScreen
import com.akustom15.mint.library.ui.screens.request.IconRequestScreen
import com.akustom15.mint.library.ui.screens.settings.NotificationHistoryScreen
import com.akustom15.mint.library.ui.screens.settings.SettingsScreen
import com.akustom15.mint.library.ui.screens.wallpapers.WallpapersScreen
import com.akustom15.mint.library.ui.screens.widgets.WidgetsScreen

object MintRoutes {
    const val DASHBOARD = "dashboard"
    const val ICONS_PREVIEW = "icons_preview"
    const val WIDGETS = "widgets"
    const val WALLPAPERS = "wallpapers"
    const val ICON_REQUEST = "icon_request"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
}

@Composable
fun MintNavGraph(
    navController: NavHostController,
    config: MintConfig
) {
    NavHost(
        navController = navController,
        startDestination = MintRoutes.DASHBOARD
    ) {
        composable(MintRoutes.DASHBOARD) {
            DashboardScreen(
                config = config,
                onNavigateToIconsPreview = {
                    navController.navigate(MintRoutes.ICONS_PREVIEW) {
                        popUpTo(MintRoutes.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToIconRequest = { navController.navigate(MintRoutes.ICON_REQUEST) },
                onNavigateToNotifications = { navController.navigate(MintRoutes.NOTIFICATIONS) }
            )
        }

        composable(MintRoutes.ICONS_PREVIEW) {
            IconsPreviewScreen(
                onNavigateBack = {
                    navController.navigate(MintRoutes.DASHBOARD) {
                        popUpTo(MintRoutes.DASHBOARD) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(MintRoutes.WIDGETS) {
            WidgetsScreen(config = config)
        }

        composable(MintRoutes.WALLPAPERS) {
            WallpapersScreen(
                onNavigateBack = {
                    navController.navigate(MintRoutes.DASHBOARD) {
                        popUpTo(MintRoutes.DASHBOARD) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                wallpapersUrl = config.cloudWallpapersUrl,
                appName = config.appName
            )
        }

        composable(MintRoutes.ICON_REQUEST) {
            IconRequestScreen(
                onNavigateBack = { navController.popBackStack() },
                config = config
            )
        }

        composable(MintRoutes.SETTINGS) {
            SettingsScreen(
                config = config,
                onNavigateBack = {
                    navController.navigate(MintRoutes.DASHBOARD) {
                        popUpTo(MintRoutes.DASHBOARD) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToNotifications = { navController.navigate(MintRoutes.NOTIFICATIONS) }
            )
        }

        composable(MintRoutes.NOTIFICATIONS) {
            NotificationHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
