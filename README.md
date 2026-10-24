# SalahSync

An Android app that reminds you of the five daily prayers, with accurate times for your location, a gentle adhan, and a calm, premium design.

## Status

v1.0 Core, v1.1 Quran, v1.2 Tracker and Adhkar, and v1.3 Hadith are in progress. See the product plan for the full roadmap (word-by-word, tafseer and more come in later releases).

## Quran (v1.1)

- The full Quran offline in Uthmani script (Amiri Quran font), with Saheeh International English and Junagarhi Urdu (Noto Nastaliq Urdu), including each translation's footnotes.
- Surah, juz and bookmark indexes, search by surah name or number, and "continue reading" from the last ayah on screen.
- A listener with 8 reciters: per-ayah streaming from EveryAyah, the ayah being recited highlighted in the reader, background playback with lock-screen controls, a mini-player above the tabs and a full player (repeat ayah or surah, speed, seek by ayah, carry on into the next surah).
- Al-Fatihah and the last 32 surahs by Mishary Alafasy are bundled for offline listening; any surah can be downloaded per reciter.
- Share any ayah, or the Ayah of the Day, as an image on one of four backgrounds.

## Prayer tracker and adhkar (v1.2)

- Tick each of the five prayers as prayed. Any prayer not ticked by the end of its day counts as Qada, per prayer; log each Qada you make up, and add missed prayers from before you started tracking.
- The record lives in a local Room database (`salah_tracker.db`) that is never uploaded and is excluded from cloud backup and device transfer (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`).
- Morning and evening adhkar with tap-to-count; hold Ayat al-Kursi or the three Quls for the full text. Each dhikr's info icon shows its source book, hadith number, narrator and grading, linked to sunnah.com. The list is in `core/.../adhkar/Adhkar.kt`.

## Hadith (v1.3)

- Sahih al-Bukhari, Sahih Muslim, Sunan Abi Dawud and Jami' at-Tirmidhi in Arabic, with English and Urdu translations. Browse each book by chapter (kitab), and read any hadith with its gradings (given per hadith for Abu Dawud and Tirmidhi).
- Each book is a one-time download (14 to 24 MB, less over the network as it is compressed) from the open [hadith-api](https://github.com/fawazahmed0/hadith-api) dataset, pinned to one commit and checked against a SHA-256 per file. It is then stored in a local database (`hadith.db`) and works offline. Books can be removed again from their page.
- Search the downloaded books by number (`13`, `2564c`, `muslim 2564`, `abu dawud 23`) or by words in the English and Urdu translations (offline full-text search).
- An English / Urdu switch, and Arabic on or off. Where the dataset has no Urdu for a hadith, the English is shown with a note.
- Hadith of the Day: 20 hand-picked hadith from Bukhari and Muslim, one a day, on the Hadith screen and on Home. Their text is bundled, so they need no download. The list is `DailyHadith` in `core/.../hadith/HadithData.kt`; `scripts/build-daily-hadith.py` copies their text from the pinned dataset into `app/src/main/assets/hadith/daily.json`.
- Sahih Muslim is numbered as on sunnah.com (Fuad Abdul Baqi, with letters for each narration, e.g. 2564c); the other books use their standard numbering.

## Build

Requirements: JDK 17 and the Android SDK (API 35).

```sh
./gradlew :core:test          # prayer-time engine tests (no Android SDK needed with -PcoreOnly)
scripts/fetch-bundled-audio.sh # optional: the 33 short surahs' audio, bundled for offline play
./gradlew :app:assembleDebug  # debug APK in app/build/outputs/apk/debug/
```

Every pull request builds a debug APK in GitHub Actions; download it from the run's **Artifacts** section.

## Project layout

| Module | What it holds |
| --- | --- |
| `core` | Pure Kotlin prayer engine: times (Adhan library), regional defaults, Hijri dates, Qibla; Quran surah and juz metadata; Qada arithmetic; the adhkar and their references; hadith books, numbering, search parsing, the pinned dataset and the daily picks |
| `app` | Android app: Jetpack Compose UI, settings, AlAdhan online times, alarms and notifications, the Quran reader and Media3 player, hadith download and offline search |

## Adhan audio

No adhan recording is bundled yet. Put a licensed recording at `app/src/main/res/raw/adhan.mp3` and it is used automatically; until then the phone's alarm sound plays. Because Android fixes a channel's sound when it is created, bump `CHANNEL_ADHAN` in `Notifications.kt` after adding it.

## Licenses

Fonts are under the SIL Open Font License 1.1 (see `licenses/`). The Adhan library is MIT licensed, and AndroidX Media3 is Apache 2.0. Quran text, translations and recitation sources, with their terms, are listed in `licenses/QURAN-CONTENT.md`; hadith sources in `licenses/HADITH-CONTENT.md`.
