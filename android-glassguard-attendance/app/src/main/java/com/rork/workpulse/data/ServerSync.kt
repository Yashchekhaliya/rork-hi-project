package com.rork.workpulse.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionStatus { OFFLINE, CONNECTING, CONNECTED }

/** Cloud Worker API URL — persistent cloud backend, no local server needed. */
private const val CLOUD_API_URL = "https://li980wrgnunptwig2nzqh-backend.rork.app"

/**
 * Bridges the in-app [WorkPulseRepository] with the Med Lion HR cloud backend.
 *
 * - Connects automatically to the Cloudflare Worker (no manual IP config needed).
 * - Pings the server every few seconds. When reachable, pulls the latest snapshot.
 * - Local mutations are pushed to the server; when offline everything works locally.
 */
object ServerSync : WorkPulseRepository.SyncHook {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(ConnectionStatus.CONNECTING)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    @Volatile private var client: ServerClient? = null

    fun init(context: Context) {
        rebuildClient()
        WorkPulseRepository.syncHook = this
        startPingLoop()
    }

    private fun rebuildClient() {
        client?.close()
        client = ServerClient(CLOUD_API_URL)
    }

    private fun startPingLoop() {
        scope.launch {
            while (true) {
                val c = client
                if (c == null) {
                    _status.value = ConnectionStatus.OFFLINE
                } else {
                    if (_status.value == ConnectionStatus.OFFLINE) {
                        _status.value = ConnectionStatus.CONNECTING
                    }
                    val ok = c.health()
                    if (ok) {
                        _status.value = ConnectionStatus.CONNECTED
                        runCatching { pullSnapshot(c) }
                    } else {
                        _status.value = ConnectionStatus.OFFLINE
                    }
                }
                delay(if (_status.value == ConnectionStatus.CONNECTED) 5000 else 3000)
            }
        }
    }

    /** Pull the full server state into the repository flows. */
    private suspend fun pullSnapshot(c: ServerClient) {
        val employees = c.employees().map {
            Employee(
                id = it.id,
                employeeId = it.employeeId,
                password = it.password,
                name = it.name,
                role = it.role,
                avatarColor = it.avatarColor,
                baseSalary = it.baseSalary,
                status = runCatching { PresenceStatus.valueOf(it.status) }.getOrDefault(PresenceStatus.CHECKED_OUT),
            )
        }
        val logs = c.attendance().map {
            AttendanceLog(
                id = it.id,
                employeeId = it.employeeId,
                checkInMillis = it.checkInMillis,
                checkOutMillis = it.checkOutMillis,
                location = GeoPoint(it.location.latitude, it.location.longitude),
                distanceFromSite = it.distanceFromSite,
                verified = it.verified,
            )
        }
        val leaves = c.leaves().map {
            LeaveRequest(
                id = it.id,
                employeeId = it.employeeId,
                employeeName = it.employeeName,
                startMillis = it.startMillis,
                endMillis = it.endMillis,
                reason = it.reason,
                type = it.type,
                status = runCatching { LeaveStatus.valueOf(it.status) }.getOrDefault(LeaveStatus.PENDING),
                payType = it.payType?.let { p -> runCatching { LeavePayType.valueOf(p) }.getOrNull() },
                requestedAtMillis = it.requestedAtMillis,
            )
        }
        val ws = c.worksite()
        WorkPulseRepository.applyServerSnapshot(
            employees = employees,
            logs = logs,
            leaves = leaves,
            workSite = WorkSite(ws.name, GeoPoint(ws.center.latitude, ws.center.longitude), ws.radiusMeters),
        )
    }

    private fun push(block: suspend (ServerClient) -> Unit) {
        if (_status.value != ConnectionStatus.CONNECTED) return
        val c = client ?: return
        scope.launch { runCatching { block(c) } }
    }

    // ── SyncHook — fire-and-forget pushes; the ping loop re-pulls shortly after ──

    override fun onCheckIn(employeeId: String, lat: Double, lon: Double) =
        push { it.checkIn(employeeId, lat, lon) }

    override fun onCheckOut(employeeId: String) =
        push { it.checkOut(employeeId) }

    override fun onSubmitLeave(employeeId: String, start: Long, end: Long, reason: String, type: String) =
        push { it.submitLeave(LeaveBody(employeeId, start, end, reason, type)) }

    override fun onDecideLeave(leaveId: String, status: String, payType: String?) =
        push { it.decideLeave(leaveId, status, payType) }

    override fun onEditAttendance(logId: String, checkInMillis: Long, checkOutMillis: Long?) =
        push { it.editAttendance(logId, checkInMillis, checkOutMillis) }

    override fun onCreateEmployee(employeeId: String, password: String, name: String, role: String, baseSalary: Double) =
        push { it.createEmployee(NewEmployeeDto(employeeId, password, name, role, baseSalary)) }

    override fun onDeleteEmployee(internalId: String) =
        push { it.deleteEmployee(internalId) }

    override fun onChangeEmployeePassword(employeeIdLogin: String, current: String, next: String) =
        push { c ->
            val internal = WorkPulseRepository.employees.value.firstOrNull { it.employeeId == employeeIdLogin }?.id ?: return@push
            c.changeEmployeePassword(internal, current, next)
        }

    override fun onChangeAdminPassword(password: String) =
        push { it.changeAdminPassword(password) }

    override fun onUpdateWorksite(name: String, lat: Double, lon: Double, radius: Double) =
        push { it.updateWorksite(name, lat, lon, radius) }
}
