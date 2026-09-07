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

private const val KEY_CART_0 = "cart_items_json_0"
private const val KEY_CART_1 = "cart_items_json_1"
private const val KEY_CART_LEGACY = "cart_items_json"

class CartViewModel : ViewModel() {

    private val settings = Settings()
    private val json = Json { ignoreUnknownKeys = true }

    private val _activeSlotIndex = MutableStateFlow(0)
    val activeSlotIndex: StateFlow<Int> = _activeSlotIndex.asStateFlow()

    // Both slot item lists — kept in sync with _cartItems for the active slot
    private val _slots = MutableStateFlow(listOf(emptyList<CartItem>(), emptyList<CartItem>()))
    val slots: StateFlow<List<List<CartItem>>> = _slots.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val totalPrice: Double get() = _cartItems.value.sumOf { it.totalPrice }

    init {
        val s0: String = settings[KEY_CART_0, ""].ifEmpty { settings[KEY_CART_LEGACY, ""] }
        val s1: String = settings[KEY_CART_1, ""]
        val slot0 = if (s0.isNotEmpty()) runCatching { json.decodeFromString<List<CartItem>>(s0) }.getOrDefault(emptyList()) else emptyList()
        val slot1 = if (s1.isNotEmpty()) runCatching { json.decodeFromString<List<CartItem>>(s1) }.getOrDefault(emptyList()) else emptyList()
        _slots.value = listOf(slot0, slot1)
        _cartItems.value = slot0
    }

    fun switchSlot(newIdx: Int) {
        if (newIdx == _activeSlotIndex.value || newIdx !in 0..1) return
        val currentIdx = _activeSlotIndex.value
        persistSlot(currentIdx, _cartItems.value)
        val updated = _slots.value.toMutableList()
        updated[currentIdx] = _cartItems.value
        _slots.value = updated
        _activeSlotIndex.value = newIdx
        _cartItems.value = _slots.value[newIdx]
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
                items.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + quantity) else it }
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

    fun isInCart(productId: String): Boolean = _cartItems.value.any { it.product.id == productId }

    fun clearCart() {
        _cartItems.update { emptyList() }
        persist()
    }

    private fun persist() {
        val idx = _activeSlotIndex.value
        persistSlot(idx, _cartItems.value)
        val updated = _slots.value.toMutableList()
        updated[idx] = _cartItems.value
        _slots.value = updated
    }

    private fun persistSlot(index: Int, items: List<CartItem>) {
        val key = if (index == 0) KEY_CART_0 else KEY_CART_1
        settings[key] = json.encodeToString(items)
    }
}
