package com.elmotamyez.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.model.Receipt
import com.elmotamyez.gallery.data.model.User
import com.elmotamyez.gallery.data.model.UserRole
import com.elmotamyez.gallery.ui.screens.auth.AuthViewModel
import com.elmotamyez.gallery.ui.screens.cart.CartViewModel
import com.elmotamyez.gallery.ui.screens.products.ProductsViewModel
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptViewModel
import com.elmotamyez.gallery.ui.theme.AppTheme
import com.elmotamyez.gallery.util.dateString
import com.elmotamyez.gallery.util.exportReceiptToPdf
import com.elmotamyez.gallery.util.fmt2f
import com.elmotamyez.gallery.util.formatPrice
import com.elmotamyez.gallery.util.twoDigit
import elmotamyezgallery.composeapp.generated.resources.Cairo_Bold
import elmotamyezgallery.composeapp.generated.resources.Cairo_Regular
import elmotamyezgallery.composeapp.generated.resources.Res
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.Font
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// ── Date helpers (same logic as mobile ReceiptsListScreen) ───────────────────

private fun String.parseSupabaseDt() = runCatching {
    val normalized = this
        .replace(" ", "T")
        .replace(Regex("\\.\\d+"), "")
        .replace(Regex("[+-]\\d{2}(:\\d{2})?$"), "Z")
        .let { if (!it.endsWith("Z")) "${it}Z" else it }
    Instant.parse(normalized).toLocalDateTime(TimeZone.currentSystemDefault())
}.getOrNull()

internal fun Receipt.dateKey(): String {
    val local = createdAt?.parseSupabaseDt()
    return if (local != null) dateString(local.year, local.monthNumber, local.dayOfMonth) else "unknown"
}

private fun Receipt.timeLabel(): String {
    val local = createdAt?.parseSupabaseDt() ?: return ""
    return "${twoDigit(local.hour)}:${twoDigit(local.minute)}"
}

private enum class WebTab { HOME, CART, RECEIPTS, ADMIN }

@Composable
private fun cairoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.Cairo_Regular, weight = FontWeight.Normal),
    Font(Res.font.Cairo_Bold,    weight = FontWeight.Bold),
)

private fun TextStyle.withCairo(cairo: FontFamily) = copy(fontFamily = cairo)

@Composable
private fun arabicTypography(cairo: FontFamily): Typography {
    val t = MaterialTheme.typography
    return Typography(
        displayLarge   = t.displayLarge.withCairo(cairo),
        displayMedium  = t.displayMedium.withCairo(cairo),
        displaySmall   = t.displaySmall.withCairo(cairo),
        headlineLarge  = t.headlineLarge.withCairo(cairo),
        headlineMedium = t.headlineMedium.withCairo(cairo),
        headlineSmall  = t.headlineSmall.withCairo(cairo),
        titleLarge     = t.titleLarge.withCairo(cairo),
        titleMedium    = t.titleMedium.withCairo(cairo),
        titleSmall     = t.titleSmall.withCairo(cairo),
        bodyLarge      = t.bodyLarge.withCairo(cairo),
        bodyMedium     = t.bodyMedium.withCairo(cairo),
        bodySmall      = t.bodySmall.withCairo(cairo),
        labelLarge     = t.labelLarge.withCairo(cairo),
        labelMedium    = t.labelMedium.withCairo(cairo),
        labelSmall     = t.labelSmall.withCairo(cairo),
    )
}

// ── Entry Point ───────────────────────────────────────────────────────────────

@Composable
actual fun App() {
    val cairo = cairoFontFamily()
    AppTheme {
        MaterialTheme(typography = arabicTypography(cairo)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                KoinContext {
                    val authVm: AuthViewModel = koinInject()
                    val authState by authVm.uiState.collectAsState()
                    if (authState.user == null) {
                        WebLoginScreen(
                            isLoading = authState.isLoading,
                            error = authState.error,
                            onLogin = { u, p -> authVm.login(u, p) }
                        )
                    } else {
                        WebApp(user = authState.user!!, onLogout = { authVm.logout() })
                    }
                }
            }
        }
    }
}

// ── Main Shell ────────────────────────────────────────────────────────────────

