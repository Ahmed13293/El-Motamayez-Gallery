package com.elmotamyez.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.model.ProductVariant
import com.elmotamyez.gallery.util.rememberCameraLauncher
import com.elmotamyez.gallery.util.rememberImagePickerLauncher
import com.elmotamyez.gallery.util.rotateImage90CW
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEditProductSheet(
    product: Product,
    variants: List<ProductVariant> = emptyList(),
    onSave: (price: Double, wholesalePrice: Double?, stock: Int, newImageBytes: ByteArray?, variantStocks: Map<String, Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var priceText by remember { mutableStateOf(product.price.toString()) }
    var wsText    by remember { mutableStateOf(product.wholesalePrice?.toString() ?: "") }
    var stockText by remember { mutableStateOf(product.stock.toString()) }
    var pendingImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Mutable per-variant stock text — keyed by variant id
    val variantStockTexts = remember(variants) {
        mutableStateMapOf<String, String>().also { map ->
            variants.forEach { v -> map[v.id] = v.stock.toString() }
        }
    }

    val galleryLauncher = rememberImagePickerLauncher { bytes -> pendingImageBytes = bytes }
    val cameraLauncher  = rememberCameraLauncher     { bytes -> pendingImageBytes = bytes }

    var isRotating by remember { mutableStateOf(false) }
    val scope      = rememberCoroutineScope()
    val httpClient = remember { HttpClient() }
    DisposableEffect(Unit) { onDispose { httpClient.close() } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
                        // Rotate 90° CW — downloads existing URL into pendingImageBytes first if needed
                        IconButton(
                            onClick = {
                                if (isRotating) return@IconButton
                                scope.launch(Dispatchers.IO) {
                                    isRotating = true
                                    val src = pendingImageBytes
                                        ?: product.imageUrl?.let { url ->
                                            runCatching { httpClient.get(url).body<ByteArray>() }.getOrNull()
                                        }
                                    if (src != null) pendingImageBytes = rotateImage90CW(src)
                                    isRotating = false
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), CircleShape)
                        ) {
                            if (isRotating)
                                CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            else
                                Icon(Icons.Default.RotateRight, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = galleryLauncher, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("اختر من المعرض")
                    }
                    if (cameraLauncher != null) {
                        OutlinedButton(onClick = cameraLauncher, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("التقط صورة")
                        }
                    }
                }
            }

            // ── Pricing ───────────────────────────────────────────────────────
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

            // ── Stock — either base product or per variant ────────────────────
            if (variants.isEmpty()) {
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text("المخزون") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("المخزون بالنوعيات",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary)
                variants.forEach { variant ->
                    OutlinedTextField(
                        value = variantStockTexts[variant.id] ?: "",
                        onValueChange = { variantStockTexts[variant.id] = it },
                        label = { Text(variant.name) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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
                        val stock = if (variants.isEmpty()) stockText.toIntOrNull() ?: return@Button else product.stock
                        val variantStocks = variantStockTexts.mapNotNull { (id, text) ->
                            text.toIntOrNull()?.let { id to it }
                        }.toMap()
                        onSave(price, ws, stock, pendingImageBytes, variantStocks)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("حفظ") }
            }
        }
    }
}
