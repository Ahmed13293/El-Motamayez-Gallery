package com.elmotamyez.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmotamyez.gallery.data.model.Brand
import com.elmotamyez.gallery.data.model.Category
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.model.User
import com.elmotamyez.gallery.data.model.UserRole
import com.elmotamyez.gallery.ui.screens.admin.AdminViewModel
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptViewModel
import com.elmotamyez.gallery.util.fmt2f
import com.elmotamyez.gallery.util.formatPrice
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private enum class AdminSection(val label: String, val icon: ImageVector) {
    PROFILE(  "الحساب",    Icons.Default.Person),
    CATEGORIES("الأقسام",  Icons.Default.Category),
    BRANDS(   "الفئات",    Icons.Default.SubdirectoryArrowRight),
    PRODUCTS( "المنتجات",  Icons.Default.Inventory),
    REPORT(   "التقارير",  Icons.Default.BarChart),
}

@Composable
internal fun WebAdminTab(user: User, onLogout: () -> Unit) {
    val adminVm: AdminViewModel = koinViewModel()
    val state by adminVm.state.collectAsState()
    var section by remember { mutableStateOf(AdminSection.PROFILE) }

    // Show toast
    state.toast?.let { msg ->
        LaunchedEffect(msg) { adminVm.clearToast() }
    }

    Row(Modifier.fillMaxSize()) {
        // ── Sidebar nav ───────────────────────────────────────────────────────
        Surface(modifier = Modifier.width(190.dp).fillMaxHeight(), tonalElevation = 1.dp) {
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("لوحة الإدارة", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))

                AdminSection.entries.forEach { s ->
                    val selected = section == s
                    Surface(
                        onClick = { section = s },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(s.icon, null, modifier = Modifier.size(18.dp),
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(s.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Toast notification
                state.toast?.let { msg ->
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(msg, modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }

        // ── Content area ──────────────────────────────────────────────────────
        Box(Modifier.fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            when (section) {
                AdminSection.PROFILE    -> AdminProfileSection(user, onLogout)
                AdminSection.CATEGORIES -> AdminCategoriesSection(state.categories, adminVm)
                AdminSection.BRANDS     -> AdminBrandsSection(state.brands, state.categories, adminVm)
                AdminSection.PRODUCTS   -> AdminProductsSection(state.products, state.categories, state.brands, adminVm)
                AdminSection.REPORT     -> AdminReportSection()
            }
        }
    }
}

// ── Profile Section ───────────────────────────────────────────────────────────

@Composable
private fun AdminProfileSection(user: User, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("معلومات الحساب", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Avatar
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                    }
                    Column {
                        Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                if (user.role == UserRole.ADMIN) "مدير" else "مستخدم",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                HorizontalDivider()
                InfoRow("اسم المستخدم", user.username)
                InfoRow("الاسم الكامل", user.name)
                InfoRow("الصلاحية", if (user.role == UserRole.ADMIN) "مدير النظام" else "مستخدم عادي")
            }
        }

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(240.dp).height(48.dp)
        ) {
            Icon(Icons.Default.Logout, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("تسجيل الخروج", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ── Categories Section ────────────────────────────────────────────────────────

@Composable
private fun AdminCategoriesSection(categories: List<Category>, adminVm: AdminViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Category?>(null) }
    var deleteTarget by remember { mutableStateOf<Category?>(null) }
    var nameField by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("إدارة الأقسام", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { nameField = ""; showAdd = true }, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("إضافة قسم")
            }
        }

        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد أقسام", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { cat ->
                    CrudItemRow(
                        title = cat.name,
                        subtitle = "ID: ${cat.id}",
                        onEdit = { editTarget = cat; nameField = cat.name },
                        onDelete = { deleteTarget = cat }
                    )
                }
            }
        }
    }

    // Add dialog
    if (showAdd) {
        SimpleInputDialog(
            title = "إضافة قسم جديد",
            label = "اسم القسم",
            value = nameField,
            onValueChange = { nameField = it },
            onConfirm = { adminVm.addCategory(nameField); showAdd = false },
            onDismiss = { showAdd = false }
        )
    }

    // Edit dialog
    editTarget?.let { cat ->
        SimpleInputDialog(
            title = "تعديل القسم",
            label = "اسم القسم",
            value = nameField,
            onValueChange = { nameField = it },
            onConfirm = { adminVm.editCategory(cat.id, nameField); editTarget = null },
            onDismiss = { editTarget = null }
        )
    }

    // Delete confirmation
    deleteTarget?.let { cat ->
        ConfirmDeleteDialog(
            message = "هل تريد حذف القسم \"${cat.name}\"؟ سيتم حذف جميع المنتجات المرتبطة.",
            onConfirm = { adminVm.deleteCategory(cat.id); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

// ── Brands Section ────────────────────────────────────────────────────────────

@Composable
private fun AdminBrandsSection(brands: List<Brand>, categories: List<Category>, adminVm: AdminViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Brand?>(null) }
    var deleteTarget by remember { mutableStateOf<Brand?>(null) }
    var nameField by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("إدارة الفئات الفرعية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { nameField = ""; selectedCategoryId = categories.firstOrNull()?.id ?: ""; showAdd = true }, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("إضافة فئة")
            }
        }

        if (brands.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد فئات", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(brands, key = { it.id }) { brand ->
                    val catName = categories.find { it.id == brand.categoryId }?.name ?: brand.categoryId
                    CrudItemRow(
                        title = brand.name,
                        subtitle = "القسم: $catName",
                        onEdit = { editTarget = brand; nameField = brand.name; selectedCategoryId = brand.categoryId },
                        onDelete = { deleteTarget = brand }
                    )
                }
            }
        }
    }

    if (showAdd) {
        BrandDialog(
            title = "إضافة فئة جديدة",
            nameValue = nameField,
            onNameChange = { nameField = it },
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelect = { selectedCategoryId = it },
            onConfirm = { adminVm.addBrand(nameField, selectedCategoryId, null); showAdd = false },
            onDismiss = { showAdd = false }
        )
    }

    editTarget?.let { brand ->
        BrandDialog(
            title = "تعديل الفئة",
            nameValue = nameField,
            onNameChange = { nameField = it },
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelect = { selectedCategoryId = it },
            onConfirm = { adminVm.editBrand(brand.id, nameField, selectedCategoryId, null); editTarget = null },
            onDismiss = { editTarget = null }
        )
    }

    deleteTarget?.let { brand ->
        ConfirmDeleteDialog(
            message = "هل تريد حذف الفئة \"${brand.name}\"؟",
            onConfirm = { adminVm.deleteBrand(brand.id); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

// ── Products Section ──────────────────────────────────────────────────────────

@Composable
private fun AdminProductsSection(products: List<Product>, categories: List<Category>, brands: List<Brand>, adminVm: AdminViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Product?>(null) }
    var deleteTarget by remember { mutableStateOf<Product?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = if (searchQuery.isBlank()) products else products.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("إدارة المنتجات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث...") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.width(220.dp))
                Button(onClick = { showAdd = true }, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("إضافة منتج")
                }
            }
        }

        Text("${filtered.size} منتج", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد منتجات", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { product ->
                    val brandName = brands.find { it.id == product.brandId }?.name ?: ""
                    val catName   = categories.find { it.id == product.categoryId }?.name ?: ""
                    CrudItemRow(
                        title = product.name,
                        subtitle = "$catName / $brandName | السعر: ${product.price.fmt2f()} ج | المخزون: ${product.stock}",
                        onEdit = { editTarget = product },
                        onDelete = { deleteTarget = product }
                    )
                }
            }
        }
    }

    if (showAdd) {
        ProductDialog(
            title = "إضافة منتج جديد",
            initial = null,
            categories = categories,
            brands = brands,
            onConfirm = { name, price, wholesale, stock, brandId, catId ->
                adminVm.addProduct(name, price, wholesale, stock, brandId, catId)
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }

    editTarget?.let { product ->
        ProductDialog(
            title = "تعديل المنتج",
            initial = product,
            categories = categories,
            brands = brands,
            onConfirm = { name, price, wholesale, stock, brandId, catId ->
                adminVm.editProduct(product.id, name, price, wholesale, stock, brandId, catId)
                editTarget = null
            },
            onDismiss = { editTarget = null }
        )
    }

    deleteTarget?.let { product ->
        ConfirmDeleteDialog(
            message = "هل تريد حذف المنتج \"${product.name}\"؟",
            onConfirm = { adminVm.deleteProduct(product.id); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

// ── Report Section ────────────────────────────────────────────────────────────

@Composable
private fun AdminReportSection() {
    val receiptVm: ReceiptViewModel = koinInject()
    val receipts by receiptVm.receipts.collectAsState()

    val grouped = receipts
        .sortedByDescending { it.orderNumber }
        .groupBy { it.dateKey() }
        .entries.sortedByDescending { it.key }

    val grandTotal = receipts.sumOf { it.total }
    val grandCount = receipts.size

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("تقرير المبيعات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Summary cards
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("إجمالي المبيعات", "${grandTotal.formatPrice()} ج", Icons.Default.Payments, Modifier.weight(1f))
            SummaryCard("عدد الفواتير", "$grandCount فاتورة", Icons.Default.Receipt, Modifier.weight(1f))
            SummaryCard("أيام النشاط", "${grouped.size} يوم", Icons.Default.CalendarMonth, Modifier.weight(1f))
        }

        HorizontalDivider()
        Text("تفصيل يومي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            grouped.forEach { (dateKey, dayReceipts) ->
                val dayTotal = dayReceipts.sumOf { it.total }
                val cashTotal = dayReceipts.filter { it.paymentMethod == "كاش" }.sumOf { it.total }
                val netTotal  = dayReceipts.filter { it.paymentMethod == "شبكة" }.sumOf { it.total }
                val deferTotal= dayReceipts.filter { it.paymentMethod == "آجل" }.sumOf { it.total }
                item(key = "rep_$dateKey") {
                    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val displayDate = try { val (y,m,d) = dateKey.split("-"); "$d/$m/$y" } catch (_:Exception) { dateKey }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(displayDate, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${dayTotal.formatPrice()} ج", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("${dayReceipts.size} فاتورة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (cashTotal  > 0) Text("كاش: ${cashTotal.formatPrice()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                if (netTotal   > 0) Text("شبكة: ${netTotal.formatPrice()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                if (deferTotal > 0) Text("آجل: ${deferTotal.formatPrice()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp), modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Shared CRUD Components ────────────────────────────────────────────────────

@Composable
private fun CrudItemRow(title: String, subtitle: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SimpleInputDialog(title: String, label: String, value: String, onValueChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = value.isNotBlank()) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun ConfirmDeleteDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("حذف", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun BrandDialog(title: String, nameValue: String, onNameChange: (String) -> Unit, categories: List<Category>, selectedCategoryId: String, onCategorySelect: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCatName = categories.find { it.id == selectedCategoryId }?.name ?: "اختر قسماً"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nameValue, onValueChange = onNameChange, label = { Text("اسم الفئة") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                Box {
                    OutlinedTextField(value = selectedCatName, onValueChange = {}, readOnly = true, label = { Text("القسم") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } })
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name) }, onClick = { onCategorySelect(cat.id); expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = nameValue.isNotBlank() && selectedCategoryId.isNotBlank()) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun ProductDialog(title: String, initial: Product?, categories: List<Category>, brands: List<Brand>, onConfirm: (String, Double, Double?, Int, String, String) -> Unit, onDismiss: () -> Unit) {
    var name         by remember { mutableStateOf(initial?.name ?: "") }
    var price        by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var wholesale    by remember { mutableStateOf(initial?.wholesalePrice?.toString() ?: "") }
    var stock        by remember { mutableStateOf(initial?.stock?.toString() ?: "") }
    var catId        by remember { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id ?: "") }
    var brandId      by remember { mutableStateOf(initial?.brandId ?: brands.firstOrNull()?.id ?: "") }
    var catExpanded  by remember { mutableStateOf(false) }
    var brandExpanded by remember { mutableStateOf(false) }

    val filteredBrands = brands.filter { it.categoryId == catId }
    val catName   = categories.find { it.id == catId }?.name ?: "اختر قسماً"
    val brandName = brands.find { it.id == brandId }?.name ?: "اختر فئة"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المنتج") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) price = it },
                        label = { Text("السعر") }, suffix = { Text("ج") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = wholesale, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) wholesale = it },
                        label = { Text("سعر الجملة") }, suffix = { Text("ج") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = stock, onValueChange = { if (it.all { c -> c.isDigit() }) stock = it },
                    label = { Text("المخزون") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                // Category dropdown
                Box {
                    OutlinedTextField(value = catName, onValueChange = {}, readOnly = true, label = { Text("القسم") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        trailingIcon = { IconButton(onClick = { catExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } })
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name) }, onClick = { catId = cat.id; brandId = brands.firstOrNull { it.categoryId == cat.id }?.id ?: ""; catExpanded = false })
                        }
                    }
                }
                // Brand dropdown (filtered by category)
                Box {
                    OutlinedTextField(value = brandName, onValueChange = {}, readOnly = true, label = { Text("الفئة الفرعية") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        trailingIcon = { IconButton(onClick = { brandExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } })
                    DropdownMenu(expanded = brandExpanded, onDismissRequest = { brandExpanded = false }) {
                        filteredBrands.forEach { brand ->
                            DropdownMenuItem(text = { Text(brand.name) }, onClick = { brandId = brand.id; brandExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: return@Button
                    val w = wholesale.toDoubleOrNull()
                    val s = stock.toIntOrNull() ?: 0
                    onConfirm(name, p, w, s, brandId, catId)
                },
                enabled = name.isNotBlank() && price.isNotBlank() && catId.isNotBlank() && brandId.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

