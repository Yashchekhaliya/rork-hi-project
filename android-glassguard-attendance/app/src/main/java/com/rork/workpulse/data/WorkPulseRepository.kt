package com.rork.workpulse.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/**
 * In-memory real-time store backing the whole app. Exposes [StateFlow]s that every
 * screen observes, so a check-in by an employee instantly reflects on the admin
 * global dashboard — mimicking a live server / realtime database.
 *
 * Swap the mutators for Supabase / Firebase calls to go fully online; the public
 * surface (flows + suspend-friendly methods) is designed to stay identical.
 */
object WorkPulseRepository {

    /**
     * Optional bridge to a remote server. When set (by [ServerSync]), local mutations
     * are mirrored to the PC server. Null means pure offline mode.
     */
    interface SyncHook {
        fun onCheckIn(employeeId: String, lat: Double, lon: Double)
        fun onCheckOut(employeeId: String)
        fun onSubmitLeave(employeeId: String, start: Long, end: Long, reason: String, type: String)
        fun onDecideLeave(leaveId: String, status: String, payType: String?)
        fun onEditAttendance(logId: String, checkInMillis: Long, checkOutMillis: Long?)
        fun onCreateEmployee(employeeId: String, password: String, name: String, role: String, baseSalary: Double)
        fun onDeleteEmployee(internalId: String)
        fun onChangeEmployeePassword(employeeIdLogin: String, current: String, next: String)
        fun onChangeAdminPassword(password: String)
        fun onUpdateWorksite(name: String, lat: Double, lon: Double, radius: Double)
    }

    /** Set by ServerSync when a server URL is configured. */
    var syncHook: SyncHook? = null

    /** Replace local flows with a snapshot pulled from the server. */
    fun applyServerSnapshot(
        employees: List<Employee>,
        logs: List<AttendanceLog>,
        leaves: List<LeaveRequest>,
        workSite: WorkSite,
    ) {
        _employees.value = employees
        _logs.value = logs
        _leaves.value = leaves
        this.workSite = workSite
    }

    /** Standard full-day working hours. Partial days earn proportional credit. */
    const val DAILY_WORKING_HOURS = 8.0

    /** Default admin password. */
    var adminPassword: String = "Yashwant@2000"
        private set

    fun updateAdminPassword(newPassword: String): Boolean {
        if (newPassword.length < 4) return false
        adminPassword = newPassword
        syncHook?.onChangeAdminPassword(newPassword)
        return true
    }

    fun verifyAdminPassword(candidate: String): Boolean = candidate == adminPassword

    /** The geofenced office — Sonorous, Vapi, Gujarat. */
    var workSite = WorkSite(
        name = "Sonorous — Vapi",
        center = GeoPoint(20.3734, 72.9141),
        radiusMeters = 1000.0,
    )
        private set

    fun updateWorkSite(name: String, lat: Double, lon: Double, radius: Double) {
        workSite = WorkSite(name = name, center = GeoPoint(lat, lon), radiusMeters = radius)
        syncHook?.onUpdateWorksite(name, lat, lon, radius)
    }

    /** Currently logged-in employee internal id. Set after successful auth. */
    private val _loggedInEmployeeId = MutableStateFlow<String?>(null)
    val loggedInEmployeeId: StateFlow<String?> = _loggedInEmployeeId.asStateFlow()

    private val ids = AtomicLong(1000)
    private fun nextId(prefix: String) = "$prefix${ids.incrementAndGet()}"

    private val _employees = MutableStateFlow(seedEmployees())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _logs = MutableStateFlow(seedLogs())
    val logs: StateFlow<List<AttendanceLog>> = _logs.asStateFlow()

    private val _leaves = MutableStateFlow(seedLeaves())
    val leaves: StateFlow<List<LeaveRequest>> = _leaves.asStateFlow()

    fun requireLoggedEmployeeId(): String =
        _loggedInEmployeeId.value ?: error("No employee is logged in")

