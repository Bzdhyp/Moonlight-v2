#!/usr/bin/env bash
set -euo pipefail

# Resolve project root so the script works regardless of CWD.
PROJECT_ROOT="$(cd "${BASH_SOURCE[0]%/*}/.." && pwd)"
cd "$PROJECT_ROOT"
GRADLEW="$PROJECT_ROOT/gradlew"

if [ ! -f "$GRADLEW" ]; then
  echo "ERROR: gradlew not found at $GRADLEW" >&2
  exit 1
fi

# Build the project (plain classes + JAR).
"$GRADLEW" assemble --console=plain

# Build the fat/uber JAR with all dependencies bundled.
"$GRADLEW" shadowJar --console=plain

echo "Build artifacts:"
ls -la "$PROJECT_ROOT/build/libs"
