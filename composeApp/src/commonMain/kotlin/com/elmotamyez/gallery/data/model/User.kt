package com.elmotamyez.gallery.data.model

enum class UserRole { ADMIN, USER }

data class User(
    val id: String,
    val username: String,
    val name: String,
    val role: UserRole
)
