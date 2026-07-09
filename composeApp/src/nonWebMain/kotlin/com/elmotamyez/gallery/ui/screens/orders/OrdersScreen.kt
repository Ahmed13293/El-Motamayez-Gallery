package com.elmotamyez.gallery.ui.screens.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Order
import com.elmotamyez.gallery.data.model.OrderStatus
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.model.UserRole
import com.elmotamyez.gallery.ui.screens.auth.AuthViewModel
import com.elmotamyez.gallery.util.fmt2f
import com.elmotamyez.gallery.util.formatPrice
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

class OrdersScreen : Screen {
    @Composable
    override fun Content() {
        val vm: OrderViewModel   = koinInject()
        val authVm: AuthViewModel = koinInject()

        val orders    by vm.orders.collectAsState()
        val isLoading by vm.isLoading.collectAsState()
        val isSaving  by vm.isSaving.collectAsState()
        val authState by authVm.uiState.collectAsState()
        val username  = authState.user?.username

        var editingOrder  by remember { mutableStateOf<Order?>(null) }
        var deletingOrder by remember { mutableStateOf<Order?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("الطلبات", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { vm.loadOrders() }, enabled = !isLoading) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Refresh, null)
                        }
                    }
                )
            }
        ) { padding ->
            if (orders.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Text("لا توجد طلبات بعد", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        OrderCard(
                            order       = order,
                            isSaving    = isSaving,
                            onAdvance   = { vm.advanceStatus(order, username) },
                            onEdit      = { editingOrder = order },
                            onDelete    = { deletingOrder = order }
                        )
                    }
                }
            }
        }

        // ── Edit dialog ───────────────────────────────────────────────────────
        editingOrder?.let { order ->
            OrderEditDialog(
                order      = order,
                isSaving   = isSaving,
                onDismiss  = { editingOrder = null },
                onSave     = { items, discount, deliveryFee, payment ->
                    vm.updateOrder(order, items, discount, deliveryFee, payment)
                    editingOrder = null
                }
            )
        }

        // ── Delete confirmation ───────────────────────────────────────────────
        deletingOrder?.let { order ->
            AlertDialog(
                onDismissRequest = { deletingOrder = null },
                title = { Text("حذف الطلب", fontWeight = FontWeight.Bold) },
                text  = { Text("هل أنت متأكد من حذف الطلب؟") },
                confirmButton = {
                    Button(
                        onClick = { vm.deleteOrder(order); deletingOrder = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = !isSaving
                    ) { Text("حذف") }
                },
                dismissButton = { TextButton(onClick = { deletingOrder = null }) { Text("إلغاء") } }
            )
        }
    }
}

// ── Order card ────────────────────────────────────────────────────────────────

