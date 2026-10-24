package com.salahsync

import android.app.Application
import com.salahsync.data.LocationProvider
import com.salahsync.data.PrayerRepository
import com.salahsync.data.SettingsRepository
import com.salahsync.hadith.HadithLibrary
import com.salahsync.hadith.HadithStore
import com.salahsync.notify.Notifications
import com.salahsync.notify.PrayerScheduler
import com.salahsync.notify.PrayerSilence
import com.salahsync.quran.QuranRepository
import com.salahsync.quran.QuranStore
import com.salahsync.quran.audio.AudioLibrary
import com.salahsync.quran.audio.QueueBuilder
import com.salahsync.quran.audio.QuranPlayer
import com.salahsync.tracker.TrackerRepository

/** Holds the app's few long-lived objects; small enough that no DI framework is needed. */
class SalahSyncApp : Application() {
    lateinit var settings: SettingsRepository
        private set
    lateinit var prayers: PrayerRepository
        private set
    lateinit var scheduler: PrayerScheduler
        private set
    lateinit var silence: PrayerSilence
        private set
    lateinit var location: LocationProvider
        private set
    lateinit var quran: QuranRepository
        private set
    lateinit var quranStore: QuranStore
        private set
    lateinit var audioLibrary: AudioLibrary
        private set
    lateinit var quranQueue: QueueBuilder
        private set
    lateinit var quranPlayer: QuranPlayer
        private set
    lateinit var tracker: TrackerRepository
        private set
    lateinit var hadith: HadithLibrary
        private set
    lateinit var hadithStore: HadithStore
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        prayers = PrayerRepository(this)
        silence = PrayerSilence(this)
        scheduler = PrayerScheduler(this, prayers, silence)
        location = LocationProvider(this)
        quran = QuranRepository(this)
        quranStore = QuranStore(this)
        audioLibrary = AudioLibrary(this)
        quranQueue = QueueBuilder(this, audioLibrary)
        quranPlayer = QuranPlayer(this, quranQueue)
        tracker = TrackerRepository(this)
        hadith = HadithLibrary(this)
        hadithStore = HadithStore(this)
        Notifications.createChannels(this)
    }
}
