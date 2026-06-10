#!/usr/bin/env bash
set -euo pipefail

SEALED_DIR="${1:-/data/trace_spool/sealed}"
MIN_STABLE_SECONDS="${2:-30}"

now=$(date +%s)
find "$SEALED_DIR" -type f \( -name "*.jsonl.zst" -o -name "*.log.zst" \) | while read -r f; do
  [[ -f "$f.done" ]] && continue
  mtime=$(stat -c %Y "$f")
  age=$((now - mtime))
  if [[ "$age" -ge "$MIN_STABLE_SECONDS" ]]; then
    touch "$f.done"
    echo "created marker: $f.done"
  fi
done
