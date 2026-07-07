package com.elmotamyez.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.elmotamyez.gallery.data.model.Category
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.ui.screens.products.ProductsViewModel
import com.elmotamyez.gallery.util.formatPrice
import org.koin.compose.koinInject

private const val WA_NUMBER   = "201121064222"
private const val FB_PAGE_URL = "https://m.me/YOUR_FB_PAGE"   // replace with real page
private const val IG_PAGE_URL = "https://ig.me/m/almotamayz.gallery"

@JsFun("(num, msg) => { window.open('https://wa.me/' + num + '?text=' + encodeURIComponent(msg), '_blank'); }")
private external fun openWhatsApp(number: String, message: String)

@JsFun("(url) => { window.open(url, '_blank'); }")
external fun openUrl(url: String)

private fun buildProductWhatsAppMsg(product: Product, qty: Int = 1): String =
    "مرحباً، أريد طلب من مكتبة المتميز 🛒\n\n" +
    "• ${product.name} × $qty = ${(product.price * qty).formatPrice()} ج\n\n" +
    "الإجمالي: ${(product.price * qty).formatPrice()} ج"

@Composable
fun PublicCatalogScreen(onLoginClick: () -> Unit) {
    val vm: ProductsViewModel = koinInject()
    val state by vm.uiState.collectAsState()

    val categories = state.categories
    val allProducts = state.allProducts

    var selectedCatId by remember(categories) {
        mutableStateOf(categories.firstOrNull()?.id ?: "")
    }
    var searchQuery by remember { mutableStateOf("") }

    val displayedProducts = remember(allProducts, selectedCatId, searchQuery) {
        allProducts.filter { p ->
            (selectedCatId.isEmpty() || p.categoryId == selectedCatId) &&
            (searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(Modifier.fillMaxSize()) {
        // ── Store header ──────────────────────────────────────────────────────
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

                // Social order buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialButton("واتساب", Color(0xFF25D366)) {
                        openWhatsApp(WA_NUMBER, "مرحباً، أريد الاستفسار عن منتجاتكم 😊")
                    }
                    SocialButton("فيسبوك", Color(0xFF1877F2)) { openUrl(FB_PAGE_URL) }
                    SocialButton("انستغرام", Color(0xFFE1306C)) { openUrl(IG_PAGE_URL) }
                }
            }
        }

        // ── Search bar ────────────────────────────────────────────────────────
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

        // ── Category chips ────────────────────────────────────────────────────
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

        // ── Products grid ─────────────────────────────────────────────────────
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (displayedProducts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد منتجات", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedProducts, key = { it.id }) { product ->
                    PublicProductCard(product = product)
                }
            }
        }
    }
}

@Composable
private fun PublicProductCard(product: Product) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
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
                Text("نفد المخزون", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
            // WhatsApp order button
            Button(
                onClick = { openWhatsApp(WA_NUMBER, buildProductWhatsAppMsg(product)) },
                enabled = product.stock > 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Icon(Icons.Default.Phone, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("اطلب عبر واتساب", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            // Social row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { openUrl(FB_PAGE_URL) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) { Text("فيسبوك", fontSize = 11.sp, color = Color(0xFF1877F2)) }
                OutlinedButton(
                    onClick = { openUrl(IG_PAGE_URL) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) { Text("انستغرام", fontSize = 11.sp, color = Color(0xFFE1306C)) }
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
