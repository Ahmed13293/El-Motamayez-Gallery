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
import com.elmotamyez.gallery.data.model.Category
import org.koin.compose.viewmodel.koinViewModel

class ManageCategoriesScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: AdminViewModel = koinViewModel()
        val state by vm.state.collectAsState()

        // Dialog state
        var showDialog by remember { mutableStateOf(false) }
        var editTarget by remember { mutableStateOf<Category?>(null) }
        var nameField  by remember { mutableStateOf("") }

        // Delete confirm
        var deleteTarget by remember { mutableStateOf<Category?>(null) }

        // Toast + error via SnackbarHost
        val snackbarHost = remember { SnackbarHostState() }
        LaunchedEffect(state.toast) {
            state.toast?.let { snackbarHost.showSnackbar(it); vm.clearToast() }
        }
        LaunchedEffect(state.error) {
            state.error?.let { snackbarHost.showSnackbar("خطأ: $it"); vm.clearError() }
        }

        fun openAdd()   { editTarget = null; nameField = ""; showDialog = true }
        fun openEdit(c: Category) { editTarget = c; nameField = c.name; showDialog = true }

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
                        Text("إدارة الأقسام", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = ::openAdd,
                    containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة قسم", tint = Color.White)
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
                        Text("${state.categories.size} قسم",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                    items(state.categories, key = { it.id }) { cat ->
                        Card(modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(1.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.name, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge)
                                    Text("ID: ${cat.id}", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(onClick = { openEdit(cat) }) {
                                    Icon(Icons.Default.Edit, null,
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { deleteTarget = cat }) {
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
            AlertDialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
                title = { Text(if (editTarget == null) "إضافة قسم جديد" else "تعديل القسم") },
                text = {
                    OutlinedTextField(
                        value = nameField,
                        onValueChange = { nameField = it },
                        label = { Text("اسم القسم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (nameField.isNotBlank()) {
                                if (editTarget == null) vm.addCategory(nameField)
                                else vm.editCategory(editTarget!!.id, nameField)
                                showDialog = false
                            }
                        }
                    ) { Text("حفظ") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("إلغاء") }
                }
            )
        }

        // ── Delete confirm dialog ─────────────────────────────────────────────
        deleteTarget?.let { cat ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("حذف القسم") },
                text  = { Text("هل تريد حذف \"${cat.name}\"؟ سيتم حذف كل الفئات الفرعية والمنتجات التابعة له.") },
                confirmButton = {
                    Button(
                        onClick = { vm.deleteCategory(cat.id); deleteTarget = null },
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
