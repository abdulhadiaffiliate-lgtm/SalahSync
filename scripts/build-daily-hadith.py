#!/usr/bin/env python3
"""Builds the bundled hadith: app/src/main/assets/hadith/daily.json, the Hadith of the
Day, and app/src/main/assets/hadith/feelings.json, the hadith of the Feelings screen.

The picks (book and number) are read from DailyHadith in core/.../hadith/HadithData.kt
and from Feelings in core/.../feelings/Feelings.kt, and their Arabic, English and Urdu text is copied
verbatim from fawazahmed0/hadith-api at the commit pinned in HadithSource. Every
download is checked against the SHA-256 recorded there, so the bundled text is
exactly the dataset's. Nothing here writes or edits hadith text by hand.

Usage: scripts/build-daily-hadith.py [cache dir]
"""
import hashlib
import json
import os
import re
import sys
import urllib.request

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
KOTLIN = os.path.join(ROOT, "core/src/main/kotlin/com/salahsync/core/hadith/HadithData.kt")
FEELINGS = os.path.join(ROOT, "core/src/main/kotlin/com/salahsync/core/feelings/Feelings.kt")
ASSETS = os.path.join(ROOT, "app/src/main/assets/hadith")
SLUGS = {"BUKHARI": "bukhari", "MUSLIM": "muslim", "ABU_DAWUD": "abudawud", "TIRMIDHI": "tirmidhi"}


def picks_in(path):
    """Each HadithPick in a Kotlin file, in order, each once."""
    text = open(path, encoding="utf-8").read()
    found = [(SLUGS[b], ref) for b, ref in re.findall(r'HadithPick\(HadithBook\.([A-Z_]+), "([0-9a-z]+)"', text)]
    return list(dict.fromkeys(found))


def source():
    text = open(KOTLIN, encoding="utf-8").read()
    commit = re.search(r'COMMIT = "([0-9a-f]{40})"', text).group(1)
    hashes = dict(re.findall(r'"((?:ara|eng|urd)-[a-z]+)" to "([0-9a-f]{64})"', text))
    return commit, hashes, picks_in(KOTLIN)


def fetch(name, commit, sha, cache):
    path = os.path.join(cache, name + ".min.json")
    if not os.path.exists(path):
        url = f"https://raw.githubusercontent.com/fawazahmed0/hadith-api/{commit}/editions/{name}.min.json"
        print("Downloading", url, file=sys.stderr)
        with urllib.request.urlopen(url) as r, open(path + ".part", "wb") as f:
            f.write(r.read())
        os.replace(path + ".part", path)
    data = open(path, "rb").read()
    if hashlib.sha256(data).hexdigest() != sha:
        sys.exit(f"{name}: SHA-256 does not match HadithSource; refusing to use it")
    return json.loads(data)


def ref_of(book, h):
    """The sunnah.com number, as HadithNumber.of works it out in the app."""
    n = str(h["hadithnumber"]).removesuffix(".0")
    if book != "muslim":
        return n
    a = str(h.get("arabicnumber") or "")
    if not a:
        return "intro-" + n
    whole, _, part = a.partition(".")
    return whole + chr(ord("a") + int(part) - 1) if part and 0 < int(part) <= 26 else whole


def build(picks, out_name, commit, hashes, cache, editions):
    out = []
    for book, ref in picks:
        for lang in ("ara", "eng", "urd"):
            name = f"{lang}-{book}"
            if name not in editions:
                editions[name] = fetch(name, commit, hashes[name], cache)
        eng = editions[f"eng-{book}"]
        match = [h for h in eng["hadiths"] if ref_of(book, h) == ref]
        if len(match) != 1:
            sys.exit(f"{book} {ref}: found {len(match)} hadith, expected 1")
        h = match[0]
        texts = {}
        for lang in ("ara", "urd"):
            same = [x for x in editions[f"{lang}-{book}"]["hadiths"] if x["hadithnumber"] == h["hadithnumber"]]
            texts[lang] = same[0]["text"] if len(same) == 1 else ""
        entry = {
            "book": book,
            "ref": ref,
            "chapter": eng["metadata"]["sections"].get(str(h["reference"]["book"]), ""),
            "arabic": texts["ara"].strip(),
            "english": h["text"].strip(),
            "urdu": texts["urd"].strip(),
        }
        for key in ("arabic", "english", "urdu"):
            if not entry[key]:
                sys.exit(f"{book} {ref}: no {key} text in the dataset; pick another hadith")
        out.append(entry)
    path = os.path.join(ASSETS, out_name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump({"source": f"fawazahmed0/hadith-api@{commit}", "hadith": out}, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"Wrote {len(out)} hadith to {os.path.relpath(path, ROOT)}", file=sys.stderr)


def main():
    cache = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "build", "hadith-cache")
    os.makedirs(cache, exist_ok=True)
    commit, hashes, daily = source()
    for book, ref in daily:
        # Hadith of the Day is sahih only: Bukhari or Muslim, outside Muslim's introduction.
        if book not in ("bukhari", "muslim") or ref.startswith("intro-"):
            sys.exit(f"{book} {ref}: Hadith of the Day must be sahih (Bukhari or Muslim)")
    editions = {}
    build(daily, "daily.json", commit, hashes, cache, editions)
    build(picks_in(FEELINGS), "feelings.json", commit, hashes, cache, editions)


if __name__ == "__main__":
    main()
