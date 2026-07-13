package com.elmotamyez.gallery

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
        var customerName    by remember { mutableStateOf("") }
        var customerPhone   by remember { mutableStateOf("") }
        var customerAddress by remember { mutableStateOf("") }
        var customerNotes   by remember { mutableStateOf("") }
        var paymentMethod   by remember { mutableStateOf("كاش") }

        val canSend = customerName.isNotBlank() && customerPhone.isNotBlank() && customerAddress.isNotBlank()

        fun buildFullMsg(): String = buildCartMsg(cartProducts) +
            "\n\nالاسم: ${customerName.trim()}" +
            "\nالهاتف: ${customerPhone.trim()}" +
            "\nالعنوان: ${customerAddress.trim()}" +
            (if (customerNotes.isNotBlank()) "\nملاحظات: ${customerNotes.trim()}" else "") +
            "\nطريقة الدفع: $paymentMethod"

        fun saveAndSend(openPlatform: () -> Unit) {
            val items = cartProducts.map { (p, qty) -> CartItem(p, qty) }
            orderVm.createOrder(
                items           = items,
                total           = cartTotal,
                paymentMethod   = paymentMethod,
                customerName    = customerName.trim(),
                customerPhone   = customerPhone.trim(),
                customerAddress = customerAddress.trim(),
                notes           = customerNotes.trim().ifBlank { null }
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
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$cartCount منتج  •  ${cartTotal.formatPrice()} ج",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    HorizontalDivider()

                    val fieldModifier = Modifier.fillMaxWidth().height(52.dp)
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("الاسم *", fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = fieldModifier
                    )
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("رقم الهاتف *", fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = fieldModifier
                    )
                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = { customerAddress = it },
                        label = { Text("العنوان *", fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = fieldModifier
                    )
                    OutlinedTextField(
                        value = customerNotes,
                        onValueChange = { customerNotes = it },
                        label = { Text("ملاحظات (اختياري)", fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = fieldModifier
                    )

                    // Payment method
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("الدفع:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        listOf("كاش", "تحويل").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick  = { paymentMethod = method },
                                label    = { Text(method, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (!canSend) {
                        Text("* الاسم والهاتف والعنوان مطلوبة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    }

                    HorizontalDivider()

                    Text("اختر طريقة الإرسال", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { saveAndSend { openWhatsApp(WA_NUMBER, buildFullMsg()) } },
                            enabled = canSend,
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Phone, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("واتساب", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                copyToClipboard(buildFullMsg())
                                saveAndSend { copyOpenState = CopyOpenState("فيسبوك", Color(0xFF1877F2), FB_PAGE_URL) }
                            },
                            enabled = canSend,
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Person, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("فيسبوك", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                copyToClipboard(buildFullMsg())
                                saveAndSend { copyOpenState = CopyOpenState("انستغرام", Color(0xFFE1306C), IG_PAGE_URL) }
                            },
                            enabled = canSend,
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Favorite, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("انستغرام", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
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
                    Text("مكتبة المتميز",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("فرع الشيخ زايد",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SocialButton("واتساب", Color(0xFF25D366)) {
                            openWhatsApp(WA_NUMBER, "مرحباً، أريد الاستفسار عن منتجاتكم 😊")
                        }
                        SocialButton("فيسبوك", Color(0xFF1877F2)) { openUrl(FB_PAGE_URL) }
                        SocialButton("انستغرام", Color(0xFFE1306C)) { openUrl(IG_PAGE_URL) }
                    }
                }
            }

            // ── Body: sidebar + right panel ───────────────────────────────────
            // Banner, search, and breadcrumb live INSIDE the scroll container so
            // they scroll away as the user scrolls down to see more products.
            if (state.isLoading) {
                Column(Modifier.weight(1f).fillMaxWidth()
                    .verticalScroll(rememberScrollState())) {
                    BannerSlider()
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
                    Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                Row(Modifier.weight(1f).fillMaxWidth()) {

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
                                Column(Modifier.weight(1f).fillMaxHeight()
                                    .verticalScroll(rememberScrollState())) {
                                    BannerSlider()
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
                                    if (selectedCategory != null) {
                                        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.fillMaxWidth()) {
                                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(onClick = {
                                                    selectedCategory = null; selectedBrand = null
                                                    catalogView = CatalogView.ALL_PRODUCTS
                                                }, modifier = Modifier.size(30.dp)) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                                                        tint = MaterialTheme.colorScheme.primary)
                                                }
                                                Text(selectedCategory!!.name,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Box(Modifier.fillMaxWidth().padding(64.dp),
                                        contentAlignment = Alignment.Center) {
                                        Text("لا توجد أقسام فرعية",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 130.dp),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    item(span = { GridItemSpan(maxLineSpan) }) { BannerSlider() }
                                    item(span = { GridItemSpan(maxLineSpan) }) {
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
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (selectedCategory != null) {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.fillMaxWidth()) {
                                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(onClick = {
                                                        selectedCategory = null; selectedBrand = null
                                                        catalogView = CatalogView.ALL_PRODUCTS
                                                    }, modifier = Modifier.size(30.dp)) {
                                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                                                            tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Text(selectedCategory!!.name,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
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
                                Column(Modifier.weight(1f).fillMaxHeight()
                                    .verticalScroll(rememberScrollState())) {
                                    BannerSlider()
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
                                    if (selectedCategory != null) {
                                        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.fillMaxWidth()) {
                                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(onClick = {
                                                    selectedCategory = null; selectedBrand = null
                                                    catalogView = CatalogView.ALL_PRODUCTS
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
                                    Box(Modifier.fillMaxWidth().padding(64.dp),
                                        contentAlignment = Alignment.Center) {
                                        Text("لا توجد منتجات",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    contentPadding = PaddingValues(
                                        start = 16.dp, end = 16.dp, top = 0.dp,
                                        bottom = if (cartCount > 0) 100.dp else 16.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    item(span = { GridItemSpan(maxLineSpan) }) { BannerSlider() }
                                    item(span = { GridItemSpan(maxLineSpan) }) {
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
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (selectedCategory != null) {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.fillMaxWidth()) {
                                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(onClick = {
                                                        selectedCategory = null; selectedBrand = null
                                                        catalogView = CatalogView.ALL_PRODUCTS
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
                                    }
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

// ── Banner slider ─────────────────────────────────────────────────────────────

private val BANNER_COUNT = 4

@Composable
private fun BannerSlider() {
    val pagerState = rememberPagerState(pageCount = { BANNER_COUNT })
    LaunchedEffect(Unit) {
        while (true) {
            delay(7000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % BANNER_COUNT)
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> BannerStore()
                1 -> BannerDelivery()
                2 -> BannerPricing()
                else -> BannerBackToSchool()
            }
        }
        // Dot indicators
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(BANNER_COUNT) { i ->
                val isActive = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .width(if (isActive) 28.dp else 8.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isActive) Color.White else Color.White.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

// ── Banner helper: draws the common decorative background via Canvas only ─────

private fun DrawScope.bannerDecor(
    circleColor1: Color, circleColor2: Color, accentColor: Color
) {
    // Large circle top-right
    drawCircle(
        color = circleColor1,
        radius = size.height * 1.1f,
        center = androidx.compose.ui.geometry.Offset(size.width + size.height * 0.2f, -size.height * 0.3f)
    )
    // Medium circle bottom-left
    drawCircle(
        color = circleColor2,
        radius = size.height * 0.55f,
        center = androidx.compose.ui.geometry.Offset(-size.height * 0.1f, size.height * 1.1f)
    )
    // Diagonal accent band
    val path = Path().apply {
        moveTo(size.width * 0.72f, 0f)
        lineTo(size.width * 0.78f, 0f)
        lineTo(size.width * 0.55f, size.height)
        lineTo(size.width * 0.49f, size.height)
        close()
    }
    drawPath(path, color = accentColor)
}

// ── Banner 1: Store ───────────────────────────────────────────────────────────
@Composable
private fun BannerStore() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(Color(0xFF0D3B6E), Color(0xFF07213F))))
            .drawBehind {
                bannerDecor(
                    circleColor1 = Color(0x181976D2),
                    circleColor2 = Color(0x141565C0),
                    accentColor  = Color(0x0CFFFFFF)
                )
                // Gold horizontal rule near bottom
                drawLine(
                    color = Color(0xFFC8951E),
                    start = androidx.compose.ui.geometry.Offset(24.dp.toPx(), size.height - 14.dp.toPx()),
                    end   = androidx.compose.ui.geometry.Offset(size.width * 0.45f, size.height - 14.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
                // Dot cluster decoration (right side)
                val dotCx = size.width * 0.88f
                val dotCy = size.height * 0.25f
                for (row in 0..3) for (col in 0..3) {
                    drawCircle(
                        color  = Color(0x15C8951E),
                        radius = 3.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(dotCx + col * 14.dp.toPx(), dotCy + row * 14.dp.toPx())
                    )
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.padding(start = 28.dp, end = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFC8951E).copy(alpha = 0.22f)
            ) {
                Text(
                    "الأفضل في فرع الشيخ زايد",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE8B84B),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "مكتبة المتميز",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 32.sp
            )
            Text(
                "كل ما تحتاجه من مستلزمات مدرسية\nومكتبية في مكان واحد",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 19.sp
            )
        }
    }
}

// ── Banner 2: Delivery ────────────────────────────────────────────────────────
@Composable
private fun BannerDelivery() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF0A3D13))))
            .drawBehind {
                bannerDecor(
                    circleColor1 = Color(0x1A2E7D32),
                    circleColor2 = Color(0x1227AE60),
                    accentColor  = Color(0x0CFFFFFF)
                )
                // Dashed road line
                val roadY = size.height * 0.78f
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color       = Color(0x35FFFFFF),
                        start       = androidx.compose.ui.geometry.Offset(x, roadY),
                        end         = androidx.compose.ui.geometry.Offset(x + 28.dp.toPx(), roadY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap         = StrokeCap.Round
                    )
                    x += 46.dp.toPx()
                }
                // Road band
                drawRect(
                    color    = Color(0x0CFFFFFF),
                    topLeft  = androidx.compose.ui.geometry.Offset(0f, roadY - 14.dp.toPx()),
                    size     = androidx.compose.ui.geometry.Size(size.width, 28.dp.toPx())
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.padding(start = 28.dp, end = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF27AE60).copy(alpha = 0.25f)
            ) {
                Text(
                    "توصيل سريع لكل مكان",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "توصيل لجميع\nالمحافظات",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 32.sp
            )
            Text(
                "هنوصلك لو مكانك فين\nاطلب الآن عبر واتساب أو فيسبوك",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 19.sp
            )
        }
    }
}

// ── Banner 3: Pricing ─────────────────────────────────────────────────────────
@Composable
private fun BannerPricing() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(Color(0xFF4A148C), Color(0xFF2D0A6E))))
            .drawBehind {
                bannerDecor(
                    circleColor1 = Color(0x1A7B1FA2),
                    circleColor2 = Color(0x15AB47BC),
                    accentColor  = Color(0x0CFFFFFF)
                )
                // Star dots
                val positions = listOf(0.12f to 0.18f, 0.28f to 0.82f, 0.45f to 0.12f,
                    0.62f to 0.75f, 0.78f to 0.30f, 0.90f to 0.60f)
                positions.forEach { (rx, ry) ->
                    drawCircle(
                        color  = Color(0x30FFD54F),
                        radius = 4.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width * rx, size.height * ry)
                    )
                    drawCircle(
                        color  = Color(0x15FFD54F),
                        radius = 10.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width * rx, size.height * ry)
                    )
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.padding(start = 28.dp, end = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFD54F).copy(alpha = 0.18f)
            ) {
                Text(
                    "جودة عالية بأسعار مناسبة",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFE082),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "أسعار تنافسية\nلا تُقاوَم",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 32.sp
            )
            Text(
                "منتجات بجودة عالية وأسعار مناسبة\nتسوّق الآن من كتالوجنا",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 19.sp
            )
        }
    }
}

// ── Banner 4: Back to School ──────────────────────────────────────────────────
@Composable
private fun BannerBackToSchool() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(Color(0xFF7B3F00), Color(0xFF4E2200))))
            .drawBehind {
                bannerDecor(
                    circleColor1 = Color(0x1AE65100),
                    circleColor2 = Color(0x12FF8F00),
                    accentColor  = Color(0x0CFFFFFF)
                )
                // Horizontal notebook lines (right half)
                for (i in 0..5) {
                    val y = size.height * (0.22f + i * 0.14f)
                    drawLine(
                        color       = Color(0x18FFFFFF),
                        start       = androidx.compose.ui.geometry.Offset(size.width * 0.48f, y),
                        end         = androidx.compose.ui.geometry.Offset(size.width * 0.95f, y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
                // Gold left accent bar
                drawRect(
                    color   = Color(0xFFC8951E),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size    = androidx.compose.ui.geometry.Size(5.dp.toPx(), size.height)
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.padding(start = 28.dp, end = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFF8F00).copy(alpha = 0.22f)
            ) {
                Text(
                    "موسم العودة للمدرسة",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFE082),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "العودة\nللمدرسة",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 32.sp
            )
            Text(
                "جهّز شنطتك بطقم الكتابة الكامل\nوكل المستلزمات المدرسية",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 19.sp
            )
        }
    }
}
