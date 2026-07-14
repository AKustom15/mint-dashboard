package com.akustom15.mint.library.ui.screens.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akustom15.mint.library.R
import com.akustom15.mint.library.ui.composables.GradientBackground
import com.akustom15.mint.library.ui.composables.LiquidGlassCard
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun IconsPreviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: IconsPreviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val liquidColors = LocalLiquidGlassColors.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    LaunchedEffect(Unit) {
        viewModel.loadIcons(context)
    }

    // Fixed tab order: All, New, Favorites, System, Folder, Variant
    val tabTitles = remember { listOf("All", "New", "★", "System", "Folder", "Variant") }
    val totalPages = tabTitles.size
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })
    val coroutineScope = rememberCoroutineScope()

    val isDialogOpen = uiState.selectedIcon != null

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
                    text = stringResource(R.string.mint_icons_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = liquidColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${uiState.allIcons.size}",
                    fontSize = 14.sp,
                    color = liquidColors.textSecondary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }

            // Search Bar
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = liquidColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        textStyle = LocalTextStyle.current.copy(
                            color = liquidColors.textPrimary,
                            fontSize = 14.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MintColors.Primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    stringResource(R.string.mint_icons_search),
                                    color = liquidColors.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChanged("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = liquidColors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Tab Row with blur/relief effect ──
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                contentColor = liquidColors.textPrimary,
                divider = {},
                indicator = {},
                edgePadding = 12.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = pagerState.currentPage == index
                    val tabShape = RoundedCornerShape(50)
                    val bgColor = if (isSelected) {
                        if (isDark) MintColors.ButtonSurfaceDark else MintColors.ButtonSurfaceLight
                    } else Color.Transparent
                    val borderCol = if (isSelected) {
                        if (isDark) MintColors.GlassBorderDark else MintColors.GlassBorderLight
                    } else Color.Transparent
                    val highlight = if (isSelected) {
                        if (isDark) MintColors.ButtonHighlightDark else MintColors.ButtonHighlightLight
                    } else Color.Transparent

                    Tab(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .then(
                                if (isSelected) Modifier
                                    .shadow(8.dp, tabShape)
                                    .clip(tabShape)
                                    .background(bgColor)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(highlight, Color.Transparent)
                                        )
                                    )
                                    .border(0.5.dp, borderCol, tabShape)
                                else Modifier
                                    .clip(tabShape)
                                    .background(liquidColors.glassSurface.copy(alpha = 0.3f))
                            ),
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isSelected) MintColors.Primary else liquidColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Loading ──
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MintColors.Primary)
                }
            } else {
                // ── HorizontalPager ──
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val catMap = uiState.categoryMap
                    val iconsForPage = remember(
                        uiState.allIcons, catMap, uiState.favorites,
                        uiState.searchQuery, page
                    ) {
                        val query = uiState.searchQuery.lowercase()
                        val base: List<IconPreviewItem> = when (page) {
                            0 -> uiState.allIcons                                          // All
                            1 -> uiState.allIcons.filter { catMap["new"]?.contains(it.name) == true }     // New
                            2 -> uiState.allIcons.filter { uiState.favorites.contains(it.name) }          // ★
                            3 -> uiState.allIcons.filter { catMap["system"]?.contains(it.name) == true }  // System
                            4 -> uiState.allIcons.filter { catMap["folder"]?.contains(it.name) == true }  // Folder
                            5 -> uiState.allIcons.filter { catMap["variant"]?.contains(it.name) == true } // Variant
                            else -> emptyList()
                        }
                        if (query.isBlank()) base
                        else base.filter { it.name.lowercase().contains(query) }
                    }

                    IconPageContent(
                        icons = iconsForPage,
                        favorites = uiState.favorites,
                        onIconClick = { viewModel.selectIcon(it) },
                        isDark = isDark
                    )
                }
            }
        }

        // Icon Detail Dialog with blur
        uiState.selectedIcon?.let { icon ->
            IconDetailDialog(
                icon = icon,
                isFavorite = uiState.favorites.contains(icon.name),
                isDark = isDark,
                onToggleFavorite = { viewModel.toggleFavorite(context, icon.name) },
                onDismiss = { viewModel.selectIcon(null) }
            )
        }
    }
}

