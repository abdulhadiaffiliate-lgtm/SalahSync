# SalahSync

An Android app that reminds you of the five daily prayers, with accurate times for your location, a gentle adhan, and a calm, premium design.

## Status

v1.0 Core, v1.1 Quran, v1.2 Tracker and Adhkar, and v1.3 Silent mode, Weekly report and Hadith are in progress. Tafseer and word-by-word are added to the Quran reader. See the product plan for the full roadmap.

## Quran (v1.1)

- The full Quran offline in Uthmani script (Amiri Quran font), with Saheeh International English and Junagarhi Urdu (Noto Nastaliq Urdu), including each translation's footnotes.
- Surah, juz and bookmark indexes, search by surah name or number, and "continue reading" from the last ayah on screen.
- A listener with 8 reciters: per-ayah streaming from EveryAyah, the ayah being recited highlighted in the reader, background playback with lock-screen controls, a mini-player above the tabs and a full player (repeat ayah or surah, speed, seek by ayah, carry on into the next surah).
- Al-Fatihah and the last 32 surahs by Mishary Alafasy are bundled for offline listening; any surah can be downloaded per reciter.
- Share any ayah, or the Ayah of the Day, as an image on one of four backgrounds.
- Tafseer for every ayah: the book icon on an ayah opens *Al-Mukhtasar* (English, Tafsir Center for Quranic Studies) or *Al-Tafsir al-Muyassar* (Arabic, King Fahd Complex), bundled offline. The reader remembers which one you chose.
- Word by word: turn it on under the reader's text settings to show the English meaning under each Arabic word, from the Quranic Arabic Corpus. `scripts/build-tafseer-wbw.py` rebuilds both from pinned, checksummed sources.

## Prayer tracker and adhkar (v1.2)

