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
import kotlinx.datetime.*
import org.koin.compose.koinInject

// ── Group-by mode ─────────────────────────────────────────────────────────────

private enum class GroupBy { CATEGORY, SUB_CATEGORY, PRODUCT }

// ── Period preset ─────────────────────────────────────────────────────────────

private enum class Preset { ALL, TODAY, WEEK, MONTH, CUSTOM }

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
    return date.toString()
}

private fun Long.toArabicDate(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val date    = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val d = date.dayOfMonth.toString().padStart(2, '0')
    val m = date.monthNumber.toString().padStart(2, '0')
    val y = date.year
    return "$d/$m/$y"
}

private fun presetMillis(preset: Preset): Pair<Long?, Long?> {
    if (preset == Preset.ALL || preset == Preset.CUSTOM) return null to null
    val tz    = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(tz).date
    val endMs = LocalDateTime(today, LocalTime(23, 59, 59)).toInstant(tz).toEpochMilliseconds()
    val startDate = when (preset) {
        Preset.TODAY -> today
        Preset.WEEK  -> today.minus(DatePeriod(days = 6))
        Preset.MONTH -> LocalDate(today.year, today.month, 1)
        else         -> today
    }
    val startMs = LocalDateTime(startDate, LocalTime(0, 0, 0)).toInstant(tz).toEpochMilliseconds()
    return startMs to endMs
}

// ── Screen ────────────────────────────────────────────────────────────────────

class SalesAnalysisScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator   = LocalNavigator.currentOrThrow
        val receiptVm   = koinInject<ReceiptViewModel>()
        val expenseVm   = koinInject<ExpenseViewModel>()
        val productRepo = koinInject<ProductRepository>()
        val scope       = rememberCoroutineScope()

        val allReceipts by receiptVm.receipts.collectAsState()
        val allExpenses by expenseVm.expenses.collectAsState()

        // ── Filter state ──────────────────────────────────────────────────────
        var groupBy        by remember { mutableStateOf(GroupBy.CATEGORY) }
        var activePreset   by remember { mutableStateOf(Preset.ALL) }
        var showDatePicker by remember { mutableStateOf(false) }

        val dateRangeState = rememberDateRangePickerState()

        // Resolved millis: presets compute directly; CUSTOM uses date picker state
        val (fromMillis, toMillis) = remember(activePreset,
            dateRangeState.selectedStartDateMillis,
            dateRangeState.selectedEndDateMillis) {
            if (activePreset == Preset.CUSTOM)
                dateRangeState.selectedStartDateMillis to dateRangeState.selectedEndDateMillis
            else
                presetMillis(activePreset)
        }

        val fromIso = fromMillis?.toIsoDate()
        val toIso   = toMillis?.toIsoDate()

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

        // ── Filter: confirmed receipts only, within date range ────────────────
        val filtered = remember(allReceipts, fromIso, toIso) {
            allReceipts.filter { r ->
                if (r.isQuotation) return@filter false
                val d = r.createdAt?.take(10) ?: return@filter true
                val fromOk = fromIso == null || d >= fromIso
                val toOk   = toIso   == null || d <= toIso
                fromOk && toOk
            }
        }

        val filteredExpenses = remember(allExpenses, fromIso, toIso) {
            allExpenses.filter { e ->
                val d = e.createdAt?.take(10) ?: return@filter true
                val fromOk = fromIso == null || d >= fromIso
                val toOk   = toIso   == null || d <= toIso
                fromOk && toOk
            }
        }

        // ── Aggregate ─────────────────────────────────────────────────────────
        val rows = remember(filtered, groupBy, categories, brands) {
            aggregate(filtered, groupBy, categories, brands)
        }

        val totalRevenue  = rows.sumOf { it.revenue }
        val totalExpenses = filteredExpenses.sumOf { it.amount }
        val netProfit     = totalRevenue - totalExpenses

        val cashTotal     = filtered.filter { it.paymentMethod == "كاش" }.sumOf { it.total }
        val transferTotal = filtered.filter { it.paymentMethod == "تحويل" }.sumOf { it.total }

        // ── Date range picker dialog ──────────────────────────────────────────
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        activePreset = Preset.CUSTOM
                        showDatePicker = false
                    }) { Text("تأكيد", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
                }
            ) {
                DateRangePicker(
                    state = dateRangeState,
                    title = { Text("اختر الفترة الزمنية", modifier = Modifier.padding(16.dp)) },
                    headline = {
                        val from = dateRangeState.selectedStartDateMillis?.toArabicDate() ?: "من"
                        val to   = dateRangeState.selectedEndDateMillis?.toArabicDate()   ?: "إلى"
                        Text("$from  ←  $to",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.SemiBold)
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

                // ── Period presets ────────────────────────────────────────────
                item {
                    Text("الفترة الزمنية", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Preset.ALL   to "الكل",
                            Preset.TODAY to "اليوم",
                            Preset.WEEK  to "أسبوع",
                            Preset.MONTH to "الشهر"
                        ).forEach { (p, label) ->
                            val selected = activePreset == p
                            FilterChip(
                                selected = selected,
                                onClick  = { activePreset = p },
                                label    = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                        // Custom date picker chip
                        FilterChip(
                            selected = activePreset == Preset.CUSTOM,
                            onClick  = { showDatePicker = true },
                            label    = {
                                Icon(Icons.Default.CalendarMonth, null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تخصيص", style = MaterialTheme.typography.labelMedium)
                            }
                        )
                    }

                    // Show selected custom range label
                    if (activePreset == Preset.CUSTOM && fromMillis != null) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "${fromMillis.toArabicDate()}  ←  ${toMillis?.toArabicDate() ?: "—"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { activePreset = Preset.ALL; dateRangeState.setSelection(null, null) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.outline)
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

                // ── Summary cards ─────────────────────────────────────────────
                item {
                    // Revenue + expenses + net
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
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

                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                            // Payment split
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("كاش", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    Text("${cashTotal.formatPrice()} ج",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("تحويل", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    Text("${transferTotal.formatPrice()} ج",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                            // Expenses & net profit
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("المصروفات", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    Text("- ${totalExpenses.formatPrice()} ج",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE53935))
                                }
                                Column(horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("صافي الربح", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    Text("${netProfit.formatPrice()} ج",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netProfit >= 0) Color(0xFF2E7D32) else Color(0xFFE53935))
                                }
                            }
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
