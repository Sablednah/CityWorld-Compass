#!/usr/bin/env bash
# Run this checkout's self-test on a fresh CityWorld world and stop the server afterwards.
#   scripts/selftest.sh <jdk17|jdk21|jdk25> [port]
# Needs this line's CityWorld jar at gradle.properties' cityworld_jar (copied into the run dir for you). While
# CityWorld still calls itself an older version than cityworld_version_range asks for, set CW_RANGE, e.g.
#   CW_RANGE='[6.5.0,)' scripts/selftest.sh jdk25
# The server is killed by job-control process group, then by its run directory: never by a name pattern.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
JDK="$1"; PORT="${2:-25620}"
export JAVA_HOME="$ROOT/tools/$JDK" PATH="$ROOT/tools/$JDK/bin:$PATH"
MC=$(grep '^minecraft_version=' gradle.properties | cut -d= -f2); RUN="run-mc$MC"
EXTRA=(); [ -n "${CW_RANGE:-}" ] && EXTRA=("-Pcityworld_version_range=$CW_RANGE")
mkdir -p "$RUN"; echo "eula=true" > "$RUN/eula.txt"
[ -f "$RUN/server.properties" ] || printf 'level-type=cityworld\\:city\nlevel-seed=8675309\nonline-mode=false\nmax-tick-time=-1\npause-when-empty-seconds=0\n' > "$RUN/server.properties"
sed -i "/^server-port=/d" "$RUN/server.properties"; echo "server-port=$PORT" >> "$RUN/server.properties"
rm -rf "$RUN/world" "$RUN/logs/latest.log" "$RUN"/mods/cityworld-*.jar
./gradlew copyCityWorld -q "${EXTRA[@]}" || exit 1
LOG="$ROOT/build/selftest.log"; mkdir -p build
set -m; ./gradlew runServer -Pselftest "${EXTRA[@]}" --console=plain > "$LOG" 2>&1 & PID=$!; set +m
for _ in $(seq 1 200); do
    grep -q "COMPASS SELFTEST: complete" "$RUN/logs/latest.log" 2>/dev/null && break
    kill -0 "$PID" 2>/dev/null || break
    sleep 5
done
RESULT=$(grep -a "COMPASS SELFTEST: \(PASS\|FAIL\)" "$RUN/logs/latest.log" 2>/dev/null | tail -1 | sed 's/.*COMPASS SELFTEST: //')
grep -a "COMPASS SELFTEST:" "$RUN/logs/latest.log" 2>/dev/null | sed 's/.*COMPASS SELFTEST: /  /'
kill -TERM "-$PID" 2>/dev/null; sleep 8; kill -KILL "-$PID" 2>/dev/null; wait "$PID" 2>/dev/null
for p in $(pgrep -x java); do [ "$(readlink /proc/$p/cwd)" = "$ROOT/$RUN" ] && kill "$p"; done
echo "$(basename "$ROOT") $MC: ${RESULT:-NO RESULT (see $LOG)}"
