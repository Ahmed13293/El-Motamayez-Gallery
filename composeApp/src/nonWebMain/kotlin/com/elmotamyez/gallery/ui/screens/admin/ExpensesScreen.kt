package com.elmotamyez.gallery.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
            if (expenses.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
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
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ── Summary card ──────────────────────────────────────────
                    item {
                        val total = expenses.sumOf { it.amount }
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

                    // ── Expense rows ──────────────────────────────────────────
                    items(expenses, key = { it.id }) { expense ->
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
                        color = MaterialTheme.colorScheme.outline)
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
