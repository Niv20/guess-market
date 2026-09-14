#!/usr/bin/env bash
# Starts the Exercise 3 client. Deploy guess-market.war in Tomcat first.
set -euo pipefail
cd "$(dirname "$0")"
[ -f client.jar ] || cd out/artifacts/client
case "$(uname -s)/$(uname -m)" in
    Darwin/arm64) DEFAULT_PLATFORM=mac-aarch64 ;;
    Darwin/*) DEFAULT_PLATFORM=mac ;;
    MINGW*|MSYS*|CYGWIN*) DEFAULT_PLATFORM=win ;;
    *) DEFAULT_PLATFORM=linux ;;
esac
PLATFORM="${JAVAFX_PLATFORM:-$DEFAULT_PLATFORM}"
exec java --module-path "lib/javafx/$PLATFORM" \
    --add-modules javafx.controls,javafx.fxml \
    --enable-native-access=javafx.graphics -jar client.jar "$@"
