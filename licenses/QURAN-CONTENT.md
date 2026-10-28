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
| `en-mukhtasar.txt` | Tafseer: *Al-Mukhtasar in Interpreting the Noble Quran* (English), Tafsir Center for Quranic Studies | [QuranEnc.com](https://quranenc.com/en/browse/english_mokhtasar/), copied from [QUL resource 266](https://qul.tarteel.ai/resources/tafsir/266) | Same QuranEnc terms. QUL does not give the QuranEnc version number. |
| `ar-muyassar.txt` | Tafseer: *Al-Tafsir al-Muyassar* (Arabic), a committee of scholars, King Fahd Glorious Quran Printing Complex | [QuranEnc.com](https://quranenc.com/en/browse/arabic_moyassar/), copied from [QUL resource 38](https://qul.tarteel.ai/resources/tafsir/38) | Same QuranEnc terms: verbatim, credited to the King Fahd Complex. |
| `en-wbw.txt` | Word-by-word English glosses, Quranic Arabic Corpus, Kais Dukes | [corpus.quran.com](https://corpus.quran.com) | GNU General Public License v3 (`GPL-3.0.txt` in this folder). Credit and link to corpus.quran.com. |
| `core/.../SurahData.kt` | Surah names, meanings, ayah counts, juz and sajdah positions | Tanzil quran-data | Creative Commons Attribution 3.0 |

## Tafseer and word by word

`scripts/build-tafseer-wbw.py` builds the three files from pinned commits and checks a
SHA-256 over everything it downloads, so they are exactly the sources' text:

- The two tafseers come from [spa5k/tafsir_api](https://github.com/spa5k/tafsir_api) at
  commit `eb82bb6`, which mirrors the Quranic Universal Library (QUL, by Tarteel and
  Quran.com) exports of QuranEnc's editions. Only the layout changes: one ayah per line,
  with newlines escaped as in the notes files. Nothing is shortened or edited.
- The word-by-word glosses come from the Quranic Arabic Corpus's own repository,
  [kaisdukes/quranic-corpus-api](https://github.com/kaisdukes/quranic-corpus-api) at commit
  `17a9062`, whose project is licensed under the GNU GPL v3. The glosses are unchanged;
  they are grouped one ayah per line, in the corpus's word order, and the corpus credit is
  kept at the top of the file and shown in the reader. The corpus counts words as its
  Tanzil text split at spaces; in four ayat (2:181, 8:6, 13:37, 37:130) it treats two of
  our words as one, and the reader shows those two words together under the one gloss
  (`WordByWord.joined` in `core/.../Quran.kt`, checked for every ayah by `QuranTest`).

The GPL covers the word-by-word data file only. It ships as a separate, unmodified file
alongside the app with its licence, and anyone may take it from this repository under the
same terms.

The Quran text and translation files were taken from the snapshots in [risan/quran-json](https://github.com/risan/quran-json)
(commit e20bba6), which records the upstream URL, licence and SHA-256 of each one.

## Recitation audio

Audio is streamed per ayah from [EveryAyah.com](https://everyayah.com/data/), which
states no licence. `scripts/fetch-bundled-audio.sh` downloads Mishary Rashid Alafasy's
recitation of Al-Fatihah and surahs 83 to 114 at build time so they play offline;
the files are not committed. Confirm permission before publishing to Google Play.

## Fonts

Amiri Quran and Noto Nastaliq Urdu are under the SIL Open Font License 1.1 (see the
`OFL-*.txt` files in this folder).
