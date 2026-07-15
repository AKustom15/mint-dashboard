package com.akustom15.mint.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.akustom15.mint.library.config.MintConfig
import com.akustom15.mint.library.navigation.MintNavGraph
import com.akustom15.mint.library.navigation.MintRoutes
import com.akustom15.mint.library.ui.components.MintBottomNavigation
import com.akustom15.mint.library.ui.components.MintTab
import com.akustom15.mint.library.ui.composables.LocalHazeState
import com.akustom15.mint.library.ui.composables.MintChangelogDialog
import com.akustom15.mint.library.ui.theme.MintTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.akustom15.mint.library.security.SecurityManager
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.style.TextAlign

@Composable
fun MintScreen(config: MintConfig) {
    val context = LocalContext.current
    val securityManager = remember { SecurityManager.getInstance(context, config) }
    val securityState by securityManager.securityState.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Build visible tabs based on config
    val visibleTabs = remember(config.showWidgets, config.showWallpapers) {
        buildList {
            add(MintTab.Icons)
            if (config.showWidgets) add(MintTab.Widgets)
            if (config.showWallpapers) add(MintTab.Wallpapers)
            add(MintTab.Settings)
        }
    }

    val selectedTab = when (currentRoute) {
        MintRoutes.DASHBOARD -> MintTab.Icons
        MintRoutes.WIDGETS -> MintTab.Widgets
        MintRoutes.WALLPAPERS -> MintTab.Wallpapers
        MintRoutes.SETTINGS -> MintTab.Settings
        else -> null
    }

    val showBottomNav = currentRoute in listOf(
        MintRoutes.DASHBOARD,
        MintRoutes.ICONS_PREVIEW,
        MintRoutes.WIDGETS,
        MintRoutes.WALLPAPERS,
        MintRoutes.SETTINGS
    )

    MintLocalizedContent {
        MintTheme(colorConfig = config.colorConfig) {
            if (config.enableAntiPiracy && (securityState is SecurityManager.SecurityState.Invalid || securityState is SecurityManager.SecurityState.Error)) {
                // Show blocking security screen
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(32.dp)) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Piracy Warning", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        Text(
                            text = "Unlicensed Application",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        val msg = if (securityState is SecurityManager.SecurityState.Invalid) (securityState as SecurityManager.SecurityState.Invalid).reason else (securityState as SecurityManager.SecurityState.Error).message
                        Text(
                            text = "This app failed security checks: $msg\nPlease install the genuine version from the Play Store.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Dedicated Haze state for the bottom nav. The whole nav-graph content
                // (wallpapers, lists?) is the blur source; the nav is a hazeChild and a
                // SIBLING of that source, so it renders a real blur of the content behind
                // it. This state is separate from the per-screen GradientBackground state
                // used by the glass cards, so no haze/hazeChild share a state across an
                // ancestor/descendant boundary (which Haze forbids).
                val navHazeState = remember { HazeState() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Blur source for the nav: everything the screens draw.
                    Box(modifier = Modifier.fillMaxSize().haze(state = navHazeState)) {
                        MintNavGraph(
                            navController = navController,
                            config = config
                        )
                    }

                    if (showBottomNav) {
                        // The nav consumes navHazeState via hazeChild ' real content blur.
                        CompositionLocalProvider(LocalHazeState provides navHazeState) {
                            MintBottomNavigation(
                                selectedTab = selectedTab,
                                onTabSelected = { tab ->
                                    val route = when (tab) {
                                        MintTab.Icons -> MintRoutes.DASHBOARD
                                        MintTab.Widgets -> MintRoutes.WIDGETS
                                        MintTab.Wallpapers -> MintRoutes.WALLPAPERS
                                        MintTab.Settings -> MintRoutes.SETTINGS
                                    }
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            popUpTo(MintRoutes.DASHBOARD) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                visibleTabs = visibleTabs,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
                
                // Show changelog if applicable
                MintChangelogDialog(config = config)
            }
        }
    }
}

