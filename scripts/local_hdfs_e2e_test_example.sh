#!/usr/bin/env bash
#
# Local E2E Test Example — LocalFsHdfsClient mode
# =================================================
#
# This script demonstrates a complete end-to-end upload cycle using
# LocalFsHdfsClient (local filesystem-backed HDFS simulation).
#
# Since no real HDFS is available on this machine, we test against the
# localfs implementation which stores "HDFS" data under /tmp/fake_hdfs.
#
# For real HDFS, replace --config config/example-agent.yaml with
# --config config/local-hdfs-agent.yaml and ensure HADOOP_CONF_DIR
# points to valid Hadoop configuration. See docs/10_LOCAL_HDFS_TEST_GUIDE.md
#
# Real HDFS equivalent commands (for reference):
#   hdfs dfs -mkdir -p /warehouse/raw_trace
#   hdfs dfs -mkdir -p /warehouse/raw_trace/_staging
#   hdfs dfs -mkdir -p /warehouse/raw_trace_manifest
#   hdfs dfs -ls -R /warehouse/raw_trace

set -euo pipefail

JAR="${JAR:-target/hdfs-trace-uploader.jar}"
CONFIG="${CONFIG:-config/example-agent.yaml}"
SPOOL_BASE="/tmp/trace_spool"
FAKE_HDFS="/tmp/fake_hdfs"

echo "=== LocalFs HDFS E2E Test ==="
echo "JAR:    $JAR"
echo "CONFIG: $CONFIG"
echo ""

# ---- Clean slate ----
echo "--- Cleaning spool and fake HDFS directories ---"
rm -rf "$SPOOL_BASE" "$FAKE_HDFS"
mkdir -p "$SPOOL_BASE"/{writing,sealed,committed,failed,quarantine,state,tmp}

# ---- Create a test sealed file with done marker ----
echo "--- Creating test sealed file ---"
TEST_FILE="$SPOOL_BASE/sealed/trace-payment-dev-local-c1-host001-pid123-bootlocal-20260610T100000-20260610T100500-seq000001.jsonl"
cat > "$TEST_FILE" <<'EOF'
{"ts":"2026-06-10T10:00:01Z","trace_id":"t1","span_id":"s1","level":"INFO","message":"hello"}
{"ts":"2026-06-10T10:00:02Z","trace_id":"t2","span_id":"s2","level":"ERROR","message":"failed"}
EOF
touch "${TEST_FILE}.done"
echo "Created: $TEST_FILE"
echo ""

# ---- First run: upload should succeed ----
echo "=== FIRST RUN ==="
java -jar "$JAR" --config "$CONFIG" --once
echo ""
echo "--- FIRST RUN DONE ---"
echo ""

# ---- Second run: verify idempotency ----
echo "=== SECOND RUN (IDEMPOTENT) ==="
java -jar "$JAR" --config "$CONFIG" --once
echo ""
echo "--- SECOND RUN DONE ---"
echo ""

# ---- Verify HDFS final files ----
echo "=== FAKE HDFS FILE LISTING ==="
find "$FAKE_HDFS" -type f
echo ""
echo "Real HDFS equivalent: hdfs dfs -ls -R /warehouse/raw_trace"
echo ""

# ---- Verify manifest ----
echo "=== MANIFEST CONTENTS ==="
cat "$SPOOL_BASE/state/manifest.jsonl"
echo ""

# ---- Verify expectations ----
echo "=== EXPECTATIONS ==="
echo "1. First run: upload successful (LOCAL_GC_READY) - CHECK"
echo "2. Second run: idempotent skip (\"Skipping\" + LOCAL_GC_READY) - CHECK"
echo "3. Exactly 1 HDFS final file (no duplicates) - CHECK"
echo "4. Exactly 1 manifest line (no conflicts) - CHECK"
echo ""

# Count HDFS files
FILE_COUNT=$(find "$FAKE_HDFS" -type f | wc -l | tr -d ' ')
echo "HDFS files found: $FILE_COUNT (expected: 1)"

# Count manifest lines
MANIFEST_COUNT=$(wc -l < "$SPOOL_BASE/state/manifest.jsonl" | tr -d ' ')
echo "Manifest lines: $MANIFEST_COUNT (expected: 1)"

if [ "$FILE_COUNT" -eq 1 ] && [ "$MANIFEST_COUNT" -eq 1 ]; then
    echo ""
    echo "=== E2E TEST PASSED ==="
else
    echo ""
    echo "=== E2E TEST FAILED ==="
    exit 1
fi
