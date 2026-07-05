package com.elmotamyez.gallery.ui.screens.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.elmotamyez.gallery.ui.components.CartBottomBar
import com.elmotamyez.gallery.ui.components.GradientDivider
import com.elmotamyez.gallery.ui.components.ProductCard
import com.elmotamyez.gallery.util.buildProductPath
import com.elmotamyez.gallery.ui.screens.cart.CartViewModel
import com.elmotamyez.gallery.ui.screens.main.CartTab
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

data class CategoryProductsScreen(
    val categoryId: String,
    val categoryName: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current
        val vm: ProductsViewModel = koinViewModel()
        val cartVm: CartViewModel = koinInject()
        val state by vm.uiState.collectAsState()
        val cartItems by cartVm.cartItems.collectAsState()
        val keyboard = LocalSoftwareKeyboardController.current

        // Select this category on first composition
        LaunchedEffect(categoryId) {
            vm.selectCategory(categoryId)
        }

        // Level-2: top-level sub-categories (no parent)
        val brandsForCat = state.brands.filter { it.categoryId == categoryId && it.parentId == null }
        // Level-3: sub-sub-categories of the selected brand
        val subBrandsForSelected = if (state.selectedBrandId != null)
            state.brands.filter { it.parentId == state.selectedBrandId }
        else emptyList()

        Scaffold(
            topBar = {
                Column {
                    // ── App bar ──────────────────────────────────────────────
                    Surface(color = Color.White, shadowElevation = 0.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.Black
                                )
                            }
                            Text(
                                categoryName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    // ── Search bar ───────────────────────────────────────────
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { vm.search(it) },
                        placeholder = { Text("Search in $categoryName…") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { vm.search(""); keyboard?.hide() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    // ── Brand filter chips ───────────────────────────────────
                    if (brandsForCat.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedBrandId == null,
                                    onClick = { vm.selectBrand(null) },
                                    label = { Text("الكل") }
                                )
                            }
                            items(brandsForCat) { brand ->
                                FilterChip(
                                    selected = brand.id == state.selectedBrandId,
                                    onClick = { vm.selectBrand(brand.id) },
                                    label = { Text(brand.name) }
                                )
                            }
                        }
                    }

                    // ── Sub-sub-category chips (Level 3) ─────────────────────
                    if (subBrandsForSelected.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedSubBrandId == null,
                                    onClick = { vm.selectSubBrand(null) },
                                    label = { Text("الكل") }
                                )
                            }
                            items(subBrandsForSelected) { sub ->
                                FilterChip(
                                    selected = sub.id == state.selectedSubBrandId,
                                    onClick = { vm.selectSubBrand(sub.id) },
                                    label = { Text(sub.name) }
                                )
                            }
                        }
                    }

                    // ── Result count ─────────────────────────────────────────
                    if (!state.isLoading && state.error == null) {
                        Text(
                            "${state.products.size} منتج",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        GradientDivider(modifier = Modifier.padding(top = 4.dp))
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

                state.products.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (state.searchQuery.isNotEmpty()) "No results for \"${state.searchQuery}\""
                        else "لا توجد منتجات",
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.products, key = { it.id }) { product ->
                        val cartItem = cartItems.find { it.product.id == product.id }
                        ProductCard(
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
            }
        }
    }
}
