#!/usr/bin/env python3
"""Builds the bundled tafseer and word-by-word files in app/src/main/assets/quran/.

  en-mukhtasar.txt  Al-Mukhtasar in Interpreting the Noble Quran (English), Tafsir Center for Quranic Studies
  ar-muyassar.txt   Al-Tafsir al-Muyassar (Arabic), King Fahd Glorious Quran Printing Complex
  en-wbw.txt        Quranic Arabic Corpus word-by-word English, by Kais Dukes

The tafseer is copied from spa5k/tafsir_api (QUL resources 266 and 38) and the
word-by-word glosses from kaisdukes/quranic-corpus-api, each at a pinned commit and
checked against a SHA-256 over the downloaded files, so the bundled text is exactly
the source's. The text itself is never edited: it is only laid out one ayah per
line, `surah|ayah|text`, with newlines and backslashes escaped as in the notes files,
and the word glosses of an ayah joined with tabs. See licenses/QURAN-CONTENT.md.

Usage: scripts/build-tafseer-wbw.py [cache dir]
"""
import hashlib
import json
import os
import sys
import urllib.request

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
OUT = os.path.join(ROOT, "app/src/main/assets/quran")

TAFSIR_COMMIT = "eb82bb6294efe30ad5c135c03b1864afaa70e855"
CORPUS_COMMIT = "17a9062416eccc332111ef3e84f74072d709e187"

TAFSEER = [
    {
        "slug": "en-tafsir-al-mukhtasar",
        "out": "en-mukhtasar.txt",
        "sha256": "9da25e55aeeb7e408e040dd16b016b329576228f3b0c9216d0ed2c79c3b912cc",
        "header": [
            "Al-Mukhtasar in Interpreting the Noble Quran (English), by the Tafsir Center for Quranic Studies",
            "Source: QUL resource 266 (https://qul.tarteel.ai/resources/tafsir/266), as published on",
            "QuranEnc.com (https://quranenc.com/en/browse/english_mokhtasar/). Copied via",
            f"https://github.com/spa5k/tafsir_api/tree/{TAFSIR_COMMIT}/tafsir/en-tafsir-al-mukhtasar",
            "Republished verbatim under the QuranEnc terms: https://quranenc.com/en/home/api",
        ],
    },
    {
        "slug": "ar-tafsir-muyassar",
        "out": "ar-muyassar.txt",
        "sha256": "f2aab0ef5670376e87943d368c79c9fb2edf2194045994ad53b8e83fcdae4b7b",
        "header": [
            "Al-Tafsir al-Muyassar, by a committee of scholars, King Fahd Glorious Quran Printing Complex",
            "Source: QUL resource 38 (https://qul.tarteel.ai/resources/tafsir/38), also published on",
            "QuranEnc.com (https://quranenc.com/en/browse/arabic_moyassar/). Copied via",
            f"https://github.com/spa5k/tafsir_api/tree/{TAFSIR_COMMIT}/tafsir/ar-tafsir-muyassar",
            "Republished verbatim, with credit to the publisher, for free non-commercial use.",
        ],
    },
]

WBW = {
    "path": "src/main/resources/data/translation/word-by-word.txt",
    "uthmani": "src/main/resources/data/quran-uthmani.xml",
    "out": "en-wbw.txt",
    "sha256": "a1b9ebeaa93cfa5e82d589c0923c7949f2ba22f8292c9257209dbe4a9a693208",
    "header": [
        "Quranic Arabic Corpus, word-by-word English translation",
        "Copyright (C) 2009-2023 Kais Dukes. License: GNU General Public License, version 3",
        "(licenses/GPL-3.0.txt). Source: https://corpus.quran.com, copied from",
        f"https://github.com/kaisdukes/quranic-corpus-api/blob/{CORPUS_COMMIT}/src/main/resources/data/translation/word-by-word.txt",
        "One line per ayah: surah|ayah|the gloss of each word, in order, separated by tabs.",
        "The glosses are the corpus's own, unchanged. The corpus counts the words of an ayah",
        "as its Tanzil text split at spaces, except that 37:130 keeps 'il yasin' as one word.",
    ],
}


def fetch(url, cache, name):
    path = os.path.join(cache, name)
    if not os.path.exists(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        print("Downloading", url, file=sys.stderr)
        with urllib.request.urlopen(url) as r, open(path + ".part", "wb") as f:
            f.write(r.read())
        os.replace(path + ".part", path)
    return open(path, "rb").read()


def check(name, digest, expected):
    if digest.hexdigest() != expected:
        sys.exit(f"{name}: SHA-256 {digest.hexdigest()} does not match the pinned {expected}")


def escape(text):
    return text.replace("\\", "\\\\").replace("\r\n", "\n").replace("\n", "\\n")


def header(lines):
    return "".join(f"# {line}\n" for line in lines)


def build_tafseer(t, cache):
    digest = hashlib.sha256()
    rows = []
    for surah in range(1, 115):
        url = f"https://raw.githubusercontent.com/spa5k/tafsir_api/{TAFSIR_COMMIT}/tafsir/{t['slug']}/{surah}.json"
        data = fetch(url, cache, f"{t['slug']}/{surah}.json")
        digest.update(data)
        for entry in json.loads(data):
            assert entry["surah"] == surah, entry
            rows.append(f"{surah}|{entry['ayah']}|{escape(entry['text'])}\n")
    check(t["slug"], digest, t["sha256"])
    assert len(rows) == 6236, len(rows)
    with open(os.path.join(OUT, t["out"]), "w", encoding="utf-8") as f:
        f.write(header(t["header"]))
        f.writelines(rows)


def build_wbw(cache):
    base = f"https://raw.githubusercontent.com/kaisdukes/quranic-corpus-api/{CORPUS_COMMIT}/"
    words = fetch(base + WBW["path"], cache, "corpus/word-by-word.txt")
    uthmani = fetch(base + WBW["uthmani"], cache, "corpus/quran-uthmani.xml")
    digest = hashlib.sha256(words)
    digest.update(uthmani)
    check("word-by-word", digest, WBW["sha256"])

    # The corpus has no ayah numbers in this file: its words follow its Tanzil text in order.
    import xml.etree.ElementTree as ET
    glosses = words.decode("utf-8").split("\n")
    if glosses[-1] == "":
        glosses.pop()
    rows = []
    i = 0
    for sura in ET.fromstring(uthmani).iter("sura"):
        for aya in sura.iter("aya"):
            surah, ayah = int(sura.get("index")), int(aya.get("index"))
            count = len(aya.get("text").split(" ")) - (1 if (surah, ayah) == (37, 130) else 0)
            rows.append(f"{surah}|{ayah}|" + "\t".join(glosses[i:i + count]) + "\n")
            i += count
    assert i == len(glosses), (i, len(glosses))
    assert len(rows) == 6236, len(rows)
    with open(os.path.join(OUT, WBW["out"]), "w", encoding="utf-8") as f:
        f.write(header(WBW["header"]))
        f.writelines(rows)


def main():
    cache = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "build/tafseer-cache")
    for t in TAFSEER:
        build_tafseer(t, cache)
    build_wbw(cache)


if __name__ == "__main__":
    main()
