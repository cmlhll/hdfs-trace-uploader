#!/usr/bin/env bash
set -euo pipefail

# Example script for local pseudo-distributed HDFS E2E test.
# This script assumes Hadoop is already installed and HDFS is running.
# It also assumes Codex has implemented the runnable jar.

SPOOL=/tmp/trace_spool
FILE="trace-payment-dev-local-c1-host001-pid123-bootlocal-20260610T100000-20260610T100500-seq000001.jsonl"

mkdir -p "$SPOOL"/{writing,sealed,committed,failed,quarantine,state,tmp}

hdfs dfs -mkdir -p /warehouse/raw_trace/_staging /warehouse/raw_trace_manifest
hdfs dfs -chmod -R 777 /warehouse/raw_trace /warehouse/raw_trace_manifest || true

cat > "$SPOOL/sealed/$FILE" <<'DATA'
{"ts":"2026-06-10T10:00:01Z","trace_id":"t1","span_id":"s1","level":"INFO","message":"hello"}
{"ts":"2026-06-10T10:00:02Z","trace_id":"t2","span_id":"s2","level":"ERROR","message":"failed"}
DATA

touch "$SPOOL/sealed/$FILE.done"

java -jar target/hdfs-trace-uploader.jar --config config/local-hdfs-agent.yaml --once

echo "=== HDFS final files ==="
hdfs dfs -ls -R /warehouse/raw_trace/app=payment/dt=2026-06-10/hour=10 || true

echo "=== Local manifest ==="
cat "$SPOOL/state/manifest.jsonl" || true
