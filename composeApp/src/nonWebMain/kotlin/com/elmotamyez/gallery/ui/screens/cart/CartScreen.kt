package com.elmotamyez.gallery.ui.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.ui.screens.auth.AuthViewModel
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptScreen
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptViewModel
import com.elmotamyez.gallery.util.formatPrice
import org.koin.compose.koinInject
import kotlin.math.max

private enum class DiscountMode { AMOUNT, PERCENT }
private enum class PaymentMethod(val label: String) {
    CASH("كاش"), TRANSFER("تحويل")
}

class CartScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator   = LocalNavigator.currentOrThrow
        val cartVm      = koinInject<CartViewModel>()
        val receiptVm   = koinInject<ReceiptViewModel>()
        val authVm      = koinInject<AuthViewModel>()
        val currentUser by authVm.uiState.collectAsState()
        val cartItems   by cartVm.cartItems.collectAsState()

        // ── Customer optional fields ──────────────────────────────────────────
        var customerPhone by remember { mutableStateOf("") }
        var customerInfo  by remember { mutableStateOf("") }

        // ── Discount state ────────────────────────────────────────────────────
        var discountMode  by remember { mutableStateOf(DiscountMode.AMOUNT) }
        var discountInput by remember { mutableStateOf("") }

        val subtotal = cartVm.totalPrice
        val discountAmount = remember(discountInput, discountMode, subtotal) {
            val v = discountInput.toDoubleOrNull() ?: 0.0
            when (discountMode) {
                DiscountMode.AMOUNT  -> v.coerceIn(0.0, subtotal)
                DiscountMode.PERCENT -> (subtotal * (v.coerceIn(0.0, 100.0) / 100.0))
            }
        }
        val finalTotal = max(0.0, subtotal - discountAmount)

        var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }

        Scaffold(
            bottomBar = {
                if (cartItems.isNotEmpty()) {
                    Surface(tonalElevation = 8.dp) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // ── Discount row ──────────────────────────────────
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "خصم:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(36.dp)
                                )
                                // Mode toggle
                                Row(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    DiscountMode.entries.forEach { mode ->
                                        val selected = discountMode == mode
                                        Surface(
                                            onClick = { discountMode = mode; discountInput = "" },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                                    else Color.Transparent,
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    if (mode == DiscountMode.AMOUNT) "مبلغ" else "%",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (selected) Color.White
                                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                // Input — same height as the toggle pill
                                val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                val textColor   = MaterialTheme.colorScheme.onSurface
                                val hintColor   = MaterialTheme.colorScheme.outline
                                val labelStyle  = MaterialTheme.typography.labelMedium
                                BasicTextField(
                                    value = discountInput,
                                    onValueChange = { discountInput = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = labelStyle.copy(
                                        textAlign = TextAlign.Center,
                                        color = textColor
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { inner ->
                                        Box(
                                            modifier = Modifier
                                                .height(34.dp)
                                                .background(Color.White, RoundedCornerShape(8.dp))
                                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (discountInput.isEmpty()) {
                                                Text(
                                                    if (discountMode == DiscountMode.AMOUNT) "0.00" else "0",
                                                    style = labelStyle,
                                                    color = hintColor,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )
                                // Discount amount preview
                                if (discountAmount > 0.0) {
                                    Text(
                                        "-${discountAmount.formatPrice()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // ── Payment method ────────────────────────────────
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "الدفع:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(36.dp)
                                )
                                PaymentMethod.entries.forEach { method ->
                                    val selected = selectedMethod == method
                                    Surface(
                                        onClick = { selectedMethod = method },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.height(34.dp).weight(1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                method.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (selected) Color.White
                                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider()

                            // ── Totals ────────────────────────────────────────
                            if (discountAmount > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("المجموع", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline)
                                    Text(subtotal.formatPrice(), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الإجمالي", style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold)
                                Text(
                                    finalTotal.formatPrice(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // ── Optional customer fields ──────────────────────
                            val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            val textColor   = MaterialTheme.colorScheme.onSurface
                            val hintColor   = MaterialTheme.colorScheme.outline
                            val fieldStyle  = MaterialTheme.typography.labelMedium

                            BasicTextField(
                                value = customerPhone,
                                onValueChange = { customerPhone = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                textStyle = fieldStyle.copy(color = textColor, textAlign = TextAlign.End),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
                                            .background(Color.White, RoundedCornerShape(8.dp))
                                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (customerPhone.isEmpty()) {
                                            Text("رقم العميل (اختياري)", style = fieldStyle,
                                                color = hintColor, modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.End)
                                        }
                                        inner()
                                    }
                                }
                            )

                            BasicTextField(
                                value = customerInfo,
                                onValueChange = { customerInfo = it },
                                singleLine = true,
                                textStyle = fieldStyle.copy(color = textColor, textAlign = TextAlign.End),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
                                            .background(Color.White, RoundedCornerShape(8.dp))
                                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (customerInfo.isEmpty()) {
                                            Text("معلومات العميل (اختياري)", style = fieldStyle,
                                                color = hintColor, modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.End)
                                        }
                                        inner()
                                    }
                                }
                            )

                            Button(
                                onClick = {
                                    receiptVm.confirmOrder(
                                        items         = cartItems,
                                        total         = finalTotal,
                                        discount      = discountAmount,
                                        paymentMethod = selectedMethod.label,
                                        customerPhone = customerPhone,
                                        customerInfo  = customerInfo,
                                        username      = currentUser.user?.username
                                    )
                                    cartVm.clearCart()
                                    (navigator.parent ?: navigator).push(ReceiptScreen())
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تأكيد الطلب")
                            }
                        }
                    }
                }
            }
        ) { padding ->
            if (cartItems.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("السلة فارغة", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.name, fontWeight = FontWeight.Bold)
                                    Text("${item.product.price.formatPrice()} للقطعة",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("الإجمالي: ${item.totalPrice.formatPrice()}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                            .height(36.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { cartVm.decreaseQuantity(item.product.id) },
                                            modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary)
                                        }
                                        Text("${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                            textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 28.dp),
                                            style = MaterialTheme.typography.bodyMedium)
                                        val atLimit = item.quantity >= item.product.stock
                                        IconButton(onClick = { cartVm.increaseQuantity(item.product.id) },
                                            enabled = !atLimit, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp),
                                                tint = if (atLimit)
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                                else MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    IconButton(onClick = { cartVm.removeFromCart(item.product.id) },
                                        modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp))
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
