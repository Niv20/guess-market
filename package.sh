#!/usr/bin/env bash
# Exactly one WAR, a complete Windows client, and the Word submission guide.
set -euo pipefail
cd "$(dirname "$0")"
./build.sh
SUBMISSION_NAME=GuessMarket-Ex3
STAGING_DIR="out/submission/$SUBMISSION_NAME"
ZIP_FILE="out/$SUBMISSION_NAME.zip"
[ -f readme.docx ] || { echo 'The required readme.docx is missing.' >&2; exit 1; }
rm -rf "$STAGING_DIR" "$ZIP_FILE"
mkdir -p "$STAGING_DIR/client/lib/javafx"
cp out/artifacts/guess-market.war readme.docx "$STAGING_DIR/"
cp out/artifacts/client/client.jar out/artifacts/client/dto.jar out/artifacts/client/ui-common.jar "$STAGING_DIR/client/"
cp out/artifacts/client/lib/gson-2.13.2.jar "$STAGING_DIR/client/lib/"
cp -R lib/javafx/win "$STAGING_DIR/client/lib/javafx/"
cp run.bat "$STAGING_DIR/client/"
(cd out/submission && zip -q -r "../$SUBMISSION_NAME.zip" "$SUBMISSION_NAME")
echo "Submission ready: $ZIP_FILE"
