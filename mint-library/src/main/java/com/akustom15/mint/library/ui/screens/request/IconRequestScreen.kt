package com.akustom15.mint.library.ui.screens.request

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akustom15.mint.library.R
import com.akustom15.mint.library.billing.MintPremiumPreferences
import com.akustom15.mint.library.billing.PremiumPurchaseDialog
import com.akustom15.mint.library.config.MintConfig
import com.akustom15.mint.library.ui.composables.GradientBackground
import com.akustom15.mint.library.ui.composables.LiquidGlassCard
import com.akustom15.mint.library.ui.composables.LocalHazeState
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors
import com.akustom15.mint.library.ui.composables.RealBlurCard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun IconRequestScreen(
    onNavigateBack: () -> Unit,
    config: MintConfig = MintConfig(appName = "", packageName = ""),
    viewModel: IconRequestViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val liquidColors = LocalLiquidGlassColors.current

    var showPremiumDialog by remember { mutableStateOf(false) }
    var premiumAvailable by remember { mutableStateOf(MintPremiumPreferences.getPremiumRequestCount(context)) }

    LaunchedEffect(Unit) {
        viewModel.configure(
            appName = config.appName,
            email = config.iconRequestEmail.ifEmpty { "akustom15help@gmail.com" },
            collection = config.firestoreCollection
        )
        viewModel.setFreeLimit(config.freeRequestLimit)
        viewModel.setPremiumAvailable(premiumAvailable)
        viewModel.loadMissingIcons(context)
        if (config.appConfigUrl.isNotEmpty()) {
            viewModel.loadRemoteConfig(config.appConfigUrl)
        }
    }
    
    val securityManager = remember { com.akustom15.mint.library.security.SecurityManager.getInstance(context, config) }
    val securityState by securityManager.securityState.collectAsState()
    val antiPiracyStatus = securityState.javaClass.simpleName

    // Blur source for the floating Send button: the screen content (status card
    // + app list). The FAB is a hazeChild sibling of this Column, so it renders a
    // real blur of the content behind it — same technique as the bottom nav.
    val fabHazeState = remember { HazeState() }

    GradientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (showPremiumDialog) Modifier.blur(20.dp) else Modifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = fabHazeState)
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
                        text = stringResource(R.string.mint_icon_request_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = liquidColors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.selectAll() }) {
                        Icon(
                            Icons.Filled.SelectAll,
                            contentDescription = stringResource(R.string.mint_icon_request_select_all),
                            tint = liquidColors.textSecondary
                        )
                    }
                    IconButton(onClick = { viewModel.deselectAll() }) {
                        Icon(
                            Icons.Filled.Deselect,
                            contentDescription = stringResource(R.string.mint_icon_request_deselect_all),
                            tint = liquidColors.textSecondary
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        // Status card matching GlassWave design
                        RequestStatusCard(
                            totalRequests = config.freeRequestLimit + premiumAvailable,
                            usedRequests = uiState.requestedIcons.size + uiState.selectedCount,
                            availableRequests = uiState.totalAvailable - uiState.selectedCount,
                            premiumEnabled = config.premiumEnabled && uiState.remoteConfig.allowPremiumRequests,
                            pauseMessage = uiState.remoteConfig.pauseMessage,
                            onBuyPremium = { showPremiumDialog = true }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.isLoading || uiState.isSending) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MintColors.Primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (uiState.isSending)
                                            stringResource(R.string.mint_icon_request_preparing)
                                        else stringResource(R.string.mint_icon_request_loading),
                                        fontSize = 14.sp,
                                        color = liquidColors.textSecondary
                                    )
                                }
                            }
                        }
                    } else if (uiState.missingApps.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(R.string.mint_icon_request_no_apps),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MintColors.Primary
                                    )
                                    Text(
                                        text = stringResource(R.string.mint_icon_request_no_apps_desc),
                                        fontSize = 14.sp,
                                        color = liquidColors.textSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.missingApps,
                            key = { _, app -> app.packageName }
                        ) { index, app ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                MissingIconRow(
                                    app = app,
                                    canSelect = !app.alreadyRequested &&
                                        (uiState.selectedCount < uiState.totalAvailable || app.isSelected),
                                    onClick = { viewModel.toggleSelection(index) }
                                )
                            }
                        }
                    }
                }
            }

            // Liquid glass FAB overlay — real blur of the content behind it
            if (uiState.selectedCount > 0 && !uiState.isLoading && !uiState.isSending && uiState.remoteConfig.allowFreeRequests) {
              CompositionLocalProvider(LocalHazeState provides fabHazeState) {
                RealBlurCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .navigationBarsPadding()
                        .clickable { viewModel.sendIconRequest(context, antiPiracyStatus) },
                    cornerRadius = 70f,
                    blurRadius = 100,
                    blurPasses = 3,
                    addOuterShadow = true
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = liquidColors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "${stringResource(R.string.mint_icon_request_send)} (${uiState.selectedCount}/${uiState.totalAvailable})",
                            color = liquidColors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
              }
            }
        }
    }

    // Premium purchase dialog
    if (showPremiumDialog) {
        PremiumPurchaseDialog(
            products = config.premiumProducts,
            onDismiss = { showPremiumDialog = false },
            onPurchaseSuccess = { _, iconCount ->
                showPremiumDialog = false
                premiumAvailable = MintPremiumPreferences.getPremiumRequestCount(context)
                viewModel.setPremiumAvailable(premiumAvailable)
            }
        )
    }
}

