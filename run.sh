#!/usr/bin/env bash
# Guess Market - requires Java 25+. Run ./build.sh first if needed.
cd "$(dirname "$0")"
[ -f ui.jar ] || cd out/artifacts
java -jar ui.jar
