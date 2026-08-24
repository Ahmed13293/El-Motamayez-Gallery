package com.elmotamyez.gallery.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.Brand
import com.elmotamyez.gallery.data.model.Category
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.repository.ImageUploadRepository
import com.elmotamyez.gallery.data.repository.ProductRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class AdminUiState(
    val categories:          List<Category> = emptyList(),
    val brands:              List<Brand>    = emptyList(),
    val products:            List<Product>  = emptyList(),
    val isLoading:           Boolean        = false,
    val isUploadingImage:    Boolean        = false,
    val error:               String?        = null,
    val toast:               String?        = null,
    /** null = idle, (done, total) = fix-images in progress */
    val fixImagesProgress:   Pair<Int,Int>? = null
)

class AdminViewModel(
    private val repository: ProductRepository,
    private val imageRepo: ImageUploadRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    init { loadAll() }

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                repository.clearCache()
                val cats   = repository.getCategories()
                    .sortedBy { it.id.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }
                val brands = repository.getBrands()
                    .sortedBy { it.id.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }
                val prods  = repository.getProducts()
                _state.update { it.copy(categories = cats, brands = brands, products = prods, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toast = null) }
    fun clearError() = _state.update { it.copy(error = null) }

    // ── ID generator ──────────────────────────────────────────────────────────

    private fun newId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..10).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    // ── Categories ────────────────────────────────────────────────────────────

    fun addCategory(name: String) = viewModelScope.launch {
        runCatching { repository.insertCategory(newId(), name.trim()) }
            .onSuccess { loadAll(); toast("تم إضافة القسم") }
            .onFailure { _state.update { s -> s.copy(error = it.message) } }
    }

    fun editCategory(id: String, name: String) = viewModelScope.launch {
        runCatching { repository.updateCategory(id, name.trim()) }
            .onSuccess { loadAll(); toast("تم تعديل القسم") }
            .onFailure { _state.update { s -> s.copy(error = it.message) } }
    }

    fun deleteCategory(id: String) = viewModelScope.launch {
        runCatching { repository.deleteCategory(id) }
            .onSuccess { loadAll(); toast("تم حذف القسم") }
            .onFailure { _state.update { s -> s.copy(error = it.message) } }
    }

    // ── Brands ────────────────────────────────────────────────────────────────

    fun addBrand(name: String, categoryId: String, parentId: String?) = viewModelScope.launch {
        runCatching { repository.insertBrand(newId(), name.trim(), categoryId, parentId) }
            .onSuccess { loadAll(); toast("تم إضافة الفئة الفرعية") }
            .onFailure { _state.update { s -> s.copy(error = it.message) } }
    }

    fun editBrand(id: String, name: String, categoryId: String, parentId: String?) = viewModelScope.launch {
        runCatching { repository.updateBrand(id, name.trim(), categoryId, parentId) }
            .onSuccess { loadAll(); toast("تم تعديل الفئة الفرعية") }
            .onFailure { _state.update { s -> s.copy(error = it.message) } }
    }

    fun deleteBrand(id: String) = viewModelScope.launch {
        runCatching { repository.deleteBrand(id) }
            .onSuccess { loadAll(); toast("تم حذف الفئة الفرعية") }
            .onFailure { _state.update { s -> s.copy(error = it.message) } }
    }

    // ── Products ──────────────────────────────────────────────────────────────

    fun addProduct(name: String, price: Double, wholesalePrice: Double?, stock: Int, brandId: String, categoryId: String, imageUrls: List<String> = emptyList()) = viewModelScope.launch {
        runCatching { repository.insertProduct(newId(), name.trim(), price, wholesalePrice, stock, brandId, categoryId, imageUrls) }
            .onSuccess { loadAll(); toast("تم إضافة المنتج") }
            .onFailure { _state.update { s -> s.copy(error = it.message) } }
    }

    fun editProduct(id: String, name: String, price: Double, wholesalePrice: Double?, stock: Int, brandId: String, categoryId: String, imageUrls: List<String> = emptyList()) = viewModelScope.launch {
        runCatching { repository.updateProduct(id, name.trim(), price, wholesalePrice, stock, brandId, categoryId, imageUrls) }
            .onSuccess { loadAll(); toast("تم تعديل المنتج") }
            .onFailure { _state.update { s -> s.copy(error = it.message) } }
    }

    fun deleteProduct(id: String) = viewModelScope.launch {
        runCatching { repository.deleteProduct(id) }
            .onSuccess { loadAll(); toast("تم حذف المنتج") }
            .onFailure { _state.update { s -> s.copy(error = it.message) } }
    }

    /** Downloads [imageUrl], rotates 90° CW using [rotate], re-uploads, and returns the new URL via [onNewUrl]. */
    fun rotateProductImage(imageUrl: String, rotate: suspend (ByteArray) -> ByteArray, onNewUrl: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingImage = true) }
            runCatching {
                val http = HttpClient()
                val bytes = http.get(imageUrl).body<ByteArray>()
                http.close()
                val rotated = rotate(bytes)
                imageRepo.uploadProductImage(rotated)
            }
                .onSuccess { url -> onNewUrl(url) }
                .onFailure { _state.update { s -> s.copy(error = "فشل تدوير الصورة: ${it.message}") } }
            _state.update { it.copy(isUploadingImage = false) }
        }
    }

    fun uploadImage(bytes: ByteArray, onUrl: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingImage = true) }
            runCatching { imageRepo.uploadProductImage(bytes) }
                .onSuccess { url -> onUrl(url) }
                .onFailure { _state.update { s -> s.copy(error = "فشل رفع الصورة: ${it.message}") } }
            _state.update { it.copy(isUploadingImage = false) }
        }
    }

    /**
     * Downloads every product image, rotates landscape ones to portrait using [rotateIfLandscape],
     * re-uploads changed images, and updates the product record in Supabase.
     */
    fun fixExistingImages(rotateIfLandscape: suspend (ByteArray) -> ByteArray) {
        viewModelScope.launch {
            val products = _state.value.products.filter { it.displayImages.isNotEmpty() }
            _state.update { it.copy(fixImagesProgress = 0 to products.size) }
            val http = HttpClient()
            var fixed = 0
            for (product in products) {
                val newUrls = product.displayImages.map { url ->
                    val bytes = runCatching { http.get(url).body<ByteArray>() }.getOrNull() ?: return@map url
                    val rotated = runCatching { rotateIfLandscape(bytes) }.getOrNull() ?: return@map url
                    if (rotated.size == bytes.size && rotated.contentEquals(bytes)) return@map url
                    runCatching { imageRepo.uploadProductImage(rotated) }.getOrElse { url }
                }
                if (newUrls != product.displayImages) {
                    runCatching {
                        repository.updateProduct(
                            product.id, product.name, product.price, product.wholesalePrice,
                            product.stock, product.brandId, product.categoryId, newUrls
                        )
                    }
                }
                fixed++
                _state.update { it.copy(fixImagesProgress = fixed to products.size) }
            }
            http.close()
            _state.update { it.copy(fixImagesProgress = null) }
            toast("تم تصحيح صور المنتجات")
        }
    }

    private fun toast(msg: String) = _state.update { it.copy(toast = msg) }
}
