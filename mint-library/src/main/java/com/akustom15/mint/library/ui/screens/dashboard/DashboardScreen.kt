package com.akustom15.mint.library.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akustom15.mint.library.R
import com.akustom15.mint.library.config.MintConfig
import com.akustom15.mint.library.data.LauncherInfo
import androidx.compose.ui.draw.blur
import androidx.compose.ui.window.Dialog
import com.akustom15.mint.library.notifications.MintNotificationPreferences
import com.akustom15.mint.library.ui.composables.FrostedGlassDialogCard
import com.akustom15.mint.library.ui.composables.GlassButton
import com.akustom15.mint.library.ui.composables.GradientBackground
import com.akustom15.mint.library.ui.composables.LiquidGlassCard
import com.akustom15.mint.library.ui.composables.MintRatingDialog
import com.akustom15.mint.library.ui.composables.MintRatingManager
import com.akustom15.mint.library.ui.composables.RealBlurCard
import com.akustom15.mint.library.ui.composables.RotatingIconAnimation
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors

@Composable
fun DashboardScreen(
    config: MintConfig,
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToIconsPreview: () -> Unit,
    onNavigateToIconRequest: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val liquidColors = LocalLiquidGlassColors.current

    // Unread notifications count for dot indicator
    val notificationPrefs = remember { MintNotificationPreferences.getInstance(context) }
    val unreadCount = remember { notificationPrefs.getUnreadCount() }

    // Rating dialog: check if should be shown on this launch
    var showRatingDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.initData(context)
        showRatingDialog = MintRatingManager(context)
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (uiState.showLauncherDialog) Modifier.blur(20.dp) else Modifier)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))

            // ── Icon Animation Grid (like GlassWave) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.iconResourceNames.isNotEmpty()) {
                    RotatingIconAnimation(
                        modifier = Modifier.fillMaxSize(),
                        iconResourceNames = uiState.iconResourceNames,
                        batchDisplayDurationMillis = 5500L,
                        iconAppearanceDurationMillis = 1000L,
                        staggerDelayMillis = 80L,
                        iconSize = 80
                    )
                } else {
                    CircularProgressIndicator(color = MintColors.Primary)
                }

                // ── Notification dot ── shown in top-right corner, non-intrusive
                if (unreadCount > 0) {
                    val infiniteTransition = rememberInfiniteTransition(label = "notif_dot_pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(700, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_scale"
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp)
                            .scale(pulseScale)
                            .background(androidx.compose.ui.graphics.Color(0xFFFF3B30), CircleShape)
                            .border(2.dp, liquidColors.textPrimary.copy(alpha = 0.2f), CircleShape)
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔔 ${if (unreadCount > 9) "9+" else unreadCount}",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── App Name ──
            Text(
                text = config.appName,
                color = liquidColors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ── APLICAR ICONOS button (real frosted glass blur) ──
            RealBlurCard(
                modifier = Modifier
                    .width(264.dp)
                    .semantics { role = Role.Button }
                    .clickable { viewModel.loadLaunchers(context) },
                cornerRadius = 100f,
                blurRadius = 100,
                blurPasses = 3,
                addOuterShadow = true
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.mint_dashboard_apply_icons).uppercase(),
                        color = liquidColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Icon Count Card (clickable → Icons Preview) ──
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                onClick = onNavigateToIconsPreview
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 16.dp)
                ) {
                    val iconCountText = uiState.totalIcons.toString()
                    val dynamicFontSize = when {
                        iconCountText.length >= 5 -> 36.sp
                        iconCountText.length >= 4 -> 42.sp
                        else -> 48.sp
                    }
                    Text(
                        text = iconCountText,
                        fontSize = dynamicFontSize,
                        color = liquidColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.mint_dashboard_custom_icons),
                        fontSize = 14.sp,
                        color = liquidColors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Icon Request Progress Card (clickable → Icon Request) ──
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                onClick = onNavigateToIconRequest
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.mint_dashboard_request_title),
                        color = liquidColors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.mint_dashboard_total_installed, uiState.totalApps),
                        color = liquidColors.textPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val progress = if (uiState.totalApps > 0)
                        uiState.themedApps.toFloat() / uiState.totalApps else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MintColors.Primary.copy(alpha = 0.3f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(MintColors.Primary)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.mint_dashboard_themed_apps, uiState.themedApps),
                            color = liquidColors.textPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = stringResource(R.string.mint_dashboard_missing_apps, uiState.missingApps),
                            color = liquidColors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Space for additional content (e.g. PUM billing widget)
            config.additionalDashboardContent?.let { content ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    content()
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Launcher dialog with frosted glass card
        if (uiState.showLauncherDialog) {
            Dialog(onDismissRequest = { viewModel.dismissLauncherDialog() }) {
                FrostedGlassDialogCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.mint_dashboard_select_launcher),
                            color = liquidColors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (uiState.installedLaunchers.isEmpty()) {
                            Text(
                                stringResource(R.string.mint_dashboard_no_launchers),
                                color = liquidColors.textSecondary
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                uiState.installedLaunchers.forEach { launcher ->
                                    GlassButton(
                                        text = launcher.name,
                                        onClick = { viewModel.applyToLauncher(context, launcher) },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        isPrimary = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.dismissLauncherDialog() }) {
                            Text(stringResource(R.string.mint_settings_cancel), color = MintColors.Primary)
                        }
                    }
                }
            }
        }

        // Rating dialog — shown on qualifying launches (see MintRatingManager)
        if (showRatingDialog && !uiState.showLauncherDialog) {
            MintRatingDialog(onDismiss = { showRatingDialog = false })
        }
    }
}
