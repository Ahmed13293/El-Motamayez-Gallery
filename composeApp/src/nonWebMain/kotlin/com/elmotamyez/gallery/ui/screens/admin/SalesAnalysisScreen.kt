package com.elmotamyez.gallery.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.data.model.Brand
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Category
import com.elmotamyez.gallery.data.model.Receipt
import com.elmotamyez.gallery.data.repository.ProductRepository
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptViewModel
import com.elmotamyez.gallery.util.formatPrice
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

// ── Group-by mode ─────────────────────────────────────────────────────────────

private enum class GroupBy { CATEGORY, SUB_CATEGORY, PRODUCT }

// ── Result row ────────────────────────────────────────────────────────────────

private data class SalesRow(
    val label: String,
    val revenue: Double,
    val quantity: Int,
    val orderCount: Int
)

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun Long.toIsoDate(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val date    = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return date.toString()   // "YYYY-MM-DD"
}

private fun Long.toArabicDate(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val date    = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val d = date.dayOfMonth.toString().padStart(2, '0')
    val m = date.monthNumber.toString().padStart(2, '0')
    val y = date.year
    return "$d/$m/$y"
}

// ── Screen ────────────────────────────────────────────────────────────────────

class SalesAnalysisScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator   = LocalNavigator.currentOrThrow
        val receiptVm   = koinInject<ReceiptViewModel>()
        val productRepo = koinInject<ProductRepository>()
        val scope       = rememberCoroutineScope()

        val allReceipts by receiptVm.receipts.collectAsState()

        // ── Filter state ──────────────────────────────────────────────────────
        var groupBy          by remember { mutableStateOf(GroupBy.CATEGORY) }
        var showDatePicker   by remember { mutableStateOf(false) }

        val dateRangeState = rememberDateRangePickerState()
        val fromMillis = dateRangeState.selectedStartDateMillis
        val toMillis   = dateRangeState.selectedEndDateMillis

        // ── Lookup maps ───────────────────────────────────────────────────────
        var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
        var brands     by remember { mutableStateOf<List<Brand>>(emptyList()) }

        LaunchedEffect(Unit) {
            scope.launch {
                runCatching {
                    categories = productRepo.getCategories()
                    brands     = productRepo.getBrands()
                }
            }
        }

        // ── Filter receipts by selected date range ────────────────────────────
        val fromIso = fromMillis?.toIsoDate()
        val toIso   = toMillis?.toIsoDate()

        val filtered = remember(allReceipts, fromIso, toIso) {
            allReceipts.filter { r ->
                val d = r.createdAt?.take(10) ?: return@filter true
                val fromOk = fromIso == null || d >= fromIso
                val toOk   = toIso   == null || d <= toIso
                fromOk && toOk
            }
        }

        // ── Aggregate ─────────────────────────────────────────────────────────
        val rows = remember(filtered, groupBy, categories, brands) {
            aggregate(filtered, groupBy, categories, brands)
        }

        val totalRevenue = rows.sumOf { it.revenue }

        // ── Date range picker dialog ──────────────────────────────────────────
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("تأكيد", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("إلغاء")
                    }
                }
            ) {
                DateRangePicker(
                    state = dateRangeState,
                    title = { Text("اختر الفترة الزمنية", modifier = Modifier.padding(16.dp)) },
                    headline = {
                        val from = fromMillis?.toArabicDate() ?: "من"
                        val to   = toMillis?.toArabicDate()   ?: "إلى"
                        Text(
                            "$from  ←  $to",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    modifier = Modifier.heightIn(max = 520.dp)
                )
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("تحليل المبيعات", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── Date range selector ───────────────────────────────────────
                item {
                    Text("الفترة الزمنية", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))

                            if (fromMillis == null && toMillis == null) {
                                Text("اختر الفترة الزمنية",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.weight(1f))
                            } else {
                                val from = fromMillis?.toArabicDate() ?: "—"
                                val to   = toMillis?.toArabicDate()   ?: "—"
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("$from  ←  $to",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("${filtered.size} فاتورة في هذه الفترة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(
                                    onClick = {
                                        dateRangeState.setSelection(null, null)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                // ── Group-by toggle ───────────────────────────────────────────
                item {
                    Text("تجميع حسب", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        GroupBy.entries.forEach { g ->
                            val label = when (g) {
                                GroupBy.CATEGORY     -> "القسم"
                                GroupBy.SUB_CATEGORY -> "الفئة الفرعية"
                                GroupBy.PRODUCT      -> "المنتج"
                            }
                            val selected = groupBy == g
                            Surface(
                                onClick  = { groupBy = g },
                                shape    = RoundedCornerShape(10.dp),
                                color    = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // ── Summary card ──────────────────────────────────────────────
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("إجمالي المبيعات",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${filtered.size} فاتورة  •  ${rows.size} ${
                                    when (groupBy) {
                                        GroupBy.CATEGORY     -> "قسم"
                                        GroupBy.SUB_CATEGORY -> "فئة"
                                        GroupBy.PRODUCT      -> "منتج"
                                    }
                                }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                            Text("${totalRevenue.formatPrice()} ج",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // ── Results ───────────────────────────────────────────────────
                if (rows.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 32.dp),
                            contentAlignment = Alignment.Center) {
                            Text("لا توجد مبيعات في هذه الفترة",
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الاسم", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1f))
                            Text("الكمية", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center,
                                modifier = Modifier.width(48.dp))
                            Text("الإيراد", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.End,
                                modifier = Modifier.width(72.dp))
                        }
                    }
                    itemsIndexed(rows) { index, row ->
                        SalesResultRow(rank = index + 1, row = row, maxRevenue = rows.first().revenue)
                    }
                }
            }
        }
    }
}

// ── Aggregation ───────────────────────────────────────────────────────────────

private fun aggregate(
    receipts: List<Receipt>,
    groupBy: GroupBy,
    categories: List<Category>,
    brands: List<Brand>
): List<SalesRow> {
    data class FlatItem(val item: CartItem, val receiptId: String)
    val flat     = receipts.flatMap { r -> r.items.map { FlatItem(it, r.id) } }
    val catMap   = categories.associateBy { it.id }
    val brandMap = brands.associateBy { it.id }

    val grouped = flat.groupBy { fi ->
        when (groupBy) {
            GroupBy.CATEGORY     -> catMap[fi.item.product.categoryId]?.name   ?: fi.item.product.categoryId
            GroupBy.SUB_CATEGORY -> brandMap[fi.item.product.brandId]?.name    ?: fi.item.product.brandId
            GroupBy.PRODUCT      -> fi.item.product.name
        }
    }

    return grouped.map { (label, items) ->
        SalesRow(
            label      = label,
            revenue    = items.sumOf { it.item.totalPrice },
            quantity   = items.sumOf { it.item.quantity },
            orderCount = items.map { it.receiptId }.toSet().size
        )
    }.sortedByDescending { it.revenue }
}

// ── Ranked result card ────────────────────────────────────────────────────────

@Composable
private fun SalesResultRow(rank: Int, row: SalesRow, maxRevenue: Double) {
    val fraction  = if (maxRevenue > 0) (row.revenue / maxRevenue).toFloat() else 0f
    val rankColor = when (rank) {
        1    -> Color(0xFFFFD700)
        2    -> Color(0xFFC0C0C0)
        3    -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(shape = CircleShape, color = rankColor, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$rank", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = if (rank <= 3) Color(0xFF3A2A00)
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(row.label, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${row.quantity}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
                Text("${row.revenue.formatPrice()} ج",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End, modifier = Modifier.width(72.dp))
            }
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                Box(modifier = Modifier.fillMaxWidth(fraction).height(4.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape))
            }
            Text("${row.orderCount} فاتورة", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}
