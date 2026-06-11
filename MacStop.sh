#!/usr/bin/env bash
set -euo pipefail

PIDS="$(pgrep -f 'show2pc.Main' || true)"

if [[ -z "$PIDS" ]]; then
  echo "Show2PC is not running."
  exit 0
fi

echo "Stopping Show2PC: $PIDS"
kill $PIDS
