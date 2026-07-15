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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.akustom15.mint.library.config.MintConfig
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MintChangelogDialog(config: MintConfig) {
    val context = LocalContext.current
    val liquidColors = LocalLiquidGlassColors.current
    
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
                        }
                    }
                }
        }
    }

    AnimatedVisibility(
        visible = showDialog,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { 
                    showDialog = false
                    prefs.edit().putInt("last_seen_changelog", latestVersionCode).apply()
                },
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable(enabled = false) {}, // Prevent dismiss on card click
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
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
