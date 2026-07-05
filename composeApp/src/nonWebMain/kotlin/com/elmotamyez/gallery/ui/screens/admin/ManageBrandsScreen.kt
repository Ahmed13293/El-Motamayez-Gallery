package com.elmotamyez.gallery.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.data.model.Brand
import com.elmotamyez.gallery.util.buildBrandPath
import org.koin.compose.viewmodel.koinViewModel

class ManageBrandsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: AdminViewModel = koinViewModel()
        val state by vm.state.collectAsState()

        var showDialog   by remember { mutableStateOf(false) }
        var editTarget   by remember { mutableStateOf<Brand?>(null) }
        var nameField    by remember { mutableStateOf("") }
        var selectedCatId by remember { mutableStateOf("") }
        var selectedParentId by remember { mutableStateOf<String?>(null) }
        var catExpanded  by remember { mutableStateOf(false) }
        var parentExpanded by remember { mutableStateOf(false) }
        var deleteTarget by remember { mutableStateOf<Brand?>(null) }

        val snackbarHost = remember { SnackbarHostState() }
        LaunchedEffect(state.toast) {
            state.toast?.let { snackbarHost.showSnackbar(it); vm.clearToast() }
        }
        LaunchedEffect(state.error) {
            state.error?.let { snackbarHost.showSnackbar("خطأ: $it"); vm.clearError() }
        }

        // Brands that can be parents = only top-level brands (parentId == null)
        val topLevelBrands = state.brands.filter { it.parentId == null }

        fun openAdd() {
            editTarget = null
            nameField = ""
            selectedCatId = state.categories.firstOrNull()?.id ?: ""
            selectedParentId = null
            showDialog = true
        }

        fun openEdit(b: Brand) {
            editTarget = b
            nameField = b.name
            selectedCatId = b.categoryId
            selectedParentId = b.parentId
            showDialog = true
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            topBar = {
                Surface(color = Color.White, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                        }
                        Text("إدارة الفئات الفرعية", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = Color.Black)
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
                        Text("${state.brands.size} فئة فرعية",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                    // Group brands by category
                    state.categories.forEach { cat ->
                        val catBrands = state.brands.filter { it.categoryId == cat.id }
                        if (catBrands.isEmpty()) return@forEach
                        item(key = "header_${cat.id}") {
                            Text(cat.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        }
                        items(catBrands, key = { it.id }) { brand ->
                            val isSubSub = brand.parentId != null
                            Card(
                                modifier = Modifier.fillMaxWidth()
                                    .then(if (isSubSub) Modifier.padding(start = 16.dp) else Modifier),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isSubSub) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Text("فرعي ثاني",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                }
                                            }
                                            Text(brand.name, fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium)
                                        }
                                        // Full hierarchy path
                                        Text(
                                            buildBrandPath(brand, state.categories, state.brands),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                        Text("ID: ${brand.id}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline)
                                    }
                                    IconButton(onClick = { openEdit(brand) }) {
                                        Icon(Icons.Default.Edit, null,
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { deleteTarget = brand }) {
                                        Icon(Icons.Default.Delete, null,
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Add / Edit dialog ─────────────────────────────────────────────────
        if (showDialog) {
            val selectedCatName = state.categories.find { it.id == selectedCatId }?.name ?: ""
            val selectedParentName = state.brands.find { it.id == selectedParentId }?.name ?: "لا يوجد"

            AlertDialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
                title = { Text(if (editTarget == null) "إضافة فئة فرعية" else "تعديل الفئة") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Name
                        OutlinedTextField(
                            value = nameField, onValueChange = { nameField = it },
                            label = { Text("الاسم") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Category dropdown
                        ExposedDropdownMenuBox(
                            expanded = catExpanded,
                            onExpandedChange = { catExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCatName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("القسم الرئيسي") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = catExpanded,
                                onDismissRequest = { catExpanded = false }
                            ) {
                                state.categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = { selectedCatId = cat.id; catExpanded = false }
                                    )
                                }
                            }
                        }
                        // Parent brand dropdown (optional — for sub-sub)
                        ExposedDropdownMenuBox(
                            expanded = parentExpanded,
                            onExpandedChange = { parentExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedParentName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("تابع لـ (اختياري للفرعي الثاني)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(parentExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = parentExpanded,
                                onDismissRequest = { parentExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("لا يوجد") },
                                    onClick = { selectedParentId = null; parentExpanded = false }
                                )
                                topLevelBrands
                                    .filter { it.categoryId == selectedCatId }
                                    .forEach { brand ->
                                        DropdownMenuItem(
                                            text = { Text(brand.name) },
                                            onClick = { selectedParentId = brand.id; parentExpanded = false }
                                        )
                                    }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (nameField.isNotBlank() && selectedCatId.isNotBlank()) {
                            if (editTarget == null)
                                vm.addBrand(nameField, selectedCatId, selectedParentId)
                            else
                                vm.editBrand(editTarget!!.id, nameField, selectedCatId, selectedParentId)
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
        deleteTarget?.let { brand ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("حذف الفئة") },
                text  = { Text("هل تريد حذف \"${brand.name}\"؟") },
                confirmButton = {
                    Button(
                        onClick = { vm.deleteBrand(brand.id); deleteTarget = null },
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
