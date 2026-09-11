package com.elmotamyez.gallery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.model.ProductVariant
import com.elmotamyez.gallery.data.repository.ProductVariantRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantManagementSheet(
    product: Product,
    variantRepository: ProductVariantRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var variants by remember { mutableStateOf<List<ProductVariant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var newName by remember { mutableStateOf("") }
    var newStock by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf("") }
    var editStock by remember { mutableStateOf("") }

    suspend fun reload() {
        variants = variantRepository.fetchForProduct(product.id)
        isLoading = false
    }

    LaunchedEffect(product.id) { reload() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "نوعيات: ${product.name}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 260.dp)
                ) {
                    items(variants, key = { it.id }) { variant ->
                        val isEditing = editingId == variant.id
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            if (isEditing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("الاسم") },
                                        singleLine = true,
                                        modifier = Modifier.weight(2f)
                                    )
                                    OutlinedTextField(
                                        value = editStock,
                                        onValueChange = { editStock = it },
                                        label = { Text("مخزون") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        val stock = editStock.toIntOrNull() ?: return@IconButton
                                        scope.launch {
                                            variantRepository.update(variant.id, editName.trim(), stock)
                                            editingId = null
                                            reload()
                                        }
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = "حفظ",
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(variant.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            "مخزون: ${variant.stock}",
                                            fontSize = 11.sp,
                                            color = when {
                                                variant.stock == 0 -> MaterialTheme.colorScheme.error
                                                variant.stock <= 2 -> MaterialTheme.colorScheme.tertiary
                                                else -> MaterialTheme.colorScheme.outline
                                            }
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            editingId = variant.id
                                            editName = variant.name
                                            editStock = variant.stock.toString()
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = {
                                            scope.launch {
                                                variantRepository.delete(variant.id)
                                                reload()
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()
            Text("إضافة نوع جديد", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("الاسم (مثال: أحمر)") },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = newStock,
                    onValueChange = { newStock = it },
                    label = { Text("مخزون") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val name = newName.trim()
                        val stock = newStock.toIntOrNull() ?: return@IconButton
                        if (name.isBlank()) return@IconButton
                        scope.launch {
                            variantRepository.insert(product.id, name, stock)
                            newName = ""
                            newStock = ""
                            reload()
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
