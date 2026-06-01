package com.rork.workpulse.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── Wire DTOs mirroring the Med Lion HR server JSON ──

@Serializable
data class GeoPointDto(val latitude: Double, val longitude: Double)

@Serializable
data class WorkSiteDto(val name: String, val center: GeoPointDto, val radiusMeters: Double)

@Serializable
data class EmployeeDto(
    val id: String,
    val employeeId: String,
    val password: String,
    val name: String,
    val role: String,
    val avatarColor: Long,
    val baseSalary: Double,
    val status: String,
)

@Serializable
data class AttendanceLogDto(
    val id: String,
    val employeeId: String,
    val checkInMillis: Long,
    val checkOutMillis: Long? = null,
    val location: GeoPointDto,
    val distanceFromSite: Double,
    val verified: Boolean,
)

@Serializable
data class LeaveRequestDto(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val startMillis: Long,
    val endMillis: Long,
    val reason: String,
    val type: String,
    val status: String,
    val payType: String? = null,
    val requestedAtMillis: Long,
)

@Serializable
data class HealthDto(val ok: Boolean, val name: String = "", val time: Long = 0)

@Serializable
data class NewEmployeeDto(
    val employeeId: String,
    val password: String,
    val name: String,
    val role: String,
    val baseSalary: Double,
)

@Serializable
data class CheckInBody(val employeeId: String, val location: GeoPointDto)

@Serializable
data class CheckOutBody(val employeeId: String)

@Serializable
data class LeaveBody(
    val employeeId: String,
    val startMillis: Long,
    val endMillis: Long,
    val reason: String,
    val type: String,
)

@Serializable
data class LeaveDecisionBody(val status: String, val payType: String?)

@Serializable
data class AttendanceEditBody(val checkInMillis: Long, val checkOutMillis: Long?)

@Serializable
data class WorksiteBody(val name: String, val lat: Double, val lon: Double, val radius: Double)

@Serializable
data class PasswordBody(val password: String)

@Serializable
data class EmployeePasswordBody(val currentPassword: String, val newPassword: String)

/**
 * Thin Ktor wrapper over the Med Lion HR REST API. All calls are suspend and
 * throw on network failure — callers decide whether to fall back to local mode.
 */
class ServerClient(private val baseUrl: String) {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(jsonParser)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 4000
            connectTimeoutMillis = 3000
            socketTimeoutMillis = 4000
        }
    }

    private fun url(path: String) = "$baseUrl$path"

    suspend fun health(): Boolean = try {
        val res = client.get(url("/api/health"))
        res.status.isSuccess()
    } catch (_: Throwable) {
        false
    }

    suspend fun employees(): List<EmployeeDto> =
        jsonParser.decodeFromString(client.get(url("/api/employees")).bodyAsText())

    suspend fun attendance(): List<AttendanceLogDto> =
        jsonParser.decodeFromString(client.get(url("/api/attendance")).bodyAsText())

    suspend fun leaves(): List<LeaveRequestDto> =
        jsonParser.decodeFromString(client.get(url("/api/leaves")).bodyAsText())

    suspend fun worksite(): WorkSiteDto =
        jsonParser.decodeFromString(client.get(url("/api/worksite")).bodyAsText())

    suspend fun checkIn(employeeId: String, lat: Double, lon: Double) {
        client.post(url("/api/attendance/checkin")) {
            contentType(ContentType.Application.Json)
            setBody(CheckInBody(employeeId, GeoPointDto(lat, lon)))
        }
    }

    suspend fun checkOut(employeeId: String) {
        client.post(url("/api/attendance/checkout")) {
            contentType(ContentType.Application.Json)
            setBody(CheckOutBody(employeeId))
        }
    }

    suspend fun submitLeave(body: LeaveBody) {
        client.post(url("/api/leaves")) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun decideLeave(id: String, status: String, payType: String?) {
        client.put(url("/api/leaves/$id")) {
            contentType(ContentType.Application.Json)
            setBody(LeaveDecisionBody(status, payType))
        }
    }

    suspend fun editAttendance(id: String, checkInMillis: Long, checkOutMillis: Long?) {
        client.put(url("/api/attendance/$id")) {
            contentType(ContentType.Application.Json)
            setBody(AttendanceEditBody(checkInMillis, checkOutMillis))
        }
    }

    suspend fun createEmployee(body: NewEmployeeDto) {
        client.post(url("/api/employees")) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun deleteEmployee(internalId: String) {
        client.delete(url("/api/employees/$internalId"))
    }

    suspend fun changeEmployeePassword(internalId: String, current: String, next: String) {
        client.post(url("/api/employees/$internalId/change-password")) {
            contentType(ContentType.Application.Json)
            setBody(EmployeePasswordBody(current, next))
        }
    }

    suspend fun changeAdminPassword(password: String) {
        client.post(url("/api/admin/change-password")) {
            contentType(ContentType.Application.Json)
            setBody(PasswordBody(password))
        }
    }

    suspend fun updateWorksite(name: String, lat: Double, lon: Double, radius: Double) {
        client.put(url("/api/worksite")) {
            contentType(ContentType.Application.Json)
            setBody(WorksiteBody(name, lat, lon, radius))
        }
    }

    fun close() = client.close()
}
