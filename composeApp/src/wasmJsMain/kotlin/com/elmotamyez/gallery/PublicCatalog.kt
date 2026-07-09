package com.elmotamyez.gallery

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmotamyez.gallery.data.model.Brand
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Category
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.ui.screens.orders.OrderViewModel
import com.elmotamyez.gallery.ui.screens.products.ProductsViewModel
import com.elmotamyez.gallery.util.formatPrice
import org.koin.compose.koinInject

// ── Constants ─────────────────────────────────────────────────────────────────

private const val WA_NUMBER   = "201121064222"
private const val FB_PAGE_URL = "https://m.me/almotamiz.bookstore"
private const val IG_PAGE_URL = "https://ig.me/m/almotamayz.gallery"

@JsFun("(num, msg) => { window.open('https://wa.me/' + num + '?text=' + encodeURIComponent(msg), '_blank'); }")
private external fun openWhatsApp(number: String, message: String)

@JsFun("(url) => { window.open(url, '_blank'); }")
external fun openUrl(url: String)

@JsFun("""(text) => {
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).catch(function() { fallback(text); });
    } else { fallback(text); }
    function fallback(t) {
        var ta = document.createElement('textarea');
        ta.value = t; ta.style.position = 'fixed'; ta.style.opacity = '0';
        document.body.appendChild(ta); ta.focus(); ta.select();
        try { document.execCommand('copy'); } catch(e) {}
        document.body.removeChild(ta);
    }
}""")
private external fun copyToClipboard(text: String)

private fun buildCartMsg(items: Map<Product, Int>): String {
    val lines = items.entries.joinToString("\n") { (p, qty) ->
        "• ${p.name} × $qty = ${(p.price * qty).formatPrice()} ج"
    }
    val total = items.entries.sumOf { (p, qty) -> p.price * qty }
    return "مرحباً، أريد طلب من مكتبة المتميز 🛒\n\n$lines\n\nالإجمالي: ${total.formatPrice()} ج"
}

// ── Right-panel view state ────────────────────────────────────────────────────

private enum class CatalogView { ALL_PRODUCTS, SUBCATEGORIES, PRODUCTS }

// ── Platform copy-then-open dialog state ──────────────────────────────────────

