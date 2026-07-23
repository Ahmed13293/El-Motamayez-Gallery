package com.elmotamyez.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmotamyez.gallery.data.model.Brand
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Category
import com.elmotamyez.gallery.data.model.Expense
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.model.Receipt
import com.elmotamyez.gallery.data.model.User
import com.elmotamyez.gallery.data.model.UserRole
import com.elmotamyez.gallery.data.repository.ProductRepository
import com.elmotamyez.gallery.ui.screens.admin.AdminViewModel
import com.elmotamyez.gallery.ui.screens.admin.ExpenseViewModel
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptViewModel
import com.elmotamyez.gallery.util.buildBrandPath
import com.elmotamyez.gallery.util.fmt2f
import com.elmotamyez.gallery.util.formatPrice
import com.elmotamyez.gallery.util.dateTimeString
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// Safari requires setTimeout(0) before .select() — works on all browsers
@JsFun("() => { setTimeout(function(){ var el = document.activeElement; if(el && typeof el.select === 'function') el.select(); }, 0); }")
private external fun selectAllInFocusedInput()

private enum class AdminSection {
    HUB, CATEGORIES, BRANDS, PRODUCTS, REPORT, EXPENSES, ANALYSIS
}

private fun AdminSection.sectionTitle() = when (this) {
    AdminSection.CATEGORIES -> "إدارة الأقسام"
    AdminSection.BRANDS     -> "إدارة الفئات الفرعية"
    AdminSection.PRODUCTS   -> "إدارة المنتجات"
    AdminSection.REPORT     -> "تقرير المبيعات"
    AdminSection.EXPENSES   -> "المصاريف"
    AdminSection.ANALYSIS   -> "تحليل المبيعات"
    AdminSection.HUB        -> ""
}