- Tick each of the five prayers as prayed. Any prayer not ticked by the end of its day counts as Qada, per prayer; log each Qada you make up, and add missed prayers from before you started tracking.
- The record lives in a local Room database (`salah_tracker.db`) that is never uploaded and is excluded from cloud backup and device transfer (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`).
- Morning and evening adhkar with tap-to-count; hold Ayat al-Kursi or the three Quls for the full text. Each dhikr's info icon shows its source book, hadith number, narrator and grading, linked to sunnah.com. The list is in `core/.../adhkar/Adhkar.kt`.

## Silent mode and weekly report (v1.3)

- Silent during prayer: when each chosen prayer begins, SalahSync turns on Do Not Disturb (priority only, so alarms and starred contacts still come through), silent or vibrate for the minutes you choose (a separate length for Jumu'ah), then puts the sound back by itself. It starts 30 seconds after the prayer time so the adhan is not cut off.
- Do Not Disturb and silent need Android's Do Not Disturb access. Settings explains this and opens the system screen; until access is allowed the phone is set to vibrate instead.
- The sound state from before prayer is saved on the phone (`prayer_silence` preferences, left out of backups), so a reboot, update or killed app never leaves the phone stuck on silent: on boot and on every reschedule the sound is restored if the window is over, or the end alarm is re-armed. If you change the sound yourself during prayer, SalahSync leaves your choice alone. An ongoing notification shows while the phone is quiet, with a button to turn the sound back on.
- Weekly report: every Friday at 9:00 AM, a notification sums up the Friday-to-Thursday week, and the report screen shows prayers on time, late and missed, a day-by-prayer grid, each prayer's split, Qada owed and streaks, and can be shared as an image. It is worked out from the local tracker only.
- A tick made after its prayer's time has ended (Fajr at sunrise, Dhuhr at Asr, Asr at Maghrib, Maghrib at Isha, Isha at the next Fajr) is stored as late; press and hold a tick to switch it between on time and late. Ticks from before v1.3 count as on time (tracker database version 2 adds the `late` and `markedAt` columns).

## Hadith (v1.3)

- Sahih al-Bukhari, Sahih Muslim, Sunan Abi Dawud and Jami' at-Tirmidhi in Arabic, with English and Urdu translations. Browse each book by chapter (kitab), and read any hadith with its gradings (given per hadith for Abu Dawud and Tirmidhi).
- Each book is a one-time download (14 to 24 MB, less over the network as it is compressed) from the open [hadith-api](https://github.com/fawazahmed0/hadith-api) dataset, pinned to one commit and checked against a SHA-256 per file. It is then stored in a local database (`hadith.db`) and works offline. Books can be removed again from their page.
- Search the downloaded books by number (`13`, `2564c`, `muslim 2564`, `abu dawud 23`) or by words in the English and Urdu translations (offline full-text search).
- An English / Urdu switch, and Arabic on or off. Where the dataset has no Urdu for a hadith, the English is shown with a note.
- Hadith of the Day: 20 hand-picked hadith from Bukhari and Muslim, one a day, on the Hadith screen and on Home. Their text is bundled, so they need no download. The list is `DailyHadith` in `core/.../hadith/HadithData.kt`; `scripts/build-daily-hadith.py` copies their text from the pinned dataset into `app/src/main/assets/hadith/daily.json`.
- Sahih Muslim is numbered as on sunnah.com (Fuad Abdul Baqi, with letters for each narration, e.g. 2564c); the other books use their standard numbering.

## How are you feeling?

- Pick a feeling (sad, anxious, angry, lonely, afraid, guilty, grateful or unmotivated) to get two passages of the Quran, duas, and one hadith from Sahih al-Bukhari or Sahih Muslim, each with its source. Open it from the Feelings button or the "How are you feeling?" card on Home, or from More.
- The ayat are read from the bundled Quran text in the reader's translation. The duas are from the Duas list. The hadith text is bundled (`app/src/main/assets/hadith/feelings.json`, built by `scripts/build-daily-hadith.py` from the same pinned dataset as the Hadith of the Day), so the screen works offline and the hadith opens in full on the Hadith screen. The list is in `core/.../feelings/Feelings.kt`.

## Duas and worship guides

- 29 daily duas in nine groups (sleep, purification, home and mosque, food and fasting, after prayer, travel, hardship, everyday moments, and general supplications). Each shows the Arabic, an English meaning and the occasion; the info icon gives the book, hadith number, narrator and grading, linked to sunnah.com. The list is in `core/.../duas/Duas.kt`.
- Step-by-step Wudu and Ghusl, each step marked Fard or Sunnah for the chosen school, with its hadith or Quran source (`core/.../guides/Guides.kt`).
- Rak'ahs of the five prayers and Jumu'ah, Hanafi or Shafi'i, split into sunnah before, fard, sunnah after and Witr (`core/.../guides/Rakats.kt`). The school defaults to the Asr setting and can be switched on the screen.

## Islamic quiz

- 49 short multiple-choice questions on faith, prayer, the Quran, the prophets, and fasting, zakah and hajj. Pick a topic or Mixed, answer five questions, and see the score at the end.
- After each answer the right one is shown with a short explanation and its source: the ayah (tap it to open the reader) or the hadith with its grading and a sunnah.com link. Only well-established basics are asked, and every hadith was checked against the pinned hadith dataset.
- The questions are in `core/.../quiz/Quiz.kt`, with the correct option written first; each round shuffles the options. Open it from Explore on Home or from More.

## Goals

- Small daily targets: pray all five, pray all five on time, read Quran for a number of minutes (5 to 60), and morning or evening adhkar. Weekly targets, Friday to Thursday: meet your daily goals on a number of days, and make up a number of Qada prayers.
- Nothing is entered twice. Prayers and Qada come from the prayer tracker, adhkar from the counts on the adhkar screen, and Quran time from the reader (counted while it is on screen); reading from a printed mushaf can be added 5 minutes at a time.
- Home shows today's goals as a progress ring, with the streak. The streak is gentle: today never breaks it, and one missed day in a week is let go. The days met are recorded as they happen, so changing goals never rewrites earlier days.
- Settings, reading time and the days met are kept in `datastore/goals.preferences_pb`, on the phone only and left out of cloud backup and device transfer. The rules are in `core/.../goals/Goals.kt`. Open Goals from Home or More.

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

Fonts are under the SIL Open Font License 1.1 (see `licenses/`). The Adhan library is MIT licensed, and AndroidX Media3 is Apache 2.0. Quran text, translations, tafseer, word-by-word and recitation sources, with their terms, are listed in `licenses/QURAN-CONTENT.md` (the word-by-word file is GNU GPL v3, `licenses/GPL-3.0.txt`); hadith sources in `licenses/HADITH-CONTENT.md`.
