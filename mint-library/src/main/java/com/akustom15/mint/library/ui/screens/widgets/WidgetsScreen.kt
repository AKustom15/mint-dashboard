package com.akustom15.mint.library.ui.screens.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.akustom15.mint.library.R
import com.akustom15.mint.library.config.MintConfig
import com.akustom15.mint.library.data.MintGridColumns
import com.akustom15.mint.library.data.MintPreferences
import com.akustom15.mint.library.model.MintWidgetItem
import com.akustom15.mint.library.ui.composables.GradientBackground
import com.akustom15.mint.library.ui.composables.RealBlurCard
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors
import com.akustom15.mint.library.utils.MintAssetsReader
import com.akustom15.mint.library.utils.MintKustomIntegration

@Composable
fun WidgetsScreen(
    config: MintConfig
) {
    val context = LocalContext.current
    val liquidColors = LocalLiquidGlassColors.current
    val preferences = remember { MintPreferences.getInstance(context) }
    val gridColumns by preferences.gridColumns.collectAsState()
    val widgets = remember { mutableStateOf<List<MintWidgetItem>>(emptyList()) }

    // Load widgets from assets
    LaunchedEffect(Unit) {
        widgets.value = MintAssetsReader.getWidgetsFromAssets(context)
    }

    GradientBackground {
        if (widgets.value.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.mint_widgets_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = liquidColors.textSecondary
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns.count),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with title and grid toggle
                item(span = { GridItemSpan(maxLineSpan) }) {
                    WidgetsHeader(
                        widgetCount = widgets.value.size,
                        gridColumns = gridColumns,
                        onToggleColumns = {
                            val newColumns = if (gridColumns == MintGridColumns.ONE)
                                MintGridColumns.TWO else MintGridColumns.ONE
                            preferences.setGridColumns(newColumns)
                        }
                    )
                }

                // Widget items
                items(widgets.value) { widget ->
                    WidgetCard(
                        widget = widget,
                        isCompact = gridColumns.count > 1,
                        appIcon = config.appIcon,
                        appName = config.appName,
                        onApplyClick = {
                            if (widget.isKlwp) {
                                MintKustomIntegration.applyWallpaper(
                                    context = context,
                                    wallpaperFileName = widget.fileName,
                                    packageName = config.packageName
                                )
                            } else {
                                MintKustomIntegration.applyWidget(
                                    context = context,
                                    widgetFileName = widget.fileName,
                                    packageName = config.packageName
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetsHeader(
    widgetCount: Int,
    gridColumns: MintGridColumns,
    onToggleColumns: () -> Unit
) {
    val liquidColors = LocalLiquidGlassColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
            Text(
                text = stringResource(R.string.mint_tab_widgets),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = liquidColors.textPrimary
            )
            Text(
                text = "$widgetCount widgets",
                fontSize = 14.sp,
                color = liquidColors.textSecondary
            )
        }

        IconButton(onClick = onToggleColumns) {
            Icon(
                imageVector = if (gridColumns == MintGridColumns.ONE)
                    Icons.Default.GridView else Icons.Default.ViewStream,
                contentDescription = "Toggle grid",
                tint = MintColors.Primary
            )
        }
    }
}

@Composable
private fun WidgetCard(
    widget: MintWidgetItem,
    isCompact: Boolean,
    appIcon: Int?,
    appName: String,
    onApplyClick: () -> Unit
) {
    val liquidColors = LocalLiquidGlassColors.current

    RealBlurCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 40f,
        blurRadius = 80,
        blurPasses = 3,
        addOuterShadow = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Preview container - 16:9 aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (widget.previewPath != null) {
                    AsyncImage(
                        model = widget.previewPath,
                        contentDescription = widget.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = widget.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = liquidColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Name
            Text(
                text = widget.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = liquidColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Description
            Text(
                text = widget.description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = liquidColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: app info + apply button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon + name
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (appIcon != null) {
                        AsyncImage(
                            model = appIcon,
                            contentDescription = "App icon",
                            modifier = Modifier
                                .size(if (isCompact) 18.dp else 22.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(if (isCompact) 3.dp else 5.dp))
                    }

                    Text(
                        text = appName,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        color = liquidColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Apply button
                Button(
                    onClick = onApplyClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintColors.Primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(if (isCompact) 26.dp else 34.dp),
                    contentPadding = if (isCompact) {
                        PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    } else {
                        PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Apply",
                        tint = MintColors.OnPrimary,
                        modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
                    )
                    if (!isCompact) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.mint_widgets_apply),
                            color = MintColors.OnPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