@Composable
private fun OrderCard(
    order: Order,
    isSaving: Boolean,
    onAdvance: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val status = OrderStatus.fromKey(order.status)
    val nextStatus = status.next()

    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!order.customerName.isNullOrBlank()) {
                            Text(order.customerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        if (!order.customerPhone.isNullOrBlank()) {
                            Text("• ${order.customerPhone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("${order.items.size} منتج  •  ${order.total.formatPrice()} ج", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(shape = RoundedCornerShape(6.dp), color = statusColor(status).copy(alpha = 0.15f)) {
                        Text(status.arabicLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall, color = statusColor(status), fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null,
                        tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                }
            }

            // ── Status stepper ────────────────────────────────────────────────
            OrderStatusStepper(currentStatus = status, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp))

            // ── Expanded detail ───────────────────────────────────────────────
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(2.dp))

                    // Items
                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.product.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("× ${item.quantity}  =  ${item.totalPrice.formatPrice()} ج", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider()

                    // Totals
                    if (order.discount > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("خصم", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            Text("- ${order.discount.formatPrice()} ج", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (order.deliveryFee > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("توصيل", style = MaterialTheme.typography.bodySmall)
                            Text("+ ${order.deliveryFee.formatPrice()} ج", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الإجمالي", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("${order.total.formatPrice()} ج", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }

                    // Meta
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(order.paymentMethod, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        if (!order.preparedBy.isNullOrBlank()) {
                            Text("تحضير: ${order.preparedBy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (!order.notes.isNullOrBlank()) {
                        Text("ملاحظات: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Advance status button
                    if (nextStatus != null) {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onAdvance,
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تحويل إلى: ${nextStatus.arabicLabel}")
                        }
                    }
                }
            }
        }
    }
}

// ── Status stepper ────────────────────────────────────────────────────────────

@Composable
private fun OrderStatusStepper(currentStatus: OrderStatus, modifier: Modifier = Modifier) {
    val steps = OrderStatus.entries
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { i, step ->
            val done = step.ordinal <= currentStatus.ordinal
            val isCurrent = step == currentStatus
            // Circle
            Box(
                Modifier.size(24.dp).clip(CircleShape)
                    .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center
            ) {
                if (done && !isCurrent) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                } else {
                    Text("${i + 1}", color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Connecting line
                if (i < steps.size - 1) {
                    Box(
                        Modifier.fillMaxWidth().height(2.dp)
                            .background(if (step.ordinal < currentStatus.ordinal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                Text(step.arabicLabel, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun statusColor(status: OrderStatus) = when (status) {
    OrderStatus.RECEIVED  -> MaterialTheme.colorScheme.tertiary
    OrderStatus.PREPARING -> MaterialTheme.colorScheme.secondary
    OrderStatus.DELIVERING -> MaterialTheme.colorScheme.primary
}

// ── Edit dialog ───────────────────────────────────────────────────────────────

@Composable
private fun OrderEditDialog(
    order: Order,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<CartItem>, Double, Double, String) -> Unit
) {
    val editItems     = remember(order.id) { mutableStateListOf<CartItem>().also { it.addAll(order.items) } }
    var discountText  by remember(order.id) { mutableStateOf(order.discount.fmt2f()) }
    var deliveryText  by remember(order.id) { mutableStateOf(order.deliveryFee.fmt2f()) }
    var paymentMethod by remember(order.id) { mutableStateOf(order.paymentMethod) }
    var showAddOther  by remember { mutableStateOf(false) }

    val discount     = discountText.toDoubleOrNull() ?: 0.0
    val deliveryFee  = deliveryText.toDoubleOrNull() ?: 0.0
    val newTotal     = editItems.sumOf { it.totalPrice } - discount + deliveryFee

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onSave(editItems.toList(), discount, deliveryFee, paymentMethod) }, enabled = !isSaving && editItems.isNotEmpty()) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("حفظ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("تعديل الطلب", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Items
                editItems.forEachIndexed { idx, item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.product.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { if (item.quantity > 1) editItems[idx] = item.copy(quantity = item.quantity - 1) }, modifier = Modifier.size(28.dp)) {
                            Text("−", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("${item.quantity}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { editItems[idx] = item.copy(quantity = item.quantity + 1) }, modifier = Modifier.size(28.dp)) {
                            Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { editItems.removeAt(idx) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                OutlinedButton(onClick = { showAddOther = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Text("+ خدمة / أخرى", style = MaterialTheme.typography.labelSmall)
                }

                HorizontalDivider()

                // Payment method
                Text("طريقة الدفع", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("كاش", "تحويل").forEach { method ->
                        FilterChip(selected = paymentMethod == method, onClick = { paymentMethod = method },
                            label = { Text(method, style = MaterialTheme.typography.labelSmall) })
                    }
                }

                // Discount
                OutlinedTextField(value = discountText, onValueChange = { discountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("خصم (ج)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)

                // Delivery fee
                OutlinedTextField(value = deliveryText, onValueChange = { deliveryText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("رسوم التوصيل (ج)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)

                // Total preview
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الإجمالي الجديد", fontWeight = FontWeight.Bold)
                    Text("${newTotal.formatPrice()} ج", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )

    if (showAddOther) {
        AddOtherItemDialog(
            onDismiss = { showAddOther = false },
            onAdd = { item -> editItems.add(item); showAddOther = false }
        )
    }
}

@Composable
private fun AddOtherItemDialog(onDismiss: () -> Unit, onAdd: (CartItem) -> Unit) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    val ts = Clock.System.now().toEpochMilliseconds() % 10000
                    val product = Product(id = "other_${name.take(8)}_$ts", name = name.trim(), price = price, brandId = "", categoryId = "", stock = 0)
                    onAdd(CartItem(product, 1))
                },
                enabled = name.isNotBlank() && (priceText.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("خدمة / أخرى", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = priceText, onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("السعر (ج)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            }
        }
    )
}