@Composable
internal fun WebAdminTab(user: User, onLogout: () -> Unit) {
    val adminVm: AdminViewModel = koinViewModel()
    val state by adminVm.state.collectAsState()
    var section by remember { mutableStateOf(AdminSection.HUB) }

    state.toast?.let { msg -> LaunchedEffect(msg) { adminVm.clearToast() } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isMobile = maxWidth < 600.dp

        Column(Modifier.fillMaxSize()) {
            state.toast?.let { msg ->
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        msg,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (section == AdminSection.HUB) {
                AdminHubPage(
                    user = user,
                    onLogout = onLogout,
                    isMobile = isMobile,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { section = it }
            } else {
                Surface(tonalElevation = 2.dp, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { section = AdminSection.HUB }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                        Text(
                            section.sectionTitle(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    when (section) {
                        AdminSection.CATEGORIES -> AdminCategoriesSection(state.categories, adminVm)
                        AdminSection.BRANDS     -> AdminBrandsSection(state.brands, state.categories, adminVm)
                        AdminSection.PRODUCTS   -> AdminProductsSection(state.products, state.categories, state.brands, adminVm, isMobile)
                        AdminSection.REPORT     -> AdminReportSection(isMobile)
                        AdminSection.EXPENSES   -> AdminExpensesSection(isMobile)
                        AdminSection.ANALYSIS   -> AdminSalesAnalysisSection(isMobile)
                        AdminSection.HUB        -> Unit
                    }
                }
            }
        }
    }
}

// ── Admin Hub Page ────────────────────────────────────────────────────────────

@Composable
private fun AdminHubPage(
    user: User,
    onLogout: () -> Unit,
    isMobile: Boolean,
    modifier: Modifier = Modifier,
    onNavigate: (AdminSection) -> Unit
) {
    BoxWithConstraints(modifier) {
        val hPadding = if (isMobile) 16.dp
                       else ((maxWidth - 700.dp).coerceAtLeast(0.dp) / 2).coerceAtLeast(24.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = hPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                        }
                    }
                    Text(user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            if (user.role == UserRole.ADMIN) "مدير" else "مستخدم",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item { HorizontalDivider() }
            item { HubSectionLabel("إدارة البيانات") }
            item { HubManageCard(Icons.Default.Category,               "إدارة الأقسام",        "إضافة وتعديل وحذف الأقسام الرئيسية")  { onNavigate(AdminSection.CATEGORIES) } }
            item { HubManageCard(Icons.Default.SubdirectoryArrowRight, "إدارة الفئات الفرعية", "إضافة وتعديل وحذف الفئات الفرعية")    { onNavigate(AdminSection.BRANDS) } }
            item { HubManageCard(Icons.Default.Inventory,              "إدارة المنتجات",        "إضافة وتعديل وحذف المنتجات")          { onNavigate(AdminSection.PRODUCTS) } }

            item { HorizontalDivider() }
            item { HubSectionLabel("التقارير") }
            item { HubManageCard(Icons.Default.BarChart,   "تقرير المبيعات",  "عرض إجمالي الفواتير والإيرادات")          { onNavigate(AdminSection.REPORT) } }
            item { HubManageCard(Icons.Default.TrendingUp, "تحليل المبيعات",  "تحليل تفصيلي حسب القسم أو المنتج")       { onNavigate(AdminSection.ANALYSIS) } }

            item { HorizontalDivider() }
            item { HubSectionLabel("المصاريف") }
            item { HubManageCard(Icons.Default.Payments, "المصاريف", "تسجيل ومتابعة المصاريف اليومية") { onNavigate(AdminSection.EXPENSES) } }

            item { HorizontalDivider() }

            // Account info card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoRow("اسم المستخدم", user.username)
                        InfoRow("الاسم الكامل", user.name)
                        InfoRow("الصلاحية", if (user.role == UserRole.ADMIN) "مدير النظام" else "مستخدم عادي")
                    }
                }
            }

            // Logout
            item {
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Logout, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("تسجيل الخروج", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HubSectionLabel(title: String) {
    Text(
        title,
        modifier = Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun HubManageCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
private fun AdminProductsSection(products: List<Product>, categories: List<Category>, brands: List<Brand>, adminVm: AdminViewModel, isMobile: Boolean = false) {
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Product?>(null) }
    var deleteTarget by remember { mutableStateOf<Product?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var stockFilter by remember { mutableStateOf("all") }

    val filtered = products
        .filter { if (searchQuery.isBlank()) true else it.name.contains(searchQuery, ignoreCase = true) }
        .filter {
            when (stockFilter) {
                "0"  -> it.stock == 0
                "12" -> it.stock == 1 || it.stock == 2
                else -> true
            }
        }

    Column(Modifier.fillMaxSize().padding(if (isMobile) 12.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isMobile) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إدارة المنتجات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { showAdd = true }, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة", fontSize = 13.sp)
                }
            }
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("بحث...") }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
        } else {
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
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                listOf("all" to "الكل", "0" to "نفد المخزون", "12" to "مخزون 1 و 2").forEach { (key, label) ->
                    FilterChip(
                        selected = stockFilter == key,
                        onClick  = { stockFilter = key },
                        label    = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (key == "0")
                                MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
                        subtitle = buildString {
                            append("$catName / $brandName")
                            append(" | السعر: ${product.price.fmt2f()} ج")
                            if (product.wholesalePrice != null) append(" | الجملة: ${product.wholesalePrice.fmt2f()} ج")
                            append(" | المخزون: ${product.stock}")
                        },
                        subtitleColor = when {
                            product.stock == 0 -> MaterialTheme.colorScheme.error
                            product.stock <= 2 -> Color(0xFFE65100)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
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
private fun AdminReportSection(isMobile: Boolean = false) {
    val receiptVm: ReceiptViewModel = koinInject()
    val expenseVm: ExpenseViewModel = koinInject()
    val receipts by receiptVm.receipts.collectAsState()
    val expenses by expenseVm.expenses.collectAsState()

    val grouped = receipts
        .sortedByDescending { it.orderNumber }
        .groupBy { it.dateKey() }
        .entries.sortedByDescending { it.key }

    val grandRevenue  = receipts.sumOf { it.total }
    val grandExpenses = expenses.sumOf { it.amount }
    val grandProfit   = grandRevenue - grandExpenses
    val grandCount    = receipts.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(if (isMobile) 12.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("تقرير المبيعات", style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // ── Top summary cards ──────────────────────────────────────────────────
        item {
            if (isMobile) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCard("إجمالي المبيعات", "${grandRevenue.formatPrice()} ج", Icons.Default.Payments, Modifier.weight(1f))
                        SummaryCard("إجمالي المصاريف", "${grandExpenses.formatPrice()} ج", Icons.Default.MoneyOff, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCard("الفواتير", "$grandCount فاتورة", Icons.Default.Receipt, Modifier.weight(1f))
                        SummaryCard("أيام النشاط", "${grouped.size} يوم", Icons.Default.CalendarMonth, Modifier.weight(1f))
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard("إجمالي المبيعات", "${grandRevenue.formatPrice()} ج", Icons.Default.Payments, Modifier.weight(1f))
                    SummaryCard("إجمالي المصاريف", "${grandExpenses.formatPrice()} ج", Icons.Default.MoneyOff, Modifier.weight(1f))
                    SummaryCard("عدد الفواتير", "$grandCount فاتورة", Icons.Default.Receipt, Modifier.weight(1f))
                    SummaryCard("أيام النشاط", "${grouped.size} يوم", Icons.Default.CalendarMonth, Modifier.weight(1f))
                }
            }
        }

        // ── Net profit card ────────────────────────────────────────────────────
        item {
            val profitColor = if (grandProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (grandProfit >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("صافي الربح", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                            color = if (grandProfit >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer)
                        Text("المبيعات − المصاريف", style = MaterialTheme.typography.labelSmall,
                            color = if (grandProfit >= 0) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                    }
                    Text("${grandProfit.formatPrice()} ج", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = profitColor)
                }
            }
        }

        // ── Daily breakdown ────────────────────────────────────────────────────
        item { HorizontalDivider() }
        item { Text("تفصيل يومي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

        grouped.forEach { (dateKey, dayReceipts) ->
            val dayRevenue   = dayReceipts.sumOf { it.total }
            val cashTotal    = dayReceipts.filter { it.paymentMethod == "كاش" }.sumOf { it.total }
            val networkTotal = dayReceipts.filter { it.paymentMethod == "شبكة" }.sumOf { it.total }
            val deferTotal   = dayReceipts.filter { it.paymentMethod == "آجل" }.sumOf { it.total }
            val dayExpenses  = expenses.filter { it.createdAt?.take(10) == dateKey }.sumOf { it.amount }
            val dayProfit    = dayRevenue - dayExpenses

            item(key = "rep_$dateKey") {
                Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val displayDate = try { val (y,m,d) = dateKey.split("-"); "$d/$m/$y" } catch (_:Exception) { dateKey }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(displayDate, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${dayRevenue.formatPrice()} ج", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${dayReceipts.size} فاتورة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (cashTotal    > 0) Text("كاش: ${cashTotal.formatPrice()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            if (networkTotal > 0) Text("شبكة: ${networkTotal.formatPrice()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            if (deferTotal   > 0) Text("آجل: ${deferTotal.formatPrice()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        if (dayExpenses > 0) {
                            HorizontalDivider(thickness = 0.5.dp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مصاريف اليوم", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("− ${dayExpenses.formatPrice()} ج", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("صافي اليوم", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("${dayProfit.formatPrice()} ج", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                                    color = if (dayProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
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
private fun CrudItemRow(title: String, subtitle: String, onEdit: () -> Unit, onDelete: () -> Unit, subtitleColor: Color? = null) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    fun tfv(s: String) = TextFieldValue(s, TextRange(s.length))
    var name         by remember { mutableStateOf(tfv(initial?.name ?: "")) }
    var price        by remember { mutableStateOf(tfv(initial?.price?.toString() ?: "")) }
    var wholesale    by remember { mutableStateOf(tfv(initial?.wholesalePrice?.toString() ?: "")) }
    var stock        by remember { mutableStateOf(tfv(initial?.stock?.toString() ?: "")) }
    var catId        by remember { mutableStateOf(initial?.categoryId ?: categories.firstOrNull()?.id ?: "") }
    var brandId      by remember { mutableStateOf(initial?.brandId ?: brands.firstOrNull()?.id ?: "") }
    var catExpanded  by remember { mutableStateOf(false) }
    var brandExpanded by remember { mutableStateOf(false) }
    fun TextFieldValue.selectAll() = copy(selection = TextRange(0, text.length))

    val filteredBrands = brands.filter { it.categoryId == catId }
    val catName   = categories.find { it.id == catId }?.name ?: "اختر قسماً"
    val brandName = brands.find { it.id == brandId }?.name ?: "اختر فئة"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("اسم المنتج") }, singleLine = true, shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) { name = name.selectAll(); selectAllInFocusedInput() } })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { if (it.text.all { c -> c.isDigit() || c == '.' }) price = it },
                        label = { Text("السعر") }, suffix = { Text("ج") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) { price = price.selectAll(); selectAllInFocusedInput() } })
                    OutlinedTextField(value = wholesale, onValueChange = { if (it.text.all { c -> c.isDigit() || c == '.' }) wholesale = it },
                        label = { Text("سعر الجملة") }, suffix = { Text("ج") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) { wholesale = wholesale.selectAll(); selectAllInFocusedInput() } })
                }
                OutlinedTextField(value = stock, onValueChange = { if (it.text.all { c -> c.isDigit() }) stock = it },
                    label = { Text("المخزون") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) { stock = stock.selectAll(); selectAllInFocusedInput() } })
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
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(brand.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            buildBrandPath(brand, categories, brands),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                onClick = { brandId = brand.id; brandExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.text.toDoubleOrNull() ?: return@Button
                    val w = wholesale.text.toDoubleOrNull()
                    val s = stock.text.toIntOrNull() ?: 0
                    onConfirm(name.text, p, w, s, brandId, catId)
                },
                enabled = name.text.isNotBlank() && price.text.isNotBlank() && catId.isNotBlank() && brandId.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

// ── Expenses Section ──────────────────────────────────────────────────────────

private val EXPENSE_TYPES = listOf("الإيجار", "مرتبات", "النت", "الكهرباء", "بضاعه", "مصاريف عامة")

@Composable
private fun AdminExpensesSection(isMobile: Boolean = false) {
    val vm: ExpenseViewModel = koinInject()
    val expenses by vm.expenses.collectAsState()
    val isSaving by vm.isSaving.collectAsState()

    var showAdd      by remember { mutableStateOf(false) }
    var editTarget   by remember { mutableStateOf<Expense?>(null) }
    var deleteTarget by remember { mutableStateOf<Expense?>(null) }

    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var filter by remember { mutableStateOf("all") }
    val filteredExpenses = remember(expenses, filter) {
        when (filter) {
            "month" -> {
                val prefix = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
                expenses.filter { it.createdAt?.startsWith(prefix) == true }
            }
            "week" -> {
                val weekAgo = today.minus(7, DateTimeUnit.DAY)
                expenses.filter { expense ->
                    expense.createdAt?.take(10)?.let {
                        runCatching { LocalDate.parse(it) >= weekAgo }.getOrDefault(false)
                    } == true
                }
            }
            else -> expenses
        }
    }

    if (showAdd) {
        ExpenseDialog(title = "إضافة مصروف", initial = null, isSaving = isSaving,
            onConfirm = { type, amount, note, isoDate -> vm.addExpense(type, amount, note, isoDate) {}; showAdd = false },
            onDismiss = { showAdd = false })
    }
    editTarget?.let { expense ->
        ExpenseDialog(title = "تعديل: ${expense.type}", initial = expense, isSaving = isSaving,
            onConfirm = { type, amount, note, isoDate ->
                vm.updateExpense(expense.copy(type = type, amount = amount, note = note?.ifBlank { null }, createdAt = isoDate)) {}
                editTarget = null
            },
            onDismiss = { editTarget = null })
    }
    deleteTarget?.let { expense ->
        ConfirmDeleteDialog(
            message = "هل تريد حذف \"${expense.type}\" بقيمة ${expense.amount.formatPrice()} ج؟",
            onConfirm = { vm.deleteExpense(expense.id); deleteTarget = null },
            onDismiss = { deleteTarget = null })
    }

    Column(Modifier.fillMaxSize().padding(if (isMobile) 12.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("المصاريف", style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showAdd = true }, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("إضافة مصروف")
            }
        }

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "الكل", "month" to "الشهر", "week" to "الأسبوع").forEach { (key, label) ->
                FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) })
            }
        }

        if (expenses.isNotEmpty()) {
            val total = filteredExpenses.sumOf { it.amount }
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("إجمالي المصاريف", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${total.formatPrice()} ج", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد مصاريف مسجّلة", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (filteredExpenses.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد مصاريف في هذه الفترة", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredExpenses, key = { it.id }) { expense ->
                    val dateText = expense.createdAt?.let { raw ->
                        try { "${raw.substring(8,10)}/${raw.substring(5,7)}/${raw.substring(0,4)}" } catch (_: Exception) { null }
                    }
                    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(expense.type, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                if (!expense.note.isNullOrBlank()) Text(expense.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (dateText != null) Text(dateText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text("${expense.amount.formatPrice()} ج", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = { editTarget = expense }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { deleteTarget = expense }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDialog(title: String, initial: Expense?, isSaving: Boolean, onConfirm: (String, Double, String?, String) -> Unit, onDismiss: () -> Unit) {
    var selectedType by remember { mutableStateOf(initial?.type) }
    var amountInput  by remember { mutableStateOf(initial?.amount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    var noteInput    by remember { mutableStateOf(initial?.note ?: "") }
    val isEdit = initial != null

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    fun parseDate(iso: String?): LocalDate = iso?.let {
        runCatching { LocalDate(it.substring(0,4).toInt(), it.substring(5,7).toInt(), it.substring(8,10).toInt()) }.getOrNull()
    } ?: LocalDate(today.year, today.monthNumber, today.dayOfMonth)

    var selectedDate by remember { mutableStateOf(parseDate(initial?.createdAt)) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (selectedDate.toEpochDays().toLong() * 86400 + 43200) * 1000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = LocalDate.fromEpochDays((millis / 1000 / 86400).toInt())
                    }
                    showDatePicker = false
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isEdit && selectedType == null) {
                    Text("اختر نوع المصروف", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    EXPENSE_TYPES.forEach { type ->
                        Surface(onClick = { selectedType = type }, shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                            Text(type, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    if (!isEdit) Text("النوع: ${selectedType!!}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = amountInput,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountInput = it },
                        label = { Text("القيمة (جنيه)") }, suffix = { Text("ج") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = noteInput, onValueChange = { noteInput = it },
                        label = { Text("ملاحظة (اختياري)") }, minLines = 2, maxLines = 3,
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = "${selectedDate.dayOfMonth.toString().padStart(2,'0')}/${selectedDate.monthNumber.toString().padStart(2,'0')}/${selectedDate.year}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("التاريخ") },
                        trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, null) } },
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (selectedType != null || isEdit) {
                Button(onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: return@Button
                    val isoDate = dateTimeString(selectedDate.year, selectedDate.monthNumber, selectedDate.dayOfMonth, 0, 0, 0)
                    onConfirm(selectedType ?: initial!!.type, amount, noteInput.ifBlank { null }, isoDate)
                }, enabled = amountInput.toDoubleOrNull() != null && !isSaving) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(if (isEdit) "حفظ التعديل" else "تأكيد المصروف")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

// ── Sales Analysis Section ────────────────────────────────────────────────────

private enum class WebGroupBy { CATEGORY, SUB_CATEGORY, PRODUCT }

private data class WebSalesRow(val label: String, val revenue: Double, val quantity: Int, val orderCount: Int)

private fun webAggregate(
    receipts: List<Receipt>,
    groupBy: WebGroupBy,
    categories: List<Category>,
    brands: List<Brand>
): List<WebSalesRow> {
    data class FlatItem(val item: CartItem, val receiptId: String)
    val flat     = receipts.flatMap { r -> r.items.map { FlatItem(it, r.id) } }
    val catMap   = categories.associateBy { it.id }
    val brandMap = brands.associateBy { it.id }
    val grouped  = flat.groupBy { fi ->
        when (groupBy) {
            WebGroupBy.CATEGORY     -> catMap[fi.item.product.categoryId]?.name ?: fi.item.product.categoryId
            WebGroupBy.SUB_CATEGORY -> brandMap[fi.item.product.brandId]?.name  ?: fi.item.product.brandId
            WebGroupBy.PRODUCT      -> fi.item.product.name
        }
    }
    return grouped.map { (label, items) ->
        WebSalesRow(
            label      = label,
            revenue    = items.sumOf { it.item.totalPrice },
            quantity   = items.sumOf { it.item.quantity },
            orderCount = items.map { it.receiptId }.toSet().size
        )
    }.sortedByDescending { it.revenue }
}

@Composable
private fun AdminSalesAnalysisSection(isMobile: Boolean = false) {
    val receiptVm   = koinInject<ReceiptViewModel>()
    val productRepo = koinInject<ProductRepository>()
    val scope       = rememberCoroutineScope()

    val allReceipts by receiptVm.receipts.collectAsState()
    var groupBy   by remember { mutableStateOf(WebGroupBy.CATEGORY) }
    var fromDate  by remember { mutableStateOf("") }
    var toDate    by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var brands     by remember { mutableStateOf<List<Brand>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch { runCatching { categories = productRepo.getCategories(); brands = productRepo.getBrands() } }
    }

    val filtered = remember(allReceipts, fromDate, toDate) {
        allReceipts.filter { r ->
            val d = r.createdAt?.take(10) ?: return@filter true
            (fromDate.isBlank() || d >= fromDate) && (toDate.isBlank() || d <= toDate)
        }
    }
    val rows = remember(filtered, groupBy, categories, brands) { webAggregate(filtered, groupBy, categories, brands) }
    val totalRevenue = rows.sumOf { it.revenue }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(if (isMobile) 12.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("تحليل المبيعات", style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // Date filters
        item {
            Text("الفترة الزمنية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = fromDate, onValueChange = { fromDate = it },
                    label = { Text("من (YYYY-MM-DD)") }, singleLine = true,
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f))
                OutlinedTextField(value = toDate, onValueChange = { toDate = it },
                    label = { Text("إلى (YYYY-MM-DD)") }, singleLine = true,
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f))
            }
            if (fromDate.isNotBlank() || toDate.isNotBlank()) {
                TextButton(onClick = { fromDate = ""; toDate = "" }) { Text("مسح الفلتر") }
            }
        }

        // Group-by toggle
        item {
            Text("تجميع حسب", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WebGroupBy.entries.forEach { g ->
                    val label = when (g) { WebGroupBy.CATEGORY -> "القسم"; WebGroupBy.SUB_CATEGORY -> "الفئة الفرعية"; WebGroupBy.PRODUCT -> "المنتج" }
                    val selected = groupBy == g
                    Surface(onClick = { groupBy = g }, shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.weight(1f).height(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Summary card
        item {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("إجمالي المبيعات", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${filtered.size} فاتورة  •  ${rows.size} ${when (groupBy) { WebGroupBy.CATEGORY -> "قسم"; WebGroupBy.SUB_CATEGORY -> "فئة"; WebGroupBy.PRODUCT -> "منتج" }}",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                    Text("${totalRevenue.formatPrice()} ج", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Results
        if (rows.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد مبيعات في هذه الفترة", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الاسم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1f))
                    Text("الكمية", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
                    Text("الإيراد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.End, modifier = Modifier.width(if (isMobile) 80.dp else 96.dp))
                }
            }
            itemsIndexed(rows) { index, row ->
                val fraction = if (totalRevenue > 0) (row.revenue / totalRevenue).toFloat() else 0f
                val rankColor = when (index + 1) { 1 -> Color(0xFFFFD700); 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> MaterialTheme.colorScheme.surfaceVariant }
                Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = CircleShape, color = rankColor, modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        color = if (index < 3) Color(0xFF3A2A00) else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(row.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("${row.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, modifier = Modifier.width(48.dp))
                            Text("${row.revenue.formatPrice()} ج", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End, modifier = Modifier.width(if (isMobile) 80.dp else 96.dp))
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                            Box(modifier = Modifier.fillMaxWidth(fraction).height(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                        Text("${row.orderCount} فاتورة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
