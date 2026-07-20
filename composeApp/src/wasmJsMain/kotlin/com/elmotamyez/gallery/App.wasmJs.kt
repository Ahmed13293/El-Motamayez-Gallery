package com.elmotamyez.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.elmotamyez.gallery.data.repository.PushTokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.elmotamyez.gallery.data.model.Order
import com.elmotamyez.gallery.data.model.OrderStatus
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.model.Receipt
import com.elmotamyez.gallery.data.model.User
import com.elmotamyez.gallery.data.model.UserRole
import com.elmotamyez.gallery.ui.screens.auth.AuthViewModel
import com.elmotamyez.gallery.ui.screens.cart.CartViewModel
import com.elmotamyez.gallery.ui.screens.orders.OrderViewModel
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.Font
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// ── WhatsApp helpers ─────────────────────────────────────────────────────────

@JsFun("(num, msg) => { window.open('https://wa.me/' + num + '?text=' + encodeURIComponent(msg), '_blank'); }")
private external fun openWhatsApp(number: String, message: String)

@JsFun("() => window.location.search + window.location.hash")
private external fun getLocationSuffix(): String

@JsFun("() => window._fcmToken || ''")
private external fun getWebFcmToken(): String

@JsFun("() => { if (window.initFcmPush) window.initFcmPush(); }")
private external fun initFcmPush(): Unit

@JsFun("() => window._isIosSafari === true")
private external fun isIosSafari(): Boolean

@JsFun("() => window._isStandalone === true")
private external fun isStandalone(): Boolean

@JsFun("() => { const v = window._pendingNavigation || ''; window._pendingNavigation = ''; return v; }")
private external fun popPendingNavigation(): String

private fun buildCartWhatsAppMsg(
    items: List<CartItem>, total: Double, paymentMethod: String
): String = buildString {
    appendLine("مرحباً، أريد طلب من مكتبة المتميز 🛒")
    appendLine()
    items.forEach { item ->
        appendLine("• ${item.product.name} × ${item.quantity} = ${item.totalPrice.formatPrice()} ج")
    }
    appendLine()
    appendLine("الإجمالي: ${total.formatPrice()} ج")
    append("طريقة الدفع: $paymentMethod")
}

// ── Date helpers (same logic as mobile ReceiptsListScreen) ───────────────────

private fun String.parseSupabaseDt() = runCatching {
    val normalized = this.replace(" ", "T").replace(Regex("\\.\\d+"), "")
        .replace(Regex("[+-]\\d{2}(:\\d{2})?$"), "Z")
        .let { if (!it.endsWith("Z")) "${it}Z" else it }
    Instant.parse(normalized).toLocalDateTime(TimeZone.currentSystemDefault())
}.getOrNull()

internal fun Receipt.dateKey(): String {
    val local = createdAt?.parseSupabaseDt()
    return if (local != null) dateString(
        local.year,
        local.monthNumber,
        local.dayOfMonth
    ) else "unknown"
}

private fun Receipt.timeLabel(): String {
    val local = createdAt?.parseSupabaseDt() ?: return ""
    return "${twoDigit(local.hour)}:${twoDigit(local.minute)}"
}

private enum class WebTab { HOME, CART, RECEIPTS, ORDERS, ADMIN }

@Composable
private fun cairoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.Cairo_Regular, weight = FontWeight.Normal),
    Font(Res.font.Cairo_Bold, weight = FontWeight.Bold),
)

private fun TextStyle.withCairo(cairo: FontFamily) = copy(fontFamily = cairo)

