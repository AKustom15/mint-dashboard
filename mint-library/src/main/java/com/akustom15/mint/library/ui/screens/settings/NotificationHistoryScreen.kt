package com.akustom15.mint.library.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.akustom15.mint.library.R
import com.akustom15.mint.library.notifications.MintNotificationPreferences
import com.akustom15.mint.library.notifications.NotificationItem
import com.akustom15.mint.library.ui.composables.GradientBackground
import com.akustom15.mint.library.ui.composables.LiquidGlassCard
import com.akustom15.mint.library.ui.composables.FrostedGlassDialogCard
import com.akustom15.mint.library.ui.theme.LocalLiquidGlassColors
import com.akustom15.mint.library.ui.theme.MintColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val liquidColors = LocalLiquidGlassColors.current
    val prefs = remember { MintNotificationPreferences.getInstance(context) }
    var notifications by remember { mutableStateOf(prefs.getNotificationHistory()) }
    var selectedNotification by remember { mutableStateOf<NotificationItem?>(null) }
    
    val unreadCount = notifications.count { !it.isRead }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (selectedNotification != null) Modifier.blur(20.dp) else Modifier)
        ) {
            // Top bar
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
                    text = stringResource(R.string.mint_notifications_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = liquidColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (notifications.isNotEmpty()) {
                    IconButton(onClick = {
                        prefs.clearHistory()
                        notifications = emptyList()
                    }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear",
                            tint = liquidColors.textSecondary
                        )
                    }
                }
            }

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.mint_notifications_empty),
                        color = liquidColors.textSecondary,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 8.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                    prefs.deleteNotification(item.id)
                                    notifications = notifications.filter { n -> n.id != item.id }
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromEndToStart = true,
                            enableDismissFromStartToEnd = true,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Red.copy(alpha = 0.8f))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                }
                            }
                        ) {
                            NotificationCard(
                                item = item,
                                onClick = {
                                    prefs.markAsRead(item.id)
                                    // Update state locally
                                    notifications = notifications.map {
                                        if (it.id == item.id) it.copy(isRead = true) else it
                                    }
                                    selectedNotification = item
                                },
                                onDelete = {
                                    prefs.deleteNotification(item.id)
                                    notifications = notifications.filter { n -> n.id != item.id }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Custom Overlay Dialog for notification details (ensures Haze blur works)
        if (selectedNotification != null) {
            Dialog(
                onDismissRequest = { selectedNotification = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { selectedNotification = null },
                    contentAlignment = Alignment.Center
                ) {
                    selectedNotification?.let { notif ->
                        FrostedGlassDialogCard(
                            modifier = Modifier.clickable(enabled = false) {} // Prevent dismiss when clicking on the card
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = notif.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = liquidColors.textPrimary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                if (!notif.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = notif.imageUrl,
                                        contentDescription = "Notification Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .padding(bottom = 16.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                
                                val isDark = liquidColors.isDark
                                Text(
                                    text = notif.body,
                                    color = if (isDark) Color.White else Color.Black,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(bottom = 24.dp)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { selectedNotification = null }) {
                                        Text("OK", color = MintColors.Primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItem, 
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val liquidColors = LocalLiquidGlassColors.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault()) }

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Unread indicator
            if (!item.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, end = 12.dp)
                        .background(MintColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NUEVO",
                        color = MintColors.Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold,
                    fontSize = 15.sp,
                    color = liquidColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.body,
                    fontSize = 13.sp,
                    color = liquidColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dateFormat.format(Date(item.timestamp)),
                    fontSize = 11.sp,
                    color = liquidColors.textSecondary.copy(alpha = 0.6f)
                )
            }
            
            // Delete button for obvious interaction
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Borrar",
                    tint = liquidColors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
