package com.elmotamyez.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
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
    onSave: (price: Double, wholesalePrice: Double?, stock: Int, newImageBytes: ByteArray?, remainingImageUrls: List<String>, variantStocks: Map<String, Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var priceText by remember { mutableStateOf(product.price.toString()) }
    var wsText    by remember { mutableStateOf(product.wholesalePrice?.toString() ?: "") }
    var stockText by remember { mutableStateOf(product.stock.toString()) }
    var pendingImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Existing URLs — user can remove individual entries
    val imageUrlsList = remember { mutableStateListOf(*product.displayImages.toTypedArray()) }

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

            // ── Image section ─────────────────────────────────────────────────
            // Horizontal row: new-image preview (if picked) + all existing URLs
            val hasAnyImage = pendingImageBytes != null || imageUrlsList.isNotEmpty()
            if (hasAnyImage) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pending new image (from gallery/camera) — rotate only
                    if (pendingImageBytes != null) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = pendingImageBytes,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Remove new image
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .clickable { pendingImageBytes = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            // Rotate new image
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), CircleShape)
                                    .clickable(enabled = !isRotating) {
                                        scope.launch(Dispatchers.IO) {
                                            isRotating = true
                                            val src = pendingImageBytes
                                            if (src != null) pendingImageBytes = rotateImage90CW(src)
                                            isRotating = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRotating)
                                    CircularProgressIndicator(Modifier.size(12.dp), color = Color.White, strokeWidth = 1.5.dp)
                                else
                                    Icon(Icons.Default.RotateRight, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Existing URL images — remove + rotate
                    imageUrlsList.forEachIndexed { idx, url ->
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Remove this URL
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .clickable { imageUrlsList.removeAt(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            // Rotate existing URL — download → rotate → set as pendingImageBytes + remove this URL slot
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), CircleShape)
                                    .clickable(enabled = !isRotating) {
                                        scope.launch(Dispatchers.IO) {
                                            isRotating = true
                                            val bytes = runCatching { httpClient.get(url).body<ByteArray>() }.getOrNull()
                                            if (bytes != null) {
                                                pendingImageBytes = rotateImage90CW(bytes)
                                                imageUrlsList.removeAt(idx)
                                            }
                                            isRotating = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRotating)
                                    CircularProgressIndicator(Modifier.size(12.dp), color = Color.White, strokeWidth = 1.5.dp)
                                else
                                    Icon(Icons.Default.RotateRight, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // Gallery + camera buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = galleryLauncher, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("المعرض")
                }
                if (cameraLauncher != null) {
                    OutlinedButton(onClick = cameraLauncher, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("كاميرا")
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

            // ── Stock ─────────────────────────────────────────────────────────
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
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("إلغاء") }
                Button(
                    onClick = {
                        val price = priceText.toDoubleOrNull() ?: return@Button
                        val ws    = wsText.trim().toDoubleOrNull()
                        val stock = if (variants.isEmpty()) stockText.toIntOrNull() ?: return@Button else product.stock
                        val variantStocks = variantStockTexts.mapNotNull { (id, text) ->
                            text.toIntOrNull()?.let { id to it }
                        }.toMap()
                        onSave(price, ws, stock, pendingImageBytes, imageUrlsList.toList(), variantStocks)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("حفظ") }
            }
        }
    }
}
