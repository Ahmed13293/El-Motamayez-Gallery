package com.elmotamyez.gallery.ui.screens.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.DailyReconciliation
import com.elmotamyez.gallery.data.model.Receipt
import com.elmotamyez.gallery.data.repository.DailyReconciliationRepository
import com.elmotamyez.gallery.data.repository.ProductRepository
import com.elmotamyez.gallery.data.repository.ProductVariantRepository
import com.elmotamyez.gallery.data.repository.ReceiptRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import com.elmotamyez.gallery.util.dateString
import com.elmotamyez.gallery.util.dateTimeString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KEY_RECEIPTS_CACHE       = "receipts_cache_json"
private const val KEY_LAST_SEEN_ORDER      = "last_seen_confirmed_order"

class ReceiptViewModel(
    private val repository: ReceiptRepository,
    private val productRepository: ProductRepository,
    private val variantRepository: ProductVariantRepository,
    private val reconciliationRepository: DailyReconciliationRepository
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

    private val _quotationSaving = MutableStateFlow(false)
    val quotationSaving: StateFlow<Boolean> = _quotationSaving.asStateFlow()

    private val _quotationSaved = MutableStateFlow(false)
    val quotationSaved: StateFlow<Boolean> = _quotationSaved.asStateFlow()
    fun resetQuotationSaved() { _quotationSaved.value = false }

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()
    fun clearDeleteError() { _deleteError.value = null }

    // Order confirmation states — drive CartScreen loading + error popup + navigation
    private val _orderSaving = MutableStateFlow(false)
    val orderSaving: StateFlow<Boolean> = _orderSaving.asStateFlow()

    private val _orderSaved = MutableStateFlow(false)
    val orderSaved: StateFlow<Boolean> = _orderSaved.asStateFlow()
    fun resetOrderSaved() { _orderSaved.value = false }

    private val _orderError = MutableStateFlow<String?>(null)
    val orderError: StateFlow<String?> = _orderError.asStateFlow()
    fun clearOrderError() { _orderError.value = null }

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
            // Merge: keep existing state, add any new date keys as expanded so new days aren't missed
            val current = _expandedDays.value.toMutableMap()
            dateKeys.forEach { key -> if (!current.containsKey(key)) current[key] = true }
            _expandedDays.value = current
        }
    }

    fun toggleDay(dateKey: String) {
        _expandedDays.value = _expandedDays.value.toMutableMap().also {
            it[dateKey] = !(it[dateKey] ?: false)
        }
    }

    // Unseen confirmed-receipt count — persisted across sessions via Settings
    private val _newReceiptsCount = MutableStateFlow(0)
    val newReceiptsCount: StateFlow<Int> = _newReceiptsCount.asStateFlow()

    private fun recomputeNewCount() {
        val lastSeen = settings.getInt(KEY_LAST_SEEN_ORDER, 0)
        _newReceiptsCount.value = _receipts.value.count { !it.isQuotation && it.orderNumber > lastSeen }
    }

    /** Called when the user opens the receipts tab — marks all current receipts as seen. */
    fun markReceiptsSeen() {
        val maxOrder = _receipts.value.filter { !it.isQuotation }.maxOfOrNull { it.orderNumber } ?: 0
        settings.putInt(KEY_LAST_SEEN_ORDER, maxOrder)
        _newReceiptsCount.value = 0
    }

    // Full history — seeded from local cache instantly, then refreshed from Supabase
    private val _receipts = MutableStateFlow<List<Receipt>>(emptyList())
    val receipts: StateFlow<List<Receipt>> = _receipts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _insertError = MutableStateFlow<String?>(null)
    val insertError: StateFlow<String?> = _insertError.asStateFlow()

    // Cash reconciliation — map of dateKey (YYYY-MM-DD) → saved reconciliation
    private val _reconciliations = MutableStateFlow<Map<String, DailyReconciliation>>(emptyMap())
    val reconciliations: StateFlow<Map<String, DailyReconciliation>> = _reconciliations.asStateFlow()

    private val _reconciliationSaving = MutableStateFlow(false)
    val reconciliationSaving: StateFlow<Boolean> = _reconciliationSaving.asStateFlow()

    init {
        // Show cached receipts immediately so the list isn't empty on launch
        val cached: String = settings[KEY_RECEIPTS_CACHE, ""]
        if (cached.isNotEmpty()) {
            runCatching {
                _receipts.value = json.decodeFromString<List<Receipt>>(cached)
                recomputeNewCount()
            }
        }
        // Then sync latest from Supabase in the background
        loadReceipts()
        loadReconciliations()
        // Keep both devices in sync — re-fetch from Supabase every 30 seconds
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                if (!_isLoading.value) loadReceipts(reportError = false)
            }
        }
    }

    fun loadReconciliations() {
        viewModelScope.launch {
            runCatching { reconciliationRepository.fetchAll() }
                .onSuccess { list ->
                    _reconciliations.value = list.associateBy { it.date }
                }
        }
    }

    fun saveReconciliation(date: String, actualCash: Double, username: String?) {
        viewModelScope.launch {
            _reconciliationSaving.value = true
            runCatching { reconciliationRepository.upsert(date, actualCash, username) }
                .onSuccess {
                    _reconciliations.value = _reconciliations.value.toMutableMap().also {
                        it[date] = DailyReconciliation(date = date, actualCash = actualCash, enteredBy = username)
                    }
                }
            _reconciliationSaving.value = false
        }
    }

    /** Reload all receipts from the cloud (called on init and on pull-to-refresh).
     *  [reportError] = false for silent background polls — don't overwrite the error banner
     *  when we already have cached data displayed. */
    fun loadReceipts(reportError: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.fetchAll() }
                .onSuccess { result ->
                    if (reportError) _loadError.value = result.firstError
                    val fresh = result.receipts
                    runCatching {
                        val freshIds = fresh.map { it.id }.toSet()
                        // Oldest date covered by this fetch — receipts older than this are historical
                        // and not part of the current window, so keep them (they can't have been deleted in this sync)
                        val oldestFreshDate = fresh.minOfOrNull { it.createdAt ?: "" } ?: ""
                        val localOnly = _receipts.value.filter { r ->
                            r.id !in freshIds &&
                            (r.pendingSave || (r.createdAt ?: "") < oldestFreshDate)
                        }
                        val merged = (fresh + localOnly).sortedByDescending { it.createdAt ?: "" }
                        _receipts.value = merged
                        recomputeNewCount()
                        persistCache(merged)
                    }.onFailure { _loadError.value = "merge: ${it.message}" }
                }
                .onFailure { e ->
                    // Only surface the error if caller asked AND we have no data to show
                    if (reportError || _receipts.value.isEmpty())
                        _loadError.value = e.message ?: e.toString()
                }
            _isLoading.value = false
            // Attempt to sync any receipts that failed to save previously
            syncPendingReceipts()
        }
    }

    /** Tries to push any locally-pending receipts to Supabase and decrement their stock.
     *  Quotation receipts are synced (insert) but stock is NOT decremented until confirmed. */
    private suspend fun syncPendingReceipts() {
        val pending = _receipts.value.filter { it.pendingSave }
        if (pending.isEmpty()) return
        for (receipt in pending) {
            val syncResult = runCatching { repository.insert(receipt) }
            if (!syncResult.isSuccess) {
                _insertError.value = syncResult.exceptionOrNull()?.message ?: syncResult.exceptionOrNull()?.toString()
            }
            val synced = syncResult.isSuccess
            if (synced) {
                if (!receipt.isQuotation) {
                    receipt.items
                        .filter { it.product.categoryId.isNotBlank() && !it.product.id.startsWith("other_") }
                        .forEach { cartItem ->
                            runCatching {
                                if (cartItem.variantId != null)
                                    variantRepository.decrementStock(cartItem.variantId, cartItem.quantity)
                                else
                                    productRepository.decrementStock(cartItem.product.id, cartItem.quantity)
                            }
                        }
                }
                _receipts.value = _receipts.value.map {
                    if (it.id == receipt.id) it.copy(pendingSave = false) else it
                }
                if (_currentReceipt.value?.id == receipt.id)
                    _currentReceipt.value = _currentReceipt.value?.copy(pendingSave = false)
                persistCache(_receipts.value)
                _stockVersion.value += 1
            }
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
        if (_orderSaving.value) return
        _orderSaving.value = true
        val isPaid = paymentMethod != "آجل"
        viewModelScope.launch {
            val tz       = TimeZone.currentSystemDefault()
            val instant  = Clock.System.now()
            val now      = instant.toLocalDateTime(tz)
            val offset   = tz.offsetAt(instant)          // e.g. +02:00
            val (year, month, day) = overrideDate ?: Triple(now.year, now.monthNumber, now.dayOfMonth)
            val todayPrefix = dateString(year, month, day)
            val localMax = _receipts.value
                .filter { it.createdAt?.startsWith(todayPrefix) == true }
                .maxOfOrNull { it.orderNumber } ?: 0
            val remoteMax = runCatching { repository.fetchTodayMax(todayPrefix) }.getOrElse { 0 }
            val nextNumber = maxOf(localMax, remoteMax) + 1
            val nowIso = if (overrideDate != null)
                "${todayPrefix}T12:00:00+00:00"
            else
                dateTimeString(now.year, now.monthNumber, now.dayOfMonth, now.hour, now.minute, now.second) + offset
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

            // Push to Supabase — 3 attempts, 2 s apart. Do NOT navigate until confirmed saved.
            var insertResult = runCatching { repository.insert(receipt) }
            if (!insertResult.isSuccess) { delay(2000); insertResult = runCatching { repository.insert(receipt) } }
            if (!insertResult.isSuccess) { delay(2000); insertResult = runCatching { repository.insert(receipt) } }

            if (insertResult.isSuccess) {
                val updated = _receipts.value + receipt
                _receipts.value = updated
                _currentReceipt.value = receipt
                persistCache(updated)
                items
                    .filter { it.product.categoryId.isNotBlank() && !it.product.id.startsWith("other_") }
                    .forEach { cartItem ->
                        runCatching {
                            if (cartItem.variantId != null)
                                variantRepository.decrementStock(cartItem.variantId, cartItem.quantity)
                            else
                                productRepository.decrementStock(cartItem.product.id, cartItem.quantity)
                        }
                    }
                _stockVersion.value += 1
                loadReceipts()
                _orderSaved.value = true
            } else {
                _orderError.value = "فشل حفظ الفاتورة بعد 3 محاولات. تحقق من الاتصال بالإنترنت وحاول مرة أخرى."
            }
            _orderSaving.value = false
        }
    }

    /** Saves a quotation receipt (isQuotation=true) without deducting stock or navigating. */
    fun saveQuotation(
        items: List<CartItem>,
        total: Double = 0.0,
        discount: Double = 0.0,
        paymentMethod: String = "كاش",
        customerPhone: String? = null,
        customerInfo: String? = null,
        username: String? = null
    ) {
        if (_quotationSaving.value) return
        _quotationSaving.value = true
        viewModelScope.launch {
            val tz      = TimeZone.currentSystemDefault()
            val instant = Clock.System.now()
            val now     = instant.toLocalDateTime(tz)
            val offset  = tz.offsetAt(instant)
            val todayPrefix = dateString(now.year, now.monthNumber, now.dayOfMonth)
            val localMax = _receipts.value
                .filter { it.createdAt?.startsWith(todayPrefix) == true }
                .maxOfOrNull { it.orderNumber } ?: 0
            val remoteMax = runCatching { repository.fetchTodayMax(todayPrefix) }.getOrElse { 0 }
            val nextNumber = maxOf(localMax, remoteMax) + 1
            val nowIso = dateTimeString(now.year, now.monthNumber, now.dayOfMonth, now.hour, now.minute, now.second) + offset
            val receipt = Receipt(
                id            = "${todayPrefix}-${nextNumber.toString().padStart(4, '0')}",
                orderNumber   = nextNumber,
                items         = items,
                total         = maxOf(0.0, items.sumOf { it.totalPrice } - discount),
                discount      = discount,
                paymentMethod = paymentMethod,
                isPaid        = false,
                createdAt     = nowIso,
                customerPhone = customerPhone.takeIf { !it.isNullOrBlank() },
                customerInfo  = customerInfo.takeIf  { !it.isNullOrBlank() },
                username      = username.takeIf      { !it.isNullOrBlank() },
                isQuotation   = true
            )
            val updated = _receipts.value + receipt
            _receipts.value = updated
            persistCache(updated)
            runCatching { repository.insert(receipt) }
                .onFailure { e -> _insertError.value = "فشل حفظ عرض السعر: ${e.message}" }
            loadReceipts()
            _quotationSaving.value = false
            _quotationSaved.value = true
        }
    }

    /** Confirms a quotation: marks is_quotation=false and deducts stock. */
    fun confirmQuotation(receipt: Receipt) {
        viewModelScope.launch {
            _isSaving.value = true
            runCatching { repository.confirmQuotation(receipt.id) }
                .onFailure { e -> _insertError.value = "فشل تأكيد عرض السعر: ${e.message}" }
            receipt.items
                .filter { it.product.categoryId.isNotBlank() && !it.product.id.startsWith("other_") }
                .forEach { cartItem ->
                    runCatching {
                        if (cartItem.variantId != null)
                            variantRepository.decrementStock(cartItem.variantId, cartItem.quantity)
                        else
                            productRepository.decrementStock(cartItem.product.id, cartItem.quantity)
                    }
                }
            _stockVersion.value += 1
            val confirmed = receipt.copy(isQuotation = false)
            val updatedList = _receipts.value.map { if (it.id == receipt.id) confirmed else it }
            _receipts.value = updatedList
            persistCache(updatedList)
            if (_currentReceipt.value?.id == receipt.id) _currentReceipt.value = confirmed
            _isSaving.value = false
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

            if (!receipt.isQuotation) {
                // Key on variantId when present so two variants of the same product don't merge
                data class StockKey(val productId: String, val variantId: String?)
                fun CartItem.stockKey() = StockKey(product.id, variantId)

                val oldMap = receipt.items
                    .filter { !it.product.id.startsWith("other_") && it.product.categoryId.isNotBlank() }
                    .associate { it.stockKey() to it.quantity }
                val newMap = newItems
                    .filter { !it.product.id.startsWith("other_") && it.product.categoryId.isNotBlank() }
                    .associate { it.stockKey() to it.quantity }

                (oldMap.keys + newMap.keys).toSet().forEach { key ->
                    val diff = (newMap[key] ?: 0) - (oldMap[key] ?: 0)
                    when {
                        diff > 0 -> runCatching {
                            if (key.variantId != null) variantRepository.decrementStock(key.variantId, diff)
                            else productRepository.decrementStock(key.productId, diff)
                        }
                        diff < 0 -> runCatching {
                            if (key.variantId != null) variantRepository.incrementStock(key.variantId, -diff)
                            else productRepository.incrementStock(key.productId, -diff)
                        }
                    }
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

    /** Soft-delete: moves receipt to trash and restores stock immediately.
     *  Pending receipts (never saved to Supabase) are just removed locally — no stock change. */
    fun deleteReceipt(receipt: Receipt, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val moved = if (receipt.pendingSave) {
                true // never reached Supabase; nothing to soft-delete there
            } else {
                val result = runCatching { repository.softDelete(receipt.id) }
                if (!result.isSuccess) _deleteError.value = result.exceptionOrNull()?.message ?: "فشل الحذف"
                result.isSuccess
            }
            if (moved) {
                if (!receipt.pendingSave && !receipt.isQuotation) {
                    val stockErrors = mutableListOf<String>()
                    receipt.items
                        .filter { !it.product.id.startsWith("other_") && it.product.categoryId.isNotBlank() }
                        .forEach { item ->
                            runCatching {
                                if (item.variantId != null)
                                    variantRepository.incrementStock(item.variantId, item.quantity)
                                else
                                    productRepository.incrementStock(item.product.id, item.quantity)
                            }.onFailure { e -> stockErrors += "${item.product.name}: ${e.message}" }
                        }
                    if (stockErrors.isNotEmpty()) {
                        _deleteError.value = "تم نقل الفاتورة للمحذوفات لكن فشل استعادة المخزون لبعض المنتجات:\n${stockErrors.joinToString("\n")}"
                    }
                    _stockVersion.value += 1
                }
                val updatedList = _receipts.value.filter { it.id != receipt.id }
                _receipts.value = updatedList
                persistCache(updatedList)
                if (_currentReceipt.value?.id == receipt.id) _currentReceipt.value = null
                onDone()
            }
            _isSaving.value = false
        }
    }

    private fun persistCache(receipts: List<Receipt>) {
        settings[KEY_RECEIPTS_CACHE] = json.encodeToString(receipts)
    }
}
