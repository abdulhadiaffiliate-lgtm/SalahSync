package com.salahsync.core

import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Qibla
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object QiblaDirection {
    val KAABA = GeoPoint(21.4225241, 39.8261818)

    /** Bearing to the Kaaba in degrees clockwise from true north, 0 until 360. */
    fun bearingFrom(location: GeoPoint): Double =
        Qibla(Coordinates(location.latitude, location.longitude)).direction

    /** Great-circle distance to the Kaaba in kilometres. */
    fun distanceKmFrom(location: GeoPoint): Double {
        val r = 6371.0088
        val lat1 = Math.toRadians(location.latitude)
        val lat2 = Math.toRadians(KAABA.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(KAABA.longitude - location.longitude)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
