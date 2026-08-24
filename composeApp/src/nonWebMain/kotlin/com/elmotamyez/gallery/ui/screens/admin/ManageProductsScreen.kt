package com.elmotamyez.gallery.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.elmotamyez.gallery.util.rememberImagePickerLauncher
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
        var imageUrlsList by remember { mutableStateOf<List<String>>(emptyList()) }
        var addUrlField   by remember { mutableStateOf("") }
        var selectedCatId by remember { mutableStateOf("") }
        var selectedBrandId by remember { mutableStateOf("") }
        var catExpanded   by remember { mutableStateOf(false) }
        var brandExpanded by remember { mutableStateOf(false) }
        var deleteTarget  by remember { mutableStateOf<Product?>(null) }
        var searchQuery      by remember { mutableStateOf("") }
        var stockFilter     by remember { mutableStateOf("all") } // "all" | "0" | "1" | "2"
        var filterCategoryId by remember { mutableStateOf<String?>(null) }
        var filterBrandId    by remember { mutableStateOf<String?>(null) }

        val isUploadingImage = state.isUploadingImage

        val launchPicker = rememberImagePickerLauncher { bytes ->
            vm.uploadImage(bytes) { url -> imageUrlsList = imageUrlsList + url }
        }

        val snackbarHost = remember { SnackbarHostState() }
        LaunchedEffect(state.toast) {
            state.toast?.let { snackbarHost.showSnackbar(it); vm.clearToast() }
        }
        LaunchedEffect(state.error) {
            state.error?.let { snackbarHost.showSnackbar("خطأ: $it"); vm.clearError() }
        }

        // Brands filtered by selected category (all levels)
        val brandsForCat = state.brands.filter { it.categoryId == selectedCatId }

        val brandsForFilter = remember(filterCategoryId, state.brands) {
            if (filterCategoryId == null) emptyList()
            else state.brands.filter { it.categoryId == filterCategoryId && it.parentId == null }
        }

        val filteredProducts = remember(searchQuery, stockFilter, filterCategoryId, filterBrandId, state.products) {
            state.products
                .filter {
                    if (searchQuery.isBlank()) true
                    else searchQuery.trim().split(Regex("\\s+")).all { w -> it.name.contains(w, ignoreCase = true) }
                }
                .filter { filterCategoryId == null || it.categoryId == filterCategoryId }
                .filter { filterBrandId == null || it.brandId == filterBrandId }
                .filter {
                    when (stockFilter) {
                        "0"  -> it.stock == 0
                        "12" -> it.stock == 1 || it.stock == 2
                        else -> true
                    }
                }
        }

        fun openAdd() {
            editTarget = null; nameField = ""; priceField = ""; stockField = ""
            wholesalePriceField = ""; imageUrlsList = emptyList(); addUrlField = ""
            stockError = false; nameError = false
            selectedCatId = state.categories.firstOrNull()?.id ?: ""
            selectedBrandId = state.brands.firstOrNull { it.categoryId == selectedCatId }?.id ?: ""
            showDialog = true
        }

        fun openEdit(p: Product) {
            editTarget = p; nameField = p.name; priceField = p.price.toString()
            stockField = p.stock.toString(); stockError = false; nameError = false
            wholesalePriceField = p.wholesalePrice?.toString() ?: ""
            imageUrlsList = p.displayImages; addUrlField = ""
            selectedCatId = p.categoryId; selectedBrandId = p.brandId
            showDialog = true
        }

        val listState = rememberLazyListState()
        var headerExpanded by remember { mutableStateOf(true) }
        var prevIndex by remember { mutableIntStateOf(0) }
        var prevOffset by remember { mutableIntStateOf(0) }

        // Scroll to top whenever the active filter changes
        LaunchedEffect(filterCategoryId, filterBrandId) {
            listState.scrollToItem(0)
            headerExpanded = true
        }

        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .collect { (index, offset) ->
                    val scrollingDown = index > prevIndex || (index == prevIndex && offset > prevOffset + 8)
                    val scrollingUp   = index < prevIndex || (index == prevIndex && offset < prevOffset - 8)
                    if (scrollingDown) headerExpanded = false
                    else if (scrollingUp) headerExpanded = true
                    prevIndex  = index
                    prevOffset = offset
                }
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
                        AnimatedVisibility(
                            visible = headerExpanded,
                            enter   = expandVertically(),
                            exit    = shrinkVertically()
                        ) {
                            Column {
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
                                        "12"  to "مخزون 1 و 2"
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
                                // Category filter row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = filterCategoryId == null,
                                        onClick  = { filterCategoryId = null; filterBrandId = null },
                                        label    = { Text("كل الأقسام", style = MaterialTheme.typography.labelMedium) }
                                    )
                                    state.categories.forEach { cat ->
                                        FilterChip(
                                            selected = filterCategoryId == cat.id,
                                            onClick  = {
                                                filterCategoryId = if (filterCategoryId == cat.id) null else cat.id
                                                filterBrandId = null
                                            },
                                            label    = { Text(cat.name, style = MaterialTheme.typography.labelMedium) }
                                        )
                                    }
                                }
                                // Brand filter row — only when category selected and has brands
                                if (filterCategoryId != null && brandsForFilter.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = filterBrandId == null,
                                            onClick  = { filterBrandId = null },
                                            label    = { Text("كل الفئات", style = MaterialTheme.typography.labelMedium) }
                                        )
                                        brandsForFilter.forEach { brand ->
                                            FilterChip(
                                                selected = filterBrandId == brand.id,
                                                onClick  = { filterBrandId = if (filterBrandId == brand.id) null else brand.id },
                                                label    = { Text(brand.name, style = MaterialTheme.typography.labelMedium) }
                                            )
                                        }
                                    }
                                }
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
                    state = listState,
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
                        val thumbUrl = product.displayImages.firstOrNull()
                        Card(modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(1.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (thumbUrl != null) {
                                    AsyncImage(
                                        model = thumbUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                }
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
                        // Images
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (imageUrlsList.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    imageUrlsList.forEachIndexed { idx, url ->
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = url, contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                                    .clickable { imageUrlsList = imageUrlsList.toMutableList().also { it.removeAt(idx) } },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Close, null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { launchPicker() },
                                enabled = !isUploadingImage,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isUploadingImage) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(if (isUploadingImage) "جاري الرفع..." else "أضف صورة من الجهاز")
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = addUrlField,
                                    onValueChange = { addUrlField = it },
                                    label = { Text("رابط صورة") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val url = addUrlField.trim()
                                        if (url.isNotBlank()) {
                                            imageUrlsList = imageUrlsList + url
                                            addUrlField = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "إضافة رابط")
                                }
                            }
                        }
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
                                vm.addProduct(trimmedName, price, wholesalePrice, stock, selectedBrandId, selectedCatId, imageUrlsList)
                                lastAddedName = trimmedName
                            } else {
                                vm.editProduct(editTarget!!.id, trimmedName, price, wholesalePrice, stock, selectedBrandId, selectedCatId, imageUrlsList)
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
