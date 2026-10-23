# Quran content sources and terms

All Quran text and translations ship in `app/src/main/assets/quran/` exactly as
published (one ayah per line, `surah|ayah|text`). The app only changes how they
are shown: it shows the basmala once above each surah instead of inside ayah 1,
and hides QuranEnc's leading "(n)" ayah numbers because the reader shows its own.

| File | Content | Source | Terms |
| --- | --- | --- | --- |
| `ar-uthmani.txt` | Tanzil Quran Text, Uthmani, version 1.1 | [tanzil.net](https://tanzil.net) | Creative Commons Attribution 3.0. Verbatim copies only; the Tanzil copyright block is kept at the end of the file. |
| `en-saheeh.txt`, `en-saheeh-notes.txt` | Saheeh International (Noor International Center), version 1.1.2, with footnotes | [QuranEnc.com](https://quranenc.com/en/browse/english_saheeh/) | [QuranEnc terms](https://quranenc.com/en/home/api): no changes, credit the publisher and QuranEnc, state the version, keep it up to date, no inappropriate advertising. |
| `ur-junagarhi.txt`, `ur-junagarhi-notes.txt` | Muhammad Junagarhi, notes by Salahuddin Yusuf, version 1.1.3 | [QuranEnc.com](https://quranenc.com/en/browse/urdu_junagarhi/) | Same QuranEnc terms. |
| `core/.../SurahData.kt` | Surah names, meanings, ayah counts, juz and sajdah positions | Tanzil quran-data | Creative Commons Attribution 3.0 |

The files were taken from the snapshots in [risan/quran-json](https://github.com/risan/quran-json)
(commit e20bba6), which records the upstream URL, licence and SHA-256 of each one.

## Recitation audio

Audio is streamed per ayah from [EveryAyah.com](https://everyayah.com/data/), which
states no licence. `scripts/fetch-bundled-audio.sh` downloads Mishary Rashid Alafasy's
recitation of Al-Fatihah and surahs 83 to 114 at build time so they play offline;
the files are not committed. Confirm permission before publishing to Google Play.

## Fonts

Amiri Quran and Noto Nastaliq Urdu are under the SIL Open Font License 1.1 (see the
`OFL-*.txt` files in this folder).
