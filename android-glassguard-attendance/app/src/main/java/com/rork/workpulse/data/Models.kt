package com.rork.workpulse.data

/** App role determines which experience is shown. */
enum class Role { EMPLOYEE, ADMIN }

/** Live presence of an employee. */
enum class PresenceStatus { CHECKED_IN, CHECKED_OUT, ON_LEAVE }

enum class LeaveStatus { PENDING, APPROVED, REJECTED }

/** Whether an approved leave is paid. Drives the salary engine. */
enum class LeavePayType { WITH_PAY, WITHOUT_PAY }

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

/** The geofenced worksite check-ins are validated against. */
data class WorkSite(
    val name: String,
    val center: GeoPoint,
    val radiusMeters: Double,
)

/** Admin credentials container. */
data class AdminCredentials(
    val password: String = "Yashwant@2000",
)

/** DTO for creating a new employee from the admin panel. */
data class NewEmployeeRequest(
    val employeeId: String,
    val password: String,
    val name: String,
    val role: String,
    val baseSalary: Double,
)

data class Employee(
    val id: String,
    val employeeId: String,
    val password: String,
    val name: String,
    val role: String,
    val avatarColor: Long,
    val baseSalary: Double,
    val status: PresenceStatus = PresenceStatus.CHECKED_OUT,
)

/** A single check-in / check-out punch with GPS provenance. */
data class AttendanceLog(
    val id: String,
    val employeeId: String,
    val checkInMillis: Long,
    val checkOutMillis: Long? = null,
    val location: GeoPoint,
    val distanceFromSite: Double,
    val verified: Boolean,
) {
    val isOpen: Boolean get() = checkOutMillis == null

    /** Worked duration in milliseconds (live until checkout). */
    fun durationMillis(nowMillis: Long): Long =
        (checkOutMillis ?: nowMillis) - checkInMillis
}

data class LeaveRequest(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val startMillis: Long,
    val endMillis: Long,
    val reason: String,
    val type: String,
    val status: LeaveStatus = LeaveStatus.PENDING,
    val payType: LeavePayType? = null,
    val requestedAtMillis: Long,
) {
    val days: Int
        get() {
            val dayMs = 24L * 60 * 60 * 1000
            return ((endMillis - startMillis) / dayMs + 1).toInt().coerceAtLeast(1)
        }
}

/** Output of the automated salary engine for one employee for the month. */
data class SalaryBreakdown(
    val employeeId: String,
    val employeeName: String,
    val baseSalary: Double,
    val totalWorkingDays: Int,
    val daysWorked: Double,
    val paidLeaveDays: Int,
    val unpaidLeaveDays: Int,
    val absentDays: Double,
    val perDayRate: Double,
    val grossEarned: Double,
    val deductions: Double,
    val netPayable: Double,
    val dailyWorkingHours: Double = 8.0,
    val month: Int,
    val year: Int,
)

/** 12-month aggregated earnings summary for one employee. */
data class YearlySummary(
    val employeeId: String,
    val employeeName: String,
    val baseSalary: Double,
    val monthlyNet: List<Double>,   // Jan..Dec
    val totalNet: Double,
)
