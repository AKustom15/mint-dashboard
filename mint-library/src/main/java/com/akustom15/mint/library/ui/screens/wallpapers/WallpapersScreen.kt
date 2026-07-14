package com.akustom15.mint.library.ui.screens.wallpapers

import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.akustom15.mint.library.R
import com.akustom15.mint.library.ui.composables.FrostedGlassDialogCard
import com.akustom15.mint.library.ui.composables.GradientBackground
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.akustom15.mint.library.ui.theme.MintColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WallpapersScreen(
    onNavigateBack: () -> Unit,
    wallpapersUrl: String = "",
    appName: String = "Mint",
    viewModel: WallpapersViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val liquidColors = LocalLiquidGlassColors.current
    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }

    LaunchedEffect(wallpapersUrl) {
        viewModel.loadWallpapers(wallpapersUrl)
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    text = stringResource(R.string.mint_wallpapers_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = liquidColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            when (val state = uiState) {
                is WallpapersUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MintColors.Primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.mint_wallpapers_loading),
                                color = liquidColors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                is WallpapersUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MintColors.Tertiary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                color = liquidColors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.loadWallpapers(wallpapersUrl) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintColors.Primary,
                                    contentColor = MintColors.OnPrimary
                                )
                            ) {
                                Text(stringResource(R.string.mint_wallpapers_retry))
                            }
                        }
                    }
                }

                is WallpapersUiState.Success -> {
                    val wallpapers = state.wallpapers
                    if (wallpapers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.mint_wallpapers_empty),
                                color = liquidColors.textSecondary,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        val gridState = rememberLazyGridState()

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                wallpapers,
                                key = { it.url }
                            ) { wallpaper ->
                                val index = wallpapers.indexOf(wallpaper)
                                AnimatedWallpaperItem(
                                    wallpaper = wallpaper,
                                    index = index,
                                    gridState = gridState
                                ) {
                                    selectedWallpaper = wallpaper
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Detail Dialog
        selectedWallpaper?.let { wallpaper ->
            WallpaperDetailDialog(
                wallpaper = wallpaper,
                appName = appName,
                onDismiss = { selectedWallpaper = null }
            )
        }
    }
}

