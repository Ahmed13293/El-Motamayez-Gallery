package com.elmotamyez.gallery.ui.screens.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.util.buildBrandPath
import com.elmotamyez.gallery.util.buildProductPath
import com.elmotamyez.gallery.util.formatPrice
import org.koin.compose.viewmodel.koinViewModel

class ManageProductsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: AdminViewModel = koinViewModel()
        val state by vm.state.collectAsState()

        var showDialog    by remember { mutableStateOf(false) }
        var editTarget    by remember { mutableStateOf<Product?>(null) }
        var nameField     by remember { mutableStateOf("") }
        var priceField    by remember { mutableStateOf("") }
        var stockField         by remember { mutableStateOf("") }
        var stockError         by remember { mutableStateOf(false) }
        var nameError          by remember { mutableStateOf(false) }
        var lastAddedName      by remember { mutableStateOf("") }
        var wholesalePriceField by remember { mutableStateOf("") }
        var selectedCatId by remember { mutableStateOf("") }
        var selectedBrandId by remember { mutableStateOf("") }
        var catExpanded   by remember { mutableStateOf(false) }
        var brandExpanded by remember { mutableStateOf(false) }
        var deleteTarget  by remember { mutableStateOf<Product?>(null) }
        var searchQuery   by remember { mutableStateOf("") }
        var stockFilter   by remember { mutableStateOf("all") } // "all" | "0" | "1" | "2"

        val snackbarHost = remember { SnackbarHostState() }
        LaunchedEffect(state.toast) {
            state.toast?.let { snackbarHost.showSnackbar(it); vm.clearToast() }
        }
        LaunchedEffect(state.error) {
            state.error?.let { snackbarHost.showSnackbar("خطأ: $it"); vm.clearError() }
        }

        // Brands filtered by selected category (all levels)
        val brandsForCat = state.brands.filter { it.categoryId == selectedCatId }

        val filteredProducts = remember(searchQuery, stockFilter, state.products) {
            state.products
                .filter { if (searchQuery.isBlank()) true else it.name.contains(searchQuery, ignoreCase = true) }
                .filter {
                    when (stockFilter) {
                        "0"  -> it.stock == 0
                        "1"  -> it.stock <= 1
                        "2"  -> it.stock <= 2
                        else -> true
                    }
                }
        }

        fun openAdd() {
            editTarget = null; nameField = ""; priceField = ""; stockField = ""
            wholesalePriceField = ""; stockError = false; nameError = false
            selectedCatId = state.categories.firstOrNull()?.id ?: ""
            selectedBrandId = state.brands.firstOrNull { it.categoryId == selectedCatId }?.id ?: ""
            showDialog = true
        }

        fun openEdit(p: Product) {
            editTarget = p; nameField = p.name; priceField = p.price.toString()
            stockField = p.stock.toString(); stockError = false; nameError = false
            wholesalePriceField = p.wholesalePrice?.toString() ?: ""
            selectedCatId = p.categoryId; selectedBrandId = p.brandId
            showDialog = true
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            topBar = {
                Surface(color = Color.White, shadowElevation = 2.dp) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                            }
                            Text("إدارة المنتجات", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث عن منتج…") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "all" to "الكل",
                                "0"   to "نفد المخزون",
                                "1"   to "مخزون ≤ 1",
                                "2"   to "مخزون ≤ 2"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = stockFilter == key,
                                    onClick  = { stockFilter = key },
                                    label    = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (key == "0")
                                            MaterialTheme.colorScheme.errorContainer
                                        else
                                            MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = ::openAdd,
                    containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            }
        ) { padding ->
            when {
                state.isLoading -> Box(Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center) { CircularProgressIndicator() }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("${filteredProducts.size} منتج",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                    items(filteredProducts, key = { it.id }) { product ->
                        val path = buildProductPath(product, state.categories, state.brands)
                        Card(modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(1.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(product.name, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text(product.price.formatPrice(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold)
                                        if (product.wholesalePrice != null) {
                                            Text("ج: ${product.wholesalePrice.formatPrice()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                fontWeight = FontWeight.SemiBold)
                                        }
                                        Text("• مخزون: ${product.stock}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (product.stock <= 2) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                product.stock == 0 -> MaterialTheme.colorScheme.error
                                                product.stock <= 2 -> Color(0xFFE65100)
                                                else -> MaterialTheme.colorScheme.outline
                                            })
                                    }
                                    Text(path,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                }
                                IconButton(onClick = { openEdit(product) }) {
                                    Icon(Icons.Default.Edit, null,
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { deleteTarget = product }) {
                                    Icon(Icons.Default.Delete, null,
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Add / Edit dialog ─────────────────────────────────────────────────
        if (showDialog) {
            val selectedCatName   = state.categories.find { it.id == selectedCatId }?.name ?: ""
            val selectedBrandName = state.brands.find { it.id == selectedBrandId }?.name ?: ""

            AlertDialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
                title = { Text(if (editTarget == null) "إضافة منتج جديد" else "تعديل المنتج") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Name
                        OutlinedTextField(
                            value = nameField,
                            onValueChange = { nameField = it; nameError = false },
                            label = { Text("اسم المنتج") },
                            singleLine = true,
                            isError = nameError,
                            supportingText = if (nameError) {{ Text("يوجد منتج بهذا الاسم مسبقاً") }} else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (lastAddedName.isNotBlank() && editTarget == null) {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("اقتراح:", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline)
                                SuggestionChip(
                                    onClick = { nameField = lastAddedName; nameError = false },
                                    label = { Text(lastAddedName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1) }
                                )
                            }
                        }
                        // Price
                        OutlinedTextField(
                            value = priceField, onValueChange = { priceField = it },
                            label = { Text("سعر القطاعي") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Wholesale price
                        OutlinedTextField(
                            value = wholesalePriceField, onValueChange = { wholesalePriceField = it },
                            label = { Text("سعر الجملة (اختياري)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Stock
                        OutlinedTextField(
                            value = stockField,
                            onValueChange = { stockField = it; stockError = false },
                            label = { Text("المخزون") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            isError = stockError,
                            supportingText = if (stockError) {{ Text("يجب إدخال كمية أكبر من صفر") }} else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Category
                        ExposedDropdownMenuBox(
                            expanded = catExpanded,
                            onExpandedChange = { catExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCatName, onValueChange = {}, readOnly = true,
                                label = { Text("القسم") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = catExpanded,
                                onDismissRequest = { catExpanded = false }) {
                                state.categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCatId = cat.id
                                            selectedBrandId = state.brands
                                                .firstOrNull { it.categoryId == cat.id }?.id ?: ""
                                            catExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        // Brand
                        ExposedDropdownMenuBox(
                            expanded = brandExpanded,
                            onExpandedChange = { brandExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedBrandName, onValueChange = {}, readOnly = true,
                                label = { Text("الفئة الفرعية") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(brandExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = brandExpanded,
                                onDismissRequest = { brandExpanded = false }) {
                                brandsForCat.forEach { brand ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(brand.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    buildBrandPath(brand, state.categories, state.brands),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        },
                                        onClick = { selectedBrandId = brand.id; brandExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val price = priceField.toDoubleOrNull()
                        val wholesalePrice = wholesalePriceField.trim().toDoubleOrNull()
                        val stock = stockField.toIntOrNull() ?: 0
                        if (stock < 1) { stockError = true; return@Button }
                        val trimmedName = nameField.trim()
                        val isDuplicate = state.products.any {
                            it.name.trim().equals(trimmedName, ignoreCase = true) &&
                            it.id != editTarget?.id
                        }
                        if (isDuplicate) { nameError = true; return@Button }
                        if (trimmedName.isNotBlank() && price != null &&
                            selectedCatId.isNotBlank() && selectedBrandId.isNotBlank()) {
                            if (editTarget == null) {
                                vm.addProduct(trimmedName, price, wholesalePrice, stock, selectedBrandId, selectedCatId)
                                lastAddedName = trimmedName
                            } else {
                                vm.editProduct(editTarget!!.id, trimmedName, price, wholesalePrice, stock, selectedBrandId, selectedCatId)
                            }
                            showDialog = false
                        }
                    }) { Text("حفظ") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("إلغاء") }
                }
            )
        }

        // ── Delete confirm ────────────────────────────────────────────────────
        deleteTarget?.let { product ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("حذف المنتج") },
                text  = { Text("هل تريد حذف \"${product.name}\"؟") },
                confirmButton = {
                    Button(
                        onClick = { vm.deleteProduct(product.id); deleteTarget = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("حذف", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("إلغاء") }
                }
            )
        }
    }
}
