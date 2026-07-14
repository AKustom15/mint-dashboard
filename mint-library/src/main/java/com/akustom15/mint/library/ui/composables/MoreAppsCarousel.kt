package com.akustom15.mint.library.ui.composables

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.akustom15.mint.library.config.MoreApp
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors

/**
 * Horizontal carousel showing other apps from the developer.
 *
 * Each card mirrors the Pump/Lunex "More Apps" design:
 *  - 340 dp wide
 *  - 280 dp tall screenshot area (LazyRow when multiple screenshots)
 *  - App icon (44 dp, rounded) + name + "App" label
 *  - Short description (max 3 lines)
 *  - Full-width Install button that opens Play Store
 *
 * Data comes from the static `moreApps` list in MintConfig or a remote JSON URL
 * set via `moreAppsJsonUrl` in MintConfig.
 */
@Composable
fun MoreAppsCarousel(
    apps: List<MoreApp>,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return

    Column(modifier = modifier) {
        // Section header — matches other SettingsScreen section labels
        Text(
            text = stringResource(R.string.mint_more_apps_title),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MintColors.Primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(apps) { app ->
                MoreAppCard(app)
            }
        }
    }
}

@Composable
private fun MoreAppCard(app: MoreApp) {
    val context = LocalContext.current
    val liquidColors = LocalLiquidGlassColors.current

    // Helper to open Play Store — uses FLAG_ACTIVITY_NEW_TASK for safety
    fun openStore() {
        if (app.playStoreUrl.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.playStoreUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open Play Store", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Card is NOT clickable — only the Install button handles navigation.
    // This avoids click interception issues between LiquidGlassCard and the Button.
    LiquidGlassCard(
        modifier = Modifier.width(340.dp),
        shape = RoundedCornerShape(18.dp),
        onClick = null  // intentionally null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Screenshot area (280 dp tall) ──────────────────────────────
            if (app.screenshotUrls.isNotEmpty()) {
                if (app.screenshotUrls.size == 1) {
                    // Single screenshot → full-width banner
                    AsyncImage(
                        model = app.screenshotUrls.first(),
                        contentDescription = app.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Multiple screenshots → horizontal scroll strip
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(app.screenshotUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = app.name,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(160.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── App icon + name + "App" label ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (app.iconUrl.isNotEmpty()) {
                    AsyncImage(
                        model = app.iconUrl,
                        contentDescription = app.name,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MintColors.Primary.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = app.name,
                            tint = MintColors.Primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = liquidColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "App",
                        fontSize = 12.sp,
                        color = liquidColors.textSecondary
                    )
                }
            }

            // ── Description ────────────────────────────────────────────────
            if (app.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = app.description,
                    fontSize = 13.sp,
                    color = liquidColors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Install button ─────────────────────────────────────────────
            // Uses its own onClick to avoid interception by the parent card.
            Button(
                onClick = { openStore() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintColors.Primary
                )
            ) {
                Text(
                    text = stringResource(R.string.mint_more_apps_install),
                    color = MintColors.OnPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
