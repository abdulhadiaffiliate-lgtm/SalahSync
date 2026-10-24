#!/usr/bin/env bash
# Downloads Mishary Rashid Alafasy's recitation (64 kbps, one MP3 per ayah) of
# Al-Fatihah and surahs 83 to 114 from EveryAyah.com into the app's assets, so
# those 33 short surahs play offline. The files are not committed to git; CI
# runs this before building, and you can run it locally the same way.
#
# Usage: scripts/fetch-bundled-audio.sh [output dir]
set -euo pipefail

out="${1:-$(dirname "$0")/../app/src/main/assets/audio/alafasy}"
base="${AUDIO_BASE:-https://everyayah.com/data/Alafasy_64kbps}"
mkdir -p "$out"

# Ayah counts: surah 1, then surahs 83..114.
declare -A counts=([1]=7)
short=(36 25 22 17 19 26 30 20 15 21 11 8 8 19 5 8 8 11 11 8 3 9 5 4 7 3 6 3 5 4 5 6)
for i in "${!short[@]}"; do counts[$((83 + i))]=${short[$i]}; done

list=$(mktemp)
trap 'rm -f "$list"' EXIT
for surah in "${!counts[@]}"; do
  for ((ayah = 1; ayah <= counts[$surah]; ayah++)); do
    name=$(printf '%03d%03d.mp3' "$surah" "$ayah")
    [[ -s "$out/$name" ]] || echo "$name" >> "$list"
  done
done

total=$(wc -l < "$list")
echo "Fetching $total recitation files into $out"
if [[ "$total" -gt 0 ]]; then
  xargs -P 8 -I{} sh -c 'curl -fsSL --retry 4 --retry-delay 2 -o "$1/$2.part" "$3/$2" && mv "$1/$2.part" "$1/$2"' _ "$out" {} "$base" < "$list"
fi

expected=0
for surah in "${!counts[@]}"; do expected=$((expected + counts[$surah])); done
have=$(find "$out" -name '*.mp3' -size +0 | wc -l)
echo "Bundled audio: $have of $expected files, $(du -sh "$out" | cut -f1)"
[[ "$have" -eq "$expected" ]]
