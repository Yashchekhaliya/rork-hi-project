package com.rork.workpulse.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rork.workpulse.data.GeoPoint
import com.rork.workpulse.data.LocationService
import com.rork.workpulse.data.WorkPulseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the geofenced check-in flow. */
data class CheckInUiState(
    val resolving: Boolean = false,
    val lastLocation: GeoPoint? = null,
    val distanceMeters: Double? = null,
    val insideFence: Boolean = false,
    val message: String? = null,
    val rejected: Boolean = false,
)

class AttendanceViewModel(app: Application) : AndroidViewModel(app) {

    private val location = LocationService(app.applicationContext)
    private val repo = WorkPulseRepository

    private val _ui = MutableStateFlow(CheckInUiState())
    val ui: StateFlow<CheckInUiState> = _ui.asStateFlow()

    fun hasLocationPermission() = location.hasPermission()

    /** Resolve current position and update geofence preview. */
    fun refreshLocation() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(resolving = true, message = null, rejected = false)
            val point = location.currentLocation(repo.workSite.center)
            val dist = com.rork.workpulse.data.Geofence.distanceMeters(repo.workSite.center, point)
            _ui.value = _ui.value.copy(
                resolving = false,
                lastLocation = point,
                distanceMeters = dist,
                insideFence = dist <= repo.workSite.radiusMeters,
            )
        }
    }

    fun toggleCheckInOut(employeeId: String) {
        viewModelScope.launch {
            val open = repo.openLog(employeeId)
            if (open != null) {
                repo.checkOut(employeeId)
                _ui.value = _ui.value.copy(message = "Checked out. Session logged.", rejected = false)
                return@launch
            }
            _ui.value = _ui.value.copy(resolving = true, message = null, rejected = false)
            val point = location.currentLocation(repo.workSite.center)
            val dist = com.rork.workpulse.data.Geofence.distanceMeters(repo.workSite.center, point)
            val log = repo.checkIn(employeeId, point)
            _ui.value = if (log != null) {
                _ui.value.copy(
                    resolving = false,
                    lastLocation = point,
                    distanceMeters = dist,
                    insideFence = true,
                    message = "Check-in verified inside the geofence.",
                    rejected = false,
                )
            } else {
                _ui.value.copy(
                    resolving = false,
                    lastLocation = point,
                    distanceMeters = dist,
                    insideFence = false,
                    message = "You're ${dist.toInt()}m away — outside the ${repo.workSite.radiusMeters.toInt()}m worksite radius.",
                    rejected = true,
                )
            }
        }
    }
}
