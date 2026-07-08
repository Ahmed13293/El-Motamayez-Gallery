package com.elmotamyez.gallery.ui.screens.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.model.UserRole
import com.elmotamyez.gallery.ui.components.GradientDivider
import com.elmotamyez.gallery.ui.screens.auth.AuthViewModel
import com.elmotamyez.gallery.util.exportReceiptToPdf
import com.elmotamyez.gallery.util.formatPrice
import com.elmotamyez.gallery.util.twoDigit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

class ReceiptScreen : Screen {
    @Composable
    override fun Content() {
        val navigator  = LocalNavigator.currentOrThrow
        val vm: ReceiptViewModel = koinInject()
        val authVm: AuthViewModel = koinInject()

        val receipt        by vm.currentReceipt.collectAsState()
        val isSaving       by vm.isSaving.collectAsState()
        val allProducts    by vm.allProducts.collectAsState()
        val authState      by authVm.uiState.collectAsState()
        val isAdmin        = authState.user?.role == UserRole.ADMIN

        val items         = receipt?.items         ?: emptyList()
        val total         = receipt?.total         ?: 0.0
        val discount      = receipt?.discount      ?: 0.0
        val subtotal      = total + discount
        val paymentMethod = receipt?.paymentMethod ?: ""
        val orderNumber   = receipt?.orderNumber
        val customerPhone = receipt?.customerPhone
        val customerInfo  = receipt?.customerInfo

        val dateTimeText = receipt?.createdAt?.let { raw ->
            runCatching {
                val normalized = raw
                    .replace(" ", "T")
                    .replace(Regex("\\.\\d+"), "")
                    .replace(Regex("[+-]\\d{2}(:\\d{2})?$"), "Z")
                    .let { if (!it.endsWith("Z")) "${it}Z" else it }
                val instant = Instant.parse(normalized)
                val local   = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                "${twoDigit(local.dayOfMonth)}/${twoDigit(local.monthNumber)}/${local.year}  ${twoDigit(local.hour)}:${twoDigit(local.minute)}"
            }.getOrElse { raw }
        } ?: ""

        var showEditSheet by remember { mutableStateOf(false) }

        Scaffold { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Back + actions row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAdmin) {
                                IconButton(onClick = {
                                    vm.loadProductsForEdit()
                                    showEditSheet = true
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "تعديل الفاتورة",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            TextButton(onClick = {
                                receipt?.let { exportReceiptToPdf(it, "${it.id}.pdf") }
                            }) {
                                Text("إصدار PDF", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Header
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "مكتبة المتميز",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "فرع الشيخ زايد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "فاتورة الطلب",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        if (orderNumber != null) {
                            Text(
                                "رقم الفاتورة: $orderNumber",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (dateTimeText.isNotEmpty()) {
                            Text(
                                "تاريخ الفاتورة: $dateTimeText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (!customerPhone.isNullOrBlank()) {
                            Text(
                                "رقم العميل: $customerPhone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (!customerInfo.isNullOrBlank()) {
                            Text(
                                "معلومات العميل: $customerInfo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (paymentMethod.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = androidx.compose.ui.graphics.Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    "طريقة الدفع: $paymentMethod",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        GradientDivider()
                    }
                }

                // Column headers
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المنتج",   fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text("الكمية",   fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                        Text("السعر",    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),   textAlign = TextAlign.End)
                        Text("الإجمالي", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),   textAlign = TextAlign.End)
                    }
                    GradientDivider()
                }

                // Items
                items(items) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.product.name,            modifier = Modifier.weight(2f))
                        Text("${item.quantity}",           modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                        Text(item.product.price.formatPrice(), modifier = Modifier.weight(1f),   textAlign = TextAlign.End)
                        Text(item.totalPrice.formatPrice(),    modifier = Modifier.weight(1f),   textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Total section
                item {
                    GradientDivider()
                    Spacer(Modifier.height(6.dp))

                    if (discount > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المجموع", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(subtotal.formatPrice(), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الخصم", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error)
                            Text("-${discount.formatPrice()}", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الإجمالي", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text(
                            total.formatPrice(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "شكراً لتسوقكم معنا!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (showEditSheet && receipt != null) {
            EditReceiptSheet(
                initialItems  = items,
                initialDiscount = discount,
                allProducts   = allProducts,
                isSaving      = isSaving,
                onDismiss     = { showEditSheet = false },
                onSave        = { newItems, newDiscount ->
                    vm.updateReceipt(newItems, newDiscount)
                    showEditSheet = false
                }
            )
        }
    }
}

// ── Edit bottom sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditReceiptSheet(
    initialItems: List<CartItem>,
    initialDiscount: Double,
    allProducts: List<Product>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<CartItem>, Double) -> Unit
) {
    val editItems    = remember { mutableStateListOf<CartItem>().apply { addAll(initialItems) } }
    var discountText by remember { mutableStateOf(if (initialDiscount > 0.0) initialDiscount.toInt().toString() else "") }
    var showAddDialog by remember { mutableStateOf(false) }

    val computedTotal = editItems.sumOf { it.totalPrice } - (discountText.toDoubleOrNull() ?: 0.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "تعديل الفاتورة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            HorizontalDivider()

            // Items list
            editItems.forEachIndexed { index, cartItem ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        cartItem.product.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    // Quantity stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                if (cartItem.quantity > 1) {
                                    editItems[index] = cartItem.copy(quantity = cartItem.quantity - 1)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "${cartItem.quantity}",
                            modifier = Modifier.widthIn(min = 24.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                        FilledTonalIconButton(
                            onClick = {
                                editItems[index] = cartItem.copy(quantity = cartItem.quantity + 1)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        cartItem.totalPrice.formatPrice(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.widthIn(min = 56.dp),
                        textAlign = TextAlign.End
                    )
                    IconButton(
                        onClick = { editItems.removeAt(index) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Add product button
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("إضافة منتج")
            }

            HorizontalDivider()

            // Discount field
            OutlinedTextField(
                value = discountText,
                onValueChange = { discountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("خصم (ج)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Total preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الإجمالي الجديد", fontWeight = FontWeight.Bold)
                Text(
                    computedTotal.formatPrice(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Save button
            Button(
                onClick = { onSave(editItems.toList(), discountText.toDoubleOrNull() ?: 0.0) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && editItems.isNotEmpty()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("حفظ التعديلات")
                }
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            allProducts = allProducts,
            onDismiss   = { showAddDialog = false },
            onAdd       = { product ->
                val existing = editItems.indexOfFirst { it.product.id == product.id }
                if (existing >= 0) {
                    editItems[existing] = editItems[existing].copy(quantity = editItems[existing].quantity + 1)
                } else {
                    editItems.add(CartItem(product = product, quantity = 1))
                }
                showAddDialog = false
            }
        )
    }
}

// ── Add product search dialog ─────────────────────────────────────────────────

@Composable
private fun AddProductDialog(
    allProducts: List<Product>,
    onDismiss: () -> Unit,
    onAdd: (Product) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, allProducts) {
        if (query.isBlank()) allProducts.take(30)
        else allProducts.filter { it.name.contains(query, ignoreCase = true) }.take(30)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة منتج") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("بحث...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (allProducts.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    } else {
                        items(filtered) { product ->
                            TextButton(
                                onClick = { onAdd(product) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(product.name, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                                    Text(product.price.formatPrice(), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
