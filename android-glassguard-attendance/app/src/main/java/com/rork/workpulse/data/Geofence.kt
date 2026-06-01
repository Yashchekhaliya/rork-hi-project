package com.rork.workpulse.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Geospatial helpers for geofenced check-in validation. */
object Geofence {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Great-circle distance between two points in meters (Haversine). */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_M * (2 * atan2(sqrt(h), sqrt(1 - h)))
    }

    fun isInside(site: WorkSite, point: GeoPoint): Boolean =
        distanceMeters(site.center, point) <= site.radiusMeters
}