@Composable
private fun AnimatedWallpaperItem(
    wallpaper: WallpaperItem,
    index: Int,
    gridState: LazyGridState,
    onClick: () -> Unit
) {
    val visibleItems = gridState.layoutInfo.visibleItemsInfo
    val isVisible = visibleItems.any { it.index == index }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.2f,
        animationSpec = tween(durationMillis = 600, easing = EaseOutBack),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.50f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MintColors.SurfaceDark)
    ) {
        Box {
            CachedWallpaperImage(
                url = wallpaper.url,
                contentDescription = wallpaper.name,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CachedWallpaperImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    val imageAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = tween(300),
        label = "img_alpha"
    )
    val context = LocalContext.current

    Box(modifier = modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .listener(
                    onStart = { isLoading = true },
                    onSuccess = { _, _ -> isLoading = false },
                    onError = { _, _ -> isLoading = false }
                )
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .alpha(imageAlpha),
            contentScale = ContentScale.Crop
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MintColors.Primary,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun WallpaperDetailDialog(
    wallpaper: WallpaperItem,
    appName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liquidColors = LocalLiquidGlassColors.current
    var showApplyOptions by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val isOverlayOpen = showApplyOptions || showInfoDialog

    // Haze state so the bottom info panel renders a real blur of the wallpaper
    // behind it (same technique as the bottom nav). Both live in this fullscreen
    // Dialog window, so Haze can capture the image.
    val detailHazeState = remember { HazeState() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Wallpaper image — the Haze blur source for the info panel; also
            // blurs directly when an overlay dialog is open.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isOverlayOpen) Modifier.blur(20.dp) else Modifier)
                    .haze(state = detailHazeState)
            ) {
                ZoomableWallpaper(
                    url = wallpaper.url,
                    contentDescription = wallpaper.name
                )
            }

            // Close button — frosted glass style
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x331A1A2E))
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Bottom panel — frosted glass style matching bottom nav
            val panelShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            val panelGloss = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.03f)
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(panelShape)
                    .hazeChild(
                        state = detailHazeState,
                        style = HazeStyle(
                            backgroundColor = Color.Black,
                            tints = listOf(HazeTint(Color(0x591A1A2E))),
                            blurRadius = 30.dp,
                            noiseFactor = 0f
                        )
                    )
                    .background(panelGloss)
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = panelShape
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = wallpaper.name,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (wallpaper.author.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "by ${wallpaper.author}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Info button
                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color.White)
                        }

                        // Apply button
                        IconButton(
                            onClick = { showApplyOptions = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(Icons.Filled.Wallpaper, contentDescription = "Apply", tint = Color.White)
                        }

                        // Download button
                        IconButton(
                            onClick = { downloadWallpaper(context, wallpaper, appName) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = "Download", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Apply wallpaper options dialog — frosted glass card
    if (showApplyOptions) {
        Dialog(onDismissRequest = { showApplyOptions = false }) {
            FrostedGlassDialogCard {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.mint_wallpapers_apply),
                        color = liquidColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                applyWallpaper(context, wallpaper.url, WallpaperManager.FLAG_SYSTEM)
                                showApplyOptions = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintColors.Primary,
                            contentColor = MintColors.OnPrimary
                        )
                    ) {
                        Text(stringResource(R.string.mint_wallpapers_home_screen))
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                applyWallpaper(context, wallpaper.url, WallpaperManager.FLAG_LOCK)
                                showApplyOptions = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintColors.Primary,
                            contentColor = MintColors.OnPrimary
                        )
                    ) {
                        Text(stringResource(R.string.mint_wallpapers_lock_screen))
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                applyWallpaper(
                                    context,
                                    wallpaper.url,
                                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                                )
                                showApplyOptions = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintColors.Primary,
                            contentColor = MintColors.OnPrimary
                        )
                    ) {
                        Text(stringResource(R.string.mint_wallpapers_both_screens))
                    }

                    TextButton(
                        onClick = { showApplyOptions = false },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(stringResource(R.string.mint_wallpapers_close), color = liquidColors.textSecondary)
                    }
                }
            }
        }
    }

    // Info dialog — frosted glass card
    if (showInfoDialog) {
        Dialog(onDismissRequest = { showInfoDialog = false }) {
            FrostedGlassDialogCard {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.mint_wallpapers_info),
                        color = liquidColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WallpaperInfoRow(stringResource(R.string.mint_wallpapers_name_label), wallpaper.name)
                    WallpaperInfoRow(stringResource(R.string.mint_wallpapers_author_label), wallpaper.author)
                    wallpaper.dimensions?.let {
                        WallpaperInfoRow(stringResource(R.string.mint_wallpapers_dimensions_label), it)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintColors.Primary,
                            contentColor = MintColors.OnPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.mint_wallpapers_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "$label:",
            color = LocalLiquidGlassColors.current.textSecondary,
            fontSize = 14.sp,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            color = LocalLiquidGlassColors.current.textPrimary,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ZoomableWallpaper(
    url: String,
    contentDescription: String?
) {
    var scale by remember(url) { mutableStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MintColors.BackgroundDark)
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(url) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val proposedScale = (scale * zoom).coerceIn(1f, 4f)
                        if (proposedScale <= 1.001f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val scaleChange = proposedScale / scale
                            scale = proposedScale
                            var newOffset = offset + pan * scaleChange
                            val maxX = (containerWidthPx * (scale - 1f)) / 2f
                            val maxY = (containerHeightPx * (scale - 1f)) / 2f
                            newOffset = Offset(
                                x = newOffset.x.coerceIn(-maxX, maxX),
                                y = newOffset.y.coerceIn(-maxY, maxY)
                            )
                            offset = newOffset
                        }
                    }
                }
        ) {
            CachedWallpaperImage(
                url = url,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        }
    }
}

private suspend fun applyWallpaper(context: Context, imageUrl: String, which: Int) {
    try {
        val bitmap = withContext(Dispatchers.IO) {
            val stream = java.net.URL(imageUrl).openStream()
            android.graphics.BitmapFactory.decodeStream(stream)
        }
        if (bitmap != null) {
            WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, which)
            val msg = when (which) {
                WallpaperManager.FLAG_SYSTEM -> context.getString(R.string.mint_wallpapers_applied_home)
                WallpaperManager.FLAG_LOCK -> context.getString(R.string.mint_wallpapers_applied_lock)
                else -> context.getString(R.string.mint_wallpapers_applied_both)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            bitmap.recycle()
        }
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.mint_wallpapers_failed), Toast.LENGTH_SHORT).show()
    }
}

private fun downloadWallpaper(context: Context, wallpaper: WallpaperItem, appName: String) {
    if (!wallpaper.downloadable) {
        Toast.makeText(context, context.getString(R.string.mint_wallpapers_not_downloadable), Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val request = DownloadManager.Request(Uri.parse(wallpaper.url))
            .setTitle("${wallpaper.name} - $appName")
            .setDescription(context.getString(R.string.mint_wallpapers_downloading))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_PICTURES,
                "$appName/${wallpaper.name}.jpg"
            )
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, context.getString(R.string.mint_wallpapers_download_started), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.mint_wallpapers_failed), Toast.LENGTH_SHORT).show()
    }
}
