#!/usr/bin/env bash
#
# Guess Market - exercise 1.
#
# Keep this file next to ui.jar, engine.jar, dto.jar and the lib folder.
# Java 25 or newer must be installed and reachable from the command line.
#
# When it is run from the project itself it starts the program from out/artifacts,
# so ./build.sh has to have been run at least once first.

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

if [ ! -f ui.jar ] && [ -f out/artifacts/ui.jar ]; then
    cd out/artifacts
fi

if [ ! -f ui.jar ]; then
    echo "ui.jar was not found. Run ./build.sh first."
    exit 1
fi

java -jar ui.jar
