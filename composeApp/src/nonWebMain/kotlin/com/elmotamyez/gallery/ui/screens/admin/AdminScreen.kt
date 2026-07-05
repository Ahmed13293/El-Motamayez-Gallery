package com.elmotamyez.gallery.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.ui.screens.auth.AuthViewModel
import org.koin.compose.koinInject

class AdminScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authVm: AuthViewModel = koinInject()
        val state by authVm.uiState.collectAsState()
        val user = state.user

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Avatar ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AdminPanelSettings, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp))
                    }
                }
                Text(user?.name ?: "", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Surface(shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        if (user?.role?.name == "ADMIN") "مدير" else "مستخدم",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider()

            // ── Management section ────────────────────────────────────────────
            Text("إدارة البيانات", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)

            ManageCard(
                icon = Icons.Default.Category,
                title = "إدارة الأقسام",
                subtitle = "إضافة وتعديل وحذف الأقسام الرئيسية",
                onClick = { navigator.push(ManageCategoriesScreen()) }
            )
            ManageCard(
                icon = Icons.Default.SubdirectoryArrowRight,
                title = "إدارة الفئات الفرعية",
                subtitle = "إضافة وتعديل وحذف الفئات وما تحتها",
                onClick = { navigator.push(ManageBrandsScreen()) }
            )
            ManageCard(
                icon = Icons.Default.Inventory,
                title = "إدارة المنتجات",
                subtitle = "إضافة وتعديل وحذف المنتجات",
                onClick = { navigator.push(ManageProductsScreen()) }
            )

            HorizontalDivider()

            // ── Reports section ───────────────────────────────────────────────
            Text("التقارير", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)

            ManageCard(
                icon = Icons.Default.BarChart,
                title = "تقرير المبيعات",
                subtitle = "عرض الطلبات والإيرادات",
                onClick = { navigator.push(ReceiptsReportScreen()) }
            )
            ManageCard(
                icon = Icons.Default.PieChart,
                title = "تحليل المبيعات",
                subtitle = "مبيعات حسب القسم أو الفئة أو المنتج",
                onClick = { navigator.push(SalesAnalysisScreen()) }
            )

            HorizontalDivider()

            // ── Expenses section ──────────────────────────────────────────────
            Text("المصاريف", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)

            ManageCard(
                icon = Icons.Default.MoneyOff,
                title = "المصاريف",
                subtitle = "إضافة وتعديل وحذف المصاريف",
                onClick = { navigator.push(ExpensesScreen()) }
            )

            HorizontalDivider()

            // ── Account info ──────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow("اسم المستخدم", user?.username ?: "")
                    InfoRow("الاسم",         user?.name     ?: "")
                }
            }

            // ── Logout ────────────────────────────────────────────────────────
            Button(
                onClick = { authVm.logout() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("تسجيل الخروج", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun ManageCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp))
                }
            }
            Column {
                Text(title, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold)
    }
}
