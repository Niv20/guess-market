#!/usr/bin/env bash
#
# Builds the project and packs everything that is handed in into a single zip:
# the three jars, the third party jars, the run scripts and the readme.
#
# The zip unpacks into one folder, so the grader can extract it anywhere and run run.bat
# straight away without moving files around.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

SUBMISSION_NAME="GuessMarket-Ex1"
STAGING_DIR="out/submission/$SUBMISSION_NAME"
ZIP_FILE="out/$SUBMISSION_NAME.zip"

./build.sh

echo
echo "Collecting the submission"
rm -rf "out/submission" "$ZIP_FILE"
mkdir -p "$STAGING_DIR"
cp -R out/artifacts/. "$STAGING_DIR/"

if [ -f readme.docx ]; then
    cp readme.docx "$STAGING_DIR/"
else
    echo "  WARNING: readme.docx was not found, so the zip has no readme in it."
fi

(cd out/submission && zip -q -r "../../$ZIP_FILE" "$SUBMISSION_NAME")

echo
echo "Submission ready: $ZIP_FILE"
unzip -l "$ZIP_FILE"
