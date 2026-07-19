package com.elmotamyez.gallery.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Order
import com.elmotamyez.gallery.data.model.OrderStatus
import com.elmotamyez.gallery.data.model.Receipt
import com.elmotamyez.gallery.data.repository.OrderRepository
import com.elmotamyez.gallery.data.repository.ProductRepository
import com.elmotamyez.gallery.data.repository.ReceiptRepository
import com.elmotamyez.gallery.util.dateString
import com.elmotamyez.gallery.util.dateTimeString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class OrderViewModel(
    private val repository: OrderRepository,
    private val receiptRepo: ReceiptRepository,
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _products = MutableStateFlow<List<com.elmotamyez.gallery.data.model.Product>>(emptyList())
    val products: StateFlow<List<com.elmotamyez.gallery.data.model.Product>> = _products.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { loadOrders() }

    fun clearError() { _error.value = null }

    fun loadOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.fetchAll() }
                .onSuccess { list ->
                    _orders.value = list
                    _pendingCount.value = list.count { it.status != OrderStatus.DELIVERED.key }
                }
                .onFailure { e ->
                    println("OrderViewModel.loadOrders failed: $e")
                    _error.value = "فشل تحميل الطلبات: ${e.message}"
                }
            runCatching { productRepo.getProducts() }.onSuccess { _products.value = it }
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
        customerAddress: String? = null,
        notes: String? = null,
        createdBy: String? = null,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val order = Order(
                items           = items,
                total           = total,
                discount        = discount,
                paymentMethod   = paymentMethod,
                customerName    = customerName,
                customerPhone   = customerPhone,
                customerAddress = customerAddress,
                notes           = notes,
                createdBy       = createdBy
            )
            runCatching { repository.insert(order) }
                .onFailure { e ->
                    println("OrderViewModel.createOrder insert failed: $e")
                    _error.value = "فشل حفظ الطلب: ${e.message}"
                }
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

            if (next == OrderStatus.DELIVERED) {
                runCatching { createReceiptFromOrder(order, adminUsername) }
                order.items.filter { !it.product.id.startsWith("other_") }.forEach { item ->
                    runCatching { productRepo.decrementStock(item.product.id, item.quantity) }
                }
            }

            val updated = order.copy(status = next.key, preparedBy = newPreparedBy)
            val newList = _orders.value.map { if (it.id == order.id) updated else it }
            _orders.value = newList
            _pendingCount.value = newList.count { it.status != OrderStatus.DELIVERED.key }
            _isSaving.value = false
        }
    }

    private suspend fun createReceiptFromOrder(order: Order, username: String?) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val todayPrefix = dateString(now.year, now.monthNumber, now.dayOfMonth)
        val existingMax = receiptRepo.fetchAll()
            .filter { it.createdAt?.startsWith(todayPrefix) == true }
            .maxOfOrNull { it.orderNumber } ?: 0
        val nextNumber = existingMax + 1
        val nowIso = dateTimeString(now.year, now.monthNumber, now.dayOfMonth, now.hour, now.minute, now.second)
        val receipt = Receipt(
            id            = "${todayPrefix}-${nextNumber.toString().padStart(4, '0')}",
            orderNumber   = nextNumber,
            items         = order.items,
            total         = order.total,
            discount      = order.discount,
            paymentMethod = order.paymentMethod,
            isPaid        = true,
            createdAt     = nowIso,
            customerPhone = order.customerPhone,
            customerInfo  = order.customerName,
            username      = username
        )
        receiptRepo.insert(receipt)
    }

    fun updateOrder(
        order: Order,
        newItems: List<CartItem>,
        discount: Double,
        depositFee: Double,
        deliveryFee: Double,
        paymentMethod: String
    ) {
        val newTotal = newItems.sumOf { it.totalPrice } - discount - depositFee + deliveryFee
        val updated = order.copy(
            items         = newItems,
            total         = newTotal,
            discount      = discount,
            depositFee    = depositFee,
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
                _pendingCount.value = newList.count { it.status != OrderStatus.DELIVERED.key }
                onDone()
            }
            _isSaving.value = false
        }
    }
}
