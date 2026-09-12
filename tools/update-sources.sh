#!/usr/bin/env bash
# Refreshes anime-sources/upstream with the latest Kohi-den/extensions-source (Apache 2.0).
# Only the parts Watanuki compiles are checked out: src/es, lib, lib-multisrc, core, buildSrc, gradle.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/anime-sources/upstream"
TMP="$(mktemp -d)"

git clone --depth 1 --filter=blob:none --sparse https://github.com/Kohi-den/extensions-source "$TMP/upstream"
git -C "$TMP/upstream" sparse-checkout set src/es lib lib-multisrc core buildSrc gradle
REV="$(git -C "$TMP/upstream" rev-parse --short HEAD)"
rm -rf "$TMP/upstream/.git"

rm -rf "$DEST"
mv "$TMP/upstream" "$DEST"
echo "$REV" > "$DEST/UPSTREAM_REVISION"
rm -rf "$TMP"

echo "upstream updated to Kohi-den/extensions-source@$REV"
echo "now build: ./gradlew :anime-sources:compileDebugKotlin"
