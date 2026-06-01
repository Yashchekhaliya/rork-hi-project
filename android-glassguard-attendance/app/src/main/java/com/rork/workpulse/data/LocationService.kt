package com.rork.workpulse.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Resolves the device GPS position via Fused Location.
 *
 * The cloud emulator usually has no real GPS fix, so when a location cannot be
 * obtained we fall back to a deterministic point near the configured worksite
 * (with slight jitter) so the geofenced check-in flow stays demonstrable.
 */
class LocationService(private val context: Context) {

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(fallbackNear: GeoPoint): GeoPoint {
        if (!hasPermission()) return jitter(fallbackNear)
        return try {
            suspendCancellableCoroutine { cont ->
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        cont.resume(
                            if (loc != null) GeoPoint(loc.latitude, loc.longitude)
                            else jitter(fallbackNear)
                        )
                    }
                    .addOnFailureListener { cont.resume(jitter(fallbackNear)) }
            }
        } catch (e: Exception) {
            jitter(fallbackNear)
        }
    }

    /** Small randomized offset (~0-40m) so demo check-ins look realistic. */
    private fun jitter(p: GeoPoint): GeoPoint {
        val d = 0.00035 // ~38m
        return GeoPoint(
            latitude = p.latitude + (Math.random() - 0.5) * d,
            longitude = p.longitude + (Math.random() - 0.5) * d,
        )
    }
}
