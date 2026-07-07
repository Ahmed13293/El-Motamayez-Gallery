package com.elmotamyez.gallery.ui.screens.receipts

import androidx.compose.animation.AnimatedVisibility
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.data.model.Receipt
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptScreen
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptViewModel
import com.elmotamyez.gallery.util.dateString
import com.elmotamyez.gallery.util.formatPrice
import com.elmotamyez.gallery.util.twoDigit
import org.koin.compose.koinInject

// ── Date helpers ──────────────────────────────────────────────────────────────

private fun String.parseSupabaseDateTime(): kotlinx.datetime.LocalDateTime? = runCatching {
    val normalized = this
        .replace(" ", "T")                              // "2026-06-29 11:06:30" → T-separated
        .replace(Regex("\\.\\d+"), "")                 // strip fractional seconds of any length
        .replace(Regex("[+-]\\d{2}(:\\d{2})?$"), "Z") // +HH or +HH:MM → Z
        .let { if (!it.endsWith("Z")) "${it}Z" else it }
    Instant.parse(normalized).toLocalDateTime(TimeZone.currentSystemDefault())
}.getOrNull()

private fun Receipt.dateKey(): String {
    val local = createdAt?.parseSupabaseDateTime()
    return if (local != null)
        dateString(local.year, local.monthNumber, local.dayOfMonth)
    else "unknown"
}

private fun Receipt.monthKey(): String {
    val local = createdAt?.parseSupabaseDateTime() ?: return "unknown"
    return "${local.year}-${twoDigit(local.monthNumber)}"
}

private fun String.toArabicMonth(): String {
    val parts = split("-")
    if (parts.size < 2) return this
    val year  = parts[0]
    val month = parts[1].toIntOrNull() ?: return this
    val name  = listOf("", "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
                        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        .getOrElse(month) { month.toString() }
    return "$name $year"
}

private fun String.toArabicDisplayDate(): String {
    return try {
        val (y, m, d) = split("-")
        "$d/$m/$y"
    } catch (_: Exception) { this }
}

private fun Receipt.timeLabel(): String {
    val local = createdAt?.parseSupabaseDateTime() ?: return ""
    return "${twoDigit(local.hour)}:${twoDigit(local.minute)}"
}

// ── Screen ────────────────────────────────────────────────────────────────────

class ReceiptsListScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ReceiptViewModel = koinInject()
        val receipts  by vm.receipts.collectAsState()
        val isLoading by vm.isLoading.collectAsState()

        // Current month key e.g. "2026-07"
        val currentMonthKey = remember {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            "${now.year}-${twoDigit(now.monthNumber)}"
        }

        // Distinct months sorted newest first
        val months = remember(receipts) {
            receipts.map { it.monthKey() }.filter { it != "unknown" }.distinct().sortedDescending()
        }

        // Selected month tab index — default to current month, fallback to newest
        var selectedMonthIndex by remember(months) {
            val idx = months.indexOf(currentMonthKey).takeIf { it >= 0 } ?: 0
            mutableIntStateOf(idx)
        }
        val selectedMonth = months.getOrNull(selectedMonthIndex) ?: currentMonthKey

        // Group by date, filtered to selected month, newest day first
        val grouped = remember(receipts, selectedMonth) {
            receipts
                .filter { it.monthKey() == selectedMonth }
                .sortedByDescending { it.orderNumber }
                .groupBy { it.dateKey() }
                .entries
                .sortedByDescending { it.key }
        }

        val monthTotal = remember(grouped) { grouped.sumOf { it.value.sumOf { r -> r.total } } }
        val monthCount = remember(grouped) { grouped.sumOf { it.value.size } }

        // Expanded state lives in the VM so it survives back-navigation
        val expandedDays by vm.expandedDays.collectAsState()
        LaunchedEffect(receipts, selectedMonth) {
            vm.initExpandedDays(grouped.map { it.key })
        }

        // Scroll state — restore position when returning from ReceiptScreen
        val listState = rememberLazyListState()
        LaunchedEffect(Unit) {
            if (vm.listScrollIndex > 0) {
                listState.scrollToItem(vm.listScrollIndex, vm.listScrollOffset)
            }
        }
        // Reset scroll when month changes
        LaunchedEffect(selectedMonth) {
            listState.scrollToItem(0)
        }

        Scaffold(
            topBar = {
                if (months.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedMonthIndex,
                        edgePadding = 0.dp
                    ) {
                        months.forEachIndexed { index, monthKey ->
                            Tab(
                                selected = index == selectedMonthIndex,
                                onClick  = { selectedMonthIndex = index },
                                text     = {
                                    Text(
                                        monthKey.toArabicMonth(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (index == selectedMonthIndex)
                                            FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh    = { vm.loadReceipts() },
                modifier     = Modifier.padding(padding).fillMaxSize()
            ) {
                if (receipts.isEmpty() && !isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🧾", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(12.dp))
                            Text("لا توجد فواتير بعد",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("أكّد طلباً لتظهر هنا",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ── Monthly summary card ──────────────────────────────
                        item(key = "month_summary") {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            selectedMonth.toArabicMonth(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            "$monthCount فاتورة",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Text(
                                        "${monthTotal.formatPrice()} ج",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (grouped.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("لا توجد فواتير في هذا الشهر",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        grouped.forEach { (dateKey, dayReceipts) ->
                            val isOpen = expandedDays[dateKey] == true
                            val dayTotal = dayReceipts.sumOf { it.total }

                            // ── Day header ────────────────────────────────────
                            item(key = "header_$dateKey") {
                                DayHeader(
                                    dateKey     = dateKey,
                                    count       = dayReceipts.size,
                                    dayTotal    = dayTotal,
                                    isExpanded  = isOpen,
                                    onClick     = { vm.toggleDay(dateKey) }
                                )
                            }

                            // ── Day receipts (animated) ───────────────────────
                            item(key = "receipts_$dateKey") {
                                AnimatedVisibility(
                                    visible = isOpen,
                                    enter   = expandVertically(),
                                    exit    = shrinkVertically()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Spacer(Modifier.height(2.dp))
                                        dayReceipts.forEachIndexed { index, receipt ->
                                            ReceiptCard(
                                                receipt      = receipt,
                                                dayIndex     = index + 1,
                                                onClick      = {
                                                    vm.listScrollIndex  = listState.firstVisibleItemIndex
                                                    vm.listScrollOffset = listState.firstVisibleItemScrollOffset
                                                    vm.viewReceipt(receipt)
                                                    (navigator.parent ?: navigator).push(ReceiptScreen())
                                                }
                                            )
                                        }
                                        Spacer(Modifier.height(2.dp))
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

// ── Day header card ───────────────────────────────────────────────────────────

@Composable
private fun DayHeader(
    dateKey: String,
    count: Int,
    dayTotal: Double,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(14.dp),
        color     = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.CalendarMonth, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateKey.toArabicDisplayDate(),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "$count فاتورة  •  ${dayTotal.formatPrice()} ج",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Receipt card ──────────────────────────────────────────────────────────────

@Composable
private fun ReceiptCard(receipt: Receipt, dayIndex: Int, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "فاتورة #$dayIndex",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                    if (!receipt.username.isNullOrBlank()) {
                        Text("•", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            receipt.username,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "${receipt.items.size} منتج",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val time = receipt.timeLabel()
                    if (time.isNotEmpty()) {
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                        Text(time, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (receipt.paymentMethod.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                receipt.paymentMethod,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    receipt.total.formatPrice(),
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.KeyboardArrowRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