private data class CopyOpenState(val platformName: String, val platformColor: Color, val url: String)

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
fun PublicCatalogScreen(onLoginClick: () -> Unit) {
    val vm: ProductsViewModel = koinInject()
    val orderVm: OrderViewModel = koinInject()
    val state by vm.uiState.collectAsState()

    val categories  = state.categories
    val allProducts = state.allProducts
    val allBrands   = state.brands

    // Navigation
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedBrand    by remember { mutableStateOf<Brand?>(null) }
    var catalogView      by remember { mutableStateOf(CatalogView.ALL_PRODUCTS) }

    // Global search (active when no brand selected)
    var searchQuery by remember { mutableStateOf("") }

    // Cart
    val cart = remember { mutableStateMapOf<String, Int>() }
    val cartProducts = allProducts.filter { cart.containsKey(it.id) }
        .associateWith { cart[it.id] ?: 0 }
    val cartCount = cart.values.sum()
    val cartTotal = cartProducts.entries.sumOf { (p, q) -> p.price * q }

    // Dialog state
    var showOrderDialog  by remember { mutableStateOf(false) }
    var copyOpenState    by remember { mutableStateOf<CopyOpenState?>(null) }

    // Subcategories for selected category
    val subcategories = remember(selectedCategory, allBrands) {
        allBrands.filter { it.categoryId == selectedCategory?.id && it.parentId == null }
    }

    // Products shown in the right panel
    val displayedProducts = remember(catalogView, selectedCategory, selectedBrand, allProducts, searchQuery) {
        when (catalogView) {
            CatalogView.ALL_PRODUCTS -> allProducts.filter {
                searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
            }
            CatalogView.SUBCATEGORIES -> allProducts.filter {
                it.categoryId == selectedCategory?.id &&
                (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true))
            }
            CatalogView.PRODUCTS -> allProducts.filter {
                it.brandId == selectedBrand?.id &&
                (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    // ── Send order dialog ─────────────────────────────────────────────────────
    if (showOrderDialog) {
        val orderMsg = buildCartMsg(cartProducts)
        var customerName  by remember { mutableStateOf("") }
        var customerPhone by remember { mutableStateOf("") }
        var customerNotes by remember { mutableStateOf("") }
        var paymentMethod by remember { mutableStateOf("كاش") }

        fun saveAndSend(openPlatform: () -> Unit) {
            val items = cartProducts.map { (p, qty) -> CartItem(p, qty) }
            orderVm.createOrder(
                items         = items,
                total         = cartTotal,
                paymentMethod = paymentMethod,
                customerName  = customerName.trim().ifBlank { null },
                customerPhone = customerPhone.trim().ifBlank { null },
                notes         = customerNotes.trim().ifBlank { null }
            )
            openPlatform()
            showOrderDialog = false
            cart.clear()
        }

        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = {
                Text("إرسال الطلب", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("$cartCount منتج  •  ${cartTotal.formatPrice()} ج",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    HorizontalDivider()

                    // Customer info (optional)
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("الاسم (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("رقم الهاتف (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customerNotes,
                        onValueChange = { customerNotes = it },
                        label = { Text("ملاحظات (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Payment method
                    Text("طريقة الدفع", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("كاش", "تحويل").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick  = { paymentMethod = method },
                                label    = { Text(method) }
                            )
                        }
                    }

                    HorizontalDivider()

                    Text("اختر طريقة الإرسال", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // WhatsApp
                    Button(
                        onClick = { saveAndSend { openWhatsApp(WA_NUMBER, orderMsg) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Phone, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("واتساب", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    // Facebook
                    Button(
                        onClick = {
                            copyToClipboard(orderMsg)
                            saveAndSend { copyOpenState = CopyOpenState("فيسبوك", Color(0xFF1877F2), FB_PAGE_URL) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Person, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("فيسبوك", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    // Instagram
                    Button(
                        onClick = {
                            copyToClipboard(orderMsg)
                            saveAndSend { copyOpenState = CopyOpenState("انستغرام", Color(0xFFE1306C), IG_PAGE_URL) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Favorite, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("انستغرام", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showOrderDialog = false }) { Text("إلغاء") } },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Copy-then-open redirect dialog ────────────────────────────────────────
    copyOpenState?.let { cos ->
        AlertDialog(
            onDismissRequest = { copyOpenState = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = Color(0xFF2E7D32), modifier = Modifier.size(22.dp))
                    Text("تم نسخ تفاصيل طلبك!", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "تم نسخ تفاصيل طلبك. بعد فتح ${cos.platformName}، الصق الرسالة في المحادثة وأرسلها.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentPaste, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("الصق بـ Ctrl+V أو الضغط المطوّل ثم لصق",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { openUrl(cos.url); copyOpenState = null },
                    colors = ButtonDefaults.buttonColors(containerColor = cos.platformColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("افتح ${cos.platformName}", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { copyOpenState = null }) { Text("إلغاء") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Root layout ───────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Surface(color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("مكتبة المتميز",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("فرع الشيخ زايد",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f))
                        }
                        TextButton(onClick = onLoginClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                            Icon(Icons.Default.Person, null, Modifier.size(16.dp))
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

            // ── Search bar (always visible) ───────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في جميع المنتجات…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // ── Breadcrumb ────────────────────────────────────────────────────
            if (selectedCategory != null) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            selectedCategory = null
                            selectedBrand    = null
                            catalogView      = CatalogView.ALL_PRODUCTS
                        }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(selectedCategory!!.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedBrand == null) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selectedBrand == null) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                selectedBrand = null
                                catalogView   = CatalogView.SUBCATEGORIES
                            })
                        if (selectedBrand != null) {
                            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedBrand!!.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // ── Body: sidebar + right panel ───────────────────────────────────
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Row(Modifier.fillMaxSize()) {

                    // ── Category sidebar ──────────────────────────────────────
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.width(130.dp).fillMaxHeight()) {
                        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                            // "الكل" entry
                            item {
                                val selected = selectedCategory == null
                                Surface(
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                                        .clickable {
                                            selectedCategory = null
                                            selectedBrand    = null
                                            catalogView      = CatalogView.ALL_PRODUCTS
                                        }
                                ) {
                                    Text("الكل",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 12.dp))
                                }
                            }
                            items(categories) { cat ->
                                val selected = selectedCategory?.id == cat.id
                                Surface(
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                                        .clickable {
                                            selectedCategory = cat
                                            selectedBrand    = null
                                            catalogView      = CatalogView.SUBCATEGORIES
                                            searchQuery      = ""
                                        }
                                ) {
                                    Text(cat.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 12.dp))
                                }
                            }
                        }
                    }

                    // ── Right panel ───────────────────────────────────────────
                    when (catalogView) {

                        // Subcategory grid
                        CatalogView.SUBCATEGORIES -> {
                            if (subcategories.isEmpty()) {
                                Box(Modifier.weight(1f).fillMaxHeight(),
                                    contentAlignment = Alignment.Center) {
                                    Text("لا توجد أقسام فرعية",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 130.dp),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    items(subcategories, key = { it.id }) { brand ->
                                        SubcategoryCard(name = brand.name, onClick = {
                                            selectedBrand = brand
                                            catalogView   = CatalogView.PRODUCTS
                                        })
                                    }
                                }
                            }
                        }

                        // Products grid (ALL or filtered by brand)
                        else -> {
                            if (displayedProducts.isEmpty()) {
                                Box(Modifier.weight(1f).fillMaxHeight(),
                                    contentAlignment = Alignment.Center) {
                                    Text("لا توجد منتجات",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    contentPadding = PaddingValues(
                                        start = 16.dp, end = 16.dp, top = 8.dp,
                                        bottom = if (cartCount > 0) 100.dp else 16.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    items(displayedProducts, key = { it.id }) { product ->
                                        PublicProductCard(
                                            product  = product,
                                            quantity = cart[product.id] ?: 0,
                                            onAdd    = { cart[product.id] = (cart[product.id] ?: 0) + 1 },
                                            onRemove = {
                                                val cur = cart[product.id] ?: 0
                                                if (cur <= 1) cart.remove(product.id)
                                                else cart[product.id] = cur - 1
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Sticky order bar ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = cartCount > 0,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(color = MaterialTheme.colorScheme.primary,
                shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("$cartCount منتج مختار",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f))
                        Text("${cartTotal.formatPrice()} ج",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { cart.clear() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("مسح", fontSize = 13.sp) }
                        Button(
                            onClick = { showOrderDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor   = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Send, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ارسال الطلب", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Subcategory card ──────────────────────────────────────────────────────────

@Composable
private fun SubcategoryCard(name: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Product card ──────────────────────────────────────────────────────────────

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
            containerColor = if (inCart) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth())
            Text("${product.price.formatPrice()} ج",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary)
            if (product.stock == 0) {
                Text("نفد المخزون",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold)
            }
            if (product.stock == 0) {
                Button(onClick = {}, enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)) { Text("غير متاح") }
            } else if (quantity == 0) {
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("أضف للطلب", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(onClick = onRemove, modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor   = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    }
                    Text("$quantity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary)
                    FilledIconButton(onClick = onAdd, modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor   = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    }
                }
                Text("الإجمالي: ${(product.price * quantity).formatPrice()} ج",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ── Social button ─────────────────────────────────────────────────────────────

@Composable
private fun SocialButton(label: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
