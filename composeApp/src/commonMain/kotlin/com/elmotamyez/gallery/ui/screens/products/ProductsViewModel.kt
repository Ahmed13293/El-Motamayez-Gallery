package com.elmotamyez.gallery.ui.screens.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.Brand
import com.elmotamyez.gallery.data.model.Category
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductsUiState(
    val categories: List<Category> = emptyList(),
    val brands: List<Brand> = emptyList(),
    val allProducts: List<Product> = emptyList(),   // full catalogue (for best-sellers)
    val products: List<Product> = emptyList(),       // filtered view
    val selectedCategoryId: String? = null,
    val selectedBrandId: String? = null,             // Level 2 selection
    val selectedSubBrandId: String? = null,          // Level 3 selection
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProductsViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    private var allProductsCache: List<Product> = emptyList()

    init { loadData() }

    // ── Public API ────────────────────────────────────────────────────────────

    fun selectCategory(categoryId: String) {
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                selectedBrandId    = null,
                selectedSubBrandId = null,
                searchQuery        = "",
                products           = filtered(categoryId, null, null, "")
            )
        }
    }

    fun selectBrand(brandId: String?) {
        val catId = _uiState.value.selectedCategoryId ?: return
        _uiState.update {
            it.copy(
                selectedBrandId    = brandId,
                selectedSubBrandId = null,   // reset sub-brand when brand changes
                searchQuery        = "",
                products           = filtered(catId, brandId, null, "")
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
                products           = filtered(catId, s.selectedBrandId, subBrandId, "")
            )
        }
    }

    fun search(query: String) {
        val s = _uiState.value
        // Keep selected category when searching; clear brand filters
        val catId   = s.selectedCategoryId
        val brandId = if (query.isBlank()) s.selectedBrandId    else null
        val subId   = if (query.isBlank()) s.selectedSubBrandId else null
        _uiState.update {
            it.copy(
                searchQuery = query,
                products    = filtered(catId, brandId, subId, query)
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
                products           = filtered(null, null, null, "")
            )
        }
    }

    fun retry() = loadData()

    fun refreshProducts() {
        viewModelScope.launch {
            try {
                repository.clearCache()
                allProductsCache = repository.getProducts()
                val s = _uiState.value
                _uiState.update {
                    it.copy(
                        allProducts = allProductsCache,
                        products    = filtered(s.selectedCategoryId, s.selectedBrandId, s.selectedSubBrandId, s.searchQuery)
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
                val categories = repository.getCategories()
                val brands     = repository.getBrands()
                allProductsCache = repository.getProducts()

                val firstCatId = categories.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        categories         = categories,
                        brands             = brands,
                        allProducts        = allProductsCache,
                        selectedCategoryId = firstCatId,
                        selectedBrandId    = null,
                        selectedSubBrandId = null,
                        searchQuery        = "",
                        products           = filtered(firstCatId, null, null, ""),
                        isLoading          = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun filtered(
        categoryId:  String?,
        brandId:     String?,
        subBrandId:  String?,
        query:       String
    ): List<Product> = allProductsCache.filter { p ->
        (categoryId == null || p.categoryId == categoryId) &&
        (brandId    == null || p.brandId    == brandId    || p.brandId == subBrandId) &&
        (subBrandId == null || p.brandId    == subBrandId) &&
        (query.isBlank()    || p.name.contains(query, ignoreCase = true))
    }
}