    fun currentEmployee(): Employee =
        _employees.value.first { it.id == requireLoggedEmployeeId() }

    fun currentEmployeeId(): String = requireLoggedEmployeeId()

    fun logoutEmployee() {
        _loggedInEmployeeId.value = null
    }

    /** Verify employee credentials. Returns the internal id on success, null on failure. */
    fun verifyEmployeeCredentials(employeeId: String, password: String): String? {
        val emp = _employees.value.firstOrNull { it.employeeId == employeeId }
        return if (emp != null && emp.password == password) emp.id else null
    }

    /** Change an employee's password. Returns true on success. */
    fun changeEmployeePassword(employeeId: String, currentPassword: String, newPassword: String): Boolean {
        if (newPassword.length < 4) return false
        val emp = _employees.value.firstOrNull { it.employeeId == employeeId }
        if (emp == null || emp.password != currentPassword) return false
        _employees.value = _employees.value.map {
            if (it.employeeId == employeeId) it.copy(password = newPassword) else it
        }
        syncHook?.onChangeEmployeePassword(employeeId, currentPassword, newPassword)
        return true
    }

    /** Admin: create a new employee. Returns null if employeeId already taken. */
    fun createEmployee(req: NewEmployeeRequest): Employee? {
        if (req.employeeId.isBlank() || req.password.length < 4 || req.name.isBlank()) return null
        if (_employees.value.any { it.employeeId == req.employeeId }) return null
        val avatarColors = listOf(0xFF26E8FF, 0xFF9D5BFF, 0xFFFF4ECB, 0xFF5BFFB0, 0xFFFFC24B, 0xFF3B82F6, 0xFFE040FB)
        val emp = Employee(
            id = nextId("emp"),
            employeeId = req.employeeId,
            password = req.password,
            name = req.name,
            role = req.role,
            avatarColor = avatarColors.random(),
            baseSalary = req.baseSalary,
        )
        _employees.value = _employees.value + emp
        syncHook?.onCreateEmployee(req.employeeId, req.password, req.name, req.role, req.baseSalary)
        return emp
    }

    /** Admin: delete an employee and all their data. */
    fun deleteEmployee(internalId: String): Boolean {
        val emp = _employees.value.firstOrNull { it.id == internalId } ?: return false
        _employees.value = _employees.value.filter { it.id != internalId }
        _logs.value = _logs.value.filter { it.employeeId != internalId }
        _leaves.value = _leaves.value.filter { it.employeeId != internalId }
        syncHook?.onDeleteEmployee(internalId)
        return true
    }

    /** Admin login convenience — sets the logged-in employee for admin-as-employee scenarios. Not used currently. */
    fun setLoggedInEmployee(internalId: String?) {
        _loggedInEmployeeId.value = internalId
    }

    /** Open log for an employee, if currently checked in. */
    fun openLog(employeeId: String): AttendanceLog? =
        _logs.value.firstOrNull { it.employeeId == employeeId && it.isOpen }

    /** Records a verified check-in. Returns null when outside the geofence. */
    fun checkIn(employeeId: String, location: GeoPoint): AttendanceLog? {
        if (openLog(employeeId) != null) return openLog(employeeId)
        val distance = Geofence.distanceMeters(workSite.center, location)
        if (distance > workSite.radiusMeters) return null
        val log = AttendanceLog(
            id = nextId("log"),
            employeeId = employeeId,
            checkInMillis = System.currentTimeMillis(),
            location = location,
            distanceFromSite = distance,
            verified = true,
        )
        _logs.value = _logs.value + log
        setStatus(employeeId, PresenceStatus.CHECKED_IN)
        syncHook?.onCheckIn(employeeId, location.latitude, location.longitude)
        return log
    }

    fun checkOut(employeeId: String) {
        val open = openLog(employeeId) ?: return
        _logs.value = _logs.value.map {
            if (it.id == open.id) it.copy(checkOutMillis = System.currentTimeMillis()) else it
        }
        setStatus(employeeId, PresenceStatus.CHECKED_OUT)
        syncHook?.onCheckOut(employeeId)
    }