/**
 * Status card matching GlassWave screenshot design:
 * - Email icon + "Icon Request" title
 * - Description text
 * - Total / progress bar
 * - Available / Used counts
 * - Buy Premium button
 */
@Composable
private fun RequestStatusCard(
    totalRequests: Int,
    usedRequests: Int,
    availableRequests: Int,
    premiumEnabled: Boolean,
    pauseMessage: String = "",
    onBuyPremium: () -> Unit
) {
    val liquidColors = LocalLiquidGlassColors.current

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: icon + title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = liquidColors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.mint_icon_request_header),
                    color = liquidColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Description
            Text(
                text = stringResource(R.string.mint_icon_request_description),
                color = liquidColors.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            if (pauseMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = pauseMessage,
                        color = MintColors.Tertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp).fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total label
            Text(
                text = stringResource(R.string.mint_icon_request_total),
                color = liquidColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$totalRequests ${stringResource(R.string.mint_icon_request_request)}",
                color = liquidColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = {
                    if (totalRequests > 0) usedRequests.toFloat() / totalRequests.toFloat()
                    else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MintColors.Primary,
                trackColor = liquidColors.textSecondary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Available / Used row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.mint_icon_request_available),
                        color = liquidColors.textPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "$availableRequests ${stringResource(R.string.mint_icon_request_request)}",
                        color = liquidColors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.mint_icon_request_used),
                        color = liquidColors.textPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "$usedRequests ${stringResource(R.string.mint_icon_request_request)}",
                        color = liquidColors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Buy Premium button
            if (premiumEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBuyPremium,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintColors.Primary,
                        contentColor = MintColors.OnPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.mint_buy_premium),
                        color = MintColors.OnPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MissingIconRow(
    app: MissingIconApp,
    canSelect: Boolean,
    onClick: () -> Unit
) {
    val liquidColors = LocalLiquidGlassColors.current

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = if (canSelect) onClick else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            app.icon?.let { drawable ->
                val bitmap = remember(drawable) {
                    drawable.toBitmap(48, 48)
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } ?: Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1),
                    fontWeight = FontWeight.Bold,
                    color = liquidColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (app.alreadyRequested)
                        liquidColors.textSecondary.copy(alpha = 0.6f)
                    else liquidColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = liquidColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (app.alreadyRequested) {
                    Text(
                        text = stringResource(R.string.mint_icon_request_already_requested),
                        fontSize = 11.sp,
                        color = MintColors.Tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Icon(
                imageVector = if (app.isSelected) Icons.Filled.CheckBox
                else Icons.Filled.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = when {
                    app.alreadyRequested -> liquidColors.textSecondary.copy(alpha = 0.3f)
                    app.isSelected -> MintColors.Primary
                    canSelect -> liquidColors.textSecondary
                    else -> liquidColors.textSecondary.copy(alpha = 0.3f)
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
