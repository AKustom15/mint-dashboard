package com.akustom15.mint.library.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akustom15.mint.library.R
import com.akustom15.mint.library.ui.composables.RealBlurCard
import com.akustom15.mint.library.ui.theme.MintColors

enum class MintTab {
    Icons, Widgets, Wallpapers, Settings
}

@Composable
fun MintBottomNavigation(
    selectedTab: MintTab?,
    onTabSelected: (MintTab) -> Unit,
    visibleTabs: List<MintTab> = MintTab.entries,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp)
            .navigationBarsPadding()
    ) {
        RealBlurCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 400f,
            blurRadius = 100,
            blurPasses = 3,
            addOuterShadow = true
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                visibleTabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val tabColor by animateColorAsState(
                        targetValue = if (isSelected) MintColors.Primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "tabColor"
                    )
                    val tabLabel = when (tab) {
                        MintTab.Icons -> stringResource(R.string.mint_tab_icons)
                        MintTab.Widgets -> stringResource(R.string.mint_tab_widgets)
                        MintTab.Wallpapers -> stringResource(R.string.mint_tab_wallpapers)
                        MintTab.Settings -> stringResource(R.string.mint_tab_settings)
                    }
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                        label = "iconScale"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(3.dp)
                                .background(
                                    if (isSelected) MintColors.Primary else Color.Transparent,
                                    RoundedCornerShape(50)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(
                            imageVector = getTabIcon(tab, isSelected),
                            contentDescription = tabLabel,
                            tint = tabColor,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(iconScale)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tabLabel,
                            color = tabColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun getTabIcon(tab: MintTab, selected: Boolean): ImageVector {
    return when (tab) {
        MintTab.Icons -> if (selected) Icons.Filled.Apps else Icons.Outlined.Apps
        MintTab.Widgets -> if (selected) Icons.Filled.Widgets else Icons.Outlined.Widgets
        MintTab.Wallpapers -> if (selected) Icons.Filled.Image else Icons.Outlined.Image
        MintTab.Settings -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    }
}
