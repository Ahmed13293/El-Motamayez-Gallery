package com.elmotamyez.gallery.data.repository

import com.elmotamyez.gallery.data.model.Brand
import com.elmotamyez.gallery.data.model.Category
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.data.remote.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ── Update payloads (only the fields being changed) ───────────────────────────

@Serializable
private data class CategoryUpdate(val name: String)

@Serializable
private data class BrandUpdate(
    val name: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("parent_id")   val parentId: String?
)

@Serializable
private data class ProductUpdate(
    val name: String,
    val price: Double,
    @SerialName("wholesale_price") val wholesalePrice: Double?,
    val stock: Int,
    @SerialName("brand_id")    val brandId: String,
    @SerialName("category_id") val categoryId: String
)

// ─────────────────────────────────────────────────────────────────────────────

class ProductRepository {

    private var cachedCategories: List<Category>? = null
    private var cachedBrands:     List<Brand>?    = null
    private var cachedProducts:   List<Product>?  = null

    fun clearCache() {
        cachedCategories = null
        cachedBrands     = null
        cachedProducts   = null
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    suspend fun getCategories(): List<Category> =
        cachedCategories ?: supabaseClient.from("categories")
            .select().decodeList<Category>()
            .sortedBy { it.id.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }
            .also { cachedCategories = it }

    suspend fun getBrands(): List<Brand> =
        cachedBrands ?: supabaseClient.from("brands")
            .select().decodeList<Brand>()
            .sortedBy { it.id.filter { c -> c.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }
            .also { cachedBrands = it }

    suspend fun getProducts(): List<Product> =
        cachedProducts ?: supabaseClient.from("products")
            .select().decodeList<Product>().also { cachedProducts = it }

    // ── CATEGORIES ────────────────────────────────────────────────────────────

    suspend fun insertCategory(id: String, name: String) {
        supabaseClient.from("categories").insert(buildJsonObject {
            put("id", id); put("name", name)
        })
        clearCache()
    }

    suspend fun updateCategory(id: String, name: String) {
        supabaseClient.from("categories")
            .update(CategoryUpdate(name)) { filter { eq("id", id) } }
        clearCache()
    }

    suspend fun deleteCategory(id: String) {
        supabaseClient.from("categories").delete { filter { eq("id", id) } }
        clearCache()
    }

    // ── BRANDS ────────────────────────────────────────────────────────────────

    suspend fun insertBrand(id: String, name: String, categoryId: String, parentId: String?) {
        supabaseClient.from("brands").insert(buildJsonObject {
            put("id", id); put("name", name); put("category_id", categoryId)
            if (parentId != null) put("parent_id", parentId) else put("parent_id", null as String?)
        })
        clearCache()
    }

    suspend fun updateBrand(id: String, name: String, categoryId: String, parentId: String?) {
        supabaseClient.from("brands")
            .update(BrandUpdate(name, categoryId, parentId)) { filter { eq("id", id) } }
        clearCache()
    }

    suspend fun deleteBrand(id: String) {
        supabaseClient.from("brands").delete { filter { eq("id", id) } }
        clearCache()
    }

    // ── PRODUCTS ──────────────────────────────────────────────────────────────

    suspend fun insertProduct(id: String, name: String, price: Double, wholesalePrice: Double?, stock: Int, brandId: String, categoryId: String) {
        supabaseClient.from("products").insert(buildJsonObject {
            put("id", id); put("name", name); put("price", price); put("stock", stock)
            put("brand_id", brandId); put("category_id", categoryId)
            if (wholesalePrice != null) put("wholesale_price", wholesalePrice)
        })
        clearCache()
    }

    suspend fun updateProduct(id: String, name: String, price: Double, wholesalePrice: Double?, stock: Int, brandId: String, categoryId: String) {
        supabaseClient.from("products")
            .update(ProductUpdate(name, price, wholesalePrice, stock, brandId, categoryId)) { filter { eq("id", id) } }
        clearCache()
    }

    suspend fun deleteProduct(id: String) {
        supabaseClient.from("products").delete { filter { eq("id", id) } }
        clearCache()
    }

    suspend fun decrementStock(productId: String, quantity: Int) {
        // Fetch current stock, subtract, then update
        val current = supabaseClient.from("products")
            .select { filter { eq("id", productId) } }
            .decodeList<Product>()
            .firstOrNull()?.stock ?: return
        val newStock = (current - quantity).coerceAtLeast(0)
        supabaseClient.from("products")
            .update(buildJsonObject { put("stock", newStock) }) {
                filter { eq("id", productId) }
            }
        clearCache()
    }

}
