package com.salahsync.data

import android.content.Context
import android.util.Log
import com.salahsync.core.DayTimes
import com.salahsync.core.Prayer
import com.salahsync.core.PrayerCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Prayer times for a date. Uses a cached AlAdhan month when one matches the
 * current settings, and the on-device calculator otherwise, so times are always
 * available offline.
 */
class PrayerRepository(
    context: Context,
    private val client: AlAdhanClient = AlAdhanClient(),
) {
    private val cacheDir = File(context.filesDir, "aladhan").apply { mkdirs() }

    private fun cacheKey(prefs: UserPrefs, year: Int, month: Int): String? {
        val loc = prefs.location ?: return null
        val lat = "%.3f".format(java.util.Locale.US, loc.point.latitude)
        val lon = "%.3f".format(java.util.Locale.US, loc.point.longitude)
        return "${year}-${month}_${lat}_${lon}_${prefs.method}_${prefs.asrSchool}_${prefs.highLatitude}"
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
    }

    suspend fun timesFor(prefs: UserPrefs, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DayTimes? {
        val loc = prefs.location ?: return null
        val offline = PrayerCalculator.calculate(loc.point, date, zone, prefs.prayerSettings)
        if (!prefs.useOnlineTimes) return offline
        val cached = readCached(prefs, date) ?: return offline
        return runCatching {
            val month = AlAdhanClient.parse(cached)
            val day = month.days[date] ?: return offline
            DayTimes(
                date = date,
                zone = zone,
                times = Prayer.entries.associateWith { p ->
                    val offset = prefs.offsets[p]?.toLong() ?: 0L
                    day.getValue(p).atDate(date).atZone(month.zone).withZoneSameInstant(zone).plusMinutes(offset)
                },
            )
        }.getOrElse {
            Log.w(TAG, "Bad cached timings, using offline times", it)
            offline
        }
    }

    private suspend fun readCached(prefs: UserPrefs, date: LocalDate): String? = withContext(Dispatchers.IO) {
        val key = cacheKey(prefs, date.year, date.monthValue) ?: return@withContext null
        File(cacheDir, "$key.json").takeIf { it.exists() }?.readText()
    }

    /** Downloads this month and next month if they are not cached yet. Failures are silent. */
    suspend fun refreshOnline(prefs: UserPrefs, today: LocalDate = LocalDate.now()) {
        if (!prefs.useOnlineTimes) return
        val loc = prefs.location ?: return
        for (d in listOf(today, today.plusMonths(1))) {
            val key = cacheKey(prefs, d.year, d.monthValue) ?: return
            val file = File(cacheDir, "$key.json")
            if (file.exists()) continue
            runCatching {
                val json = client.fetchMonth(loc.point, d.year, d.monthValue, prefs.method, prefs.asrSchool, prefs.highLatitude)
                AlAdhanClient.parse(json) // validate before caching
                withContext(Dispatchers.IO) {
                    // Drop stale months so the cache stays small.
                    cacheDir.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 70L * 86_400_000 }
                        ?.forEach { it.delete() }
                    file.writeText(json)
                }
            }.onFailure { Log.i(TAG, "AlAdhan fetch failed; staying offline", it) }
        }
    }

    private companion object {
        const val TAG = "PrayerRepository"
    }
}
