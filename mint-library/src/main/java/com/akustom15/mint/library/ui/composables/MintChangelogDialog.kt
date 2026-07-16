package com.akustom15.mint.library.ui.composables

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.akustom15.mint.library.config.MintConfig
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun MintChangelogDialog(config: MintConfig, onShowChange: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val liquidColors = LocalLiquidGlassColors.current
    val isDark = liquidColors.isDark

    var showDialog by remember { mutableStateOf(false) }
    var changelogText by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("") }
    var latestVersionCode by remember { mutableStateOf(0) }
    var secondsLeft by remember { mutableStateOf(10) }

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

    // Countdown timer — only runs while the dialog is visible
    LaunchedEffect(showDialog) {
        if (showDialog) {
            secondsLeft = 10
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
            // Auto-close after 10 seconds
            showDialog = false
            onShowChange(false)
            prefs.edit().putInt("last_seen_changelog", latestVersionCode).apply()
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
                // ─── Efecto Glass igual que el resto del proyecto ───
                FrostedGlassDialogCard(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 🚀 Ícono — igual que proyecto viejo
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Actualización",
                            modifier = Modifier.size(48.dp),
                            tint = MintColors.Primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Título: "🚀 Nueva Actualización Disponible"
                        Text(
                            text = "🚀 Nueva Actualización Disponible",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = liquidColors.textPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Descripción con versión — igual al proyecto viejo
                        Text(
                            text = "GlassWave $versionName está listo para descargar con nuevos iconos y mejoras.",
                            fontSize = 14.sp,
                            color = liquidColors.textPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        // Changelog dinámico desde Firebase
                        if (changelogText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val changelogItems = changelogText.split("•").map { it.trim() }.filter { it.isNotEmpty() }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (changelogItems.size <= 1) {
                                    // No bullet separator found — show as plain text
                                    Text(
                                        text = changelogText,
                                        fontSize = 13.sp,
                                        color = liquidColors.textPrimary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                } else {
                                    changelogItems.forEach { item ->
                                        Text(
                                            text = "• $item",
                                            fontSize = 13.sp,
                                            color = liquidColors.textPrimary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Timer de cuenta regresiva — igual al proyecto viejo
                        Text(
                            text = "⏱ Se cerrará en: $secondsLeft segundos",
                            fontSize = 12.sp,
                            color = liquidColors.textPrimary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Botones: LUEGO y ACTUALIZAR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón LUEGO (cerrar)
                            OutlinedButton(
                                onClick = {
                                    showDialog = false
                                    onShowChange(false)
                                    prefs.edit().putInt("last_seen_changelog", latestVersionCode).apply()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = liquidColors.textPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = liquidColors.textPrimary.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LUEGO",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Botón ACTUALIZAR
                            Button(
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
                                        val webIntent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                        ).apply {
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(webIntent)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintColors.Primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = "Actualizar",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ACTUALIZAR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
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
