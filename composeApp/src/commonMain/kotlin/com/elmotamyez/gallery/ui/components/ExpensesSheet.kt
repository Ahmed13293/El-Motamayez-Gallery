package com.elmotamyez.gallery.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmotamyez.gallery.data.model.Expense
import com.elmotamyez.gallery.ui.screens.admin.ExpenseViewModel
import org.koin.compose.koinInject

val EXPENSE_TYPES = listOf("الإيجار", "مرتبات", "النت", "الكهرباء", "بضاعه", "مصاريف عامة")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesSheet(
    editExpense: Expense? = null,   // null = add mode, non-null = edit mode
    onDismiss: () -> Unit
) {
    val vm: ExpenseViewModel = koinInject()
    val isSaving by vm.isSaving.collectAsState()

    // In edit mode, jump straight to step 2 with pre-filled values
    var selectedType by remember { mutableStateOf(editExpense?.type) }
    var amountInput  by remember { mutableStateOf(editExpense?.amount?.let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
    } ?: "") }
    var noteInput    by remember { mutableStateOf(editExpense?.note ?: "") }

    val isEditMode = editExpense != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Title row ─────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedType != null && !isEditMode) {
                    IconButton(
                        onClick = { selectedType = null; amountInput = "" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    when {
                        isEditMode       -> "تعديل: ${editExpense!!.type}"
                        selectedType != null -> selectedType!!
                        else             -> "اختر نوع المصروف"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            AnimatedContent(targetState = selectedType) { type ->
                if (type == null) {
                    // ── Step 1: type list ─────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EXPENSE_TYPES.forEach { expenseType ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedType = expenseType },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(expenseType,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.CheckCircle, null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                } else {
                    // ── Step 2: amount + note entry ───────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { v ->
                                if (v.all { it.isDigit() || it == '.' }) amountInput = v
                            },
                            label = { Text("القيمة (جنيه)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            suffix = { Text("ج") }
                        )

                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            label = { Text("ملاحظة (اختياري)") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val amount = amountInput.toDoubleOrNull() ?: return@Button
                                val note = noteInput.ifBlank { null }
                                if (isEditMode) {
                                    vm.updateExpense(editExpense!!.copy(type = type, amount = amount, note = note)) {
                                        onDismiss()
                                    }
                                } else {
                                    vm.addExpense(type, amount, note) {
                                        onDismiss()
                                    }
                                }
                            },
                            enabled = amountInput.toDoubleOrNull() != null && !isSaving,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    if (isEditMode) "حفظ التعديل" else "تأكيد المصروف",
                                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                                )
                            }
                        }

                    }
                }
            }
        }
    }
}
