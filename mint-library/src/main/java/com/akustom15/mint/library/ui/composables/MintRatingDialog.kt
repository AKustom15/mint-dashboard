package com.akustom15.mint.library.ui.composables

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors
import com.akustom15.mint.library.R
import kotlinx.coroutines.delay

private const val RATING_PREFS = "mint_rating_prefs"
private const val KEY_LAUNCH_COUNT = "launch_count"
private const val KEY_RATED = "user_rated"
private const val KEY_NEVER_RATE = "never_rate"
private const val KEY_LAST_ASKED = "last_asked_timestamp"
private const val LAUNCHES_BEFORE_PROMPT = 3
private const val DAYS_BETWEEN_PROMPTS = 1L

/**
 * Call this once on app launch (e.g. in MainActivity.onCreate).
 * Returns true if the rating dialog should be shown.
 */
fun MintRatingManager(context: Context): Boolean {
    val prefs = context.getSharedPreferences(RATING_PREFS, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_RATED, false)) return false
    if (prefs.getBoolean(KEY_NEVER_RATE, false)) return false

    val launches = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1
    prefs.edit().putInt(KEY_LAUNCH_COUNT, launches).apply()

    if (launches < LAUNCHES_BEFORE_PROMPT) return false

    val lastAsked = prefs.getLong(KEY_LAST_ASKED, 0L)
    val daysSinceLast = (System.currentTimeMillis() - lastAsked) / (1000L * 60 * 60 * 24)

    return if (lastAsked == 0L || daysSinceLast >= DAYS_BETWEEN_PROMPTS) {
        prefs.edit().putLong(KEY_LAST_ASKED, System.currentTimeMillis()).apply()
        true
    } else false
}

/**
 * Modern star-rating dialog shown when the user should be prompted to rate the app.
 * Uses the FrostedGlassDialogCard for visual consistency with the rest of the app.
 */
@Composable
fun MintRatingDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val liquidColors = LocalLiquidGlassColors.current
    val isDark = liquidColors.isDark
    var selectedStars by remember { mutableIntStateOf(0) }
    var animateStars by remember { mutableStateOf(false) }

    // Trigger star entrance animation once
    LaunchedEffect(Unit) {
        delay(200)
        animateStars = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                FrostedGlassDialogCard(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clickable(enabled = false) {}
                ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Emoji grande
                    Text(
                        text = "⭐",
                        fontSize = 52.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.mint_rating_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = liquidColors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.mint_rating_desc),
                        fontSize = 13.sp,
                        color = liquidColors.textPrimary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Star row with animated scale entrance
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            val isSelected = star <= selectedStars
                            val delay = (star - 1) * 60

                            // Each star pops in with a bounce
                            val scale by animateFloatAsState(
                                targetValue = if (animateStars) 1f else 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "star_$star"
                            )

                            // Pulse when selected
                            val selectedScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.15f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "selected_$star"
                            )

                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star estrellas",
                                tint = if (isSelected) Color(0xFFFFB800) else liquidColors.textPrimary.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(scale * selectedScale)
                                    .clickable { selectedStars = star }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Star label
                    val starLabel = when (selectedStars) {
                        1 -> stringResource(R.string.mint_rating_star_1)
                        2 -> stringResource(R.string.mint_rating_star_2)
                        3 -> stringResource(R.string.mint_rating_star_3)
                        4 -> stringResource(R.string.mint_rating_star_4)
                        5 -> stringResource(R.string.mint_rating_star_5)
                        else -> ""
                    }
                    Text(
                        text = starLabel,
                        fontSize = 13.sp,
                        color = if (selectedStars > 0) MintColors.Primary else Color.Transparent,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary CTA button
                    Button(
                        onClick = {
                            if (selectedStars >= 4) {
                                // High rating → go to Play Store
                                val prefs = context.getSharedPreferences(RATING_PREFS, Context.MODE_PRIVATE)
                                prefs.edit().putBoolean(KEY_RATED, true).apply()
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("market://details?id=${context.packageName}")
                                ).apply {
                                    setPackage("com.android.vending")
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val web = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                    ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    context.startActivity(web)
                                }
                            } else if (selectedStars > 0) {
                                // Low rating → just dismiss and ask later
                                val prefs = context.getSharedPreferences(RATING_PREFS, Context.MODE_PRIVATE)
                                prefs.edit().putLong(KEY_LAST_ASKED, System.currentTimeMillis()).apply()
                            }
                            onDismiss()
                        },
                        enabled = selectedStars > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintColors.Primary,
                            contentColor = Color.White,
                            disabledContainerColor = MintColors.Primary.copy(alpha = 0.3f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (selectedStars >= 4) stringResource(R.string.mint_rating_btn_rate_store) else stringResource(R.string.mint_rating_btn_submit),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // "Not now" subtle link
                    TextButton(
                        onClick = {
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.mint_rating_btn_not_now),
                            color = liquidColors.textPrimary.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }

                    // "Never ask again" very subtle
                    TextButton(
                        onClick = {
                            val prefs = context.getSharedPreferences(RATING_PREFS, Context.MODE_PRIVATE)
                            prefs.edit().putBoolean(KEY_NEVER_RATE, true).apply()
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.mint_rating_btn_never),
                            color = liquidColors.textPrimary.copy(alpha = 0.3f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    

        }
            }
        }
    }
}