@Composable
private fun WebApp(user: User, onLogout: () -> Unit) {
    var currentTab by remember { mutableStateOf(WebTab.HOME) }
    val cartVm: CartViewModel = koinInject()
    val cartItems by cartVm.cartItems.collectAsState()
    val isAdmin = user.role == UserRole.ADMIN

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isMobile = maxWidth < 600.dp

        Column(Modifier.fillMaxSize()) {
            // Top bar — compact on mobile
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = if (isMobile) 12.dp else 24.dp, vertical = if (isMobile) 8.dp else 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "مكتبة المتميز",
                        style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isMobile) {
                            Text("مرحباً، ${user.name}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = if (isMobile) PaddingValues(horizontal = 10.dp, vertical = 4.dp) else ButtonDefaults.ContentPadding
                        ) { Text("خروج", fontSize = if (isMobile) 12.sp else 14.sp) }
                    }
                }
            }

            // Tab Row — icons only on mobile to avoid overflow
            val selectedIndex = currentTab.ordinal.coerceAtMost(if (isAdmin) 3 else 2)
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = if (isMobile) 0.dp else 16.dp
            ) {
                Tab(selected = currentTab == WebTab.HOME, onClick = { currentTab = WebTab.HOME },
                    icon = { Icon(Icons.Default.Home, null, modifier = Modifier.size(20.dp)) },
                    text = if (isMobile) null else ({ Text("المنتجات") })
                )
                Tab(selected = currentTab == WebTab.CART, onClick = { currentTab = WebTab.CART },
                    icon = {
                        BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text("${cartItems.size}") } }) {
                            Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(20.dp))
                        }
                    },
                    text = if (isMobile) null else ({ Text("السلة") })
                )
                Tab(selected = currentTab == WebTab.RECEIPTS, onClick = { currentTab = WebTab.RECEIPTS },
                    icon = { Icon(Icons.Default.Receipt, null, modifier = Modifier.size(20.dp)) },
                    text = if (isMobile) null else ({ Text("الفواتير") })
                )
                if (isAdmin) {
                    Tab(selected = currentTab == WebTab.ADMIN, onClick = { currentTab = WebTab.ADMIN },
                        icon = { Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(20.dp)) },
                        text = if (isMobile) null else ({ Text("الإدارة") })
                    )
                }
            }

            // Content
            when (currentTab) {
                WebTab.HOME     -> WebHomeTab(cartVm = cartVm, isMobile = isMobile)
                WebTab.CART     -> WebCartTab(cartVm = cartVm, user = user, isMobile = isMobile, onOrderConfirmed = { currentTab = WebTab.RECEIPTS })
                WebTab.RECEIPTS -> WebReceiptsTab()
                WebTab.ADMIN    -> if (isAdmin) WebAdminTab(user = user, onLogout = onLogout)
            }
        }
    }
}

// ── Home Tab ──────────────────────────────────────────────────────────────────

