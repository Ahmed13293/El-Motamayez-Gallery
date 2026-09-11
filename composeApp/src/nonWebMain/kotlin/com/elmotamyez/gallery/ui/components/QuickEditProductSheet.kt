package com.elmotamyez.gallery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elmotamyez.gallery.data.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEditProductSheet(
    product: Product,
    onSave: (price: Double, wholesalePrice: Double?, stock: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var priceText by remember { mutableStateOf(product.price.toString()) }
    var wsText    by remember { mutableStateOf(product.wholesalePrice?.toString() ?: "") }
    var stockText by remember { mutableStateOf(product.stock.toString()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(product.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("سعر القطاعي") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = wsText,
                onValueChange = { wsText = it },
                label = { Text("سعر الجملة (اختياري)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = stockText,
                onValueChange = { stockText = it },
                label = { Text("المخزون") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("إلغاء")
                }
                Button(
                    onClick = {
                        val price = priceText.toDoubleOrNull() ?: return@Button
                        val ws    = wsText.trim().toDoubleOrNull()
                        val stock = stockText.toIntOrNull() ?: return@Button
                        onSave(price, ws, stock)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("حفظ") }
            }
        }
    }
}
