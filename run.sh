#!/usr/bin/env bash
# Guess Market - requires Java 25 or newer. Run ./build.sh first if needed.
#
# JavaFX is not part of the JDK, so it is handed to the java command from the lib/javafx folder
# that matches this machine.
cd "$(dirname "$0")"
[ -f ui.jar ] || cd out/artifacts

case "$(uname -s)/$(uname -m)" in
    Darwin/arm64) PLATFORM="mac-aarch64" ;;
    Darwin/*)     PLATFORM="mac" ;;
    *)            PLATFORM="linux" ;;
esac
PLATFORM="${JAVAFX_PLATFORM:-$PLATFORM}"

java --module-path "lib/javafx/$PLATFORM" \
     --add-modules javafx.controls,javafx.fxml \
     --enable-native-access=javafx.graphics \
     -jar ui.jar
