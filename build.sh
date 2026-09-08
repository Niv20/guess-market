#!/usr/bin/env bash
#
# Builds Guess Market into one jar per module, exactly the way it is submitted.
#
#   out/classes/...      compiled classes and resources, one folder per module
#   out/artifacts/       dto.jar, engine.jar, ui.jar, the lib folder and the run scripts
#
# Run ./run.sh afterwards to start the program from those jars, outside the IDE.
#
# JavaFX is not part of the JDK, so it travels with the project in lib/javafx, one folder per
# operating system. Compiling only needs the classes, which are the same everywhere; running
# needs the native libraries as well, which is why the folder is chosen by the machine.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

CLASSES_DIR="out/classes"
ARTIFACTS_DIR="out/artifacts"
MAIN_CLASS="guessmarket.ui.Main"
JAVA_RELEASE="25"
JAVAFX_MODULES="javafx.controls,javafx.fxml"

# The folder of JavaFX jars that matches this machine. Override it by setting JAVAFX_PLATFORM.
detect_javafx_platform() {
    case "$(uname -s)/$(uname -m)" in
        Darwin/arm64)   echo "mac-aarch64" ;;
        Darwin/*)       echo "mac" ;;
        Linux/*)        echo "linux" ;;
        MINGW*|MSYS*|CYGWIN*) echo "win" ;;
        *)              echo "linux" ;;
    esac
}

JAVAFX_PLATFORM="${JAVAFX_PLATFORM:-$(detect_javafx_platform)}"
JAVAFX_DIR="lib/javafx/$JAVAFX_PLATFORM"

if [ ! -d "$JAVAFX_DIR" ]; then
    echo "There is no JavaFX folder at $JAVAFX_DIR." >&2
    echo "Set JAVAFX_PLATFORM to one of: $(ls lib/javafx | tr '\n' ' ')" >&2
    exit 1
fi

# Every third party jar, written the way it will be found next to the built jars.
lib_classpath_entries() {
    local entries=""
    for jar in lib/*.jar; do
        entries="$entries lib/$(basename "$jar")"
    done
    echo "$entries"
}

compile_module() {
    local module="$1"
    local classpath="$2"
    shift 2
    echo "  compiling $module"
    mkdir -p "$CLASSES_DIR/$module"
    # Gathered one name at a time and kept in an array rather than left to word splitting, so that
    # a project sitting in a folder whose name contains a space still compiles.
    local sources=()
    local source
    while IFS= read -r -d '' source; do
        sources+=("$source")
    done < <(find "$module/src" -name '*.java' -print0)
    # -Xlint:serial is left out on purpose: the domain classes are serializable and hold their
    # collections behind the List and Map interfaces, which is good design but which that
    # particular check always complains about.
    javac --release "$JAVA_RELEASE" -Xlint:all,-serial -Werror \
        -classpath "$classpath" \
        "$@" \
        -d "$CLASSES_DIR/$module" \
        "${sources[@]}"
}

# The layout files and the stylesheets live next to the classes that use them, so they are copied
# into the same folder structure and end up inside the jar at the path the code asks for.
copy_resources() {
    local module="$1"
    local target="$PROJECT_DIR/$CLASSES_DIR/$module"
    echo "  copying $module resources"
    (
        cd "$module/src"
        find . -type f ! -name '*.java' -print | while IFS= read -r resource; do
            mkdir -p "$target/$(dirname "$resource")"
            cp "$resource" "$target/$resource"
        done
    )
}

package_module() {
    local module="$1"
    local manifest="$2"
    echo "  packaging $module.jar"
    jar --create --file "$ARTIFACTS_DIR/$module.jar" \
        --manifest "$manifest" \
        -C "$CLASSES_DIR/$module" .
}

write_manifest() {
    local file="$1"
    local classpath="$2"
    local main_class="${3:-}"
    {
        echo "Manifest-Version: 1.0"
        if [ -n "$classpath" ]; then
            echo "Class-Path:$classpath"
        fi
        if [ -n "$main_class" ]; then
            echo "Main-Class: $main_class"
        fi
    } > "$file"
}

echo "Cleaning previous build output"
rm -rf out
mkdir -p "$CLASSES_DIR" "$ARTIFACTS_DIR"

LIB_ENTRIES="$(lib_classpath_entries)"
LIB_CLASSPATH="lib/*"

echo "Compiling the modules (JavaFX: $JAVAFX_PLATFORM)"
compile_module dto ""
compile_module engine "$CLASSES_DIR/dto:$LIB_CLASSPATH"
compile_module ui "$CLASSES_DIR/dto:$CLASSES_DIR/engine" \
    --module-path "$JAVAFX_DIR" --add-modules "$JAVAFX_MODULES"
copy_resources ui

echo "Building the jars"
MANIFEST_DIR="out/manifests"
mkdir -p "$MANIFEST_DIR"
write_manifest "$MANIFEST_DIR/dto.mf" ""
write_manifest "$MANIFEST_DIR/engine.mf" " dto.jar$LIB_ENTRIES"
write_manifest "$MANIFEST_DIR/ui.mf" " engine.jar dto.jar$LIB_ENTRIES" "$MAIN_CLASS"
package_module dto "$MANIFEST_DIR/dto.mf"
package_module engine "$MANIFEST_DIR/engine.mf"
package_module ui "$MANIFEST_DIR/ui.mf"

echo "Copying the libraries and the run scripts"
mkdir -p "$ARTIFACTS_DIR/lib"
cp lib/*.jar "$ARTIFACTS_DIR/lib/"
cp -R lib/javafx "$ARTIFACTS_DIR/lib/"
cp run.bat run.sh "$ARTIFACTS_DIR/"
chmod +x "$ARTIFACTS_DIR/run.sh"

echo
echo "Build finished. The runnable program is in $ARTIFACTS_DIR:"
ls -1 "$ARTIFACTS_DIR"