    private fun setStatus(employeeId: String, status: PresenceStatus) {
        _employees.value = _employees.value.map {
            if (it.id == employeeId) it.copy(status = status) else it
        }
    }

    fun submitLeave(employeeId: String, start: Long, end: Long, reason: String, type: String) {
        val emp = _employees.value.first { it.id == employeeId }
        val request = LeaveRequest(
            id = nextId("lv"),
            employeeId = employeeId,
            employeeName = emp.name,
            startMillis = start,
            endMillis = end,
            reason = reason,
            type = type,
            requestedAtMillis = System.currentTimeMillis(),
        )
        _leaves.value = listOf(request) + _leaves.value
        syncHook?.onSubmitLeave(employeeId, start, end, reason, type)
    }

    fun decideLeave(leaveId: String, status: LeaveStatus, payType: LeavePayType?) {
        syncHook?.onDecideLeave(leaveId, status.name, if (status == LeaveStatus.APPROVED) payType?.name else null)
        _leaves.value = _leaves.value.map { lv ->
            if (lv.id != leaveId) lv
            else lv.copy(status = status, payType = if (status == LeaveStatus.APPROVED) payType else null)
        }
        // Reflect ON_LEAVE presence for approved leaves covering today.
        val now = System.currentTimeMillis()
        _leaves.value.firstOrNull { it.id == leaveId }?.let { lv ->
            if (status == LeaveStatus.APPROVED && now in lv.startMillis..lv.endMillis) {
                setStatus(lv.employeeId, PresenceStatus.ON_LEAVE)
            }
        }
    }

    fun leavesFor(employeeId: String): List<LeaveRequest> =
        _leaves.value.filter { it.employeeId == employeeId }

    fun logsFor(employeeId: String): List<AttendanceLog> =
        _logs.value.filter { it.employeeId == employeeId }.sortedByDescending { it.checkInMillis }

    fun allAttendanceLogs(): List<AttendanceLog> =
        _logs.value.sortedByDescending { it.checkInMillis }

    // ---------------------------------------------------------------------
    //  Automated salary engine — month-aware
    // ---------------------------------------------------------------------

    fun updateAttendanceLog(logId: String, newCheckInMillis: Long, newCheckOutMillis: Long?) {
        _logs.value = _logs.value.map {
            if (it.id == logId) it.copy(checkInMillis = newCheckInMillis, checkOutMillis = newCheckOutMillis) else it
        }
        syncHook?.onEditAttendance(logId, newCheckInMillis, newCheckOutMillis)
    }

    /** Salary for current month (convenience). */
    fun salaryFor(employeeId: String): SalaryBreakdown {
        val now = Calendar.getInstance()
        return salaryFor(employeeId, now.get(Calendar.MONTH), now.get(Calendar.YEAR))
    }

