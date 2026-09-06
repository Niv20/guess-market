#!/usr/bin/env bash
#
# Builds the project and packs everything that is handed in into a single zip:
# the three jars, the third party jars, the JavaFX runtime, the Windows run script and the readme.
#
# The zip unpacks into one folder, so the grader can extract it anywhere and run run.bat straight
# away without moving files around or installing anything beyond Java itself.
#
# Only the Windows build of JavaFX goes into the zip, and run.sh is left out, because the
# submission targets the Windows grader. The repository carries all three platforms so that the
# project can be built and run on any of them.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

SUBMISSION_NAME="GuessMarket-Ex2"
STAGING_DIR="out/submission/$SUBMISSION_NAME"
ZIP_FILE="out/$SUBMISSION_NAME.zip"
SUBMITTED_JAVAFX_PLATFORM="win"

./build.sh

echo
echo "Collecting the submission"
rm -rf "out/submission" "$ZIP_FILE"
mkdir -p "$STAGING_DIR/lib/javafx"
cp out/artifacts/dto.jar out/artifacts/engine.jar out/artifacts/ui.jar "$STAGING_DIR/"
cp lib/*.jar "$STAGING_DIR/lib/"
cp -R "lib/javafx/$SUBMITTED_JAVAFX_PLATFORM" "$STAGING_DIR/lib/javafx/"
cp run.bat "$STAGING_DIR/"

# The sample files the exercise ships with, so the grader has something to load immediately.
if [ -d data ]; then
    # Copied whole, so the folder whose name contains spaces travels with it.
    cp -R data "$STAGING_DIR/data"
fi

if [ -f readme.docx ]; then
    cp readme.docx "$STAGING_DIR/"
else
    echo "  WARNING: readme.docx was not found, so the zip has no readme in it."
fi

(cd out/submission && zip -q -r "../../$ZIP_FILE" "$SUBMISSION_NAME")

echo
echo "Submission ready: $ZIP_FILE"
du -h "$ZIP_FILE"
unzip -l "$ZIP_FILE" | tail -20
