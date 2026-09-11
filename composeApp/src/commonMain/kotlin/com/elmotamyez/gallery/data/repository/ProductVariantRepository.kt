package com.elmotamyez.gallery.data.repository

import com.elmotamyez.gallery.data.model.ProductVariant
import com.elmotamyez.gallery.data.remote.supabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class VariantInsert(
    @SerialName("product_id") val productId: String,
    val name: String,
    val stock: Int
)

@Serializable
private data class VariantUpdate(val name: String, val stock: Int)

class ProductVariantRepository {

    suspend fun fetchAll(): List<ProductVariant> =
        supabaseClient.from("product_variants")
            .select { order("created_at", Order.ASCENDING) }
            .decodeList()

    suspend fun fetchForProduct(productId: String): List<ProductVariant> =
        supabaseClient.from("product_variants")
            .select { filter { eq("product_id", productId) }; order("created_at", Order.ASCENDING) }
            .decodeList()

    suspend fun insert(productId: String, name: String, stock: Int): ProductVariant =
        supabaseClient.from("product_variants")
            .insert(VariantInsert(productId, name, stock)) { select() }
            .decodeSingle()

    suspend fun update(id: String, name: String, stock: Int) {
        supabaseClient.from("product_variants")
            .update(VariantUpdate(name, stock)) { filter { eq("id", id) } }
    }

    suspend fun updateStock(id: String, stock: Int) {
        supabaseClient.from("product_variants")
            .update(mapOf("stock" to stock)) { filter { eq("id", id) } }
    }

    suspend fun delete(id: String) {
        supabaseClient.from("product_variants")
            .delete { filter { eq("id", id) } }
    }
}
