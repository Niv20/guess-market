#!/usr/bin/env bash
# Start the local Exercise 3 server; the JavaFX client is launched separately.
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME="$PROJECT_DIR/.runtime"
SERVER_BASE="$RUNTIME/tomcat-base"
PID_FILE="$SERVER_BASE/tomcat.pid"
PORT=8080
BUILD=true
STOP=false
fail() { echo "$*" >&2; exit 1; }
while [ "$#" -gt 0 ]; do
    case "$1" in
        --no-build) BUILD=false; shift ;;
        --stop) STOP=true; shift ;;
        --port)
            [ "$#" -ge 2 ] || fail "--port requires a number."
            PORT="$2"; shift 2 ;;
        --help|-h)
            echo 'Usage: ./start-guess-market.sh [--port 8080] [--no-build]'
            echo '       ./start-guess-market.sh --stop'
            echo 'Starts only the server. First run downloads Tomcat if needed.'
            exit 0 ;;
        *) fail "Unknown option: $1. Use --help for usage." ;;
    esac
done
case "$PORT" in ''|*[!0-9]*) fail 'Port must be a number from 1 to 65535.' ;; esac
[ "${#PORT}" -le 5 ] || fail 'Port must be a number from 1 to 65535.'
PORT=$((10#$PORT))
[ "$PORT" -ge 1 ] && [ "$PORT" -le 65535 ] || fail 'Port must be a number from 1 to 65535.'
mkdir -p "$RUNTIME"
mkdir "$RUNTIME/launcher.lock" 2>/dev/null || fail 'Another server command is running. Try again when it finishes.'
trap 'rmdir "$RUNTIME/launcher.lock" 2>/dev/null || true' EXIT
# Never stop a process unless both its PID and Catalina base identify our server.
server_running() {
    [ -f "$PID_FILE" ] || return 1
    PID="$(cat "$PID_FILE")"
    case "$PID" in ''|*[!0-9]*) return 1 ;; esac
    PROCESS_ARGS="$(ps -p "$PID" -o args= 2>/dev/null)" || return 1
    case "$PROCESS_ARGS" in *"-Dcatalina.base=$SERVER_BASE "*"org.apache.catalina.startup.Bootstrap"*) return 0 ;; esac
    return 1
}
# Identify this instance in the response, even if another server owns the port.
server_ready() {
    local port="$1" expected headers
    [ -f "$SERVER_BASE/instance-id" ] || return 1
    expected="$(cat "$SERVER_BASE/instance-id")"
    headers="$(curl --noproxy '*' -fsS --max-time 1 -D - -o /dev/null \
        "http://localhost:$port/guess-market/api/health" 2>/dev/null)" || return 1
    printf '%s\n' "$headers" | awk -v expected="$expected" '
        tolower($1) == "server:" {gsub(/\r/, "", $2); if ($2 == expected) found=1}
        END {exit !found}'
}
if $STOP; then
    if server_running; then
        kill -TERM "$PID"
        for ((attempt=0; attempt<20; attempt++)); do
            if ! server_running; then
                rm -f "$PID_FILE"
                echo 'Guess Market server stopped. In-memory market data has been cleared.'
                exit 0
            fi
            sleep 0.5
        done
        fail "The server is still shutting down. Check $SERVER_BASE/logs/catalina.out"
    fi
    echo 'The local Guess Market server is not running.'
    exit 0
fi
command -v curl >/dev/null || fail 'Install curl to download Tomcat and check server readiness.'
if server_running; then
    RUNNING_PORT="$(cat "$SERVER_BASE/http-port")"
    if server_ready "$RUNNING_PORT"; then
        echo "Guess Market server is already running: http://localhost:$RUNNING_PORT/guess-market/api/"
        echo 'To rebuild or change ports, stop it first: ./start-guess-market.sh --stop'
        exit 0
    fi
    fail "The local server is running but not ready. Check $SERVER_BASE/logs/catalina.out"
fi
if [ -n "${JAVA_HOME:-}" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
else
    JAVAC_PATH="$(command -v javac)" || fail 'Install JDK 25 or newer first.'
    # Ask Java for its real home; /usr/bin/java may be a launcher on macOS.
    JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 | awk '/java.home =/ {sub(/^.*java.home = /, ""); print; exit}')"
    export JAVA_HOME
