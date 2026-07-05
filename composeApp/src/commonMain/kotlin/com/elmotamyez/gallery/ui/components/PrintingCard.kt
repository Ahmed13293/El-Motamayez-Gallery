package com.elmotamyez.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.util.fmt2f

// ── Pricing config ────────────────────────────────────────────────────────────
// Single-sided prices
private const val PRICE_PRINT_SINGLE = 2.00
private const val PRICE_PHOTO_SINGLE = 1.50
// Double-sided (وش وضهر) prices
private const val PRICE_PRINT_DOUBLE = 3.50
private const val PRICE_PHOTO_DOUBLE = 2.50

private val NavyGradient = listOf(Color(0xFF08396C), Color(0xFF1565C0))

private enum class ServiceType(val label: String) { PRINT("طباعة"), PHOTO("تصوير") }

// ── Entry point ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintingButton(
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier,
    squareMode: Boolean = false
) {
    var showSheet by remember { mutableStateOf(false) }

    if (squareMode) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(NavyGradient))
                .clickable { showSheet = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Print, contentDescription = "طباعة / تصوير",
                tint = Color.White, modifier = Modifier.size(20.dp))
        }
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(NavyGradient))
                .clickable { showSheet = true }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Print, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text("طباعة / تصوير", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { false }
        )
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "طباعة / تصوير",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showSheet = false }) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            PrintingSheetContent(onAddToCart = { product ->
                onAddToCart(product)
                showSheet = false
            })
        }
    }
}

// ── Bottom sheet content ──────────────────────────────────────────────────────

@Composable
private fun PrintingSheetContent(onAddToCart: (Product) -> Unit) {
    var serviceType    by remember { mutableStateOf(ServiceType.PRINT) }
    var paperCountText by remember { mutableStateOf("1") }
    var doubleSided    by remember { mutableStateOf(false) }

    val paperCount = paperCountText.toIntOrNull()?.coerceAtLeast(1) ?: 1

    val pricePerPage = when {
        serviceType == ServiceType.PRINT && doubleSided  -> PRICE_PRINT_DOUBLE
        serviceType == ServiceType.PRINT && !doubleSided -> PRICE_PRINT_SINGLE
        serviceType == ServiceType.PHOTO && doubleSided  -> PRICE_PHOTO_DOUBLE
        else                                             -> PRICE_PHOTO_SINGLE
    }
    val totalPrice = paperCount * pricePerPage

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Title ─────────────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Print, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("طباعة / تصوير", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge)
                Text("اختر الخدمة المطلوبة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }

        HorizontalDivider()

        // ── Service type toggle ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ServiceType.entries.forEach { type ->
                val selected = serviceType == type
                val singlePrice = if (type == ServiceType.PRINT) PRICE_PRINT_SINGLE else PRICE_PHOTO_SINGLE
                val doublePrice = if (type == ServiceType.PRINT) PRICE_PRINT_DOUBLE else PRICE_PHOTO_DOUBLE
                Surface(
                    onClick = { serviceType = type },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            type.label,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "وجه: ${singlePrice.fmt2f()}  |  وجهين: ${doublePrice.fmt2f()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color.White.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Paper count ───────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("عدد الأوراق", fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge)
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { if (paperCount > 1) paperCountText = (paperCount - 1).toString() },
                    contentAlignment = Alignment.Center) {
                    Text("−", color = MaterialTheme.colorScheme.primary,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = paperCountText,
                    onValueChange = { v ->
                        if (v.all { it.isDigit() } && v.length <= 4) paperCountText = v
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(100.dp)
                )
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { paperCountText = (paperCount + 1).toString() },
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
        }

        // ── Double-sided toggle ───────────────────────────────────────────────
        Surface(shape = RoundedCornerShape(14.dp),
            color = if (doubleSided) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().clickable { doubleSided = !doubleSided }
                .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("وش وضهر", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (doubleSided) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface)
                    Text(
                        "سعر الورقة: ${pricePerPage.fmt2f()} جنيه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                Icon(if (doubleSided) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    null,
                    tint = if (doubleSided) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp))
            }
        }

        // ── Price breakdown ───────────────────────────────────────────────────
        Surface(shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${pricePerPage.fmt2f()} جنيه / ورقة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$paperCount ورقة × ${pricePerPage.fmt2f()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("الإجمالي", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                    Text("${totalPrice.fmt2f()} ج",
                        fontWeight = FontWeight.ExtraBold, fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ── Add to cart ───────────────────────────────────────────────────────
        Button(
            onClick = {
                val label = buildString {
                    append("${serviceType.label} $paperCount ورقة")
                    if (doubleSided) append(" (وش وضهر)")
                }
                onAddToCart(Product(
                    id         = "${serviceType.name.lowercase()}_${paperCount}_${if (doubleSided) "dbl" else "sgl"}",
                    name       = label,
                    price      = totalPrice,
                    stock      = 999,
                    brandId    = "printing",
                    categoryId = "printing"
                ))
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("إضافة للسلة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
