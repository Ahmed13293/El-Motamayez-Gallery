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

            // ── Banner slider ─────────────────────────────────────────────────
            BannerSlider()

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

// ── Banner slider ─────────────────────────────────────────────────────────────

private val BANNER_COUNT = 4

@Composable
private fun BannerSlider() {
    val pagerState = rememberPagerState(pageCount = { BANNER_COUNT })
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(BANNER_COUNT) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == pagerState.currentPage) 28.dp else 8.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (i == pagerState.currentPage) Color.White
                            else Color.White.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

// ── Banner 1: Store ───────────────────────────────────────────────────────────
@Composable
private fun BannerStore() {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF0D3B6E)))
        ).drawBehind {
            // large decorative circle top-right
            drawCircle(color = Color(0x221976D2), radius = 160f, center = androidx.compose.ui.geometry.Offset(size.width - 80f, 0f))
            drawCircle(color = Color(0x151976D2), radius = 110f, center = androidx.compose.ui.geometry.Offset(size.width - 40f, size.height * 0.4f))
            // small circle bottom-left
            drawCircle(color = Color(0x20FFFFFF), radius = 70f, center = androidx.compose.ui.geometry.Offset(40f, size.height))
            // diagonal stripe
            val path = Path().apply {
                moveTo(size.width * 0.55f, 0f)
                lineTo(size.width * 0.65f, 0f)
                lineTo(size.width * 0.45f, size.height)
                lineTo(size.width * 0.35f, size.height)
                close()
            }
            drawPath(path, color = Color(0x10FFFFFF))
        }
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            // Illustration: stacked books
            Box(Modifier.size(110.dp, 130.dp)) {
                // Book 3 (back)
                Box(Modifier.size(80.dp, 100.dp).offset(20.dp, 20.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Color(0xFF42A5F5)))
                // Book 2
                Box(Modifier.size(80.dp, 100.dp).offset(10.dp, 10.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Color(0xFF66BB6A)))
                // Book 1 (front)
                Box(Modifier.size(80.dp, 100.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFEF5350))) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.6f)))
                        Box(Modifier.fillMaxWidth(0.7f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.4f)))
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.4f)))
                        Box(Modifier.fillMaxWidth(0.8f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.3f)))
                    }
                    // Book spine
                    Box(Modifier.width(8.dp).fillMaxHeight().background(Color.Black.copy(0.15f)))
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFD54F).copy(0.25f)) {
                    Text("⭐ الأفضل في الشيخ زايد", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                }
                Text("مكتبة المتميز", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 30.sp)
                Text("كل ما تحتاجه من مستلزمات\nمدرسية ومكتبية في مكان واحد",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
            }
        }
    }
}

// ── Banner 2: Delivery ────────────────────────────────────────────────────────
@Composable
private fun BannerDelivery() {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)))
        ).drawBehind {
            drawCircle(color = Color(0x2043A047), radius = 140f, center = androidx.compose.ui.geometry.Offset(size.width - 60f, size.height * 0.3f))
            drawCircle(color = Color(0x15FFFFFF), radius = 90f, center = androidx.compose.ui.geometry.Offset(60f, size.height))
            // Road dashes
            val dashY = size.height * 0.72f
            var x = 0f
            while (x < size.width) {
                drawLine(color = Color(0x40FFFFFF), start = androidx.compose.ui.geometry.Offset(x, dashY),
                    end = androidx.compose.ui.geometry.Offset(x + 30f, dashY), strokeWidth = 3f)
                x += 50f
            }
        }
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            // Truck illustration
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(120.dp, 70.dp)) {
                    // Truck body
                    Box(Modifier.size(80.dp, 50.dp).align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(Color(0xFFFFFFFF).copy(0.9f)))
                    // Cab
                    Box(Modifier.size(45.dp, 45.dp).align(Alignment.CenterStart).offset(y = 5.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 4.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                        .background(Color(0xFFE8F5E9)))
                    // Window
                    Box(Modifier.size(22.dp, 18.dp).align(Alignment.TopStart).offset(6.dp, 4.dp)
                        .clip(RoundedCornerShape(4.dp)).background(Color(0xFF81D4FA)))
                    // Wheels
                    Box(Modifier.size(22.dp).align(Alignment.BottomStart).offset(8.dp)
                        .clip(CircleShape).background(Color(0xFF37474F)))
                    Box(Modifier.size(22.dp).align(Alignment.BottomEnd).offset((-8).dp)
                        .clip(CircleShape).background(Color(0xFF37474F)))
                }
                // Location pin below truck
                Icon(Icons.Default.LocationOn, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0x3081C784)) {
                    Text("🚀 توصيل سريع", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text("توصيل لجميع\nالمحافظات", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 30.sp)
                Text("هنوصلك لو مكانك فين\nاطلب الآن عبر واتساب",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
            }
        }
    }
}

