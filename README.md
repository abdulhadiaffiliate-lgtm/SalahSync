# SalahSync

An Android app that reminds you of the five daily prayers, with accurate times for your location, a gentle adhan, and a calm, premium design.

## Status

v1.0 Core is in progress. See the product plan for the full roadmap (Quran, hadith, duas and more come in later releases).

## Build

Requirements: JDK 17 and the Android SDK (API 35).

```sh
./gradlew :core:test          # prayer-time engine tests (no Android SDK needed with -PcoreOnly)
./gradlew :app:assembleDebug  # debug APK in app/build/outputs/apk/debug/
```

Every pull request builds a debug APK in GitHub Actions; download it from the run's **Artifacts** section.

## Project layout

| Module | What it holds |
| --- | --- |
| `core` | Pure Kotlin prayer engine: times (Adhan library), regional defaults, Hijri dates, Qibla |
| `app` | Android app: Jetpack Compose UI, settings, AlAdhan online times, alarms and notifications |

## Adhan audio

No adhan recording is bundled yet. Put a licensed recording at `app/src/main/res/raw/adhan.mp3` and it is used automatically; until then the phone's alarm sound plays. Because Android fixes a channel's sound when it is created, bump `CHANNEL_ADHAN` in `Notifications.kt` after adding it.

## Licenses

Fonts are under the SIL Open Font License 1.1 (see `licenses/`). The Adhan library is MIT licensed.