fi
[ -x "$JAVA_HOME/bin/javac" ] || fail 'JAVA_HOME must point to JDK 25 or newer.'
JAVA_MAJOR="$("$JAVA_HOME/bin/javac" -version 2>&1 | awk '{split($2, parts, "."); print parts[1]}')"
case "$JAVA_MAJOR" in ''|*[!0-9]*) fail 'Could not determine the Java version. Use JDK 25 or newer.' ;; esac
[ "$JAVA_MAJOR" -ge 25 ] || fail "JDK 25 or newer is required; found Java $JAVA_MAJOR."
# Give a useful conflict message without stopping or modifying another server.
if command -v lsof >/dev/null && lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    fail "Port $PORT is in use. Choose another port: ./start-guess-market.sh --port 8081"
fi
TOMCAT_VERSION=10.1.59
TOMCAT_HOME="${CATALINA_HOME:-$RUNTIME/apache-tomcat-$TOMCAT_VERSION}"
if [ ! -f "$TOMCAT_HOME/bin/catalina.sh" ]; then
    [ -z "${CATALINA_HOME:-}" ] || fail "CATALINA_HOME is not a Tomcat directory: $CATALINA_HOME. Unset it to use automatic setup."
    command -v shasum >/dev/null || fail 'Install shasum to verify the Tomcat download.'
    ARCHIVE="apache-tomcat-$TOMCAT_VERSION.tar.gz"
    DOWNLOAD_DIR="$(mktemp -d "$RUNTIME/download.XXXXXX")"
    trap 'rm -rf "${DOWNLOAD_DIR:-}"; rmdir "$RUNTIME/launcher.lock" 2>/dev/null || true' EXIT
    echo "Downloading Apache Tomcat $TOMCAT_VERSION (first run only)…"
    DOWNLOADED=false
    for SOURCE in "https://dlcdn.apache.org/tomcat/tomcat-10/v$TOMCAT_VERSION/bin" "https://archive.apache.org/dist/tomcat/tomcat-10/v$TOMCAT_VERSION/bin"; do
        if curl -fLsS --connect-timeout 15 --max-time 180 "$SOURCE/$ARCHIVE" -o "$DOWNLOAD_DIR/$ARCHIVE" && \
           curl -fLsS --connect-timeout 15 --max-time 30 "$SOURCE/$ARCHIVE.sha512" -o "$DOWNLOAD_DIR/checksum"; then
            DOWNLOADED=true; break
        fi
    done
    $DOWNLOADED || fail 'Tomcat download failed. Check your internet connection, then run the command again.'
    EXPECTED="$(awk '{print $1; exit}' "$DOWNLOAD_DIR/checksum")"
    ACTUAL="$(shasum -a 512 "$DOWNLOAD_DIR/$ARCHIVE" | awk '{print $1}')"
    [ "$ACTUAL" = "$EXPECTED" ] || fail 'Tomcat checksum did not match. Run the command again to download a fresh copy.'
    tar -xzf "$DOWNLOAD_DIR/$ARCHIVE" -C "$DOWNLOAD_DIR"
    mv "$DOWNLOAD_DIR/apache-tomcat-$TOMCAT_VERSION" "$TOMCAT_HOME"
    rm -rf "$DOWNLOAD_DIR"
fi
VERSION_INFO="$("$JAVA_HOME/bin/java" -cp "$TOMCAT_HOME/lib/catalina.jar" org.apache.catalina.util.ServerInfo)"
case "$VERSION_INFO" in *"Apache Tomcat/10.1."*) ;; *) fail 'Use Apache Tomcat 10.1 (Jakarta Servlet 6).' ;; esac
if $BUILD; then
    "$PROJECT_DIR/build.sh" --server-only
fi
WAR_PATH="$PROJECT_DIR/out/artifacts/guess-market.war"
[ -f "$WAR_PATH" ] || fail 'WAR not found. Run this command without --no-build.'
mkdir -p "$SERVER_BASE/bin" "$SERVER_BASE/conf" "$SERVER_BASE/logs" "$SERVER_BASE/temp" "$SERVER_BASE/work" "$SERVER_BASE/webapps"
# Tomcat otherwise falls back to the installation's setenv.sh, which may configure another server.
cat > "$SERVER_BASE/bin/setenv.sh" <<'SH'
#!/bin/sh
# Runtime settings are supplied by this project's launcher, not by the shared Tomcat installation.
:
SH
cp "$TOMCAT_HOME/conf/web.xml" "$SERVER_BASE/conf/web.xml"
cp "$TOMCAT_HOME/conf/logging.properties" "$SERVER_BASE/conf/logging.properties"
INSTANCE_FILE="$(mktemp "$SERVER_BASE/instance.XXXXXXXX")"
INSTANCE_ID="GuessMarket-$(basename "$INSTANCE_FILE")"
rm -f "$INSTANCE_FILE"
printf '%s\n' "$INSTANCE_ID" > "$SERVER_BASE/instance-id"
cat > "$SERVER_BASE/conf/server.xml" <<XML
<?xml version="1.0" encoding="UTF-8"?>
<Server port="-1">
  <Service name="Catalina">
    <Connector address="127.0.0.1" port="$PORT" protocol="HTTP/1.1" connectionTimeout="20000" server="$INSTANCE_ID" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false" deployOnStartup="true" />
    </Engine>
  </Service>
</Server>
XML
# Only our stopped deployment is replaced; no other Tomcat installation is touched.
rm -rf "$SERVER_BASE/webapps/guess-market" "$SERVER_BASE/work/Catalina/localhost/guess-market"
cp "$WAR_PATH" "$SERVER_BASE/webapps/guess-market.war"
printf '%s\n' "$PORT" > "$SERVER_BASE/http-port"
rm -f "$PID_FILE"
export CATALINA_HOME="$TOMCAT_HOME" CATALINA_BASE="$SERVER_BASE" CATALINA_PID="$PID_FILE"
export CATALINA_TMPDIR="$SERVER_BASE/temp" CATALINA_OUT="$SERVER_BASE/logs/catalina.out"
# Tomcat prefers JRE_HOME and _RUNJAVA over JAVA_HOME; keep the validated JDK and all logs local.
export JRE_HOME="$JAVA_HOME"
unset _RUNJAVA CATALINA_OUT_CMD
"$TOMCAT_HOME/bin/startup.sh" > "$SERVER_BASE/logs/startup.out" 2>&1 || {
    cat "$SERVER_BASE/logs/startup.out" >&2
    fail 'Tomcat could not start.'
}
for ((attempt=0; attempt<60; attempt++)); do
    if server_running && server_ready "$PORT"; then
        echo "Guess Market server is ready: http://localhost:$PORT/guess-market/api/"
        echo "Logs: $SERVER_BASE/logs/catalina.out"
        echo 'Stop: ./start-guess-market.sh --stop'
        exit 0
    fi
    sleep 0.5
done
if server_running; then kill -TERM "$PID"; fi
tail -n 40 "$SERVER_BASE/logs/catalina.out" >&2 || true
fail 'Server did not become ready. Check the log above for the cause.'
