#!/usr/bin/env bash
#
# Builds Guess Market into one jar per module, exactly the way it is submitted.
#
#   out/classes/...      compiled classes, one folder per module
#   out/artifacts/       dto.jar, engine.jar, ui.jar, the lib folder and the run scripts
#
# Run ./run.sh afterwards to start the program from those jars, outside the IDE.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

CLASSES_DIR="out/classes"
ARTIFACTS_DIR="out/artifacts"
MAIN_CLASS="guessmarket.ui.GuessMarketApplication"
JAVA_RELEASE="25"

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
    echo "  compiling $module"
    mkdir -p "$CLASSES_DIR/$module"
    # -Xlint:serial is left out on purpose: the domain classes are serializable and hold their
    # collections behind the List and Map interfaces, which is good design but which that
    # particular check always complains about.
    javac --release "$JAVA_RELEASE" -Xlint:all,-serial -Werror \
        -classpath "$classpath" \
        -d "$CLASSES_DIR/$module" \
        $(find "$module/src" -name '*.java')
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

echo "Compiling the modules"
compile_module dto ""
compile_module engine "$CLASSES_DIR/dto:$LIB_CLASSPATH"
compile_module ui "$CLASSES_DIR/dto:$CLASSES_DIR/engine"

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
cp run.bat run.sh "$ARTIFACTS_DIR/"
chmod +x "$ARTIFACTS_DIR/run.sh"

echo
echo "Build finished. The runnable program is in $ARTIFACTS_DIR:"
ls -1 "$ARTIFACTS_DIR"
