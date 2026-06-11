#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SHOW2PC_JAVA_HOME="${SHOW2PC_JAVA_HOME:-/Users/jia/Desktop/service/javaSDK/jdk-17.jdk/Contents/Home}"
JAVAC="$SHOW2PC_JAVA_HOME/bin/javac"
JAVA="$SHOW2PC_JAVA_HOME/bin/java"
CLASSES="$ROOT/target/classes"
RESOURCES="$ROOT/src/main/resources"

if [[ ! -x "$JAVAC" ]]; then
  echo "javac not found: $JAVAC" >&2
  exit 1
fi

mkdir -p "$CLASSES"

find "$ROOT/src/main/java" -name '*.java' -print0 | xargs -0 "$JAVAC" -encoding UTF-8 -d "$CLASSES"

if [[ -d "$RESOURCES" ]]; then
  cp -R "$RESOURCES"/. "$CLASSES"/
fi

exec "$JAVA" -cp "$CLASSES" show2pc.Main
