package com.elmotamyez.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.util.rememberCameraLauncher
import com.elmotamyez.gallery.util.rememberImagePickerLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEditProductSheet(
    product: Product,
    onSave: (price: Double, wholesalePrice: Double?, stock: Int, newImageBytes: ByteArray?) -> Unit,
    onDismiss: () -> Unit
) {
    var priceText by remember { mutableStateOf(product.price.toString()) }
    var wsText    by remember { mutableStateOf(product.wholesalePrice?.toString() ?: "") }
    var stockText by remember { mutableStateOf(product.stock.toString()) }
    var pendingImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val galleryLauncher = rememberImagePickerLauncher { bytes -> pendingImageBytes = bytes }
    val cameraLauncher  = rememberCameraLauncher     { bytes -> pendingImageBytes = bytes }

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

            // ── Image preview + picker buttons ────────────────────────────────
            val previewModel: Any? = when {
                pendingImageBytes != null -> pendingImageBytes
                !product.imageUrl.isNullOrBlank() -> product.imageUrl
                else -> null
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewModel != null) {
                        AsyncImage(
                            model = previewModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = galleryLauncher,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("اختر من المعرض")
                    }
                    if (cameraLauncher != null) {
                        OutlinedButton(
                            onClick = cameraLauncher,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("التقط صورة")
                        }
                    }
                }
            }

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
                        onSave(price, ws, stock, pendingImageBytes)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("حفظ") }
            }
        }
    }
}
