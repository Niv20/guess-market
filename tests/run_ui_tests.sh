#!/usr/bin/env bash
# Run after build.sh. Requires an interactive desktop/display; no market server is used.
set -euo pipefail
cd "$(dirname "$0")/.."
case "$(uname -s)/$(uname -m)" in
    Darwin/arm64) default_platform=mac-aarch64 ;;
    Darwin/*) default_platform=mac ;;
    MINGW*|MSYS*|CYGWIN*) default_platform=win ;;
    *) default_platform=linux ;;
esac
javafx_dir="lib/javafx/${JAVAFX_PLATFORM:-$default_platform}"
mkdir -p out/ui-test-classes
classpath='out/artifacts/client/client.jar:out/artifacts/client/dto.jar:out/artifacts/client/ui-common.jar:out/artifacts/client/lib/gson-2.13.2.jar'
# Java on Windows uses semicolon classpaths even from a Unix-compatible shell.
case "$default_platform" in win) classpath="${classpath//:/;}"; separator=';' ;; *) separator=':' ;; esac
javac --release 25 -Xlint:all,-serial -Werror --module-path "$javafx_dir" \
    --add-modules javafx.controls,javafx.fxml -classpath "$classpath" -d out/ui-test-classes \
    tests/client/CharacterCountTest.java tests/client/ChatDrawerTest.java tests/client/ClientUiTest.java
for test in CharacterCountTest ChatDrawerTest ClientUiTest; do
    java -Djavafx.cachedir=out/javafx-cache --enable-native-access=javafx.graphics \
        --module-path "$javafx_dir" --add-modules javafx.controls,javafx.fxml \
        -classpath "out/ui-test-classes$separator$classpath" "guessmarket.client.$test"
done
