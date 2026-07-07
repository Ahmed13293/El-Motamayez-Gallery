package com.elmotamyez.gallery

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.ui.screens.products.ProductsViewModel
import com.elmotamyez.gallery.util.formatPrice
import org.koin.compose.koinInject

private const val WA_NUMBER   = "201121064222"
private const val FB_PAGE_URL = "https://m.me/almotamiz.bookstore"
private const val IG_PAGE_URL = "https://ig.me/m/almotamayz.gallery"

@JsFun("(num, msg) => { window.open('https://wa.me/' + num + '?text=' + encodeURIComponent(msg), '_blank'); }")
private external fun openWhatsApp(number: String, message: String)

@JsFun("(url) => { window.open(url, '_blank'); }")
external fun openUrl(url: String)

@JsFun("(text) => { navigator.clipboard.writeText(text).catch(function(){}); }")
private external fun copyToClipboard(text: String)

private fun buildCartWhatsAppMsg(items: Map<Product, Int>): String {
    val lines = items.entries.joinToString("\n") { (p, qty) ->
        "• ${p.name} × $qty = ${(p.price * qty).formatPrice()} ج"
    }
    val total = items.entries.sumOf { (p, qty) -> p.price * qty }
    return "مرحباً، أريد طلب من مكتبة المتميز 🛒\n\n$lines\n\nالإجمالي: ${total.formatPrice()} ج"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicCatalogScreen(onLoginClick: () -> Unit) {
    val vm: ProductsViewModel = koinInject()
    val state by vm.uiState.collectAsState()

    val categories  = state.categories
    val allProducts = state.allProducts

    var selectedCatId by remember(categories) {
        mutableStateOf(categories.firstOrNull()?.id ?: "")
    }
    var searchQuery by remember { mutableStateOf("") }

    // cart: productId -> quantity
    val cart = remember { mutableStateMapOf<String, Int>() }

    val displayedProducts = remember(allProducts, selectedCatId, searchQuery) {
        allProducts.filter { p ->
            (selectedCatId.isEmpty() || p.categoryId == selectedCatId) &&
            (searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true))
        }
    }

    val cartProducts = remember(cart, allProducts) {
        allProducts.filter { cart.containsKey(it.id) }
            .associateWith { cart[it.id] ?: 0 }
    }
    val cartCount   = cart.values.sum()
    val cartTotal   = cartProducts.entries.sumOf { (p, q) -> p.price * q }

    var showOrderDialog by remember { mutableStateOf(false) }
    var copiedFor       by remember { mutableStateOf("") } // "fb" | "ig" | ""

    // Platform selection dialog
    if (showOrderDialog) {
        val orderMsg = buildCartWhatsAppMsg(cartProducts)
        AlertDialog(
            onDismissRequest = { showOrderDialog = false; copiedFor = "" },
            title = {
                Text(
                    "اختر طريقة الإرسال",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "$cartCount منتج  •  ${cartTotal.formatPrice()} ج",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()

                    // WhatsApp — pre-filled message
                    Button(
                        onClick = {
                            openWhatsApp(WA_NUMBER, orderMsg)
                            showOrderDialog = false
                            copiedFor = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("واتساب", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    // Facebook — copy then open
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                copyToClipboard(orderMsg)
                                copiedFor = "fb"
                                openUrl(FB_PAGE_URL)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("فيسبوك", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        if (copiedFor == "fb") {
                            Text(
                                "✓ تم نسخ الطلب — الصقه في الرسالة",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1877F2),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // Instagram — copy then open
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                copyToClipboard(orderMsg)
                                copiedFor = "ig"
                                openUrl(IG_PAGE_URL)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Favorite, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("انستغرام", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        if (copiedFor == "ig") {
                            Text(
                                "✓ تم نسخ الطلب — الصقه في الرسالة",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE1306C),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOrderDialog = false; copiedFor = "" }) { Text("إلغاء") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // ── Store header ──────────────────────────────────────────────────
            Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "مكتبة المتميز",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                "فرع الشيخ زايد",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        TextButton(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تسجيل الدخول")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SocialButton("واتساب", Color(0xFF25D366)) {
                            openWhatsApp(WA_NUMBER, "مرحباً، أريد الاستفسار عن منتجاتكم 😊")
                        }
                        SocialButton("فيسبوك", Color(0xFF1877F2)) { openUrl(FB_PAGE_URL) }
                        SocialButton("انستغرام", Color(0xFFE1306C)) { openUrl(IG_PAGE_URL) }
                    }
                }
            }

            // ── Search bar ────────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن أي منتج…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // ── Category chips ────────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCatId.isEmpty(),
                        onClick  = { selectedCatId = "" },
                        label    = { Text("الكل") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCatId == cat.id,
                        onClick  = { selectedCatId = cat.id },
                        label    = { Text(cat.name) }
                    )
                }
            }

            // ── Products grid ─────────────────────────────────────────────────
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (displayedProducts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد منتجات", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Extra bottom padding so last row isn't hidden by the sticky bar
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 16.dp,
                        bottom = if (cartCount > 0) 100.dp else 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedProducts, key = { it.id }) { product ->
                        PublicProductCard(
                            product  = product,
                            quantity = cart[product.id] ?: 0,
                            onAdd    = { cart[product.id] = (cart[product.id] ?: 0) + 1 },
                            onRemove = {
                                val cur = cart[product.id] ?: 0
                                if (cur <= 1) cart.remove(product.id) else cart[product.id] = cur - 1
                            }
                        )
                    }
                }
            }
        }

        // ── Sticky order bar ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = cartCount > 0,
            enter   = slideInVertically { it } + fadeIn(),
            exit    = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color  = MaterialTheme.colorScheme.primary,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "$cartCount منتج مختار",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            "${cartTotal.formatPrice()} ج",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Clear cart
                        OutlinedButton(
                            onClick = { cart.clear() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("مسح", fontSize = 13.sp) }

                        // Send order
                        Button(
                            onClick = { showOrderDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor   = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ارسال الطلب", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicProductCard(
    product: Product,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val inCart = quantity > 0
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(if (inCart) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (inCart)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${product.price.formatPrice()} ج",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (product.stock == 0) {
                Text(
                    "نفد المخزون",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quantity controls / Add button
            if (product.stock == 0) {
                // disabled placeholder
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("غير متاح") }
            } else if (quantity == 0) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("أضف للطلب", fontWeight = FontWeight.Bold)
                }
            } else {
                // Stepper row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor   = MaterialTheme.colorScheme.error
                        )
                    ) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)) }

                    Text(
                        "$quantity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FilledIconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor   = MaterialTheme.colorScheme.primary
                        )
                    ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                }
                // Show subtotal
                Text(
                    "الإجمالي: ${(product.price * quantity).formatPrice()} ج",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SocialButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
