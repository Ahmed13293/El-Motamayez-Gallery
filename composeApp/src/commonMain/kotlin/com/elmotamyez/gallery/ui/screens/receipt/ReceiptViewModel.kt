package com.elmotamyez.gallery.ui.screens.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Receipt
import com.elmotamyez.gallery.data.repository.ProductRepository
import com.elmotamyez.gallery.data.repository.ReceiptRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.elmotamyez.gallery.util.dateString
import com.elmotamyez.gallery.util.dateTimeString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KEY_RECEIPTS_CACHE = "receipts_cache_json"

class ReceiptViewModel(
    private val repository: ReceiptRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val settings = Settings()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // Incremented every time stock is decremented — observers use this to trigger a refresh
    private val _stockVersion = MutableStateFlow(0)
    val stockVersion: StateFlow<Int> = _stockVersion.asStateFlow()

    private val _allProducts = MutableStateFlow<List<com.elmotamyez.gallery.data.model.Product>>(emptyList())
    val allProducts: StateFlow<List<com.elmotamyez.gallery.data.model.Product>> = _allProducts.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Currently viewed receipt (shown in ReceiptScreen)
    private val _currentReceipt = MutableStateFlow<Receipt?>(null)
    val currentReceipt: StateFlow<Receipt?> = _currentReceipt.asStateFlow()

    // Expanded state for each day group in ReceiptsListScreen — survives back-navigation
    private val _expandedDays = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val expandedDays: StateFlow<Map<String, Boolean>> = _expandedDays.asStateFlow()

    // Scroll position for ReceiptsListScreen — survives back-navigation
    var listScrollIndex: Int = 0
    var listScrollOffset: Int = 0

    /** Called once when the grouped list is first built to seed default state (newest day open). */
    fun initExpandedDays(dateKeys: List<String>) {
        if (_expandedDays.value.isEmpty()) {
            _expandedDays.value = dateKeys.mapIndexed { i, key -> key to (i == 0) }.toMap()
        } else {
            // Merge: keep existing state, add any new date keys as collapsed
            val current = _expandedDays.value.toMutableMap()
            dateKeys.forEach { key -> if (!current.containsKey(key)) current[key] = false }
            _expandedDays.value = current
        }
    }

    fun toggleDay(dateKey: String) {
        _expandedDays.value = _expandedDays.value.toMutableMap().also {
            it[dateKey] = !(it[dateKey] ?: false)
        }
    }

    // Full history — seeded from local cache instantly, then refreshed from Supabase
    private val _receipts = MutableStateFlow<List<Receipt>>(emptyList())
    val receipts: StateFlow<List<Receipt>> = _receipts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Show cached receipts immediately so the list isn't empty on launch
        val cached: String = settings[KEY_RECEIPTS_CACHE, ""]
        if (cached.isNotEmpty()) {
            runCatching {
                _receipts.value = json.decodeFromString<List<Receipt>>(cached)
            }
        }
        // Then sync latest from Supabase in the background
        loadReceipts()
    }

    /** Reload all receipts from the cloud (called on init and on pull-to-refresh). */
    fun loadReceipts() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.fetchAll() }
                .onSuccess { fresh ->
                    _receipts.value = fresh
                    persistCache(fresh)
                }
            _isLoading.value = false
        }
    }

    /** Called when the user confirms an order from CartScreen.
     *  [overrideDate] allows admins to back-date a receipt: Triple(year, month, day). */
    fun confirmOrder(
        items: List<CartItem>,
        total: Double,
        discount: Double = 0.0,
        paymentMethod: String = "كاش",
        customerPhone: String? = null,
        customerInfo: String? = null,
        username: String? = null,
        overrideDate: Triple<Int, Int, Int>? = null
    ) {
        val isPaid = paymentMethod != "آجل"
        viewModelScope.launch {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val (year, month, day) = overrideDate ?: Triple(now.year, now.monthNumber, now.dayOfMonth)
            val todayPrefix = dateString(year, month, day)
            val todayMax = _receipts.value
                .filter { it.createdAt?.startsWith(todayPrefix) == true }
                .maxOfOrNull { it.orderNumber } ?: 0
            val nextNumber = todayMax + 1
            val nowIso = if (overrideDate != null)
                "${todayPrefix}T12:00:00+00:00"
            else
                dateTimeString(now.year, now.monthNumber, now.dayOfMonth, now.hour, now.minute, now.second)
            val receipt = Receipt(
                id            = "${todayPrefix}-${nextNumber.toString().padStart(4, '0')}",
                orderNumber   = nextNumber,
                items         = items,
                total         = total,
                discount      = discount,
                paymentMethod = paymentMethod,
                isPaid        = isPaid,
                createdAt     = nowIso,
                customerPhone = customerPhone.takeIf { !it.isNullOrBlank() },
                customerInfo  = customerInfo.takeIf  { !it.isNullOrBlank() },
                username      = username.takeIf      { !it.isNullOrBlank() }
            )
            // Update local state and cache immediately
            val updated = _receipts.value + receipt
            _receipts.value = updated
            _currentReceipt.value = receipt
            persistCache(updated)

            // Push receipt to Supabase
            runCatching { repository.insert(receipt) }

            // Decrement stock for each real product (skip printing/other virtual items)
            items
                .filter { it.product.categoryId.isNotBlank() && !it.product.id.startsWith("other_") }
                .forEach { cartItem ->
                    runCatching {
                        productRepository.decrementStock(cartItem.product.id, cartItem.quantity)
                    }
                }
            // Signal observers (e.g. CategoriesHomeScreen) to refresh product stock
            _stockVersion.value += 1
        }
    }

    /** Called when tapping a receipt from the history list. */
    fun viewReceipt(receipt: Receipt) {
        _currentReceipt.value = receipt
    }

    /** Loads all products so the edit sheet can offer an add-product search. */
    fun loadProductsForEdit() {
        if (_allProducts.value.isNotEmpty()) return
        viewModelScope.launch {
            runCatching { productRepository.getProducts() }
                .onSuccess { _allProducts.value = it }
        }
    }

    /**
     * Persists edited receipt items/total/discount to Supabase and reconciles
     * product stock (restores stock for removed/reduced items, deducts for added/increased ones).
     */
    fun updateReceipt(newItems: List<CartItem>, discount: Double, paymentMethod: String) {
        val receipt = _currentReceipt.value ?: return
        val newTotal = newItems.sumOf { it.totalPrice } - discount

        viewModelScope.launch {
            _isSaving.value = true

            val oldQtyMap = receipt.items
                .filter { !it.product.id.startsWith("other_") && it.product.categoryId.isNotBlank() }
                .associate { it.product.id to it.quantity }
            val newQtyMap = newItems
                .filter { !it.product.id.startsWith("other_") && it.product.categoryId.isNotBlank() }
                .associate { it.product.id to it.quantity }

            (oldQtyMap.keys + newQtyMap.keys).toSet().forEach { id ->
                val diff = (newQtyMap[id] ?: 0) - (oldQtyMap[id] ?: 0)
                when {
                    diff > 0 -> runCatching { productRepository.decrementStock(id, diff) }
                    diff < 0 -> runCatching { productRepository.incrementStock(id, -diff) }
                }
            }

            val updated = receipt.copy(items = newItems, total = newTotal, discount = discount, paymentMethod = paymentMethod)
            runCatching { repository.update(updated) }

            _currentReceipt.value = updated
            val updatedList = _receipts.value.map { if (it.id == updated.id) updated else it }
            _receipts.value = updatedList
            persistCache(updatedList)
            _stockVersion.value += 1
            _isSaving.value = false
        }
    }

    /** Restores stock for all items in the receipt, then deletes it from Supabase and local cache. */
    fun deleteReceipt(receipt: Receipt, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            receipt.items
                .filter { !it.product.id.startsWith("other_") && it.product.categoryId.isNotBlank() }
                .forEach { runCatching { productRepository.incrementStock(it.product.id, it.quantity) } }
            runCatching { repository.delete(receipt.id) }
            val updatedList = _receipts.value.filter { it.id != receipt.id }
            _receipts.value = updatedList
            persistCache(updatedList)
            if (_currentReceipt.value?.id == receipt.id) _currentReceipt.value = null
            _stockVersion.value += 1
            _isSaving.value = false
            onDone()
        }
    }

    private fun persistCache(receipts: List<Receipt>) {
        settings[KEY_RECEIPTS_CACHE] = json.encodeToString(receipts)
    }
}
