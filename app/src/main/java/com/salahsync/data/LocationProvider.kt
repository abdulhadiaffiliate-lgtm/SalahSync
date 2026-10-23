package com.salahsync.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
import com.salahsync.core.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Gets a one-off location fix with the platform LocationManager (no Play Services needed). */
class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): SavedLocation? {
        if (!hasPermission()) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val fix: Location? = provider?.let {
            withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine { cont ->
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    LocationManagerCompat.getCurrentLocation(
                        lm, it, signal, ContextCompat.getMainExecutor(context),
                    ) { loc -> if (cont.isActive) cont.resume(loc) }
                }
            }
        } ?: listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        fix ?: return null
        val point = GeoPoint(fix.latitude, fix.longitude)
        val (place, country) = reverseGeocode(point)
        return SavedLocation(point, place ?: "%.2f, %.2f".format(Locale.US, point.latitude, point.longitude), country)
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(point: GeoPoint): Pair<String?, String?> = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null to null
        runCatching {
            val a = Geocoder(context, Locale.ENGLISH).getFromLocation(point.latitude, point.longitude, 1)?.firstOrNull()
            val city = a?.locality ?: a?.subAdminArea ?: a?.adminArea
            val name = listOfNotNull(city, a?.countryName).joinToString(", ").ifBlank { null }
            name to a?.countryCode
        }.getOrDefault(null to null)
    }
}

/** A short offline list for choosing a city by hand when location is off. */
object Cities {
    val all: List<SavedLocation> = listOf(
        SavedLocation(GeoPoint(31.5204, 74.3587), "Lahore, Pakistan", "PK"),
        SavedLocation(GeoPoint(24.8607, 67.0011), "Karachi, Pakistan", "PK"),
        SavedLocation(GeoPoint(33.6844, 73.0479), "Islamabad, Pakistan", "PK"),
        SavedLocation(GeoPoint(34.0151, 71.5249), "Peshawar, Pakistan", "PK"),
        SavedLocation(GeoPoint(28.6139, 77.2090), "Delhi, India", "IN"),
        SavedLocation(GeoPoint(19.0760, 72.8777), "Mumbai, India", "IN"),
        SavedLocation(GeoPoint(17.3850, 78.4867), "Hyderabad, India", "IN"),
        SavedLocation(GeoPoint(23.8103, 90.4125), "Dhaka, Bangladesh", "BD"),
        SavedLocation(GeoPoint(21.3891, 39.8579), "Makkah, Saudi Arabia", "SA"),
        SavedLocation(GeoPoint(24.5247, 39.5692), "Madinah, Saudi Arabia", "SA"),
        SavedLocation(GeoPoint(24.7136, 46.6753), "Riyadh, Saudi Arabia", "SA"),
        SavedLocation(GeoPoint(25.2048, 55.2708), "Dubai, UAE", "AE"),
        SavedLocation(GeoPoint(25.2854, 51.5310), "Doha, Qatar", "QA"),
        SavedLocation(GeoPoint(30.0444, 31.2357), "Cairo, Egypt", "EG"),
        SavedLocation(GeoPoint(41.0082, 28.9784), "Istanbul, Turkey", "TR"),
        SavedLocation(GeoPoint(-6.2088, 106.8456), "Jakarta, Indonesia", "ID"),
        SavedLocation(GeoPoint(3.1390, 101.6869), "Kuala Lumpur, Malaysia", "MY"),
        SavedLocation(GeoPoint(51.5074, -0.1278), "London, United Kingdom", "GB"),
        SavedLocation(GeoPoint(53.4808, -2.2426), "Manchester, United Kingdom", "GB"),
        SavedLocation(GeoPoint(40.7128, -74.0060), "New York, United States", "US"),
        SavedLocation(GeoPoint(41.8781, -87.6298), "Chicago, United States", "US"),
        SavedLocation(GeoPoint(43.6532, -79.3832), "Toronto, Canada", "CA"),
    )
}
