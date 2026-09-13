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
import com.elmotamyez.gallery.util.arabicContains
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
        newImageBytes: ByteArray? = null,
        variantStocks: Map<String, Int> = emptyMap()
    ) {
        viewModelScope.launch {
            val imageUrls = if (newImageBytes != null) {
                val url = runCatching { imageRepo.uploadProductImage(newImageBytes) }.getOrNull()
                // Replace the first image (not prepend) so rotating/replacing doesn't add a duplicate
                if (url != null) listOf(url) + product.displayImages.drop(1) else product.displayImages
            } else product.displayImages
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
            }
            // Update each variant's stock independently
            variantStocks.forEach { (variantId, stock) ->
                val variant = _uiState.value.variantsMap[product.id]?.find { it.id == variantId }
                if (variant != null) {
                    runCatching { variantRepository.update(variantId, variant.name, stock) }
                }
            }
            refreshProducts()
        }
    }

    fun refreshProducts() {
        viewModelScope.launch {
            try {
                repository.clearCache()
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
