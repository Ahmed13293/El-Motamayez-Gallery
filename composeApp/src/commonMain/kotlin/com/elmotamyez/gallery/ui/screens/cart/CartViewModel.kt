package com.elmotamyez.gallery.ui.screens.cart

import androidx.lifecycle.ViewModel
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Product
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KEY_CART = "cart_items_json"

class CartViewModel : ViewModel() {

    private val settings = Settings()
    private val json = Json { ignoreUnknownKeys = true }

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val totalPrice: Double get() = _cartItems.value.sumOf { it.totalPrice }

    init {
        // Restore saved cart on startup
        val stored: String = settings[KEY_CART, ""]
        if (stored.isNotEmpty()) {
            runCatching {
                _cartItems.value = json.decodeFromString<List<CartItem>>(stored)
            }
        }
    }

    fun addToCart(product: Product) {
        _cartItems.update { items ->
            val existing = items.find { it.product.id == product.id }
            if (existing != null) {
                if (existing.quantity >= product.stock) return@update items
                items.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
            } else {
                if (product.stock <= 0) return@update items
                items + CartItem(product)
            }
        }
        persist()
    }

    fun addWithQuantity(product: Product, quantity: Int) {
        if (quantity <= 0) return
        _cartItems.update { items ->
            val existing = items.find { it.product.id == product.id }
            if (existing != null) {
                items.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + quantity)
                    else it
                }
            } else {
                items + CartItem(product, quantity)
            }
        }
        persist()
    }

    fun removeFromCart(productId: String) {
        _cartItems.update { items -> items.filter { it.product.id != productId } }
        persist()
    }

    fun increaseQuantity(productId: String) {
        _cartItems.update { items ->
            items.map {
                if (it.product.id == productId && it.quantity < it.product.stock)
                    it.copy(quantity = it.quantity + 1)
                else it
            }
        }
        persist()
    }

    fun decreaseQuantity(productId: String) {
        _cartItems.update { items ->
            items.mapNotNull {
                if (it.product.id == productId) {
                    if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null
                } else it
            }
        }
        persist()
    }

    fun isInCart(productId: String): Boolean =
        _cartItems.value.any { it.product.id == productId }

    fun clearCart() {
        _cartItems.update { emptyList() }
        persist()
    }

    private fun persist() {
        settings[KEY_CART] = json.encodeToString(_cartItems.value)
    }
}
