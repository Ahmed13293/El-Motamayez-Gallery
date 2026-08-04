package com.elmotamyez.gallery.data.repository

import com.elmotamyez.gallery.data.model.Attendance
import com.elmotamyez.gallery.data.remote.supabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order as SbOrder
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class UserSummary(val id: String, val name: String)

@Serializable
private data class AppUserRow(
    @SerialName("id")   val id: String,
    @SerialName("name") val name: String
)

@Serializable
private data class AttendanceRow(
    @SerialName("id")         val id: String,
    @SerialName("user_id")    val userId: String,
    @SerialName("user_name")  val userName: String,
    @SerialName("check_in")   val checkIn: String,
    @SerialName("check_out")  val checkOut: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
private data class AttendanceInsert(
    @SerialName("user_id")   val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("check_in")  val checkIn: String
)

@Serializable
private data class AttendanceManualInsert(
    @SerialName("user_id")   val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("check_in")  val checkIn: String,
    @SerialName("check_out") val checkOut: String? = null
)

@Serializable
private data class CheckOutUpdate(
    @SerialName("check_out") val checkOut: String
)

@Serializable
private data class AttendanceFullUpdate(
    @SerialName("check_in")  val checkIn: String,
    @SerialName("check_out") val checkOut: String? = null
)

class AttendanceRepository {

    suspend fun getUsers(): List<UserSummary> =
        supabaseClient.from("app_users")
            .select()
            .decodeList<AppUserRow>()
            .map { UserSummary(it.id, it.name) }

    suspend fun recordSignIn(userId: String, userName: String) {
        val now = Clock.System.now().toString()
        supabaseClient.from("attendance").insert(
            AttendanceInsert(userId = userId, userName = userName, checkIn = now)
        )
    }

    suspend fun recordSignOut(userId: String) {
        val now = Clock.System.now().toString()
        supabaseClient.from("attendance")
            .update(CheckOutUpdate(checkOut = now)) {
                filter {
                    eq("user_id", userId)
                    filter("check_out", FilterOperator.IS, null)
                }
            }
    }

    suspend fun insertManual(userId: String, userName: String, checkIn: String, checkOut: String?) {
        supabaseClient.from("attendance").insert(
            AttendanceManualInsert(userId = userId, userName = userName, checkIn = checkIn, checkOut = checkOut)
        )
    }

    suspend fun updateRecord(id: String, checkIn: String, checkOut: String?) {
        supabaseClient.from("attendance")
            .update(AttendanceFullUpdate(checkIn = checkIn, checkOut = checkOut)) {
                filter { eq("id", id) }
            }
    }

    suspend fun getAll(): List<Attendance> =
        supabaseClient.from("attendance")
            .select { order("check_in", SbOrder.DESCENDING) }
            .decodeList<AttendanceRow>()
            .map { it.toDomain() }

    private fun AttendanceRow.toDomain() = Attendance(
        id        = id,
        userId    = userId,
        userName  = userName,
        checkIn   = checkIn,
        checkOut  = checkOut,
        createdAt = createdAt
    )
}
