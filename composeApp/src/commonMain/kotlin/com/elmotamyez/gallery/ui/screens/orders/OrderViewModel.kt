package com.elmotamyez.gallery.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Order
import com.elmotamyez.gallery.data.model.OrderStatus
import com.elmotamyez.gallery.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    init { loadOrders() }

    fun loadOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.fetchAll() }
                .onSuccess { list ->
                    _orders.value = list
                    _pendingCount.value = list.count { it.status != OrderStatus.DELIVERING.key }
                }
            _isLoading.value = false
        }
    }

    fun createOrder(
        items: List<CartItem>,
        total: Double,
        paymentMethod: String = "كاش",
        discount: Double = 0.0,
        customerName: String? = null,
        customerPhone: String? = null,
        notes: String? = null,
        createdBy: String? = null,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val order = Order(
                items         = items,
                total         = total,
                discount      = discount,
                paymentMethod = paymentMethod,
                customerName  = customerName,
                customerPhone = customerPhone,
                notes         = notes,
                createdBy     = createdBy
            )
            runCatching { repository.insert(order) }
            loadOrders()
            _isSaving.value = false
            onDone()
        }
    }

    fun advanceStatus(order: Order, adminUsername: String? = null) {
        val current = OrderStatus.fromKey(order.status)
        val next = current.next() ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val newPreparedBy = if (next == OrderStatus.PREPARING) adminUsername else order.preparedBy
            runCatching { repository.updateStatus(order.id, next.key, newPreparedBy) }
            val updated = order.copy(status = next.key, preparedBy = newPreparedBy)
            val newList = _orders.value.map { if (it.id == order.id) updated else it }
            _orders.value = newList
            _pendingCount.value = newList.count { it.status != OrderStatus.DELIVERING.key }
            _isSaving.value = false
        }
    }

    fun updateOrder(
        order: Order,
        newItems: List<CartItem>,
        discount: Double,
        deliveryFee: Double,
        paymentMethod: String
    ) {
        val newTotal = newItems.sumOf { it.totalPrice } - discount + deliveryFee
        val updated = order.copy(
            items         = newItems,
            total         = newTotal,
            discount      = discount,
            deliveryFee   = deliveryFee,
            paymentMethod = paymentMethod
        )
        viewModelScope.launch {
            _isSaving.value = true
            runCatching { repository.update(updated) }
            _orders.value = _orders.value.map { if (it.id == order.id) updated else it }
            _isSaving.value = false
        }
    }

    fun deleteOrder(order: Order, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = runCatching { repository.delete(order.id) }
            if (result.isSuccess) {
                val newList = _orders.value.filter { it.id != order.id }
                _orders.value = newList
                _pendingCount.value = newList.count { it.status != OrderStatus.DELIVERING.key }
                onDone()
            }
            _isSaving.value = false
        }
    }
}
