package com.akustom15.mint.library.ui.composables

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import com.akustom15.mint.library.config.MintConfig
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MintChangelogDialog(config: MintConfig, onShowChange: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val liquidColors = LocalLiquidGlassColors.current
    val isDark = liquidColors.isDark
    
    var showDialog by remember { mutableStateOf(false) }
    var changelogText by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("") }
    var latestVersionCode by remember { mutableStateOf(0) }
    
    val prefs = remember { context.getSharedPreferences("mint_changelog_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(config.firestoreUpdateDocument) {
        if (config.firestoreUpdateDocument.isNotBlank()) {
            val db = FirebaseFirestore.getInstance()
            db.collection("app_versions").document(config.firestoreUpdateDocument)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val vCode = doc.getLong("versionCode")?.toInt() ?: 0
                        val vName = doc.getString("versionName") ?: ""
                        val text = doc.getString("changelog") ?: ""
                        
                        val lastSeenVersion = prefs.getInt("last_seen_changelog", 0)
                        
                        if (vCode > lastSeenVersion) {
                            changelogText = text
                            versionName = vName
                            latestVersionCode = vCode
                            showDialog = true
                            onShowChange(true)
                        }
                    }
                }
        }
    }

    if (showDialog) {
        val dialogShape = RoundedCornerShape(28.dp)
        val surfaceColor = if (isDark) MintColors.ButtonSurfaceDark else MintColors.ButtonSurfaceLight
        val borderColor = if (isDark) MintColors.GlassBorderDark else MintColors.GlassBorderLight
        val highlight = if (isDark) MintColors.ButtonHighlightDark else MintColors.ButtonHighlightLight
        val shadowColor = if (isDark) MintColors.ButtonShadowDark else MintColors.ButtonShadowLight

        Dialog(
            onDismissRequest = { 
                showDialog = false
                onShowChange(false)
                prefs.edit().putInt("last_seen_changelog", latestVersionCode).apply()
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { 
                        showDialog = false
                        onShowChange(false)
                        prefs.edit().putInt("last_seen_changelog", latestVersionCode).apply()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(enabled = false) {} // Prevent dismiss on card click
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
                            androidx.compose.ui.graphics.Brush.verticalGradient(
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
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Text(
                            text = "¡Nueva Actualización $versionName!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = liquidColors.textPrimary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Text(
                            text = changelogText,
                            color = liquidColors.textSecondary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { 
                                showDialog = false
                                onShowChange(false)
                                prefs.edit().putInt("last_seen_changelog", latestVersionCode).apply()
                            }) {
                                Text("OK", color = MintColors.Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
