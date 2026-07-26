package com.elmotamyez.gallery

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.elmotamyez.gallery.util.buildProductPath
import com.elmotamyez.gallery.util.formatPrice
import org.koin.compose.koinInject

// ── Contact ───────────────────────────────────────────────────────────────────

private const val PRL_WA  = "201121064222"   // update to pirlanta's WA
private const val PRL_IG  = "https://ig.me/m/almotamayz.gallery"
private const val PRL_FB  = "https://m.me/almotamiz.bookstore"

@JsFun("(num, msg) => { window.open('https://wa.me/' + num + '?text=' + encodeURIComponent(msg), '_blank'); }")
private external fun prlOpenWhatsApp(number: String, message: String)

@JsFun("""(text) => {
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).catch(function() { fallback(text); });
    } else { fallback(text); }
    function fallback(t) {
        var ta = document.createElement('textarea');
        ta.value = t; ta.style.position = 'fixed'; ta.style.opacity = '0';
        document.body.appendChild(ta); ta.focus(); ta.select();
        try { document.execCommand('copy'); } catch(e) {} document.body.removeChild(ta);
    }
}""")
private external fun prlCopyToClipboard(text: String)

// ── Color scheme — extracted from pirlanta logo ───────────────────────────────

private val PirlantaColorScheme = lightColorScheme(
    primary              = Color(0xFF9B3453),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFFFD9E3),
    onPrimaryContainer   = Color(0xFF3D0018),
    secondary            = Color(0xFFC27080),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFFFD9E3),
    onSecondaryContainer = Color(0xFF3D0018),
    tertiary             = Color(0xFFD4849A),
    onTertiary           = Color.White,
    background           = Color(0xFFFFF8F9),
    onBackground         = Color(0xFF1A0D10),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1A0D10),
    surfaceVariant       = Color(0xFFF5DEE4),
    onSurfaceVariant     = Color(0xFF5C3039),
    outline              = Color(0xFFBF8A91),
    error                = Color(0xFFBA1A1A),
    onError              = Color.White,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
)

// ── View state ────────────────────────────────────────────────────────────────

private enum class PrlView { ALL_PRODUCTS, SUBCATEGORIES, PRODUCTS }
private data class PrlCopyOpen(val platformName: String, val platformColor: Color, val url: String)

// ── Entry composable ──────────────────────────────────────────────────────────

