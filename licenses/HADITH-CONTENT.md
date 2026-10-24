# Hadith content sources and terms

## Dataset

All hadith text comes from [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api)
by Fawaz Ahmed, which is released under **the Unlicense** (a public-domain dedication).
The app uses one fixed commit, `df57907be35291c91ad6a6691180e22ca9920784`, so the text
never changes under it. Every file is checked against the SHA-256 recorded in
`core/.../hadith/HadithData.kt` (`HadithSource`) before it is used; a file that does not
match is discarded.

| Book | Editions used | Translator named by the dataset |
| --- | --- | --- |
| Sahih al-Bukhari | `ara-bukhari`, `eng-bukhari`, `urd-bukhari` | English: Muhsin Khan. Arabic and Urdu: not named |
| Sahih Muslim | `ara-muslim`, `eng-muslim`, `urd-muslim` | English: Abdul Hamid Siddiqui. Arabic and Urdu: not named |
| Sunan Abi Dawud | `ara-abudawud`, `eng-abudawud`, `urd-abudawud` | Not named |
| Jami' at-Tirmidhi | `ara-tirmidhi`, `eng-tirmidhi`, `urd-tirmidhi` | Not named |

Gradings for Abu Dawud and Tirmidhi (Al-Albani, Zubair Ali Zai, Shuaib al-Arnaut and
others) are shown exactly as the dataset gives them. Bukhari and Muslim carry no
per-hadith grading in the dataset; the app labels them as being in a sahih collection.

### How the app presents the text

The text is shown unchanged. The app only:

- trims leading and trailing spaces,
- leaves out entries that have no text in any of the three languages (the dataset keeps
  a few numbers that are chains of narration only),
- places the few hadith the dataset gives no chapter under the chapter of the hadith
  before them (at the very start of Sahih Muslim they stay in its Introduction),
- cites Sahih Muslim by Fuad Abdul Baqi's number with a letter per narration, as
  sunnah.com does (the dataset's `2564.03` is shown as 2564c).

## Hadith of the Day

`app/src/main/assets/hadith/daily.json` holds 20 hadith from Sahih al-Bukhari and Sahih
Muslim. The choice of hadith and the one-line topic shown above each are ours
(`DailyHadith` in `core/.../hadith/HadithData.kt`). The hadith text itself is copied
verbatim from the pinned dataset by `scripts/build-daily-hadith.py`, which checks each
file's SHA-256 and stops if a pick has no Arabic, English or Urdu text.

## Before publishing to Google Play

The Unlicense covers the dataset as compiled. The dataset does not state the copyright
status of the individual translations it collects (for example Muhsin Khan's English
Bukhari), so confirm the translations' terms with their publishers before a commercial
release, as with the recitation audio.
