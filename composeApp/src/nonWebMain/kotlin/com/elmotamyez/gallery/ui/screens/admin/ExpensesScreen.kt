package com.elmotamyez.gallery.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.data.model.Expense
import com.elmotamyez.gallery.ui.components.ExpensesSheet
import com.elmotamyez.gallery.util.formatPrice
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

class ExpensesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ExpenseViewModel = koinInject()
        val expenses by vm.expenses.collectAsState()
        val error    by vm.error.collectAsState()

        LaunchedEffect(Unit) { vm.load() }

        error?.let {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("خطأ") },
                text  = { Text(it) },
                confirmButton = { TextButton(onClick = {}) { Text("حسناً") } }
            )
        }

        var showAddSheet  by remember { mutableStateOf(false) }
        var editTarget    by remember { mutableStateOf<Expense?>(null) }
        var deleteTarget  by remember { mutableStateOf<Expense?>(null) }

        var showDatePicker by remember { mutableStateOf(false) }
        val dateRangeState = rememberDateRangePickerState()
        val fromMillis = dateRangeState.selectedStartDateMillis
        val toMillis   = dateRangeState.selectedEndDateMillis
        val fromIso = fromMillis?.let {
            Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        }
        val toIso = toMillis?.let {
            Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        }

        val filteredExpenses = remember(expenses, fromIso, toIso) {
            expenses.filter { expense ->
                val d = expense.createdAt?.take(10) ?: return@filter true
                val fromOk = fromIso == null || d >= fromIso
                val toOk   = toIso   == null || d <= toIso
                fromOk && toOk
            }
        }

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
                    TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
                }
            ) {
                DateRangePicker(
                    state = dateRangeState,
                    title = { Text("اختر الفترة الزمنية", modifier = Modifier.padding(16.dp)) },
                    headline = {
                        val from = fromMillis?.let {
                            val d = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                            "${d.dayOfMonth.toString().padStart(2,'0')}/${d.monthNumber.toString().padStart(2,'0')}/${d.year}"
                        } ?: "من"
                        val to = toMillis?.let {
                            val d = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                            "${d.dayOfMonth.toString().padStart(2,'0')}/${d.monthNumber.toString().padStart(2,'0')}/${d.year}"
                        } ?: "إلى"
                        Text("$from  ←  $to", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.SemiBold)
                    },
                    modifier = Modifier.heightIn(max = 520.dp)
                )
            }
        }

        // ── Add sheet ─────────────────────────────────────────────────────────
        if (showAddSheet) {
            ExpensesSheet(onDismiss = { showAddSheet = false; vm.load() })
        }

        // ── Edit sheet ────────────────────────────────────────────────────────
        editTarget?.let { expense ->
            ExpensesSheet(editExpense = expense, onDismiss = { editTarget = null; vm.load() })
        }

        // ── Delete confirm dialog ─────────────────────────────────────────────
        deleteTarget?.let { expense ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("حذف المصروف") },
                text  = { Text("هل تريد حذف \"${expense.type}\" بقيمة ${expense.amount.formatPrice()} ج؟") },
                confirmButton = {
                    TextButton(onClick = {
                        vm.deleteExpense(expense.id)
                        deleteTarget = null
                    }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("إلغاء") }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("المصاريف", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("إضافة مصاريف", fontWeight = FontWeight.Bold) }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Date range selector ───────────────────────────────────────
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            if (fromMillis != null) {
                                val from = Instant.fromEpochMilliseconds(fromMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date.let {
                                    "${it.dayOfMonth.toString().padStart(2,'0')}/${it.monthNumber.toString().padStart(2,'0')}/${it.year}"
                                }
                                val to = (toMillis ?: fromMillis).let {
                                    Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date.let { d ->
                                        "${d.dayOfMonth.toString().padStart(2,'0')}/${d.monthNumber.toString().padStart(2,'0')}/${d.year}"
                                    }
                                }
                                Text("$from – $to")
                            } else {
                                Text("تحديد الفترة")
                            }
                        }
                        if (fromMillis != null) {
                            IconButton(onClick = { dateRangeState.setSelection(null, null) }) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (expenses.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillParentMaxWidth().padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💸", style = MaterialTheme.typography.displayMedium)
                                Text("لا توجد مصاريف مسجّلة",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                } else {
                    // ── Summary card ──────────────────────────────────────────
                    item {
                        val total = filteredExpenses.sumOf { it.amount }
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
                                Text("إجمالي المصاريف",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${total.formatPrice()} ج",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    if (filteredExpenses.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillParentMaxWidth().padding(top = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد مصاريف في هذه الفترة",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    } else {
                        // ── Expense rows ──────────────────────────────────────
                        items(filteredExpenses, key = { it.id }) { expense ->
                            ExpenseRow(
                                expense   = expense,
                                onEdit    = { editTarget = expense },
                                onDelete  = { deleteTarget = expense }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = expense.createdAt?.let { raw ->
        try {
            val d = raw.substring(8, 10)
            val m = raw.substring(5, 7)
            val y = raw.substring(0, 4)
            "$d/$m/$y"
        } catch (_: Exception) { null }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(expense.type,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold)
                if (!expense.note.isNullOrBlank()) {
                    Text(expense.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (dateText != null) {
                    Text(dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text("${expense.amount.formatPrice()} ج",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
