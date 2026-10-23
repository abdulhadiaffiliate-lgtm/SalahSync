# SalahSync

An Android app that reminds you of the five daily prayers, with accurate times for your location, a gentle adhan, and a calm, premium design.

## Status

v1.0 Core and v1.1 Quran are in progress. See the product plan for the full roadmap (word-by-word, tafseer, hadith and more come in later releases).

## Quran (v1.1)

- The full Quran offline in Uthmani script (Amiri Quran font), with Saheeh International English and Junagarhi Urdu (Noto Nastaliq Urdu), including each translation's footnotes.
- Surah, juz and bookmark indexes, search by surah name or number, and "continue reading" from the last ayah on screen.
- A listener with 8 reciters: per-ayah streaming from EveryAyah, the ayah being recited highlighted in the reader, background playback with lock-screen controls, a mini-player above the tabs and a full player (repeat ayah or surah, speed, seek by ayah, carry on into the next surah).
- Al-Fatihah and the last 32 surahs by Mishary Alafasy are bundled for offline listening; any surah can be downloaded per reciter.
- Share any ayah, or the Ayah of the Day, as an image on one of four backgrounds.

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
| `core` | Pure Kotlin prayer engine: times (Adhan library), regional defaults, Hijri dates, Qibla; Quran surah and juz metadata |
| `app` | Android app: Jetpack Compose UI, settings, AlAdhan online times, alarms and notifications, the Quran reader and Media3 player |

## Adhan audio

No adhan recording is bundled yet. Put a licensed recording at `app/src/main/res/raw/adhan.mp3` and it is used automatically; until then the phone's alarm sound plays. Because Android fixes a channel's sound when it is created, bump `CHANNEL_ADHAN` in `Notifications.kt` after adding it.

## Licenses

Fonts are under the SIL Open Font License 1.1 (see `licenses/`). The Adhan library is MIT licensed, and AndroidX Media3 is Apache 2.0. Quran text, translations and recitation sources, with their terms, are listed in `licenses/QURAN-CONTENT.md`.
