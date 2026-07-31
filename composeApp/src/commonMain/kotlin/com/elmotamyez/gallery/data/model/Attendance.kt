package com.elmotamyez.gallery.data.model

import kotlinx.datetime.Instant

data class Attendance(
    val id: String,
    val userId: String,
    val userName: String,
    val checkIn: String,
    val checkOut: String?,
    val createdAt: String?
) {
    val hoursWorked: Double?
        get() {
            val out = checkOut ?: return null
            return try {
                val inMs  = Instant.parse(checkIn).toEpochMilliseconds()
                val outMs = Instant.parse(out).toEpochMilliseconds()
                (outMs - inMs) / 3_600_000.0
            } catch (_: Exception) { null }
        }
}