@Composable
private fun IconPageContent(
    icons: List<IconPreviewItem>,
    favorites: Set<String>,
    onIconClick: (IconPreviewItem) -> Unit,
    isDark: Boolean
) {
    val liquidColors = LocalLiquidGlassColors.current

    if (icons.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.mint_icons_no_results),
                color = liquidColors.textSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        val gridState = rememberLazyGridState()
        val columnsCount = 4

        // Scroll indicator calculations
        val totalItems = icons.size
        val totalRows = remember(totalItems) {
            ((totalItems + columnsCount - 1) / columnsCount).coerceAtLeast(1)
        }
        val firstVisibleRow by remember { derivedStateOf { gridState.firstVisibleItemIndex / columnsCount } }
        val startFraction by remember(totalRows) {
            derivedStateOf { (firstVisibleRow.toFloat() / totalRows.toFloat()).coerceIn(0f, 0.85f) }
        }
        var trackHeightPx by remember { mutableIntStateOf(0) }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                contentPadding = PaddingValues(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(icons, key = { it.name }) { icon ->
                    IconGridItem(
                        icon = icon,
                        isFavorite = favorites.contains(icon.name),
                        onClick = { onIconClick(icon) }
                    )
                }
            }

            // Scroll indicator (right side)
            if (totalItems > columnsCount * 4) {
                val thumbHeightDp = 48.dp
                val thumbOffset = with(LocalDensity.current) {
                    (trackHeightPx * startFraction).toDp()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(5.dp)
                        .padding(end = 2.dp, top = 8.dp, bottom = 100.dp)
                        .onGloballyPositioned { trackHeightPx = it.size.height }
                ) {
                    // Track
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                            .background(liquidColors.textSecondary.copy(alpha = 0.1f))
                    )
                    // Thumb
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(thumbHeightDp)
                            .offset(y = thumbOffset)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MintColors.Primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun IconGridItem(
    icon: IconPreviewItem,
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) MintColors.GlassDark else MintColors.GlassLight

    LiquidGlassCard(
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        cornerRadiusPx = 36f,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBg)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = icon.resourceId),
                contentDescription = icon.name,
                modifier = Modifier.fillMaxSize(0.75f)
            )
            if (isFavorite) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MintColors.Tertiary,
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
private fun IconDetailDialog(
    icon: IconPreviewItem,
    isFavorite: Boolean,
    isDark: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    val liquidColors = LocalLiquidGlassColors.current
    val dialogShape = RoundedCornerShape(28.dp)
    val surfaceColor = if (isDark) MintColors.ButtonSurfaceDark else MintColors.ButtonSurfaceLight
    val borderColor = if (isDark) MintColors.GlassBorderDark else MintColors.GlassBorderLight
    val highlight = if (isDark) MintColors.ButtonHighlightDark else MintColors.ButtonHighlightLight
    val shadowColor = if (isDark) MintColors.ButtonShadowDark else MintColors.ButtonShadowLight

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 32.dp,
                    shape = dialogShape,
                    ambientColor = shadowColor,
                    spotColor = shadowColor
                )
                .clip(dialogShape)
                .background(
                    if (isDark) Color(0x331A1A2E) else Color(0x33FFFFFF)
                )
                .background(
                    Brush.verticalGradient(
                        listOf(
                            highlight.copy(alpha = 0.15f),
                            Color.Transparent,
                            surfaceColor.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(1.dp, borderColor.copy(alpha = 0.5f), dialogShape)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = icon.resourceId),
                    contentDescription = icon.name,
                    modifier = Modifier.size(128.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = icon.name.removePrefix("icon_").replace("_", " ")
                        .replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = liquidColors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = icon.name,
                    fontSize = 12.sp,
                    color = liquidColors.textSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (isFavorite) MintColors.Tertiary else liquidColors.textSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        stringResource(R.string.mint_icons_close),
                        color = MintColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