@Composable
private fun arabicTypography(cairo: FontFamily): Typography {
    val t = MaterialTheme.typography
    return Typography(
        displayLarge = t.displayLarge.withCairo(cairo),
        displayMedium = t.displayMedium.withCairo(cairo),
        displaySmall = t.displaySmall.withCairo(cairo),
        headlineLarge = t.headlineLarge.withCairo(cairo),
        headlineMedium = t.headlineMedium.withCairo(cairo),
        headlineSmall = t.headlineSmall.withCairo(cairo),
        titleLarge = t.titleLarge.withCairo(cairo),
        titleMedium = t.titleMedium.withCairo(cairo),
        titleSmall = t.titleSmall.withCairo(cairo),
        bodyLarge = t.bodyLarge.withCairo(cairo),
        bodyMedium = t.bodyMedium.withCairo(cairo),
        bodySmall = t.bodySmall.withCairo(cairo),
        labelLarge = t.labelLarge.withCairo(cairo),
        labelMedium = t.labelMedium.withCairo(cairo),
        labelSmall = t.labelSmall.withCairo(cairo),
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
                    var showPublicCatalog by remember {
                        mutableStateOf(getLocationSuffix().contains("catalog", ignoreCase = true))
                    }
                    when {
                        authState.user != null -> WebApp(
                            user = authState.user!!,
                            onLogout = { authVm.logout() })

                        showPublicCatalog -> PublicCatalogScreen(onLoginClick = {
                            showPublicCatalog = false
                        })

                        else -> WebLoginScreen(
                            isLoading = authState.isLoading,
                            error = authState.error,
                            onLogin = { u, p -> authVm.login(u, p) },
                            onBrowseAsGuest = { showPublicCatalog = true })
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
    val orderVm: OrderViewModel = koinInject()

    // Poll for navigation requests from service worker or URL param
    LaunchedEffect(Unit) {
        while (true) {
            val nav = popPendingNavigation()
            if (nav == "orders") currentTab = WebTab.ORDERS
            delay(500L)
        }
    }

    // Request push permission tied to the login tap (satisfies mobile Chrome's user-gesture requirement),
    // then poll every second for up to 30s until the FCM token is ready.
    LaunchedEffect(Unit) {
        initFcmPush()
        var attempts = 0
        while (attempts < 30) {
            delay(1000L)
            attempts++
            val token = getWebFcmToken()
            if (token.isNotEmpty()) {
                launch(Dispatchers.Default) { PushTokenRepository().upsertToken(token, "web") }
                break
            }
        }
    }
    val cartItems     by cartVm.cartItems.collectAsState()
    val pendingOrders by orderVm.pendingCount.collectAsState()
    val isAdmin = user.role == UserRole.ADMIN

    // Show "Add to Home Screen" banner for iOS Safari users not yet in standalone mode
    val showIosBanner = remember { isIosSafari() && !isStandalone() }
    var iosBannerDismissed by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isMobile = maxWidth < 600.dp

        Column(Modifier.fillMaxSize()) {
            // iOS Safari install hint
            if (showIosBanner && !iosBannerDismissed) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "لتفعيل الإشعارات على iOS: اضغط على مشاركة ثم «إضافة للشاشة الرئيسية»",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { iosBannerDismissed = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Top bar — compact on mobile
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                            horizontal = if (isMobile) 12.dp else 24.dp,
                            vertical = if (isMobile) 8.dp else 12.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "مكتبة المتميز",
                        style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isMobile) {
                            Text(
                                "مرحباً، ${user.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = if (isMobile) PaddingValues(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            ) else ButtonDefaults.ContentPadding
                        ) { Text("خروج", fontSize = if (isMobile) 12.sp else 14.sp) }
                    }
                }
            }

            // Desktop: tab row at top
            if (!isMobile) {
                val selectedIndex = currentTab.ordinal.coerceAtMost(if (isAdmin) 4 else 2)
                ScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 16.dp
                ) {
                    Tab(
                        selected = currentTab == WebTab.HOME,
                        onClick = { currentTab = WebTab.HOME },
                        icon = { Icon(Icons.Default.Home, null, modifier = Modifier.size(20.dp)) },
                        text = { Text("المنتجات") }
                    )
                    Tab(
                        selected = currentTab == WebTab.CART,
                        onClick = { currentTab = WebTab.CART },
                        icon = {
                            BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text("${cartItems.size}") } }) {
                                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(20.dp))
                            }
                        },
                        text = { Text("السلة") }
                    )
                    Tab(
                        selected = currentTab == WebTab.RECEIPTS,
                        onClick = { currentTab = WebTab.RECEIPTS },
                        icon = { Icon(Icons.Default.Receipt, null, modifier = Modifier.size(20.dp)) },
                        text = { Text("الفواتير") }
                    )
                    if (isAdmin) {
                        Tab(
                            selected = currentTab == WebTab.ORDERS,
                            onClick = { currentTab = WebTab.ORDERS },
                            icon = {
                                BadgedBox(badge = { if (pendingOrders > 0) Badge { Text("$pendingOrders") } }) {
                                    Icon(Icons.Default.ListAlt, null, modifier = Modifier.size(20.dp))
                                }
                            },
                            text = { Text("الطلبات") }
                        )
                        Tab(
                            selected = currentTab == WebTab.ADMIN,
                            onClick = { currentTab = WebTab.ADMIN },
                            icon = { Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(20.dp)) },
                            text = { Text("الإدارة") }
                        )
                    }
                }
            }

            // Content — fills remaining space
            Box(Modifier.weight(1f)) {
                when (currentTab) {
                    WebTab.HOME -> WebHomeTab(cartVm = cartVm, isMobile = isMobile)
                    WebTab.CART -> WebCartTab(
                        cartVm = cartVm,
                        user = user,
                        isMobile = isMobile,
                        onOrderConfirmed = { currentTab = WebTab.RECEIPTS })

                    WebTab.RECEIPTS -> WebReceiptsTab(isAdmin = isAdmin, isMobile = isMobile)
                    WebTab.ORDERS  -> if (isAdmin) WebOrdersTab(user = user)
                    WebTab.ADMIN   -> if (isAdmin) WebAdminTab(user = user, onLogout = onLogout)
                }
            }

            // Mobile: navigation bar at bottom
            if (isMobile) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == WebTab.HOME,
                        onClick  = { currentTab = WebTab.HOME },
                        icon     = { Icon(Icons.Default.Home, null) },
                        label    = { Text("المنتجات", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentTab == WebTab.CART,
                        onClick  = { currentTab = WebTab.CART },
                        icon = {
                            BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text("${cartItems.size}") } }) {
                                Icon(Icons.Default.ShoppingCart, null)
                            }
                        },
                        label = { Text("السلة", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentTab == WebTab.RECEIPTS,
                        onClick  = { currentTab = WebTab.RECEIPTS },
                        icon     = { Icon(Icons.Default.Receipt, null) },
                        label    = { Text("الفواتير", fontSize = 11.sp) }
                    )
                    if (isAdmin) {
                        NavigationBarItem(
                            selected = currentTab == WebTab.ORDERS,
                            onClick  = { currentTab = WebTab.ORDERS },
                            icon = {
                                BadgedBox(badge = { if (pendingOrders > 0) Badge { Text("$pendingOrders") } }) {
                                    Icon(Icons.Default.ListAlt, null)
                                }
                            },
                            label = { Text("الطلبات", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentTab == WebTab.ADMIN,
                            onClick  = { currentTab = WebTab.ADMIN },
                            icon     = { Icon(Icons.Default.AdminPanelSettings, null) },
                            label    = { Text("الإدارة", fontSize = 11.sp) }
                        )
                    }
                }
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
    var showOtherDialog by remember { mutableStateOf(false) }

    if (showOtherDialog) {
        OtherProductDialog(
            onDismiss = { showOtherDialog = false },
            onAddToCart = { product, qty ->
                cartVm.addWithQuantity(product, qty)
                showOtherDialog = false
            }
        )
    }

    when {
        state.isLoading -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        if (state.searchQuery.isNotEmpty()) IconButton(onClick = {
                            productsVm.search(
                                ""
                            )
                        }) { Icon(Icons.Default.Clear, null) }
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
                            label = { Text(cat.name, maxLines = 1) })
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
                            WebProductCard(
                                product = product,
                                quantity = qty,
                                isMobile = true,
                                onAdd = { cartVm.addToCart(product) },
                                onIncrease = { cartVm.increaseQuantity(product.id) },
                                onDecrease = { cartVm.decreaseQuantity(product.id) })
                        }
                        item {
                            OtherProductGridCard(onClick = { showOtherDialog = true })
                        }
                    }
                }
            }
        } else {
            // ── Desktop: sidebar + products ──────────────────────────────────
            Row(Modifier.fillMaxSize()) {
                Surface(modifier = Modifier.width(210.dp).fillMaxHeight(), tonalElevation = 1.dp) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            Text(
                                "الأقسام",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        items(state.categories) { cat ->
                            val selected = state.selectedCategoryId == cat.id
                            Surface(
                                onClick = { productsVm.selectCategory(cat.id) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    cat.name,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 10.dp
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { productsVm.search(it) },
                        placeholder = { Text("بحث عن منتج...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) IconButton(onClick = {
                                productsVm.search("")
                            }) { Icon(Icons.Default.Clear, null) }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.products.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "لا توجد منتجات",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 180.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(state.products) { product ->
                                val qty =
                                    cartItems.find { it.product.id == product.id }?.quantity ?: 0
                                WebProductCard(
                                    product = product,
                                    quantity = qty,
                                    isMobile = false,
                                    onAdd = { cartVm.addToCart(product) },
                                    onIncrease = { cartVm.increaseQuantity(product.id) },
                                    onDecrease = { cartVm.decreaseQuantity(product.id) })
                            }
                            item {
                                OtherProductGridCard(onClick = { showOtherDialog = true })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebProductCard(
    product: Product,
    quantity: Int,
    isMobile: Boolean = false,
    onAdd: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    val outOfStock = product.stock == 0
    val inCart = quantity > 0
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(if (isMobile) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isMobile) 6.dp else 8.dp)
        ) {
            Text(
                product.name,
                style = if (isMobile) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(if (isMobile) 36.dp else 44.dp)
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${product.price.fmt2f()} ج",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (outOfStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        if (outOfStock) "نفد" else "متوفر ${product.stock}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (outOfStock) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            if (!inCart) {
                Button(
                    onClick = onAdd,
                    enabled = !outOfStock,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (outOfStock) "نفد" else "+ إضافة للسلة", fontSize = 13.sp)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onDecrease() }, contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "−",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Text(
                        "$quantity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(
                                if (quantity >= product.stock) MaterialTheme.colorScheme.outline.copy(
                                    alpha = 0.3f
                                ) else MaterialTheme.colorScheme.primaryContainer
                            ).clickable(enabled = quantity < product.stock) { onIncrease() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+",
                            color = if (quantity >= product.stock) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Other product tile ────────────────────────────────────────────────────────

@Composable
private fun OtherProductGridCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEEDD)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth().heightIn(min = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF08396C).copy(alpha = 0.75f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "منتج اخر",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OtherProductDialog(
    onDismiss: () -> Unit,
    onAddToCart: (Product, Int) -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var priceText   by remember { mutableStateOf("") }
    var quantity    by remember { mutableStateOf(1) }
    var nameError   by remember { mutableStateOf(false) }
    var priceError  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("منتج اخر", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it; nameError = false },
                    label = { Text("اسم المنتج") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {{ Text("يرجى إدخال اسم المنتج") }} else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it; priceError = false },
                    label = { Text("السعر") },
                    singleLine = true,
                    isError = priceError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = if (priceError) {{ Text("يرجى إدخال سعر صحيح") }} else null,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الكمية", style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                Modifier.size(30.dp).clickable { if (quantity > 1) quantity-- },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("−", color = Color.White, fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                            }
                            Text(
                                "$quantity",
                                color = Color.White, fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 28.dp),
                                textAlign = TextAlign.Center
                            )
                            Box(
                                Modifier.size(30.dp).clickable { quantity++ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.trim().toDoubleOrNull()
                    nameError  = productName.isBlank()
                    priceError = price == null || price <= 0
                    if (!nameError && !priceError) {
                        val product = Product(
                            id         = "other_${productName.trim().hashCode()}",
                            name       = productName.trim(),
                            price      = price!!,
                            stock      = 999,
                            brandId    = "",
                            categoryId = ""
                        )
                        onAddToCart(product, quantity)
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) { Text("إضافة للسلة", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// ── Cart Tab ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebCartTab(
    cartVm: CartViewModel,
    user: User,
    isMobile: Boolean = false,
    onOrderConfirmed: () -> Unit
) {
    val receiptVm: ReceiptViewModel = koinInject()
    val cartItems by cartVm.cartItems.collectAsState()
    val isAdmin = user.role == UserRole.ADMIN
    var discount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("كاش") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var overrideDate   by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val overrideDateLabel = overrideDate?.let { (y, m, d) ->
        "${twoDigit(d)}/${twoDigit(m)}/$y"
    }

    val discountValue = discount.toDoubleOrNull() ?: 0.0
    val total = (cartVm.totalPrice - discountValue).coerceAtLeast(0.0)

    if (cartItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
                Text(
                    "السلة فارغة",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (isMobile) {
        // Mobile: stacked vertically — items list + summary card in one LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "عناصر السلة (${cartItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(cartItems) { item ->
                WebCartItemRow(
                    item = item,
                    isMobile = true,
                    onIncrease = { cartVm.increaseQuantity(item.product.id) },
                    onDecrease = { cartVm.decreaseQuantity(item.product.id) },
                    onRemove = { cartVm.removeFromCart(item.product.id) })
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "ملخص الطلب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider()
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "المجموع",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("${cartVm.totalPrice.fmt2f()} ج", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedTextField(
                            value = discount,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() || c == '.' }) discount = it
                            },
                            label = { Text("خصم") },
                            suffix = { Text("ج") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        HorizontalDivider()
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "الإجمالي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${total.fmt2f()} ج",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider()
                        Text(
                            "طريقة الدفع",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("كاش", "تحويل").forEach { method ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { paymentMethod = method },
                                    label = { Text(method) })
                            }
                        }
                        if (isAdmin) {
                            Surface(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (overrideDate != null) MaterialTheme.colorScheme.tertiaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp),
                                            tint = if (overrideDate != null) MaterialTheme.colorScheme.tertiary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("تاريخ الفاتورة:", style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(overrideDateLabel ?: "اليوم", style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (overrideDate != null) MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("تأكيد الطلب", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Button(
                            onClick = {
                                val msg = buildCartWhatsAppMsg(cartItems, total, paymentMethod)
                                openWhatsApp("201121064222", msg)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("اطلب عبر واتساب", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        OutlinedButton(
                            onClick = { cartVm.clearCart() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("مسح السلة")
                        }
                    }
                }
            }
        }
    } else {
        // Desktop: side-by-side
        Row(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text(
                        "عناصر السلة (${cartItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(cartItems) { item ->
                    WebCartItemRow(
                        item = item,
                        onIncrease = { cartVm.increaseQuantity(item.product.id) },
                        onDecrease = { cartVm.decreaseQuantity(item.product.id) },
                        onRemove = { cartVm.removeFromCart(item.product.id) })
                }
            }
            Card(
                modifier = Modifier.width(300.dp).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "ملخص الطلب",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "المجموع",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("${cartVm.totalPrice.fmt2f()} ج", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedTextField(
                        value = discount,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() || c == '.' }) discount = it
                        },
                        label = { Text("خصم") },
                        suffix = { Text("ج") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "الإجمالي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${total.fmt2f()} ج",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider()
                    Text(
                        "طريقة الدفع",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("كاش", "تحويل").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method) })
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (isAdmin) {
                        Surface(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (overrideDate != null) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp),
                                        tint = if (overrideDate != null) MaterialTheme.colorScheme.tertiary
                                               else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("تاريخ الفاتورة:", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(overrideDateLabel ?: "اليوم", style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (overrideDate != null) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("تأكيد الطلب", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            val msg = buildCartWhatsAppMsg(cartItems, total, paymentMethod)
                            openWhatsApp("201121064222", msg)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("اطلب عبر واتساب", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    OutlinedButton(
                        onClick = { cartVm.clearCart() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
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
                    receiptVm.confirmOrder(
                        items = cartItems,
                        total = total,
                        discount = discountValue,
                        paymentMethod = paymentMethod,
                        username = user.name,
                        overrideDate = overrideDate
                    )
                    cartVm.clearCart()
                    showConfirmDialog = false
                    onOrderConfirmed()
                }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("إلغاء") }
            })
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val local = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC)
                        overrideDate = Triple(local.year, local.monthNumber, local.dayOfMonth)
                    }
                    showDatePicker = false
                }) { Text("موافق") }
            },
            dismissButton = {
                Row {
                    if (overrideDate != null) {
                        TextButton(onClick = { overrideDate = null; showDatePicker = false }) {
                            Text("اليوم", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun WebCartItemRow(
    item: CartItem,
    isMobile: Boolean = false,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        if (isMobile) {
            Column(
                Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Close,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onDecrease() }, contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "−",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "${item.quantity}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            Modifier.size(28.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onIncrease() }, contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        "${item.totalPrice.fmt2f()} ج",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${item.product.price.fmt2f()} ج للقطعة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onDecrease() }, contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "−",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "${item.quantity}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        Modifier.size(28.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onIncrease() }, contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${item.totalPrice.fmt2f()} ج",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.End
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Receipts Tab — grouped by date, expand/collapse like mobile ───────────────

private fun Receipt.webMonthKey(): String {
    val local = createdAt?.let { raw ->
        runCatching {
            val normalized = raw.replace(" ", "T").replace(Regex("\\.\\d+"), "")
                .replace(Regex("[+-]\\d{2}(:\\d{2})?$"), "Z")
                .let { if (!it.endsWith("Z")) "${it}Z" else it }
            Instant.parse(normalized)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        }.getOrNull()
    } ?: return "unknown"
    return "${local.year}-${twoDigit(local.monthNumber)}"
}

private fun String.webToArabicMonth(): String {
    val parts = split("-")
    if (parts.size < 2) return this
    val year = parts[0]
    val month = parts[1].toIntOrNull() ?: return this
    val name = listOf(
        "",
        "يناير",
        "فبراير",
        "مارس",
        "أبريل",
        "مايو",
        "يونيو",
        "يوليو",
        "أغسطس",
        "سبتمبر",
        "أكتوبر",
        "نوفمبر",
        "ديسمبر"
    ).getOrElse(month) { month.toString() }
    return "$name $year"
}

@Composable
internal fun WebReceiptsTab(isAdmin: Boolean = false, isMobile: Boolean = false) {
    val receiptVm: ReceiptViewModel = koinInject()
    val receipts by receiptVm.receipts.collectAsState()
    val isLoading by receiptVm.isLoading.collectAsState()
    val allProducts by receiptVm.allProducts.collectAsState()
    val isSaving by receiptVm.isSaving.collectAsState()
    val deleteError by receiptVm.deleteError.collectAsState()
    var editingReceipt  by remember { mutableStateOf<Receipt?>(null) }
    var deletingReceipt by remember { mutableStateOf<Receipt?>(null) }

    // Current month key e.g. "2026-07"
    val currentMonthKey = remember {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        "${now.year}-${twoDigit(now.monthNumber)}"
    }

    // Distinct months sorted newest first
    val months = remember(receipts) {
        receipts.map { it.webMonthKey() }.filter { it != "unknown" }.distinct().sortedDescending()
    }

    var selectedMonthIndex by remember(months) {
        val idx = months.indexOf(currentMonthKey).takeIf { it >= 0 } ?: 0
        mutableIntStateOf(idx)
    }
    val selectedMonth = months.getOrNull(selectedMonthIndex) ?: currentMonthKey

    // Group by date filtered to selected month, newest day first
    val grouped = remember(receipts, selectedMonth) {
        receipts.filter { it.webMonthKey() == selectedMonth }.sortedByDescending { it.orderNumber }
            .groupBy { it.dateKey() }.entries.sortedByDescending { it.key }
    }

    val monthTotal = remember(grouped) { grouped.sumOf { it.value.sumOf { r -> r.total } } }
    val monthCount = remember(grouped) { grouped.sumOf { it.value.size } }

    // Newest day starts expanded
    val expandedMap = remember(grouped) {
        mutableStateMapOf<String, Boolean>().also { map ->
            grouped.forEachIndexed { i, entry -> map[entry.key] = (i == 0) }
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // ── Header row ────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(
                horizontal = if (isMobile) 12.dp else 16.dp,
                vertical = if (isMobile) 8.dp else 12.dp
            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "سجل الفواتير",
                style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = { receiptVm.loadReceipts() },
                shape = RoundedCornerShape(10.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                else Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isLoading) "جاري التحديث..." else "تحديث")
            }
        }

        // ── Month tabs ────────────────────────────────────────────────────────
        if (months.isNotEmpty()) {
            ScrollableTabRow(selectedTabIndex = selectedMonthIndex, edgePadding = 0.dp) {
                months.forEachIndexed { index, monthKey ->
                    Tab(
                        selected = index == selectedMonthIndex,
                        onClick = { selectedMonthIndex = index },
                        text = {
                            Text(
                                monthKey.webToArabicMonth(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (index == selectedMonthIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        })
                }
            }
        }

        when {
            isLoading && receipts.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            receipts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "لا توجد فواتير بعد",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "أكّد طلباً لتظهر هنا",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(if (isMobile) 8.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (isMobile) 8.dp else 10.dp)
            ) {
                // ── Monthly summary card ──────────────────────────────────────
                item(key = "month_summary") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    selectedMonth.webToArabicMonth(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    "$monthCount فاتورة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                "${monthTotal.formatPrice()} ج",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (grouped.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "لا توجد فواتير في هذا الشهر",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                grouped.forEach { (dateKey, dayReceipts) ->
                    val isOpen = expandedMap[dateKey] == true
                    val dayTotal = dayReceipts.sumOf { it.total }

                    item(key = "header_$dateKey") {
                        ReceiptDayHeader(
                            dateKey = dateKey,
                            count = dayReceipts.size,
                            dayTotal = dayTotal,
                            isExpanded = isOpen,
                            onClick = { expandedMap[dateKey] = !isOpen })
                    }

                    item(key = "body_$dateKey") {
                        AnimatedVisibility(
                            visible = isOpen,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Spacer(Modifier.height(2.dp))
                                dayReceipts.forEachIndexed { index, receipt ->
                                    WebReceiptCard(
                                        receipt = receipt,
                                        dayIndex = dayReceipts.size - index,
                                        isAdmin = isAdmin,
                                        onEdit = {
                                            receiptVm.loadProductsForEdit()
                                            receiptVm.viewReceipt(receipt)
                                            editingReceipt = receipt
                                        },
                                        onDelete = { deletingReceipt = receipt }
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    deleteError?.let { err ->
        AlertDialog(
            onDismissRequest = { receiptVm.clearDeleteError() },
            title = { Text("فشل الحذف", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(err) },
            confirmButton = { TextButton(onClick = { receiptVm.clearDeleteError() }) { Text("حسناً") } }
        )
    }

    deletingReceipt?.let { receipt ->
        AlertDialog(
            onDismissRequest = { deletingReceipt = null },
            title = { Text("حذف الفاتورة", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف الفاتورة ${receipt.id}؟\nسيتم استعادة المخزون تلقائياً.") },
            confirmButton = {
                Button(
                    onClick = {
                        receiptVm.deleteReceipt(receipt)
                        deletingReceipt = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isSaving
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { deletingReceipt = null }) { Text("إلغاء") }
            }
        )
    }

    editingReceipt?.let { receipt ->
        WebEditReceiptDialog(
            receipt = receipt,
            allProducts = allProducts,
            isSaving = isSaving,
            onDismiss = { editingReceipt = null },
            onSave = { newItems, discount, paymentMethod ->
                receiptVm.updateReceipt(newItems, discount, paymentMethod)
                editingReceipt = null
            }
        )
    }
}

@Composable
private fun ReceiptDayHeader(
    dateKey: String,
    count: Int,
    dayTotal: Double,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    // Show date as DD/MM/YYYY — same as mobile toArabicDisplayDate()
    val displayDate = try {
        val (y, m, d) = dateKey.split("-")
        "$d/$m/$y"
    } catch (_: Exception) {
        dateKey
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    displayDate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "$count فاتورة  •  ${dayTotal.formatPrice()} ج",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
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
internal fun WebReceiptCard(
    receipt: Receipt,
    dayIndex: Int,
    isAdmin: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val discount = receipt.discount
    val subtotal = receipt.total + discount

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            // ── Summary row (always visible, clickable to expand) ─────────────
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "فاتورة #$dayIndex",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!receipt.username.isNullOrBlank()) {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                receipt.username,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    val time = receipt.timeLabel()
                    val metaParts = buildList {
                        add("${receipt.items.size} منتج")
                        if (time.isNotEmpty()) add(time)
                    }.joinToString(" • ")
                    Text(
                        metaParts,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (receipt.paymentMethod.isNotEmpty() || !receipt.isPaid) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (receipt.paymentMethod.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        receipt.paymentMethod,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 1.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (!receipt.isPaid) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        "آجل",
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 1.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        receipt.total.formatPrice(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isAdmin) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "تعديل الفاتورة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف الفاتورة",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Expanded detail ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider()
                    Spacer(Modifier.height(2.dp))

                    // Customer info
                    if (!receipt.customerPhone.isNullOrBlank()) {
                        Text(
                            "رقم العميل: ${receipt.customerPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!receipt.customerInfo.isNullOrBlank()) {
                        Text(
                            "معلومات العميل: ${receipt.customerInfo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Column headers
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "المنتج",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            "الكمية",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(48.dp)
                        )
                        Text(
                            "الإجمالي",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(72.dp)
                        )
                    }
                    HorizontalDivider()

                    // Items
                    receipt.items.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.product.name,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(2f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${item.quantity}",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(48.dp)
                            )
                            Text(
                                item.totalPrice.formatPrice(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(72.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    // Discount + total
                    if (discount > 0.0) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "المجموع",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                subtotal.formatPrice(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "الخصم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "-${discount.formatPrice()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "الإجمالي",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            receipt.total.formatPrice(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
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

@Composable
private fun WebEditReceiptDialog(
    receipt: Receipt,
    allProducts: List<Product>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<CartItem>, Double, String) -> Unit
) {
    val editItems = remember(receipt.id) { mutableStateListOf<CartItem>().also { it.addAll(receipt.items) } }
    var discountText by remember(receipt.id) { mutableStateOf(receipt.discount.fmt2f()) }
    var paymentMethod by remember(receipt.id) { mutableStateOf(receipt.paymentMethod) }
    var showAddProduct by remember { mutableStateOf(false) }
    var showAddOther by remember { mutableStateOf(false) }

    val discount = discountText.toDoubleOrNull() ?: 0.0
    val newTotal = editItems.sumOf { it.totalPrice } - discount

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(editItems.toList(), discount, paymentMethod) },
                enabled = !isSaving && editItems.isNotEmpty()
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("حفظ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("تعديل الفاتورة ${receipt.id}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Items list ────────────────────────────────────────────────
                editItems.forEachIndexed { idx, item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(item.product.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        // Qty stepper
                        IconButton(onClick = {
                            if (item.quantity > 1) editItems[idx] = item.copy(quantity = item.quantity - 1)
                        }, modifier = Modifier.size(28.dp)) {
                            Text("−", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("${item.quantity}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = {
                            editItems[idx] = item.copy(quantity = item.quantity + 1)
                        }, modifier = Modifier.size(28.dp)) {
                            Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("${item.totalPrice.formatPrice()} ج", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
                        IconButton(onClick = { editItems.removeAt(idx) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // ── Add buttons ───────────────────────────────────────────────
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showAddProduct = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("+ إضافة منتج", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(onClick = { showAddOther = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("+ خدمة / أخرى", style = MaterialTheme.typography.labelSmall)
                    }
                }

                HorizontalDivider()

                // ── Payment method ────────────────────────────────────────────
                Text("طريقة الدفع", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("كاش", "تحويل").forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // ── Discount ──────────────────────────────────────────────────
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("خصم (ج)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                // ── Total preview ─────────────────────────────────────────────
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الإجمالي الجديد", fontWeight = FontWeight.Bold)
                    Text("${newTotal.formatPrice()} ج", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )

    if (showAddProduct) {
        WebAddProductDialog(
            allProducts = allProducts,
            onDismiss = { showAddProduct = false },
            onAdd = { product ->
                val existing = editItems.indexOfFirst { it.product.id == product.id }
                if (existing >= 0) editItems[existing] = editItems[existing].let { it.copy(quantity = it.quantity + 1) }
                else editItems.add(CartItem(product, 1))
                showAddProduct = false
            }
        )
    }

    if (showAddOther) {
        WebOtherProductDialog(
            onDismiss = { showAddOther = false },
            onAdd = { item ->
                editItems.add(item)
                showAddOther = false
            }
        )
    }
}

@Composable
private fun WebAddProductDialog(
    allProducts: List<Product>,
    onDismiss: () -> Unit,
    onAdd: (Product) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val filtered = remember(searchText, allProducts) {
        if (searchText.isBlank()) allProducts
        else allProducts.filter { it.name.contains(searchText, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("إضافة منتج", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("بحث...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) }
                )
                Column(
                    modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (allProducts.isEmpty()) {
                        Text("جاري تحميل المنتجات...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        filtered.forEach { product ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onAdd(product) }.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(product.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text("${product.price.formatPrice()} ج", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun WebOtherProductDialog(
    onDismiss: () -> Unit,
    onAdd: (CartItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    val ts = Clock.System.now().toEpochMilliseconds() % 10000
                    val product = Product(
                        id = "other_${name.take(8)}_$ts",
                        name = name.trim(),
                        price = price,
                        brandId = "",
                        categoryId = "",
                        stock = 0
                    )
                    onAdd(CartItem(product, 1))
                },
                enabled = name.isNotBlank() && (priceText.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("خدمة / أخرى", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("السعر (ج)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        }
    )
}

internal fun formatArabicDate(iso: String): String {
    val parts = iso.split("-")
    if (parts.size < 3) return iso
    val months = listOf(
        "يناير",
        "فبراير",
        "مارس",
        "أبريل",
        "مايو",
        "يونيو",
        "يوليو",
        "أغسطس",
        "سبتمبر",
        "أكتوبر",
        "نوفمبر",
        "ديسمبر"
    )
    val day = parts[2].trimStart('0').ifEmpty { "0" }
    val month = parts[1].trimStart('0').toIntOrNull()?.let { months.getOrNull(it - 1) } ?: parts[1]
    val year = parts[0]
    return "$day $month $year"
}

// ── Login Screen ──────────────────────────────────────────────────────────────

@Composable
private fun WebLoginScreen(
    isLoading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onBrowseAsGuest: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "مكتبة المتميز",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "تسجيل الدخول",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (error != null) Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { onLogin(username, password) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    else Text("دخول", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                OutlinedButton(
                    onClick = onBrowseAsGuest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("تصفح المنتجات كضيف", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Web Orders Tab
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun WebOrdersTab(user: User) {
    val vm: OrderViewModel = koinInject()
    val orders    by vm.orders.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val isSaving  by vm.isSaving.collectAsState()
    val products  by vm.products.collectAsState()
    val error     by vm.error.collectAsState()
    val username  = user.username

    var editingOrder  by remember { mutableStateOf<Order?>(null) }
    var deletingOrder by remember { mutableStateOf<Order?>(null) }

    // Refresh immediately when this tab becomes visible, then every 30s
    LaunchedEffect(Unit) {
        vm.loadOrders()
        while (true) {
            delay(30_000L)
            vm.loadOrders()
        }
    }

    // Show error from insert or fetch failures
    error?.let {
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            title = { Text("خطأ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text  = { Text(it) },
            confirmButton = { TextButton(onClick = { vm.clearError() }) { Text("حسناً") } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("الطلبات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { vm.loadOrders() }, enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, null)
            }
        }
        HorizontalDivider()

        if (orders.isEmpty() && !isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ListAlt, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Text("لا توجد طلبات بعد", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    WebOrderCard(
                        order     = order,
                        isSaving  = isSaving,
                        onAdvance = { vm.advanceStatus(order, username) },
                        onEdit    = { editingOrder = order },
                        onDelete  = { deletingOrder = order }
                    )
                }
            }
        }
    }

    editingOrder?.let { order ->
        WebOrderEditDialog(
            order     = order,
            isSaving  = isSaving,
            products  = products,
            onDismiss = { editingOrder = null },
            onSave    = { items, discount, depositFee, deliveryFee, payment ->
                vm.updateOrder(order, items, discount, depositFee, deliveryFee, payment)
                editingOrder = null
            }
        )
    }

    deletingOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { deletingOrder = null },
            title = { Text("حذف الطلب", fontWeight = FontWeight.Bold) },
            text  = { Text("هل أنت متأكد من حذف الطلب؟") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteOrder(order); deletingOrder = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isSaving
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deletingOrder = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun WebOrderCard(
    order: Order,
    isSaving: Boolean,
    onAdvance: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val status     = OrderStatus.fromKey(order.status)
    val nextStatus = status.next()

    Card(
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!order.customerName.isNullOrBlank())
                            Text(order.customerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        if (!order.customerPhone.isNullOrBlank())
                            Text("• ${order.customerPhone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${order.items.size} منتج  •  ${order.total.formatPrice()} ج", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!order.customerAddress.isNullOrBlank())
                        Text("العنوان: ${order.customerAddress}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!order.preparedBy.isNullOrBlank())
                        Text("تحضير: ${order.preparedBy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(shape = RoundedCornerShape(6.dp), color = webOrderStatusColor(status).copy(alpha = 0.15f)) {
                        Text(
                            status.arabicLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = webOrderStatusColor(status),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Status stepper
            WebOrderStatusStepper(currentStatus = status, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp))

            // Expanded detail
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider()
                    Spacer(Modifier.height(2.dp))

                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.product.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("× ${item.quantity}  =  ${item.totalPrice.formatPrice()} ج", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider()

                    if (order.discount > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("خصم", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            Text("- ${order.discount.formatPrice()} ج", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (order.depositFee > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("عربون", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            Text("- ${order.depositFee.formatPrice()} ج", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    if (order.deliveryFee > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("توصيل", style = MaterialTheme.typography.bodySmall)
                            Text("+ ${order.deliveryFee.formatPrice()} ج", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الإجمالي", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("${order.total.formatPrice()} ج", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(order.paymentMethod, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    if (!order.notes.isNullOrBlank()) {
                        Text("ملاحظات: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (nextStatus != null) {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick  = onAdvance,
                            enabled  = !isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp)
                        ) {
                            Text("تحويل إلى: ${nextStatus.arabicLabel}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebOrderStatusStepper(currentStatus: OrderStatus, modifier: Modifier = Modifier) {
    val steps = OrderStatus.entries
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { i, step ->
            val done      = step.ordinal <= currentStatus.ordinal
            val isCurrent = step == currentStatus
            Box(
                Modifier.size(24.dp).clip(CircleShape)
                    .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center
            ) {
                if (done && !isCurrent)
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                else
                    Text("${i + 1}", color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (i < steps.size - 1) {
                Column(Modifier.weight(1f).padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().height(2.dp)
                            .background(if (step.ordinal < currentStatus.ordinal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    )
                    Text(step.arabicLabel, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                        color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        textAlign = TextAlign.Center, maxLines = 1)
                }
            } else {
                Text(step.arabicLabel, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun webOrderStatusColor(status: OrderStatus) = when (status) {
    OrderStatus.RECEIVED   -> MaterialTheme.colorScheme.tertiary
    OrderStatus.PREPARING  -> MaterialTheme.colorScheme.secondary
    OrderStatus.DELIVERING -> MaterialTheme.colorScheme.primary
    OrderStatus.DELIVERED  -> MaterialTheme.colorScheme.outline
}

@Composable
private fun WebOrderEditDialog(
    order: Order,
    isSaving: Boolean,
    products: List<com.elmotamyez.gallery.data.model.Product>,
    onDismiss: () -> Unit,
    onSave: (List<CartItem>, Double, Double, Double, String) -> Unit
) {
    val editItems     = remember(order.id) { mutableStateListOf<CartItem>().also { it.addAll(order.items) } }
    var discountText  by remember(order.id) { mutableStateOf(if (order.discount > 0) order.discount.fmt2f() else "") }
    var depositText   by remember(order.id) { mutableStateOf(if (order.depositFee > 0) order.depositFee.fmt2f() else "") }
    var deliveryText  by remember(order.id) { mutableStateOf(if (order.deliveryFee > 0) order.deliveryFee.fmt2f() else "") }
    var paymentMethod by remember(order.id) { mutableStateOf(order.paymentMethod) }
    var showAddOther  by remember { mutableStateOf(false) }
    var showAddStock  by remember { mutableStateOf(false) }

    val discount    = discountText.toDoubleOrNull() ?: 0.0
    val depositFee  = depositText.toDoubleOrNull()  ?: 0.0
    val deliveryFee = deliveryText.toDoubleOrNull() ?: 0.0
    val newTotal    = editItems.sumOf { it.totalPrice } - discount - depositFee + deliveryFee

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick  = { onSave(editItems.toList(), discount, depositFee, deliveryFee, paymentMethod) },
                enabled  = !isSaving && editItems.isNotEmpty()
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("حفظ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("تعديل الطلب", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                editItems.forEachIndexed { idx, item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.product.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { if (item.quantity > 1) editItems[idx] = item.copy(quantity = item.quantity - 1) }, modifier = Modifier.size(28.dp)) {
                            Text("−", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("${item.quantity}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { editItems[idx] = item.copy(quantity = item.quantity + 1) }, modifier = Modifier.size(28.dp)) {
                            Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { editItems.removeAt(idx) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { showAddStock = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("+ من المخزون", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(onClick = { showAddOther = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("+ خدمة / أخرى", style = MaterialTheme.typography.labelSmall)
                    }
                }

                HorizontalDivider()

                Text("طريقة الدفع", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("كاش", "تحويل").forEach { method ->
                        FilterChip(selected = paymentMethod == method, onClick = { paymentMethod = method },
                            label = { Text(method, style = MaterialTheme.typography.labelSmall) })
                    }
                }

                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("خصم (ج)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true
                )
                OutlinedTextField(
                    value = depositText,
                    onValueChange = { depositText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("عربون (ج)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true
                )
                OutlinedTextField(
                    value = deliveryText,
                    onValueChange = { deliveryText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("رسوم التوصيل (ج)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الإجمالي الجديد", fontWeight = FontWeight.Bold)
                    Text("${newTotal.formatPrice()} ج", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )

    if (showAddOther) {
        WebOrderAddOtherDialog(
            onDismiss = { showAddOther = false },
            onAdd     = { item -> editItems.add(item); showAddOther = false }
        )
    }
    if (showAddStock) {
        WebAddStockItemDialog(
            products  = products,
            onDismiss = { showAddStock = false },
            onAdd     = { product, qty ->
                val existing = editItems.indexOfFirst { it.product.id == product.id }
                if (existing >= 0) editItems[existing] = editItems[existing].copy(quantity = editItems[existing].quantity + qty)
                else editItems.add(CartItem(product = product, quantity = qty))
                showAddStock = false
            }
        )
    }
}

@Composable
private fun WebAddStockItemDialog(
    products: List<com.elmotamyez.gallery.data.model.Product>,
    onDismiss: () -> Unit,
    onAdd: (com.elmotamyez.gallery.data.model.Product, Int) -> Unit
) {
    var search   by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<com.elmotamyez.gallery.data.model.Product?>(null) }
    var qtyText  by remember { mutableStateOf("1") }

    val filtered = products.filter { it.name.contains(search, ignoreCase = true) }

    if (selected == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
            title = { Text("إضافة من المخزون", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        label = { Text("بحث...") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Column(
                        modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filtered.isEmpty()) {
                            Text("لا توجد منتجات", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        filtered.forEach { product ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().clickable { selected = product }
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(product.name, style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${product.price.formatPrice()} ج", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        )
    } else {
        val product = selected!!
        AlertDialog(
            onDismissRequest = { selected = null },
            confirmButton = {
                Button(
                    onClick = { onAdd(product, qtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1) },
                    enabled = (qtyText.toIntOrNull() ?: 0) > 0
                ) { Text("إضافة") }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("رجوع") } },
            title = { Text(product.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("السعر: ${product.price.formatPrice()} ج  •  المخزون: ${product.stock}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = qtyText, onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                        label = { Text("الكمية") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                }
            }
        )
    }
}

@Composable
private fun WebOrderAddOtherDialog(onDismiss: () -> Unit, onAdd: (CartItem) -> Unit) {
    var name      by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick  = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    val ts    = Clock.System.now().toEpochMilliseconds() % 10000
                    val prod  = Product(id = "other_${name.take(8)}_$ts", name = name.trim(), price = price, brandId = "", categoryId = "", stock = 0)
                    onAdd(CartItem(prod, 1))
                },
                enabled  = name.isNotBlank() && (priceText.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
        title = { Text("خدمة / أخرى", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("السعر (ج)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true
                )
            }
        }
    )
}
