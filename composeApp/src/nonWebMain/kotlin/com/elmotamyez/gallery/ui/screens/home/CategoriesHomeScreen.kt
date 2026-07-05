package com.elmotamyez.gallery.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Category
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.ui.components.CartBottomBar
import com.elmotamyez.gallery.ui.components.PrintingButton
import com.elmotamyez.gallery.ui.components.StockBadge
import com.elmotamyez.gallery.util.buildProductPath
import com.elmotamyez.gallery.ui.screens.cart.CartViewModel
import com.elmotamyez.gallery.ui.screens.main.CartTab
import com.elmotamyez.gallery.ui.screens.products.CategoryProductsScreen
import com.elmotamyez.gallery.ui.screens.products.ProductsViewModel
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptViewModel
import com.elmotamyez.gallery.util.formatPrice
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// Light pastel palette — pairs with the white/light-blue-gray theme
private val categoryPalette = listOf(
    Color(0xFFD0E4F7), // light navy tint    (primary container)
    Color(0xFFDCEEFA), // light action blue   (secondary container)
    Color(0xFFE8F2FC), // pale sky blue
    Color(0xFFDFF0F0), // pale teal
    Color(0xFFE8EEFF), // lavender blue
    Color(0xFFEAF4E8), // pale mint green
    Color(0xFFFFF0E6), // pale peach
    Color(0xFFF0E8FF), // pale lilac
    Color(0xFFFFEEDD), // pale amber
    Color(0xFFE0F4F8), // light steel blue
)

class CategoriesHomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current
        val vm: ProductsViewModel = koinViewModel()
        val cartVm: CartViewModel = koinInject()
        val receiptVm: ReceiptViewModel = koinInject()
        val keyboard = LocalSoftwareKeyboardController.current

        val state by vm.uiState.collectAsState()
        val cartItems by cartVm.cartItems.collectAsState()
        val receipts by receiptVm.receipts.collectAsState()
        val stockVersion by receiptVm.stockVersion.collectAsState()

        // Refresh products whenever stock is updated after a confirmed order
        LaunchedEffect(stockVersion) {
            if (stockVersion > 0) vm.refreshProducts()
        }

        // Global search query (local — doesn't affect category filter)
        var searchQuery by remember { mutableStateOf("") }

        // Compute best-sellers from receipt history
        val bestSellers: List<Product> = remember(receipts, state.allProducts) {
            if (receipts.isEmpty() || state.allProducts.isEmpty()) return@remember emptyList()
            val salesMap = receipts
                .flatMap { it.items }
                .groupBy { it.product.id }
                .mapValues { (_, items) -> items.sumOf { it.quantity } }
            state.allProducts
                .filter { (salesMap[it.id] ?: 0) > 0 }
                .sortedByDescending { salesMap[it.id] ?: 0 }
                .take(12)
        }

        // Search results across all products
        val searchResults: List<Product> = remember(searchQuery, state.allProducts) {
            if (searchQuery.isBlank()) emptyList()
            else state.allProducts.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        Scaffold(
            topBar = {
                Surface(color = Color.White, shadowElevation = 2.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "مكتبة المتميز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث عن أي منتج…") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = ""; keyboard?.hide() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            bottomBar = {
                CartBottomBar(
                    totalPrice = cartVm.totalPrice,
                    itemCount = cartItems.sumOf { it.quantity },
                    onNavigateToCart = { tabNavigator.current = CartTab }
                )
            }
        ) { padding ->
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.error != null -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = vm::retry) { Text("إعادة المحاولة") }
                    }
                }

                // ── Search results view ──────────────────────────────────
                searchQuery.isNotBlank() -> LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (searchResults.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "لا توجد نتائج لـ \"$searchQuery\"",
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        item {
                            Text(
                                "${searchResults.size} نتيجة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                        items(searchResults.chunked(2)) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { product ->
                                    val cartItem = cartItems.find { it.product.id == product.id }
                                    Box(modifier = Modifier.weight(1f)) {
                                        com.elmotamyez.gallery.ui.components.ProductCard(
                                            product = product,
                                            isInCart = cartItem != null,
                                            quantity = cartItem?.quantity ?: 0,
                                            onAddToCart = { cartVm.addToCart(product) },
                                            onIncrease = { cartVm.increaseQuantity(product.id) },
                                            onDecrease = { cartVm.decreaseQuantity(product.id) },
                                            categoryPath = buildProductPath(product, state.categories, state.brands)
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // ── Normal home view ─────────────────────────────────────────
                else -> LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // ── Top Categories ───────────────────────────────────────
                    item {
                        SectionHeader(
                            title = "الأقسام",
                            trailing = {
                                PrintingButton(
                                    onAddToCart = { cartVm.addToCart(it) },
                                    squareMode = true
                                )
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                        CategoryGrid(
                            categories = state.categories,
                            onCategoryClick = { category ->
                                navigator.push(CategoryProductsScreen(category.id, category.name))
                            },
                            onAddOtherToCart = { cartVm.addToCart(it) }
                        )
                    }

                    // ── Most Selling ─────────────────────────────────────────
                    if (bestSellers.isNotEmpty()) {
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            SectionHeader(title = "الأكثر مبيعاً")
                            Spacer(Modifier.height(8.dp))
                            BestSellersPager(
                                products = bestSellers,
                                cartItems = cartItems,
                                categories = state.categories,
                                brands = state.brands,
                                onAddToCart = { cartVm.addToCart(it) },
                                onIncrease = { cartVm.increaseQuantity(it.id) },
                                onDecrease = { cartVm.decreaseQuantity(it.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        trailing?.invoke()
    }
}

// ── Best Sellers Pager (2 cards per page, dot indicators) ────────────────────

@Composable
private fun BestSellersPager(
    products: List<Product>,
    cartItems: List<CartItem>,
    categories: List<com.elmotamyez.gallery.data.model.Category>,
    brands: List<com.elmotamyez.gallery.data.model.Brand>,
    onAddToCart: (Product) -> Unit,
    onIncrease: (Product) -> Unit,
    onDecrease: (Product) -> Unit
) {
    // Chunk into pages of 2
    val pages = products.chunked(2)
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pages[pageIndex].forEach { product ->
                    val cartItem = cartItems.find { it.product.id == product.id }
                    Box(modifier = Modifier.weight(1f)) {
                        BestSellerCard(
                            product = product,
                            quantity = cartItem?.quantity ?: 0,
                            categoryPath = buildProductPath(product, categories, brands),
                            onAddToCart = { onAddToCart(product) },
                            onIncrease = { onIncrease(product) },
                            onDecrease = { onDecrease(product) }
                        )
                    }
                }
                // Fill empty slot if odd number on last page
                if (pages[pageIndex].size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Dot indicators
        if (pages.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ── Category grid (3 columns, no nested scrolling) ────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryGrid(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    onAddOtherToCart: (Product) -> Unit
) {
    var showOtherSheet by remember { mutableStateOf(false) }

    // Append a sentinel null to represent the "منتج اخر" tile
    val items: List<Category?> = categories + listOf(null)

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        items.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { category ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (category != null) {
                            val globalIndex = categories.indexOf(category)
                            CategoryTile(
                                category = category,
                                color = categoryPalette[globalIndex % categoryPalette.size],
                                onClick = { onCategoryClick(category) }
                            )
                        } else {
                            OtherProductTile(onClick = { showOtherSheet = true })
                        }
                    }
                }
                repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }

    if (showOtherSheet) {
        OtherProductSheet(
            onDismiss = { showOtherSheet = false },
            onAddToCart = { product ->
                onAddOtherToCart(product)
                showOtherSheet = false
            }
        )
    }
}

@Composable
private fun CategoryTile(category: Category, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(12.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.name.take(1),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF08396C).copy(alpha = 0.75f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

// ── "منتج اخر" tile ───────────────────────────────────────────────────────────

@Composable
private fun OtherProductTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFEEDD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF08396C).copy(alpha = 0.75f),
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "منتج اخر",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            lineHeight = 13.sp
        )
    }
}

// ── "منتج اخر" bottom sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtherProductSheet(
    onDismiss: () -> Unit,
    onAddToCart: (Product) -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var priceText   by remember { mutableStateOf("") }
    var quantity    by remember { mutableStateOf(1) }
    var nameError   by remember { mutableStateOf(false) }
    var priceError  by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { false }
    )

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "منتج اخر",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Product name
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it; nameError = false },
                label = { Text("اسم المنتج") },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) {{ Text("يرجى إدخال اسم المنتج") }} else null,
                modifier = Modifier.fillMaxWidth()
            )

            // Price
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it; priceError = false },
                label = { Text("السعر") },
                singleLine = true,
                isError = priceError,
                supportingText = if (priceError) {{ Text("يرجى إدخال سعر صحيح") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Quantity stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الكمية", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
                            modifier = Modifier.size(30.dp).clickable { if (quantity > 1) quantity-- },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                        }
                        Text(
                            "$quantity",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 28.dp),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier.size(30.dp).clickable { quantity++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Add to cart button
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
                        repeat(quantity) { onAddToCart(product) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("إضافة للسلة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Best-seller card (pager) ──────────────────────────────────────────────────

@Composable
private fun BestSellerCard(
    product: Product,
    quantity: Int,
    categoryPath: String = "",
    onAddToCart: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                spotColor   = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 10.dp, end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    product.price.formatPrice(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                StockBadge(stock = product.stock)
                if (categoryPath.isNotBlank()) {
                    Text(
                        categoryPath,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (quantity == 0) {
                    val outOfStock = product.stock == 0
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (outOfStock) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.primary
                            )
                            .clickable(enabled = !outOfStock) { onAddToCart() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp).clickable { onDecrease() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("−", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                            }
                            Text("$quantity", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 20.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            val atStockLimit = quantity >= product.stock
                            Box(
                                modifier = Modifier.size(24.dp).clickable(enabled = !atStockLimit) { onIncrease() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, null,
                                    tint = if (atStockLimit) Color.White.copy(alpha = 0.35f) else Color.White,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
