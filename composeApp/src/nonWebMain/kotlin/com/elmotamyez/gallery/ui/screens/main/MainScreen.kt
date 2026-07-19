package com.elmotamyez.gallery.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.elmotamyez.gallery.NavigationController
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import cafe.adriel.voyager.transitions.SlideTransition
import cafe.adriel.voyager.navigator.tab.*
import com.elmotamyez.gallery.data.model.UserRole
import com.elmotamyez.gallery.ui.screens.admin.AdminScreen
import com.elmotamyez.gallery.ui.screens.auth.AuthViewModel
import com.elmotamyez.gallery.ui.screens.cart.CartViewModel
import com.elmotamyez.gallery.ui.screens.home.CategoriesHomeScreen
import com.elmotamyez.gallery.ui.screens.orders.OrderViewModel
import com.elmotamyez.gallery.ui.screens.orders.OrdersScreen
import com.elmotamyez.gallery.ui.screens.receipts.ReceiptsListScreen
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptViewModel
import com.elmotamyez.gallery.ui.screens.cart.CartScreen
import org.koin.compose.koinInject

// ── Tab definitions ──────────────────────────────────────────────────────────

object CategoriesTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(index = 0u, title = "الأقسام")

    @Composable
    override fun Content() {
        Navigator(CategoriesHomeScreen()) { SlideTransition(it) }
    }
}

object CartTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(index = 1u, title = "السلة")

    @Composable
    override fun Content() = CartScreen().Content()
}

object ReceiptsTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(index = 2u, title = "الفواتير")

    @Composable
    override fun Content() = ReceiptsListScreen().Content()
}

object OrdersTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(index = 3u, title = "الطلبات")

    @Composable
    override fun Content() = OrdersScreen().Content()
}

object AdminTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(index = 4u, title = "الإدارة")

    @Composable
    override fun Content() {
        Navigator(AdminScreen()) { SlideTransition(it) }
    }
}

// ── MainScreen ────────────────────────────────────────────────────────────────

class MainScreen : Screen {
    @Composable
    override fun Content() {
        val cartVm: CartViewModel       = koinInject()
        val receiptVm: ReceiptViewModel = koinInject()
        val orderVm: OrderViewModel     = koinInject()
        val authVm: AuthViewModel       = koinInject()

        val cartItems    by cartVm.cartItems.collectAsState()
        val receipts     by receiptVm.receipts.collectAsState()
        val authState    by authVm.uiState.collectAsState()
        val pendingOrders by orderVm.pendingCount.collectAsState()

        val isAdmin = authState.user?.role == UserRole.ADMIN

        // Track how many receipts were visible when user last opened the receipts tab
        var seenReceiptsCount by remember { mutableIntStateOf(receipts.size) }
        val newReceiptsCount = (receipts.size - seenReceiptsCount).coerceAtLeast(0)

        TabNavigator(CategoriesTab) { tabNavigator ->
            val navController: NavigationController = koinInject()
            val pendingTab by navController.pendingTab.collectAsState()

            LaunchedEffect(pendingTab) {
                if (pendingTab == "orders" && isAdmin) {
                    tabNavigator.current = OrdersTab
                    navController.consume()
                }
            }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavItem(CategoriesTab, Icons.Default.Home,       tabNavigator)
                        NavItem(CartTab,       Icons.Default.ShoppingCart, tabNavigator,
                                badgeCount = cartItems.size)
                        NavItem(ReceiptsTab,   Icons.Default.Receipt,    tabNavigator,
                                badgeCount = newReceiptsCount,
                                onSelected = { seenReceiptsCount = receipts.size })
                        if (isAdmin) {
                            NavItem(OrdersTab, Icons.Default.ListAlt, tabNavigator, badgeCount = pendingOrders)
                            NavItem(AdminTab,  Icons.Default.Person,  tabNavigator)
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    CurrentTab()
                }
            }
        }
    }
}

// ── Helper composable ─────────────────────────────────────────────────────────

@Composable
private fun RowScope.NavItem(
    tab: Tab,
    icon: ImageVector,
    tabNavigator: TabNavigator,
    badgeCount: Int = 0,
    onSelected: () -> Unit = {}
) {
    val selected = tabNavigator.current == tab

    NavigationBarItem(
        selected = selected,
        onClick  = { tabNavigator.current = tab; onSelected() },
        icon = {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge {
                            Text(
                                text = if (badgeCount > 99) "99+" else "$badgeCount",
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            ) {
                Icon(imageVector = icon, contentDescription = tab.options.title)
            }
        },
        label = {
            Text(
                text = tab.options.title,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor   = MaterialTheme.colorScheme.primary,
            selectedTextColor   = MaterialTheme.colorScheme.primary,
            indicatorColor      = MaterialTheme.colorScheme.primaryContainer
        )
    )
}
