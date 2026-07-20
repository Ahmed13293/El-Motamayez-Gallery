package com.elmotamyez.gallery.data.repository

import com.elmotamyez.gallery.data.remote.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class PushTokenRepository {
    @Serializable
    private data class PushTokenRow(
        val token: String,
        val platform: String,
        @SerialName("device_id") val deviceId: String,
    )

    suspend fun upsertToken(token: String, platform: String, deviceId: String) {
        runCatching {
            supabaseClient.from("push_tokens").upsert(PushTokenRow(token, platform, deviceId)) {
                onConflict = "device_id"
            }
        }
    }
}
