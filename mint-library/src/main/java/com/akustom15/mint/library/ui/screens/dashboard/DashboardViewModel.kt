package com.akustom15.mint.library.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akustom15.mint.library.data.IconPackManager
import com.akustom15.mint.library.data.LauncherInfo
import com.akustom15.mint.library.utils.LauncherUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun initData(context: Context) {
        viewModelScope.launch {
            IconPackManager.updateIconCounts(context)
            IconPackManager.loadCategoriesFromXml(context)
            val newIcons = IconPackManager.getNewIconResourceNames()

            _uiState.value = _uiState.value.copy(
                totalApps = IconPackManager.getTotalInstalledApps(),
                themedApps = IconPackManager.getThemedAppsCount(),
                totalIcons = IconPackManager.getTotalIconsCount(),
                missingApps = IconPackManager.getMissingAppsCount(),
                iconResourceNames = newIcons.ifEmpty { IconPackManager.getIconResourceNames() }
            )
        }
    }

    fun loadLaunchers(context: Context) {
        viewModelScope.launch {
            val launchers = LauncherUtils.getInstalledCompatibleLaunchers(context)
            _uiState.value = _uiState.value.copy(
                installedLaunchers = launchers,
                showLauncherDialog = true
            )
        }
    }

    fun dismissLauncherDialog() {
        _uiState.value = _uiState.value.copy(showLauncherDialog = false)
    }

    fun applyToLauncher(context: Context, launcher: LauncherInfo) {
        LauncherUtils.applyIconPackToLauncher(context, launcher)
        dismissLauncherDialog()
    }
}

data class DashboardUiState(
    val totalApps: Int = 0,
    val themedApps: Int = 0,
    val totalIcons: Int = 0,
    val missingApps: Int = 0,
    val iconResourceNames: List<String> = emptyList(),
    val installedLaunchers: List<LauncherInfo> = emptyList(),
    val showLauncherDialog: Boolean = false
)
