package com.elmotamyez.gallery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantPickerSheet(
    product: Product,
    variants: List<ProductVariant>,
    onAddToCart: (variantId: String, variantName: String, quantity: Int, variantStock: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedVariant by remember { mutableStateOf<ProductVariant?>(null) }
    var quantity by remember { mutableIntStateOf(1) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                product.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Text("اختر النوع", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 260.dp)
            ) {
                items(variants) { variant ->
                    val isSelected = selectedVariant?.id == variant.id
                    val outOfStock = variant.stock <= 0
                    OutlinedCard(
                        onClick = { if (!outOfStock) { selectedVariant = variant; quantity = 1 } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder().let {
                            if (isSelected) CardDefaults.outlinedCardBorder(enabled = true) else it
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                variant.name,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (outOfStock) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (outOfStock) "نفد" else "متاح: ${variant.stock}",
                                fontSize = 12.sp,
                                color = if (outOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            if (selectedVariant != null) {
                val maxQty = selectedVariant!!.stock
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الكمية", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = quantity > 1
                        ) { Icon(Icons.Default.Remove, contentDescription = null) }
                        Text(
                            "$quantity",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.widthIn(min = 32.dp),
                        )
                        IconButton(
                            onClick = { if (quantity < maxQty) quantity++ },
                            enabled = quantity < maxQty
                        ) { Icon(Icons.Default.Add, contentDescription = null) }
                    }
                }

                Button(
                    onClick = {
                        val v = selectedVariant ?: return@Button
                        onAddToCart(v.id, v.name, quantity, v.stock)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إضافة للسلة")
                }
            }
        }
    }
}
