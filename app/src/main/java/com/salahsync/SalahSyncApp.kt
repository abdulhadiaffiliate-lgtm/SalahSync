package com.salahsync

import android.app.Application
import com.salahsync.data.LocationProvider
import com.salahsync.data.PrayerRepository
import com.salahsync.data.SettingsRepository
import com.salahsync.notify.Notifications
import com.salahsync.notify.PrayerScheduler

/** Holds the app's few long-lived objects; small enough that no DI framework is needed. */
class SalahSyncApp : Application() {
    lateinit var settings: SettingsRepository
        private set
    lateinit var prayers: PrayerRepository
        private set
    lateinit var scheduler: PrayerScheduler
        private set
    lateinit var location: LocationProvider
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        prayers = PrayerRepository(this)
        scheduler = PrayerScheduler(this, prayers)
        location = LocationProvider(this)
        Notifications.createChannels(this)
    }
}
