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
import com.akustom15.mint.library.ui.composables.FrostedGlassDialogCard
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
                androidx.compose.material3.Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "¡Nueva Actualización $versionName!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.padding(bottom = 16.dp),
                            textAlign = TextAlign.Center
                        )
                        
                        val changelogItems = changelogText.split("•").map { it.trim() }.filter { it.isNotEmpty() }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            if (changelogItems.isEmpty()) {
                                Text(
                                    text = changelogText,
                                    color = if (isDark) Color.White else Color.Black,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                changelogItems.forEach { item ->
                                    Text(
                                        text = "• $item",
                                        color = if (isDark) Color.White else Color.Black,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { 
                                    showDialog = false
                                    onShowChange(false)
                                    prefs.edit().putInt("last_seen_changelog", latestVersionCode).apply()
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("LUEGO", color = if (isDark) Color.LightGray else Color.DarkGray, fontWeight = FontWeight.Bold)
                            }
                            
                            androidx.compose.material3.Button(
                                onClick = { 
                                    showDialog = false
                                    onShowChange(false)
                                    prefs.edit().putInt("last_seen_changelog", latestVersionCode).apply()
                                    
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("market://details?id=${context.packageName}")
                                        setPackage("com.android.vending")
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, 
                                            android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                        ).apply {
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(webIntent)
                                    }
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MintColors.Primary
                                )
                            ) {
                                Text("ACTUALIZAR", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
