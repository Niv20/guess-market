#!/usr/bin/env bash
# Java 25 build: legacy desktop jars, reusable views, HTTP client and one Tomcat WAR.
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"
CLASSES_DIR="out/classes"
ARTIFACTS_DIR="out/artifacts"
case "$(uname -s)/$(uname -m)" in
    Darwin/arm64) DEFAULT_PLATFORM=mac-aarch64 ;;
    Darwin/*) DEFAULT_PLATFORM=mac ;;
    MINGW*|MSYS*|CYGWIN*) DEFAULT_PLATFORM=win ;;
    *) DEFAULT_PLATFORM=linux ;;
esac
JAVAFX_PLATFORM="${JAVAFX_PLATFORM:-$DEFAULT_PLATFORM}"
JAVAFX_DIR="lib/javafx/$JAVAFX_PLATFORM"
if [ ! -d "$JAVAFX_DIR" ]; then
    echo "Missing JavaFX runtime: $JAVAFX_DIR. Set JAVAFX_PLATFORM to an available folder." >&2
    exit 1
fi
JAVAFX_ARGS=(--module-path "$JAVAFX_DIR" --add-modules javafx.controls,javafx.fxml)
compile_module() {
    local module="$1" classpath="$2"
    shift 2
    local sources=() source
    while IFS= read -r -d '' source; do sources+=("$source"); done < <(find "$module/src" -name '*.java' -print0)
    mkdir -p "$CLASSES_DIR/$module"
    echo "Compiling $module"
    javac --release 25 -Xlint:all,-serial -Werror -classpath "$classpath" "$@" \
        -d "$CLASSES_DIR/$module" "${sources[@]}"
}
copy_resources() {
    local module="$1" target="$PROJECT_DIR/$CLASSES_DIR/$1"
    (cd "$module/src" && find . -type f ! -name '*.java' -print | while IFS= read -r resource; do
        mkdir -p "$target/$(dirname "$resource")"
        cp "$resource" "$target/$resource"
    done)
}
write_manifest() {
    local module="$1" classpath="$2" main="${3:-}"
    {
        echo 'Manifest-Version: 1.0'
        [ -z "$classpath" ] || echo "Class-Path: $classpath"
        [ -z "$main" ] || echo "Main-Class: $main"
        echo
    } > "out/manifests/$module.mf"
}
# Clean generated build products only; tests and submission staging have separate lifetimes.
rm -rf "$CLASSES_DIR" "$ARTIFACTS_DIR" out/manifests out/war
mkdir -p "$ARTIFACTS_DIR/client/lib" out/manifests out/war/WEB-INF/lib
compile_module dto ""
compile_module engine "$CLASSES_DIR/dto:lib/*"
copy_resources engine
compile_module ui "$CLASSES_DIR/dto:$CLASSES_DIR/engine" "${JAVAFX_ARGS[@]}"
copy_resources ui
# Repackage only presentation helpers and DTO-based views; never ship engine code to clients.
mkdir -p "$CLASSES_DIR/ui-common/guessmarket/ui"
cp -R "$CLASSES_DIR/ui/guessmarket/ui/common" "$CLASSES_DIR/ui-common/guessmarket/ui/"
cp -R "$CLASSES_DIR/ui/guessmarket/ui/css" "$CLASSES_DIR/ui-common/guessmarket/ui/"
for view in events/EventDetailsController events/OptionBookController events/EventTile users/UserInvolvementController; do
    mkdir -p "$CLASSES_DIR/ui-common/guessmarket/ui/$(dirname "$view")"
    for file in "$CLASSES_DIR/ui/guessmarket/ui/$view"*.class; do
        cp "$file" "$CLASSES_DIR/ui-common/guessmarket/ui/$(dirname "$view")/"
    done
done
for resource in events/event-details.fxml events/option-book.fxml users/user-involvement.fxml; do
    cp "$CLASSES_DIR/ui/guessmarket/ui/$resource" "$CLASSES_DIR/ui-common/guessmarket/ui/$resource"
done
compile_module server "$CLASSES_DIR/dto:$CLASSES_DIR/engine:lib/*:lib/provided/*"
compile_module client "$CLASSES_DIR/dto:$CLASSES_DIR/ui-common:lib/gson-2.13.2.jar:lib/provided/*" "${JAVAFX_ARGS[@]}"
copy_resources client
LIB_ENTRIES=""
for file in lib/*.jar; do LIB_ENTRIES="$LIB_ENTRIES lib/$(basename "$file")"; done
write_manifest dto ""
write_manifest engine "dto.jar$LIB_ENTRIES"
write_manifest ui "engine.jar dto.jar$LIB_ENTRIES" guessmarket.ui.Main
write_manifest ui-common "dto.jar"
write_manifest client "dto.jar ui-common.jar lib/gson-2.13.2.jar" guessmarket.client.Main
for module in dto engine ui ui-common client; do
    jar --create --file "$ARTIFACTS_DIR/$module.jar" --manifest "out/manifests/$module.mf" -C "$CLASSES_DIR/$module" .
done
cp "$ARTIFACTS_DIR/engine.jar" "$ARTIFACTS_DIR/dto.jar" lib/*.jar out/war/WEB-INF/lib/
cp -R "$CLASSES_DIR/server" out/war/WEB-INF/classes
cp -R server/web/. out/war/
jar --create --file "$ARTIFACTS_DIR/guess-market.war" -C out/war .
cp "$ARTIFACTS_DIR/client.jar" "$ARTIFACTS_DIR/dto.jar" "$ARTIFACTS_DIR/ui-common.jar" "$ARTIFACTS_DIR/client/"
cp lib/gson-2.13.2.jar "$ARTIFACTS_DIR/client/lib/"
cp -R lib/javafx "$ARTIFACTS_DIR/client/lib/"
cp run.sh run.bat "$ARTIFACTS_DIR/client/"
chmod +x "$ARTIFACTS_DIR/client/run.sh"
# Preserve the offline Exercise 2 artifacts for regression runs.
mkdir -p "$ARTIFACTS_DIR/lib"
cp lib/*.jar "$ARTIFACTS_DIR/lib/"
echo "Build complete: $ARTIFACTS_DIR/guess-market.war and $ARTIFACTS_DIR/client"
