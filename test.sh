#!/usr/bin/env bash
# Engine checks always run; set CATALINA_HOME to add isolated real-HTTP checks.
set -euo pipefail
cd "$(dirname "$0")"
./build.sh
mkdir -p out/test-classes
javac --release 25 -Xlint:all,-serial -Werror \
    -classpath 'out/artifacts/dto.jar:out/artifacts/engine.jar:lib/*' \
    -d out/test-classes tests/engine/OnlineMarketEngineTest.java
java -ea -classpath 'out/test-classes:out/artifacts/dto.jar:out/artifacts/engine.jar:lib/*' \
    guessmarket.tests.OnlineMarketEngineTest
javac --release 25 -Xlint:all,-serial -Werror \
    -classpath 'out/artifacts/client/client.jar:out/artifacts/client/dto.jar:lib/*:lib/provided/*' \
    -d out/test-classes tests/client/ApiClientIntegrationTest.java
if [ -n "${CATALINA_HOME:-}" ]; then
    python3 tests/run_http_tests.py
else
    echo 'Engine checks complete. Set CATALINA_HOME to run the real Tomcat HTTP checks too.'
fi
