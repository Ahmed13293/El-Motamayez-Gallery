package com.elmotamyez.gallery.di

import com.elmotamyez.gallery.data.repository.AuthRepository
import com.elmotamyez.gallery.data.repository.ExpenseRepository
import com.elmotamyez.gallery.data.repository.OrderRepository
import com.elmotamyez.gallery.data.repository.ProductRepository
import com.elmotamyez.gallery.data.repository.ReceiptRepository
import com.elmotamyez.gallery.ui.screens.admin.AdminViewModel
import com.elmotamyez.gallery.ui.screens.admin.ExpenseViewModel
import com.elmotamyez.gallery.ui.screens.auth.AuthViewModel
import com.elmotamyez.gallery.ui.screens.cart.CartViewModel
import com.elmotamyez.gallery.ui.screens.products.ProductsViewModel
import com.elmotamyez.gallery.ui.screens.orders.OrderViewModel
import com.elmotamyez.gallery.ui.screens.receipt.ReceiptViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Repositories
    single { ProductRepository() }
    single { ReceiptRepository() }
    single { OrderRepository() }
    single { ExpenseRepository() }
    single { AuthRepository() }

    // Singletons — shared state across all tabs
    singleOf(::CartViewModel)
    singleOf(::ReceiptViewModel)
    singleOf(::OrderViewModel)
    singleOf(::AuthViewModel)
    singleOf(::ExpenseViewModel)

    // Per-screen ViewModels
    viewModelOf(::ProductsViewModel)
    viewModelOf(::AdminViewModel)
}
