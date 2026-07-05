package com.elmotamyez.gallery.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.data.model.Receipt
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptViewModel
import com.elmotamyez.gallery.util.formatPrice
import kotlinx.datetime.*
import org.koin.compose.koinInject

private enum class ReportFilter { WEEKLY, MONTHLY, ALL }

class ReceiptsReportScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val receiptVm: ReceiptViewModel = koinInject()
        val allReceipts by receiptVm.receipts.collectAsState()
        val isLoading by receiptVm.isLoading.collectAsState()

        var filter by remember { mutableStateOf(ReportFilter.MONTHLY) }

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val filtered = remember(allReceipts, filter) {
            when (filter) {
                ReportFilter.ALL -> allReceipts
                ReportFilter.MONTHLY -> {
                    val prefix = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
                    allReceipts.filter { r ->
                        r.createdAt?.startsWith(prefix) == true
                    }
                }
                ReportFilter.WEEKLY -> {
                    val sevenDaysAgo = today.minus(6, DateTimeUnit.DAY)
                    allReceipts.filter { r ->
                        val dateStr = r.createdAt?.take(10) ?: return@filter false
                        dateStr >= sevenDaysAgo.toString() && dateStr <= today.toString()
                    }
                }
            }
        }

        val totalIncome  = filtered.filter { it.isPaid }.sumOf { it.total }
        val totalDebited = filtered.filter { !it.isPaid }.sumOf { it.total }
        val paidCount    = filtered.count { it.isPaid }
        val debitCount   = filtered.count { !it.isPaid }

        Scaffold(
            topBar = {
                Surface(color = Color.White, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                        }
                        Text(
                            "تقرير المبيعات",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Filter toggle ─────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportFilter.entries.forEach { f ->
                            val label = when (f) {
                                ReportFilter.WEEKLY  -> "اسبوعي"
                                ReportFilter.MONTHLY -> "شهري"
                                ReportFilter.ALL     -> "الكل"
                            }
                            FilterChip(
                                selected = filter == f,
                                onClick  = { filter = f },
                                label    = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ── Summary cards ─────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "الإيرادات",
                            value = totalIncome.formatPrice(),
                            count = "$paidCount طلب",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "الديون",
                            value = totalDebited.formatPrice(),
                            count = "$debitCount طلب",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Net total ─────────────────────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "إجمالي الفترة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                (totalIncome + totalDebited).formatPrice(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // ── Receipts list ─────────────────────────────────────────────
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "لا توجد طلبات في هذه الفترة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "${filtered.size} طلب",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    items(filtered.sortedByDescending { it.createdAt }, key = { it.id }) { receipt ->
                        ReceiptReportRow(receipt)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = color)
            Text(count, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ReceiptReportRow(receipt: Receipt) {
    val dateStr = receipt.createdAt?.take(10) ?: "—"
    val timeStr = receipt.createdAt?.let {
        if (it.length >= 16) it.substring(11, 16) else ""
    } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "طلب #${receipt.orderNumber}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "$dateStr  $timeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "${receipt.items.size} منتجات",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    receipt.total.formatPrice(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                val isPaidColor = if (receipt.isPaid)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
                val isPaidLabel = if (receipt.isPaid) "نقدا" else "دين"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = isPaidColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        isPaidLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = isPaidColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