    /** All salaries for current month (convenience). */
    fun allSalaries(): List<SalaryBreakdown> = allSalaries(
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.YEAR),
    )

    /**
     * Computes monthly salary for a specific month [0=Jan..11=Dec].
     *   perDay   = baseSalary / totalWorkingDays
     *   A day is only fully credited when hours >= DAILY_WORKING_HOURS.
     *   Partial days earn proportional credit: hoursWorked / DAILY_WORKING_HOURS.
     */
    fun salaryFor(employeeId: String, month: Int, year: Int): SalaryBreakdown {
        val emp = _employees.value.first { it.id == employeeId }
        val totalDays = standardWorkingDays(month, year)
        val perDay = if (totalDays > 0) emp.baseSalary / totalDays else 0.0

        val fractionalDays = fractionalWorkedDays(employeeId, month, year)
        val approved = _leaves.value.filter {
            it.employeeId == employeeId && it.status == LeaveStatus.APPROVED && inMonth(it.startMillis, month, year)
        }
        val paidLeaveDays = approved.filter { it.payType == LeavePayType.WITH_PAY }.sumOf { it.days }
        val unpaidLeaveDays = approved.filter { it.payType == LeavePayType.WITHOUT_PAY }.sumOf { it.days }

        val accountedDays = fractionalDays + paidLeaveDays + unpaidLeaveDays
        val absentDays = (totalDays.toDouble() - accountedDays).coerceAtLeast(0.0)

        val gross = perDay * (fractionalDays + paidLeaveDays)
        val deductions = perDay * (unpaidLeaveDays.toDouble() + absentDays)

        return SalaryBreakdown(
            employeeId = emp.id,
            employeeName = emp.name,
            baseSalary = emp.baseSalary,
            totalWorkingDays = totalDays,
            daysWorked = fractionalDays,
            paidLeaveDays = paidLeaveDays,
            unpaidLeaveDays = unpaidLeaveDays,
            absentDays = absentDays,
            perDayRate = perDay,
            grossEarned = gross,
            deductions = deductions,
            netPayable = gross,
            dailyWorkingHours = DAILY_WORKING_HOURS,
            month = month,
            year = year,
        )
    }

    fun allSalaries(month: Int, year: Int): List<SalaryBreakdown> =
        _employees.value.map { salaryFor(it.id, month, year) }

    /**
     * 12-month yearly summary: one entry per employee with monthly net amounts.
     * Months in the future or without working days yet return 0.0.
     */
    fun yearlySummaries(year: Int): List<YearlySummary> =
        _employees.value.map { emp ->
            val monthly = (0..11).map { month ->
                salaryFor(emp.id, month, year).netPayable
            }
            YearlySummary(
                employeeId = emp.id,
                employeeName = emp.name,
                baseSalary = emp.baseSalary,
                monthlyNet = monthly,
                totalNet = monthly.sum(),
            )
        }

    /**
     * Sums fractional days for a specific month: for each unique calendar day,
     * contributes min(hoursWorked / DAILY_WORKING_HOURS, 1.0).
     */
    private fun fractionalWorkedDays(employeeId: String, month: Int, year: Int): Double =
        _logs.value
            .filter { it.employeeId == employeeId && inMonth(it.checkInMillis, month, year) }
            .groupBy { dayKey(it.checkInMillis) }
            .entries
            .sumOf { (_, dayLogs) ->
                val totalMs = dayLogs.sumOf { log ->
                    (log.checkOutMillis ?: System.currentTimeMillis()) - log.checkInMillis
                }
                val hours = totalMs / 3_600_000.0
                hours.coerceAtMost(DAILY_WORKING_HOURS) / DAILY_WORKING_HOURS
            }

    /** Attendance logs filtered to a specific month. */
    fun logsForMonth(employeeId: String, month: Int, year: Int): List<AttendanceLog> =
        _logs.value
            .filter { it.employeeId == employeeId && inMonth(it.checkInMillis, month, year) }
            .sortedByDescending { it.checkInMillis }

    fun allLogsForMonth(month: Int, year: Int): List<AttendanceLog> =
        _logs.value
            .filter { inMonth(it.checkInMillis, month, year) }
            .sortedByDescending { it.checkInMillis }

    private fun inMonth(millis: Long, month: Int, year: Int): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
    }

    private fun dayKey(millis: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    /** Standard working days = weekdays in a specific month. */
    fun standardWorkingDays(month: Int = Calendar.getInstance().get(Calendar.MONTH),
                            year: Int = Calendar.getInstance().get(Calendar.YEAR)): Int {
        val c = Calendar.getInstance()
        c.set(Calendar.MONTH, month)
        c.set(Calendar.YEAR, year)
        val days = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        var count = 0
        for (d in 1..days) {
            c.set(Calendar.DAY_OF_MONTH, d)
            val dow = c.get(Calendar.DAY_OF_WEEK)
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) count++
        }
        return count
    }

    fun formatMoney(value: Double): String = "₹" + value.roundToInt().toString()
        .reversed().chunked(3).joinToString(",").reversed()

    // ---------------------------------------------------------------------
    //  Seed data — gives the app a lived-in feel on first launch
    // ---------------------------------------------------------------------

    private fun seedEmployees(): List<Employee> = listOf(
        Employee("e1", "EMP001", "pass1234", "Aria Nakamura", "Lead Product Designer", 0xFF26E8FF, 85000.0, PresenceStatus.CHECKED_OUT),
        Employee("e2", "EMP002", "pass1234", "Marcus Vela", "Backend Engineer", 0xFF9D5BFF, 75000.0, PresenceStatus.CHECKED_IN),
        Employee("e3", "EMP003", "pass1234", "Lena Frost", "QA Analyst", 0xFFFF4ECB, 55000.0, PresenceStatus.CHECKED_IN),
        Employee("e4", "EMP004", "pass1234", "Dev Okafor", "DevOps", 0xFF5BFFB0, 80000.0, PresenceStatus.ON_LEAVE),
        Employee("e5", "EMP005", "pass1234", "Sora Pierce", "Data Scientist", 0xFFFFC24B, 72000.0, PresenceStatus.CHECKED_OUT),
    )

    private fun seedLogs(): List<AttendanceLog> {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        val hour = 60L * 60 * 1000
        val list = mutableListOf<AttendanceLog>()
        // Build ~16 worked days for current employee this month.
        var d = 1
        var added = 0
        val cal = Calendar.getInstance()
        while (added < 15 && d <= 28) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY && cal.timeInMillis < now) {
                val start = cal.apply { set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 2) }.timeInMillis
                list += AttendanceLog(
                    id = "seedlog$d",
                    employeeId = "e1",
                    checkInMillis = start,
                    checkOutMillis = start + 8 * hour + 12 * 60 * 1000,
                    location = GeoPoint(20.3735, 72.9142),
                    distanceFromSite = 22.0 + (d % 5) * 6,
                    verified = true,
                )
                added++
            }
            d++
        }
        // A couple of active open sessions for other employees.
        list += AttendanceLog("seedopen2", "e2", now - 3 * hour, null, GeoPoint(20.3733, 72.9139), 35.0, true)
        list += AttendanceLog("seedopen3", "e3", now - 5 * hour - 20 * 60 * 1000, null, GeoPoint(20.3736, 72.9143), 61.0, true)
        list += AttendanceLog("seed5y", "e5", now - day, now - day + 7 * hour, GeoPoint(20.3732, 72.9138), 88.0, true)
        return list
    }

    private fun seedLeaves(): List<LeaveRequest> {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        return listOf(
            LeaveRequest("seedlv1", "e4", "Dev Okafor", now - day, now + day, "Family event out of town.", "Personal", LeaveStatus.APPROVED, LeavePayType.WITH_PAY, now - 3 * day),
            LeaveRequest("seedlv2", "e3", "Lena Frost", now + 4 * day, now + 6 * day, "Medical appointment & recovery.", "Sick", LeaveStatus.PENDING, null, now - day),
            LeaveRequest("seedlv3", "e2", "Marcus Vela", now + 9 * day, now + 12 * day, "Vacation — long planned trip.", "Vacation", LeaveStatus.PENDING, null, now - 2 * day),
            LeaveRequest("seedlv4", "e1", "Aria Nakamura", now - 10 * day, now - 9 * day, "Conference attendance.", "Work", LeaveStatus.APPROVED, LeavePayType.WITH_PAY, now - 14 * day),
            LeaveRequest("seedlv5", "e5", "Sora Pierce", now - 5 * day, now - 5 * day, "Personal errand.", "Personal", LeaveStatus.APPROVED, LeavePayType.WITHOUT_PAY, now - 8 * day),
        )
    }
}
