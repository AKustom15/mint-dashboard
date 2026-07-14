package com.akustom15.mint.library.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akustom15.mint.library.R
import com.akustom15.mint.library.config.MintConfig
import com.akustom15.mint.library.ui.composables.GlassButton
import com.akustom15.mint.library.ui.composables.GradientBackground
import com.akustom15.mint.library.ui.composables.LiquidGlassCard
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors

@Composable
fun AboutScreen(
    config: MintConfig,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val liquidColors = LocalLiquidGlassColors.current

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    text = stringResource(R.string.mint_about_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = liquidColors.textPrimary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // App Info Card
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (config.appIcon != null) {
                            Image(
                                painter = painterResource(id = config.appIcon),
                                contentDescription = "App Icon",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Text(
                            text = config.appName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = liquidColors.textPrimary
                        )
                        if (config.appSubtitle.isNotEmpty()) {
                            Text(
                                text = config.appSubtitle,
                                fontSize = 14.sp,
                                color = liquidColors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "v${config.versionName}",
                            fontSize = 12.sp,
                            color = MintColors.Primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Developer Card
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.mint_about_developer),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintColors.Primary,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = config.developerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = liquidColors.textPrimary
                        )
                        if (config.developerWebsite.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = config.developerWebsite,
                                fontSize = 13.sp,
                                color = MintColors.Primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Social Links
                val socialLinks = listOf(
                    Triple("X / Twitter", config.xUrl, config.xIcon),
                    Triple("Instagram", config.instagramUrl, config.instagramIcon),
                    Triple("YouTube", config.youtubeUrl, config.youtubeIcon),
                    Triple("Facebook", config.facebookUrl, config.facebookIcon),
                    Triple("Telegram", config.telegramUrl, config.telegramIcon)
                ).filter { it.second.isNotEmpty() }

                if (socialLinks.isNotEmpty()) {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.mint_about_social),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintColors.Primary,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            socialLinks.forEach { (name, url, iconRes) ->
                                GlassButton(
                                    text = name,
                                    icon = painterResource(id = iconRes),
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // More Apps / Links
                if (config.moreAppsUrl.isNotEmpty()) {
                    GlassButton(
                        text = stringResource(R.string.mint_about_more_apps),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.moreAppsUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Credits
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.mint_about_powered_by),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintColors.Primary,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.mint_about_library_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = liquidColors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.mint_about_library_desc),
                            fontSize = 13.sp,
                            color = liquidColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
