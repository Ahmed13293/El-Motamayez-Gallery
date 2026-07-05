package com.elmotamyez.gallery.data.repository

import com.elmotamyez.gallery.data.model.User
import com.elmotamyez.gallery.data.model.UserRole
import com.elmotamyez.gallery.data.remote.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
private data class UserRow(
    val id: String,
    val username: String,
    val password: String,
    val role: String,
    val name: String
)

class AuthRepository {

    /** Returns the matching [User] or null if credentials are wrong. */
    suspend fun login(username: String, password: String): User? {
        val rows = supabaseClient
            .from("app_users")
            .select {
                filter {
                    eq("username", username)
                    eq("password", password)
                }
            }
            .decodeList<UserRow>()

        val row = rows.firstOrNull() ?: return null
        return User(
            id       = row.id,
            username = row.username,
            name     = row.name,
            role     = if (row.role == "admin") UserRole.ADMIN else UserRole.USER
        )
    }
}