@Composable
private fun WebHomeTab(cartVm: CartViewModel, isMobile: Boolean) {
    val productsVm: ProductsViewModel = koinViewModel()
    val state by productsVm.uiState.collectAsState()
    val cartItems by cartVm.cartItems.collectAsState()

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("حدث خطأ: ${state.error}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { productsVm.retry() }) { Text("إعادة المحاولة") }
            }
        }
        else -> if (isMobile) {
            // ── Mobile: categories as horizontal chips, products below ──────
            Column(Modifier.fillMaxSize()) {
                // Search bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { productsVm.search(it) },
                    placeholder = { Text("بحث عن منتج...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty())
                            IconButton(onClick = { productsVm.search("") }) { Icon(Icons.Default.Clear, null) }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                )
                // Category chips row
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.categories) { cat ->
                        val selected = state.selectedCategoryId == cat.id
                        FilterChip(
                            selected = selected,
                            onClick = { productsVm.selectCategory(cat.id) },
                            label = { Text(cat.name, maxLines = 1) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Products grid
                if (state.products.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد منتجات", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        items(state.products) { product ->
                            val qty = cartItems.find { it.product.id == product.id }?.quantity ?: 0
                            WebProductCard(product = product, quantity = qty, isMobile = true,
                                onAdd = { cartVm.addToCart(product) },
                                onIncrease = { cartVm.increaseQuantity(product.id) },
                                onDecrease = { cartVm.decreaseQuantity(product.id) })
                        }
                    }
                }
            }
        } else {
            // ── Desktop: sidebar + products ──────────────────────────────────
            Row(Modifier.fillMaxSize()) {
                Surface(modifier = Modifier.width(210.dp).fillMaxHeight(), tonalElevation = 1.dp) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item {
                            Text("الأقسام", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                        }
                        items(state.categories) { cat ->
                            val selected = state.selectedCategoryId == cat.id
                            Surface(onClick = { productsVm.selectCategory(cat.id) }, shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()) {
                                Text(cat.name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = state.searchQuery, onValueChange = { productsVm.search(it) },
                        placeholder = { Text("بحث عن منتج...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = { if (state.searchQuery.isNotEmpty()) IconButton(onClick = { productsVm.search("") }) { Icon(Icons.Default.Clear, null) } },
                        singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    if (state.products.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد منتجات", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 180.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)) {
                            items(state.products) { product ->
                                val qty = cartItems.find { it.product.id == product.id }?.quantity ?: 0
                                WebProductCard(product = product, quantity = qty, isMobile = false,
                                    onAdd = { cartVm.addToCart(product) },
                                    onIncrease = { cartVm.increaseQuantity(product.id) },
                                    onDecrease = { cartVm.decreaseQuantity(product.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebProductCard(product: Product, quantity: Int, isMobile: Boolean = false, onAdd: () -> Unit, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    val outOfStock = product.stock == 0
    val inCart = quantity > 0
    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(if (isMobile) 8.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(if (isMobile) 6.dp else 8.dp)) {
            Text(product.name, style = if (isMobile) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(if (isMobile) 36.dp else 44.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${product.price.fmt2f()} ج", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Surface(shape = RoundedCornerShape(6.dp),
                    color = if (outOfStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer) {
                    Text(if (outOfStock) "نفد" else "متوفر ${product.stock}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (outOfStock) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            if (!inCart) {
                Button(onClick = onAdd, enabled = !outOfStock, modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(0.dp)) {
                    Text(if (outOfStock) "نفد" else "+ إضافة للسلة", fontSize = 13.sp)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable { onDecrease() },
                        contentAlignment = Alignment.Center) {
                        Text("−", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("$quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(32.dp).clip(CircleShape)
                            .background(if (quantity >= product.stock) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer)
                            .clickable(enabled = quantity < product.stock) { onIncrease() },
                        contentAlignment = Alignment.Center) {
                        Text("+", color = if (quantity >= product.stock) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

// ── Cart Tab ──────────────────────────────────────────────────────────────────

@Composable
private fun WebCartTab(cartVm: CartViewModel, user: User, isMobile: Boolean = false, onOrderConfirmed: () -> Unit) {
    val receiptVm: ReceiptViewModel = koinInject()
    val cartItems by cartVm.cartItems.collectAsState()
    var discount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("كاش") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val discountValue = discount.toDoubleOrNull() ?: 0.0
    val total = (cartVm.totalPrice - discountValue).coerceAtLeast(0.0)

    if (cartItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                Text("السلة فارغة", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    if (isMobile) {
        // Mobile: stacked vertically — items list + summary card in one LazyColumn
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("عناصر السلة (${cartItems.size})", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            }
            items(cartItems) { item ->
                WebCartItemRow(item = item, isMobile = true,
                    onIncrease = { cartVm.increaseQuantity(item.product.id) },
                    onDecrease = { cartVm.decreaseQuantity(item.product.id) },
                    onRemove   = { cartVm.removeFromCart(item.product.id) })
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ملخص الطلب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المجموع", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${cartVm.totalPrice.fmt2f()} ج", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedTextField(value = discount, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) discount = it },
                            label = { Text("خصم") }, suffix = { Text("ج") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("الإجمالي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${total.fmt2f()} ج", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()
                        Text("طريقة الدفع", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("كاش", "شبكة", "آجل").forEach { method ->
                                FilterChip(selected = paymentMethod == method, onClick = { paymentMethod = method }, label = { Text(method) })
                            }
                        }
                        Button(onClick = { showConfirmDialog = true }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp)) {
                            Text("تأكيد الطلب", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        OutlinedButton(onClick = { cartVm.clearCart() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text("مسح السلة")
                        }
                    }
                }
            }
        }
    } else {
        // Desktop: side-by-side
        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Text("عناصر السلة (${cartItems.size})", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                }
                items(cartItems) { item ->
                    WebCartItemRow(item = item,
                        onIncrease = { cartVm.increaseQuantity(item.product.id) },
                        onDecrease = { cartVm.decreaseQuantity(item.product.id) },
                        onRemove   = { cartVm.removeFromCart(item.product.id) })
                }
            }
            Card(modifier = Modifier.width(300.dp).fillMaxHeight(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("ملخص الطلب", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المجموع", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${cartVm.totalPrice.fmt2f()} ج", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedTextField(value = discount, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) discount = it },
                        label = { Text("خصم") }, suffix = { Text("ج") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("الإجمالي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${total.fmt2f()} ج", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider()
                    Text("طريقة الدفع", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("كاش", "شبكة", "آجل").forEach { method ->
                            FilterChip(selected = paymentMethod == method, onClick = { paymentMethod = method }, label = { Text(method) })
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { showConfirmDialog = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                        Text("تأكيد الطلب", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    OutlinedButton(onClick = { cartVm.clearCart() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Text("مسح السلة")
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("تأكيد الطلب", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("الإجمالي: ${total.fmt2f()} جنيه")
                    if (discountValue > 0) Text("الخصم: ${discountValue.fmt2f()} جنيه")
                    Text("طريقة الدفع: $paymentMethod")
                    Text("عدد المنتجات: ${cartItems.sumOf { it.quantity }} قطعة")
                }
            },
            confirmButton = {
                Button(onClick = {
                    receiptVm.confirmOrder(items = cartItems, total = total, discount = discountValue,
                        paymentMethod = paymentMethod, username = user.name)
                    cartVm.clearCart()
                    showConfirmDialog = false
                    onOrderConfirmed()
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun WebCartItemRow(item: CartItem, isMobile: Boolean = false, onIncrease: () -> Unit, onDecrease: () -> Unit, onRemove: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        if (isMobile) {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(item.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable { onDecrease() }, contentAlignment = Alignment.Center) {
                            Text("−", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Text("${item.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                        Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable { onIncrease() }, contentAlignment = Alignment.Center) {
                            Text("+", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("${item.totalPrice.fmt2f()} ج", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${item.product.price.fmt2f()} ج للقطعة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable { onDecrease() }, contentAlignment = Alignment.Center) {
                        Text("−", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text("${item.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable { onIncrease() }, contentAlignment = Alignment.Center) {
                        Text("+", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text("${item.totalPrice.fmt2f()} ج", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Receipts Tab — grouped by date, expand/collapse like mobile ───────────────

@Composable
internal fun WebReceiptsTab() {
    val receiptVm: ReceiptViewModel = koinInject()
    val receipts  by receiptVm.receipts.collectAsState()
    val isLoading by receiptVm.isLoading.collectAsState()

    // Same grouping logic as mobile ReceiptsListScreen
    val grouped = remember(receipts) {
        receipts
            .sortedByDescending { it.orderNumber }
            .groupBy { it.dateKey() }
            .entries
            .sortedByDescending { it.key }
    }

    // Newest day starts expanded — same as mobile
    val expandedMap = remember(grouped) {
        mutableStateMapOf<String, Boolean>().also { map ->
            grouped.forEachIndexed { i, entry -> map[entry.key] = (i == 0) }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header row with refresh
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("سجل الفواتير", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = { receiptVm.loadReceipts() },
                shape = RoundedCornerShape(10.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(if (isLoading) "جاري التحديث..." else "تحديث")
            }
        }

        when {
            isLoading && receipts.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            receipts.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("لا توجد فواتير بعد", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("أكّد طلباً لتظهر هنا", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                grouped.forEach { (dateKey, dayReceipts) ->
                    val isOpen   = expandedMap[dateKey] == true
                    val dayTotal = dayReceipts.sumOf { it.total }

                    // Collapsible day header
                    item(key = "header_$dateKey") {
                        ReceiptDayHeader(
                            dateKey    = dateKey,
                            count      = dayReceipts.size,
                            dayTotal   = dayTotal,
                            isExpanded = isOpen,
                            onClick    = { expandedMap[dateKey] = !isOpen }
                        )
                    }

                    // Animated receipts list — dayIndex matches mobile (#1, #2… within the day)
                    item(key = "body_$dateKey") {
                        AnimatedVisibility(
                            visible = isOpen,
                            enter   = expandVertically(),
                            exit    = shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Spacer(Modifier.height(2.dp))
                                dayReceipts.forEachIndexed { index, receipt ->
                                    WebReceiptCard(receipt = receipt, dayIndex = index + 1)
                                }
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptDayHeader(dateKey: String, count: Int, dayTotal: Double, isExpanded: Boolean, onClick: () -> Unit) {
    // Show date as DD/MM/YYYY — same as mobile toArabicDisplayDate()
    val displayDate = try {
        val (y, m, d) = dateKey.split("-")
        "$d/$m/$y"
    } catch (_: Exception) { dateKey }

    Surface(
        modifier       = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape          = RoundedCornerShape(14.dp),
        color          = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier  = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(displayDate, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("$count فاتورة  •  ${dayTotal.formatPrice()} ج",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// dayIndex = 1-based position within the day — same display as mobile "فاتورة #$dayIndex"
@Composable
internal fun WebReceiptCard(receipt: Receipt, dayIndex: Int) {
    var expanded by remember { mutableStateOf(false) }
    val discount = receipt.discount
    val subtotal = receipt.total + discount

    Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            // ── Summary row (always visible, clickable to expand) ─────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("فاتورة #$dayIndex", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (!receipt.username.isNullOrBlank()) {
                            Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(receipt.username, style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    val time = receipt.timeLabel()
                    val metaParts = buildList {
                        add("${receipt.items.size} منتج")
                        if (time.isNotEmpty()) add(time)
                    }.joinToString(" • ")
                    Text(metaParts, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (receipt.paymentMethod.isNotEmpty() || !receipt.isPaid) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (receipt.paymentMethod.isNotEmpty()) {
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(receipt.paymentMethod, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (!receipt.isPaid) {
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                    Text("آجل", modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(receipt.total.formatPrice(), style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Expanded detail ───────────────────────────────────────────────
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider()
                    Spacer(Modifier.height(2.dp))

                    // Customer info
                    if (!receipt.customerPhone.isNullOrBlank()) {
                        Text("رقم العميل: ${receipt.customerPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!receipt.customerInfo.isNullOrBlank()) {
                        Text("معلومات العميل: ${receipt.customerInfo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Column headers
                    Row(Modifier.fillMaxWidth()) {
                        Text("المنتج", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2f))
                        Text("الكمية", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
                        Text("الإجمالي", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.End, modifier = Modifier.width(72.dp))
                    }
                    HorizontalDivider()

                    // Items
                    receipt.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.product.name, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(2f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${item.quantity}", style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
                            Text(item.totalPrice.formatPrice(), style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
                                modifier = Modifier.width(72.dp))
                        }
                    }

                    HorizontalDivider()

                    // Discount + total
                    if (discount > 0.0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المجموع", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(subtotal.formatPrice(), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الخصم", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                            Text("-${discount.formatPrice()}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الإجمالي", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(receipt.total.formatPrice(), style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(Modifier.height(4.dp))

                    // PDF export button
                    OutlinedButton(
                        onClick = { exportReceiptToPdf(receipt, "${receipt.id}.pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("طباعة / تصدير PDF")
                    }
                }
            }
        }
    }
}

internal fun formatArabicDate(iso: String): String {
    val parts = iso.split("-")
    if (parts.size < 3) return iso
    val months = listOf("يناير","فبراير","مارس","أبريل","مايو","يونيو","يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر")
    val day   = parts[2].trimStart('0').ifEmpty { "0" }
    val month = parts[1].trimStart('0').toIntOrNull()?.let { months.getOrNull(it - 1) } ?: parts[1]
    val year  = parts[0]
    return "$day $month $year"
}

// ── Login Screen ──────────────────────────────────────────────────────────────

@Composable
private fun WebLoginScreen(isLoading: Boolean, error: String?, onLogin: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(0.92f), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("مكتبة المتميز", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("تسجيل الدخول", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة المرور") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
                Button(onClick = { onLogin(username, password) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), enabled = !isLoading) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Text("دخول", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
