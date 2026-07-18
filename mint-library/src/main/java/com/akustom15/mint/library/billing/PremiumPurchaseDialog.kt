package com.akustom15.mint.library.billing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.akustom15.mint.library.R
import com.akustom15.mint.library.ui.composables.FrostedGlassDialogCard
import com.akustom15.mint.library.ui.theme.MintColors
import kotlinx.coroutines.launch

@Composable
fun PremiumPurchaseDialog(
    products: List<MintPremiumProduct>,
    onDismiss: () -> Unit,
    onPurchaseSuccess: (String, Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.productId ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val billingManager = remember { MintBillingManager(context, products) }

    LaunchedEffect(Unit) {
        billingManager.initialize()
    }

    val billingState by billingManager.billingState.collectAsState()

    LaunchedEffect(billingState) {
        when (val state = billingState) {
            is MintBillingManager.BillingState.PurchaseSuccess -> {
                isLoading = false
                errorMessage = null
                val product = products.find { it.productId == state.productId }
                val iconCount = product?.requestCount ?: 0
                onPurchaseSuccess(state.productId, iconCount)
            }
            is MintBillingManager.BillingState.Error -> {
                isLoading = false
                errorMessage = state.message
            }
            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose { billingManager.destroy() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {

        com.akustom15.mint.library.ui.MintLocalizedContent {

        FrostedGlassDialogCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.mint_premium_dialog_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )

                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = error,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    products.forEach { product ->
                        PremiumProductItem(
                            product = product,
                            isSelected = selectedProductId == product.productId,
                            onSelect = { selectedProductId = product.productId }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.mint_premium_dialog_close),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            if (!isLoading) {
                                isLoading = true
                                errorMessage = null

                                lifecycleOwner.lifecycleScope.launch {
                                    try {
                                        val activity = findActivity(context)
                                        if (activity != null) {
                                            val success = billingManager.launchPurchaseFlow(activity, selectedProductId)
                                            if (!success) {
                                                isLoading = false
                                                errorMessage = "Error starting purchase. Check Google Play connection."
                                            }
                                        } else {
                                            isLoading = false
                                            errorMessage = "Technical error: Activity not found"
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMessage = "Unexpected error: ${e.message}"
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintColors.Primary,
                            contentColor = MintColors.OnPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MintColors.OnPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.mint_premium_dialog_buy),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MintColors.OnPrimary
                            )
                        }
                    }
                }
            }
        }
    

        }

    }
}

@Composable
private fun PremiumProductItem(
    product: MintPremiumProduct,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MintColors.Primary,
                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "${product.displayPrice} - ${product.requestCount} premium icons",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun findActivity(context: Context): Activity? {
    return when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }
}
