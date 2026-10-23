package com.salahsync.data

import com.salahsync.core.AsrSchool
import com.salahsync.core.GeoPoint
import com.salahsync.core.HighLatitude
import com.salahsync.core.Method
import com.salahsync.core.Prayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Fetches a month of timings from the public AlAdhan API (aladhan.com). Only
 * coordinates and calculation settings are sent: no name or identifiers.
 */
class AlAdhanClient(private val baseUrl: String = "https://api.aladhan.com/v1") {

    data class MonthTimings(val zone: ZoneId, val days: Map<LocalDate, Map<Prayer, LocalTime>>)

    suspend fun fetchMonth(
        point: GeoPoint,
        year: Int,
        month: Int,
        method: Method,
        school: AsrSchool,
        highLatitude: HighLatitude,
    ): String = withContext(Dispatchers.IO) {
        val url = URL(
            "$baseUrl/calendar/$year/$month" +
                "?latitude=${point.latitude}&longitude=${point.longitude}" +
                "&method=${method.alAdhanId}&school=${if (school == AsrSchool.HANAFI) 1 else 0}" +
                "&latitudeAdjustmentMethod=${highLatitude.alAdhanId}",
        )
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("Accept", "application/json")
            check(conn.responseCode == 200) { "AlAdhan returned ${conn.responseCode}" }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private val dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        private val keys = mapOf(
            Prayer.FAJR to "Fajr",
            Prayer.SUNRISE to "Sunrise",
            Prayer.DHUHR to "Dhuhr",
            Prayer.ASR to "Asr",
            Prayer.MAGHRIB to "Maghrib",
            Prayer.ISHA to "Isha",
        )

        /** Parses a /calendar response. Times look like "04:57 (PKT)". */
        fun parse(json: String): MonthTimings {
            val data = JSONObject(json).getJSONArray("data")
            var zone: ZoneId = ZoneId.systemDefault()
            val days = buildMap {
                for (i in 0 until data.length()) {
                    val day = data.getJSONObject(i)
                    val timings = day.getJSONObject("timings")
                    val dateObj = day.getJSONObject("date")
                    val date = LocalDate.parse(dateObj.getJSONObject("gregorian").getString("date"), dateFormat)
                    day.optJSONObject("meta")?.optString("timezone")?.takeIf { it.isNotBlank() }?.let {
                        zone = runCatching { ZoneId.of(it) }.getOrDefault(zone)
                    }
                    put(date, keys.mapValues { (_, key) ->
                        LocalTime.parse(timings.getString(key).substringBefore(' ').trim())
                    })
                }
            }
            return MonthTimings(zone, days)
        }

        private val Method.alAdhanId: Int
            get() = when (this) {
                Method.KARACHI -> 1
                Method.NORTH_AMERICA -> 2
                Method.MUSLIM_WORLD_LEAGUE -> 3
                Method.UMM_AL_QURA -> 4
                Method.EGYPTIAN -> 5
                Method.KUWAIT -> 9
                Method.QATAR -> 10
                Method.SINGAPORE -> 11
                Method.MOON_SIGHTING_COMMITTEE -> 15
                Method.DUBAI -> 16
            }

        private val HighLatitude.alAdhanId: Int
            get() = when (this) {
                HighLatitude.MIDDLE_OF_THE_NIGHT -> 1
                HighLatitude.SEVENTH_OF_THE_NIGHT -> 2
                HighLatitude.TWILIGHT_ANGLE -> 3
            }
    }
}