// ── Banner 3: Pricing ─────────────────────────────────────────────────────────
@Composable
private fun BannerPricing() {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(Color(0xFF6A1B9A), Color(0xFF4A148C)))
        ).drawBehind {
            drawCircle(color = Color(0x307B1FA2), radius = 150f, center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, -20f))
            drawCircle(color = Color(0x20AB47BC), radius = 100f, center = androidx.compose.ui.geometry.Offset(size.width - 40f, size.height + 20f))
            // Decorative stars
            for (i in 0..4) {
                val cx = size.width * (0.1f + i * 0.18f)
                val cy = size.height * 0.15f
                drawCircle(color = Color(0x40FFD54F), radius = 4f, center = androidx.compose.ui.geometry.Offset(cx, cy))
            }
        }
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            // Price tag illustration
            Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                // Tag shape
                Box(Modifier.size(90.dp, 90.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(0.15f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("%", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD54F))
                        Box(Modifier.fillMaxWidth(0.6f).height(2.dp).background(Color.White.copy(0.4f)))
                        Text("خصم", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                // Small circle top-right
                Box(Modifier.size(22.dp).align(Alignment.TopEnd).offset((-4).dp, 4.dp)
                    .clip(CircleShape).background(Color(0xFFFFD54F)), contentAlignment = Alignment.Center) {
                    Text("★", fontSize = 10.sp, color = Color(0xFF4A148C))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0x40CE93D8)) {
                    Text("💎 جودة عالية", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text("أسعار تنافسية\nلا تُقاوَم", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 30.sp)
                Text("جودة عالية بأسعار مناسبة\nتسوّق الآن من كتالوجنا",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
            }
        }
    }
}

// ── Banner 4: Back to School ──────────────────────────────────────────────────
@Composable
private fun BannerBackToSchool() {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(Color(0xFFE65100), Color(0xFFBF360C)))
        ).drawBehind {
            drawCircle(color = Color(0x30F57C00), radius = 160f, center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, -30f))
            drawCircle(color = Color(0x20FFFFFF), radius = 80f, center = androidx.compose.ui.geometry.Offset(20f, size.height * 0.8f))
            // Notebook lines in background
            for (i in 0..3) {
                val y = size.height * (0.25f + i * 0.18f)
                drawLine(color = Color(0x15FFFFFF), start = androidx.compose.ui.geometry.Offset(size.width * 0.5f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 2f)
            }
        }
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            // Pencil + ruler illustration
            Box(Modifier.size(110.dp, 130.dp), contentAlignment = Alignment.Center) {
                // Ruler
                Box(Modifier.size(18.dp, 110.dp).offset((-20).dp).clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFD54F))) {
                    Column(Modifier.fillMaxSize().padding(vertical = 6.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                        repeat(6) {
                            Box(Modifier.fillMaxWidth(0.5f).height(1.dp).background(Color.Black.copy(0.3f)))
                        }
                    }
                }
                // Pencil body
                Box(Modifier.size(22.dp, 110.dp).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(Color(0xFFFFCC02))) {
                    // Eraser
                    Box(Modifier.size(22.dp, 16.dp).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(Color(0xFFEF9A9A)))
                    // Metal band
                    Box(Modifier.size(22.dp, 8.dp).align(Alignment.TopCenter).offset(y = 16.dp)
                        .background(Color(0xFFBDBDBD)))
                    // Tip
                    Box(Modifier.size(22.dp, 20.dp).align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(Color(0xFFFFCC02)))
                }
                // Backpack icon
                Box(Modifier.size(40.dp).align(Alignment.BottomEnd).offset(10.dp, 10.dp)
                    .clip(CircleShape).background(Color.White.copy(0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ShoppingBag, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0x30FF8F00)) {
                    Text("🎒 موسم المدارس", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFE082), fontWeight = FontWeight.Bold)
                }
                Text("العودة\nللمدرسة", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 30.sp)
                Text("جهّز شنطتك بطقم الكتابة الكامل\nوكل المستلزمات المدرسية",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 18.sp)
            }
        }
    }
}