@Composable
fun PirlantaCatalogScreen(onLoginClick: () -> Unit) {
    MaterialTheme(
        colorScheme = PirlantaColorScheme,
        typography  = MaterialTheme.typography,
        shapes      = MaterialTheme.shapes
    ) {
        PirlantaContent(onLoginClick)
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PirlantaContent(onLoginClick: () -> Unit) {
    val vm: ProductsViewModel = koinInject()
    val orderVm: OrderViewModel = koinInject()
    val state    by vm.uiState.collectAsState()
    val orderErr by orderVm.error.collectAsState()

    orderErr?.let {
        AlertDialog(
            onDismissRequest = { orderVm.clearError() },
            title = { Text("فشل حفظ الطلب", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text  = { Text(it) },
            confirmButton = { TextButton(onClick = { orderVm.clearError() }) { Text("حسناً") } }
        )
    }

    // ── Filter to pirlanta categories only ────────────────────────────────────
    val allCategories = state.categories
    val categories = remember(allCategories) {
        allCategories.filter { cat ->
            val n = cat.name.trim()
            n.contains("تجميل", ignoreCase = true) || n.contains("توك", ignoreCase = true)
        }
    }
    val allBrands   = state.brands
    val allProducts = remember(state.allProducts, categories) {
        val ids = categories.map { it.id }.toSet()
        state.allProducts.filter { it.categoryId in ids }
    }

    // ── Navigation state ──────────────────────────────────────────────────────
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedBrand    by remember { mutableStateOf<Brand?>(null) }
    var prlView          by remember { mutableStateOf(PrlView.ALL_PRODUCTS) }
    var searchQuery      by remember { mutableStateOf("") }

    // ── Cart ──────────────────────────────────────────────────────────────────
    val cart         = remember { mutableStateMapOf<String, Int>() }
    val cartProducts = allProducts.filter { cart.containsKey(it.id) }.associateWith { cart[it.id] ?: 0 }
    val cartCount    = cart.values.sum()
    val cartTotal    = cartProducts.entries.sumOf { (p, q) -> p.price * q }

    // ── Dialog state ──────────────────────────────────────────────────────────
    var showOrderDialog by remember { mutableStateOf(false) }
    var copyOpenState   by remember { mutableStateOf<PrlCopyOpen?>(null) }

    val subcategories = remember(selectedCategory, allBrands) {
        allBrands.filter { it.categoryId == selectedCategory?.id && it.parentId == null }
    }

    val displayedProducts = remember(prlView, selectedCategory, selectedBrand, allProducts, searchQuery) {
        when (prlView) {
            PrlView.ALL_PRODUCTS  -> allProducts.filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
            PrlView.SUBCATEGORIES -> allProducts.filter { it.categoryId == selectedCategory?.id && (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)) }
            PrlView.PRODUCTS      -> allProducts.filter { it.brandId == selectedBrand?.id    && (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)) }
        }
    }

    fun buildMsg(): String {
        val lines = cartProducts.entries.joinToString("\n") { (p, qty) ->
            "• ${p.name} × $qty = ${(p.price * qty).formatPrice()} ج"
        }
        return "مرحباً، أريد طلب من بيرلانتا 🎀\n\n$lines\n\nالإجمالي: ${cartTotal.formatPrice()} ج"
    }

    // ── Order dialog ──────────────────────────────────────────────────────────
    if (showOrderDialog) {
        var name    by remember { mutableStateOf("") }
        var phone   by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var notes   by remember { mutableStateOf("") }
        var payment by remember { mutableStateOf("كاش") }
        val canSend = name.isNotBlank() && phone.isNotBlank() && address.isNotBlank()

        fun fullMsg() = buildMsg() +
            "\n\nالاسم: ${name.trim()}\nالهاتف: ${phone.trim()}\nالعنوان: ${address.trim()}" +
            (if (notes.isNotBlank()) "\nملاحظات: ${notes.trim()}" else "") +
            "\nطريقة الدفع: $payment"

        fun saveAndSend(open: () -> Unit) {
            orderVm.createOrder(
                items           = cartProducts.map { (p, qty) -> CartItem(p, qty) },
                total           = cartTotal,
                paymentMethod   = payment,
                customerName    = name.trim(),
                customerPhone   = phone.trim(),
                customerAddress = address.trim(),
                notes           = notes.trim().ifBlank { null }
            )
            open(); showOrderDialog = false; cart.clear()
        }

        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = { Text("إرسال الطلب", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$cartCount منتج  •  ${cartTotal.formatPrice()} ج", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    val fm = Modifier.fillMaxWidth().height(52.dp)
                    OutlinedTextField(value = name,    onValueChange = { name = it },    label = { Text("الاسم *",       fontSize = 11.sp) }, singleLine = true, textStyle = MaterialTheme.typography.bodySmall, modifier = fm)
                    OutlinedTextField(value = phone,   onValueChange = { phone = it },   label = { Text("رقم الهاتف *", fontSize = 11.sp) }, singleLine = true, textStyle = MaterialTheme.typography.bodySmall, modifier = fm)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان *",     fontSize = 11.sp) }, singleLine = true, textStyle = MaterialTheme.typography.bodySmall, modifier = fm)
                    OutlinedTextField(value = notes,   onValueChange = { notes = it },   label = { Text("ملاحظات",      fontSize = 11.sp) }, singleLine = true, textStyle = MaterialTheme.typography.bodySmall, modifier = fm)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("الدفع:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        listOf("كاش", "تحويل").forEach { m ->
                            FilterChip(selected = payment == m, onClick = { payment = m }, label = { Text(m, fontSize = 11.sp) })
                        }
                    }
                    if (!canSend) Text("* الاسم والهاتف والعنوان مطلوبة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    HorizontalDivider()
                    Text("اختر طريقة الإرسال", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { saveAndSend { prlOpenWhatsApp(PRL_WA, fullMsg()) } }, enabled = canSend, modifier = Modifier.weight(1f).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Icon(Icons.Default.Phone, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("واتساب", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(onClick = { prlCopyToClipboard(fullMsg()); saveAndSend { copyOpenState = PrlCopyOpen("انستغرام", Color(0xFFE1306C), PRL_IG) } }, enabled = canSend, modifier = Modifier.weight(1f).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Icon(Icons.Default.Favorite, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("انستغرام", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showOrderDialog = false }) { Text("إلغاء") } },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Copy-redirect dialog ──────────────────────────────────────────────────
    copyOpenState?.let { cos ->
        AlertDialog(
            onDismissRequest = { copyOpenState = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(22.dp))
                    Text("تم نسخ تفاصيل طلبك!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تم نسخ تفاصيل طلبك. بعد فتح ${cos.platformName}، الصق الرسالة وأرسلها.", style = MaterialTheme.typography.bodyMedium)
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentPaste, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("الصق بـ Ctrl+V أو الضغط المطوّل ثم لصق", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { openUrl(cos.url); copyOpenState = null }, colors = ButtonDefaults.buttonColors(containerColor = cos.platformColor), shape = RoundedCornerShape(10.dp)) {
                    Text("افتح ${cos.platformName}", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { copyOpenState = null }) { Text("إلغاء") } },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Root layout ───────────────────────────────────────────────────────────
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val isMobile = maxWidth < 600.dp
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {

            // ── Category sidebar (desktop) ────────────────────────────────────
            if (!isMobile) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.width(130.dp).fillMaxHeight()) {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        item {
                            val sel = selectedCategory == null
                            Surface(
                                color = if (sel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                                    .clickable { selectedCategory = null; selectedBrand = null; prlView = PrlView.ALL_PRODUCTS }
                            ) {
                                Text("الكل", style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp))
                            }
                        }
                        items(categories) { cat ->
                            val sel = selectedCategory?.id == cat.id
                            Surface(
                                color = if (sel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                                    .clickable { selectedCategory = cat; selectedBrand = null; prlView = PrlView.SUBCATEGORIES; searchQuery = "" }
                            ) {
                                Text(cat.name, style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp))
                            }
                        }
                    }
                }
            }

            // ── Right panel ───────────────────────────────────────────────────
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                val colCount    = maxOf(1, (maxWidth / 165.dp).toInt())
                val subColCount = maxOf(1, (maxWidth / 140.dp).toInt())

                LazyColumn(Modifier.fillMaxSize()) {

                    // Store header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF9B3453), Color(0xFFC06080))
                                    )
                                )
                                .drawBehind {
                                    // Decorative circles
                                    drawCircle(color = Color(0x22FFFFFF), radius = size.height * 1.8f,
                                        center = Offset(size.width + size.height * 0.5f, -size.height * 0.5f))
                                    drawCircle(color = Color(0x15FFFFFF), radius = size.height * 0.8f,
                                        center = Offset(-size.height * 0.2f, size.height * 1.2f))
                                    // Diagonal petal stroke
                                    val p = Path().apply {
                                        moveTo(size.width * 0.70f, 0f); lineTo(size.width * 0.76f, 0f)
                                        lineTo(size.width * 0.53f, size.height); lineTo(size.width * 0.47f, size.height); close()
                                    }
                                    drawPath(p, color = Color(0x12FFFFFF))
                                }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("بيرلانتا",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("أدوات تجميل واكسسوارات",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PrlSocialButton("واتساب", Color(0xFF25D366)) {
                                        prlOpenWhatsApp(PRL_WA, "مرحباً، أريد الاستفسار عن منتجات بيرلانتا 🎀")
                                    }
                                    PrlSocialButton("انستغرام", Color(0xFFE1306C)) { openUrl(PRL_IG) }
                                }
                            }
                        }
                    }

                    // Banner slider
                    item {
                        Box(Modifier.fillMaxWidth().padding(12.dp)) {
                            PirlantaBannerSlider()
                        }
                    }

                    // Sticky search + mobile category chips
                    stickyHeader {
                        Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()) {
                            Column {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("ابحث في المنتجات…") },
                                    leadingIcon  = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty())
                                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                if (isMobile) {
                                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()) {
                                        item {
                                            FilterChip(selected = selectedCategory == null,
                                                onClick = { selectedCategory = null; selectedBrand = null; prlView = PrlView.ALL_PRODUCTS },
                                                label = { Text("الكل") })
                                        }
                                        items(categories) { cat ->
                                            FilterChip(selected = selectedCategory?.id == cat.id,
                                                onClick = { selectedCategory = cat; selectedBrand = null; prlView = PrlView.SUBCATEGORIES; searchQuery = "" },
                                                label = { Text(cat.name) })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Breadcrumb
                    if (selectedCategory != null) {
                        item {
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { selectedCategory = null; selectedBrand = null; prlView = PrlView.ALL_PRODUCTS }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(selectedCategory!!.name, style = MaterialTheme.typography.labelMedium,
                                        color = if (selectedBrand == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (selectedBrand == null) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.clickable { selectedBrand = null; prlView = PrlView.SUBCATEGORIES })
                                    if (selectedBrand != null) {
                                        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(selectedBrand!!.name, style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    // Loading
                    if (state.isLoading) {
                        item { Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                    } else when (prlView) {

                        PrlView.SUBCATEGORIES -> {
                            if (subcategories.isEmpty()) {
                                item { Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) { Text("لا توجد أقسام فرعية", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                            } else {
                                val rows = subcategories.chunked(subColCount)
                                items(rows) { row ->
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        row.forEach { brand ->
                                            Box(Modifier.weight(1f)) {
                                                PrlSubcategoryCard(name = brand.name) {
                                                    selectedBrand = brand; prlView = PrlView.PRODUCTS
                                                }
                                            }
                                        }
                                        repeat(subColCount - row.size) { Box(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }

                        else -> {
                            if (displayedProducts.isEmpty()) {
                                item { Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) { Text("لا توجد منتجات", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                            } else {
                                val rows = displayedProducts.chunked(colCount)
                                items(rows, contentType = { "product-row" }) { row ->
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        row.forEach { product ->
                                            Box(Modifier.weight(1f)) {
                                                PrlProductCard(
                                                    product      = product,
                                                    categoryPath = buildProductPath(product, allCategories, allBrands),
                                                    quantity     = cart[product.id] ?: 0,
                                                    onAdd        = { cart[product.id] = (cart[product.id] ?: 0) + 1 },
                                                    onRemove     = {
                                                        val cur = cart[product.id] ?: 0
                                                        if (cur <= 1) cart.remove(product.id) else cart[product.id] = cur - 1
                                                    }
                                                )
                                            }
                                        }
                                        repeat(colCount - row.size) { Box(Modifier.weight(1f)) }
                                    }
                                }
                                if (cartCount > 0) { item { Spacer(Modifier.height(100.dp)) } }
                            }
                        }
                    }
                }
            }
        }

        // ── Sticky order bar ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = cartCount > 0,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("$cartCount منتج مختار", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                        Text("${cartTotal.formatPrice()} ج", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { cart.clear() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)) { Text("مسح", fontSize = 13.sp) }
                        Button(onClick = { showOrderDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Send, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ارسال الطلب", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
    } // end BoxWithConstraints
}

// ── Pirlanta banner slider ────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PirlantaBannerSlider() {
    val pagerState = rememberPagerState(pageCount = { 3 })
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % 3)
        }
    }
    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
        ) { page ->
            when (page) {
                0 -> PrlBannerBrand()
                1 -> PrlBannerBeauty()
                else -> PrlBannerAccessories()
            }
        }
        // Pager dots in pirlanta pink
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                val selected = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Color(0xFF9B3453) else Color(0xFFD4A0AA))
                        .size(if (selected) 20.dp else 8.dp, 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PrlBannerBrand() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(Color(0xFF8B2545), Color(0xFFBC4D70))))
            .drawBehind {
                drawCircle(color = Color(0x22FFFFFF), radius = size.height * 1.1f,
                    center = Offset(size.width + size.height * 0.2f, -size.height * 0.3f))
                drawCircle(color = Color(0x15FFFFFF), radius = size.height * 0.55f,
                    center = Offset(-size.height * 0.1f, size.height * 1.1f))
                val p = Path().apply {
                    moveTo(size.width * 0.72f, 0f); lineTo(size.width * 0.78f, 0f)
                    lineTo(size.width * 0.55f, size.height); lineTo(size.width * 0.49f, size.height); close()
                }
                drawPath(p, color = Color(0x0CFFFFFF))
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Column(modifier = Modifier.padding(start = 28.dp, end = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFD9E3).copy(alpha = 0.25f)) {
                Text("موضة وجمال", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFCCDB), fontWeight = FontWeight.Bold)
            }
            Text("بيرلانتا", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 32.sp)
            Text("تشكيلة مميزة من أدوات التجميل\nوالاكسسوارات لكل إطلالة",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), lineHeight = 19.sp)
        }
    }
}

@Composable
private fun PrlBannerBeauty() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(Color(0xFFC06080), Color(0xFFD4849A))))
            .drawBehind {
                drawCircle(color = Color(0x20FFFFFF), radius = size.height * 1.1f,
                    center = Offset(size.width + size.height * 0.2f, -size.height * 0.3f))
                drawCircle(color = Color(0x15FFFFFF), radius = size.height * 0.55f,
                    center = Offset(-size.height * 0.1f, size.height * 1.1f))
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Column(modifier = Modifier.padding(start = 28.dp, end = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFFFFF).copy(alpha = 0.2f)) {
                Text("قسم التجميل", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("أدوات\nالتجميل", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 32.sp)
            Text("أحدث أدوات المكياج والعناية\nبأسعار مناسبة وجودة عالية",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), lineHeight = 19.sp)
        }
    }
}

@Composable
private fun PrlBannerAccessories() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(listOf(Color(0xFF7D2340), Color(0xFFAA4060))))
            .drawBehind {
                drawCircle(color = Color(0x20FFFFFF), radius = size.height * 1.1f,
                    center = Offset(size.width + size.height * 0.2f, -size.height * 0.3f))
                drawCircle(color = Color(0x15FFFFFF), radius = size.height * 0.55f,
                    center = Offset(-size.height * 0.1f, size.height * 1.1f))
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Column(modifier = Modifier.padding(start = 28.dp, end = 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFFFFF).copy(alpha = 0.2f)) {
                Text("الاكسسوارات والتوك", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("اكسسوارات\nوتوك", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 32.sp)
            Text("مجموعة متنوعة من الاكسسوارات\nوالتوك الأنيقة لكل إطلالة",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), lineHeight = 19.sp)
        }
    }
}

// ── Subcategory card ──────────────────────────────────────────────────────────

@Composable
private fun PrlSubcategoryCard(name: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(110.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ── Product card ──────────────────────────────────────────────────────────────

@Composable
private fun PrlProductCard(product: Product, categoryPath: String, quantity: Int,
                            onAdd: () -> Unit, onRemove: () -> Unit) {
    val inCart = quantity > 0
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(if (inCart) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (inCart) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
            if (categoryPath.isNotBlank()) {
                Text(categoryPath, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth())
            }
            Text("${product.price.formatPrice()} ج", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            if (product.stock == 0) {
                Text("نفد المخزون", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) { Text("غير متاح") }
            } else if (quantity == 0) {
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("أضف للطلب", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(onClick = onRemove, modifier = Modifier.size(36.dp), shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    }
                    Text("$quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    FilledIconButton(onClick = onAdd, modifier = Modifier.size(36.dp), shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    }
                }
                Text("الإجمالي: ${(product.price * quantity).formatPrice()} ج",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ── Social button ─────────────────────────────────────────────────────────────

@Composable
private fun PrlSocialButton(label: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
