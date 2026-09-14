package com.elmotamyez.gallery.ui.screens.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.Brand
import com.elmotamyez.gallery.data.model.Category
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.model.ProductVariant
import com.elmotamyez.gallery.data.repository.ImageUploadRepository
import com.elmotamyez.gallery.data.repository.ProductRepository
import com.elmotamyez.gallery.data.repository.ProductVariantRepository
import com.elmotamyez.gallery.ui.model.PendingImage
import com.elmotamyez.gallery.util.arabicContains
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductsUiState(
    val categories: List<Category> = emptyList(),
    val brands: List<Brand> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val products: List<Product> = emptyList(),
    val variantsMap: Map<String, List<ProductVariant>> = emptyMap(), // productId → variants
    val selectedCategoryId: String? = null,
    val selectedBrandId: String? = null,
    val selectedSubBrandId: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProductsViewModel(
    private val repository: ProductRepository,
    private val variantRepository: ProductVariantRepository,
    private val imageRepo: ImageUploadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    // Persistent search history
    private val settings = Settings()
    private val historyKey = "search_history_v2"

    private val _searchHistory = MutableStateFlow(
        settings.getString(historyKey, "")
            .split("\n")
            .filter { it.isNotBlank() }
    )
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    fun addToSearchHistory(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        val updated = (listOf(q) + _searchHistory.value.filter { it != q }).take(10)
        _searchHistory.value = updated
        settings[historyKey] = updated.joinToString("\n")
    }

    init {
        loadData()
        // Re-fetch whenever AdminViewModel clears the product cache (e.g. after editing a product image)
        viewModelScope.launch {
            repository.modifiedVersion.drop(1).collect { refreshProducts() }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun selectCategory(categoryId: String) {
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                selectedBrandId    = null,
                selectedSubBrandId = null,
                searchQuery        = "",
                products           = filtered(it.allProducts, categoryId, null, null, "")
            )
        }
    }

    fun selectBrand(brandId: String?) {
        val catId = _uiState.value.selectedCategoryId ?: return
        _uiState.update {
            it.copy(
                selectedBrandId    = brandId,
                selectedSubBrandId = null,
                searchQuery        = "",
                products           = filtered(it.allProducts, catId, brandId, null, "")
            )
        }
    }

    fun selectSubBrand(subBrandId: String?) {
        val s = _uiState.value
        val catId = s.selectedCategoryId ?: return
        _uiState.update {
            it.copy(
                selectedSubBrandId = subBrandId,
                searchQuery        = "",
                products           = filtered(it.allProducts, catId, s.selectedBrandId, subBrandId, "")
            )
        }
    }

    fun search(query: String) {
        val s = _uiState.value
        val catId   = if (query.isBlank()) s.selectedCategoryId else null
        val brandId = if (query.isBlank()) s.selectedBrandId    else null
        val subId   = if (query.isBlank()) s.selectedSubBrandId else null
        _uiState.update {
            it.copy(
                searchQuery = query,
                products    = filtered(it.allProducts, catId, brandId, subId, query)
            )
        }
    }

    fun selectAllCategories() {
        _uiState.update {
            it.copy(
                selectedCategoryId = null,
                selectedBrandId    = null,
                selectedSubBrandId = null,
                searchQuery        = "",
                products           = filtered(it.allProducts, null, null, null, "")
            )
        }
    }

    fun retry() = loadData()

    fun quickEditProduct(
        product: Product,
        newPrice: Double,
        newWholesalePrice: Double?,
        newStock: Int,
        images: List<PendingImage> = product.displayImages.map { PendingImage.Remote(it) },
        variantStocks: Map<String, Int> = emptyMap()
    ) {
        viewModelScope.launch {
            // Upload any local images; keep remote URLs as-is — order is preserved
            val imageUrls = images.mapNotNull { img ->
                when (img) {
                    is PendingImage.Remote -> img.url
                    is PendingImage.Local  -> runCatching { imageRepo.uploadProductImage(img.bytes) }.getOrNull()
                }
            }
            runCatching {
                repository.updateProduct(
                    id             = product.id,
                    name           = product.name,
                    price          = newPrice,
                    wholesalePrice = newWholesalePrice,
                    stock          = newStock,
                    brandId        = product.brandId,
                    categoryId     = product.categoryId,
                    imageUrls      = imageUrls,
                    barcode        = product.barcode
                )
            }.onSuccess {
                // Patch local state immediately so the UI reflects the change
                val updated = product.copy(price = newPrice, wholesalePrice = newWholesalePrice, stock = newStock, imageUrls = imageUrls)
                val newAll  = _uiState.value.allProducts.map { if (it.id == product.id) updated else it }
                val s = _uiState.value
                _uiState.update {
                    it.copy(
                        allProducts = newAll,
                        products    = filtered(newAll, s.selectedCategoryId, s.selectedBrandId, s.selectedSubBrandId, s.searchQuery)
                    )
                }
            }
            // Update each variant's stock independently
            variantStocks.forEach { (variantId, stock) ->
                val variant = _uiState.value.variantsMap[product.id]?.find { it.id == variantId }
                if (variant != null) {
                    runCatching { variantRepository.update(variantId, variant.name, stock) }
                }
            }
            // Patch variant stocks in local state
            if (variantStocks.isNotEmpty()) {
                val updatedVariants = _uiState.value.variantsMap[product.id]?.map { v ->
                    variantStocks[v.id]?.let { s -> v.copy(stock = s) } ?: v
                }
                if (updatedVariants != null) {
                    _uiState.update { it.copy(variantsMap = it.variantsMap + (product.id to updatedVariants)) }
                }
            }
            // No refreshProducts() here — updateProduct() already clears the cache and bumps
            // modifiedVersion, which the observer below picks up and calls refreshProducts() once.
        }
    }

    fun refreshProducts() {
        viewModelScope.launch {
            try {
                // Do NOT call clearCache() here — updateProduct() already did it, and calling
                // it again would re-increment modifiedVersion, causing an infinite observer loop.
                val newProducts = repository.getProducts()
                val variantsMap = variantRepository.fetchAll().groupBy { it.productId }
                val s = _uiState.value
                _uiState.update {
                    it.copy(
                        allProducts = newProducts,
                        variantsMap = variantsMap,
                        products    = filtered(newProducts, s.selectedCategoryId, s.selectedBrandId, s.selectedSubBrandId, s.searchQuery)
                    )
                }
            } catch (_: Exception) {}
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val categories  = repository.getCategories()
                val brands      = repository.getBrands()
                val allProducts = repository.getProducts()
                val variantsMap = variantRepository.fetchAll().groupBy { it.productId }

                val firstCatId = categories.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        categories         = categories,
                        brands             = brands,
                        allProducts        = allProducts,
                        variantsMap        = variantsMap,
                        selectedCategoryId = firstCatId,
                        selectedBrandId    = null,
                        selectedSubBrandId = null,
                        searchQuery        = "",
                        products           = filtered(allProducts, firstCatId, null, null, ""),
                        isLoading          = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun filtered(
        source:      List<Product>,
        categoryId:  String?,
        brandId:     String?,
        subBrandId:  String?,
        query:       String
    ): List<Product> = source.filter { p ->
        (categoryId == null || p.categoryId == categoryId) &&
        (brandId    == null || p.brandId    == brandId    || p.brandId == subBrandId) &&
        (subBrandId == null || p.brandId    == subBrandId) &&
        (query.isBlank() || arabicContains(p.name, query))
    }
}
