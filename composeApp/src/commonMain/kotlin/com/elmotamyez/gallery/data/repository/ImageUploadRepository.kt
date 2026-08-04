package com.elmotamyez.gallery.data.repository

import com.elmotamyez.gallery.data.remote.supabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.datetime.Clock

private const val BUCKET = "product-images"

class ImageUploadRepository {

    suspend fun uploadProductImage(bytes: ByteArray): String {
        val filename = "${Clock.System.now().toEpochMilliseconds()}.jpg"
        supabaseClient.storage.from(BUCKET).upload(filename, bytes) {
            upsert = true
        }
        return supabaseClient.storage.from(BUCKET).publicUrl(filename)
    }
}
