# 04 - 状态机与 Manifest 设计

## 1. 状态机

```text
DISCOVERED
  -> SEALED
  -> CHECKSUMED
  -> UPLOADING
  -> UPLOADED_TO_STAGING
  -> VERIFYING
  -> RENAMING
  -> COMMITTED_TO_HDFS
  -> MANIFEST_COMMITTED
  -> LOCAL_GC_READY
  -> LOCAL_GC_DONE
```

异常状态：

```text
RETRYABLE_FAILED
PERMANENT_FAILED
QUARANTINED
```

## 2. 状态解释

| 状态 | 含义 |
|---|---|
| DISCOVERED | scanner 发现 done marker 和数据文件 |
| SEALED | 文件稳定且可读取 |
| CHECKSUMED | 已计算 size/checksum/record_count |
| UPLOADING | 正在上传 HDFS staging |
| UPLOADED_TO_STAGING | staging 上传完成 |
| VERIFYING | 正在校验 staging |
| RENAMING | 正在 rename 到 final |
| COMMITTED_TO_HDFS | final 文件已经确认存在且正确 |
| MANIFEST_COMMITTED | manifest 已经写入成功 |
| LOCAL_GC_READY | 等待本地延迟删除 |
| LOCAL_GC_DONE | 本地文件和 done marker 已删除或归档 |
| RETRYABLE_FAILED | 可重试失败 |
| PERMANENT_FAILED | 不可重试失败 |
| QUARANTINED | 冲突、损坏、严重异常，等待人工处理 |

## 3. 本地状态表

```sql
CREATE TABLE upload_files (
  file_id TEXT PRIMARY KEY,
  app TEXT,
  env TEXT,
  region TEXT,
  cluster TEXT,
  host TEXT,
  pid TEXT,
  boot_id TEXT,
  local_path TEXT NOT NULL,
  done_path TEXT,
  hdfs_staging_path TEXT,
  hdfs_final_path TEXT,
  dt TEXT,
  hour INTEGER,
  bucket INTEGER,
  start_time_ms INTEGER,
  end_time_ms INTEGER,
  size_bytes INTEGER,
  checksum TEXT,
  record_count INTEGER,
  state TEXT NOT NULL,
  attempt INTEGER DEFAULT 0,
  first_seen_time_ms INTEGER,
  last_update_time_ms INTEGER,
  next_retry_time_ms INTEGER,
  last_error TEXT
);
```

## 4. Manifest 记录

Manifest JSONL 示例：

```json
{
  "file_id": "trace-payment-prod-useast-host001-20260610T100000-seq000123",
  "app": "payment",
  "env": "prod",
  "region": "us-east",
  "cluster": "c1",
  "host": "host001",
  "pid": "2345",
  "boot_id": "boot9f2c",
  "dt": "2026-06-10",
  "hour": 10,
  "bucket": 3,
  "start_time": "2026-06-10T10:00:00Z",
  "end_time": "2026-06-10T10:05:00Z",
  "hdfs_path": "/warehouse/raw_trace/app=payment/dt=2026-06-10/hour=10/region=us-east/bucket=003/file.jsonl.zst",
  "size_bytes": 536870912,
  "checksum": "sha256:...",
  "record_count": 12345678,
  "raw_format": "jsonl",
  "compression": "zstd",
  "upload_attempt": 2,
  "commit_time": "2026-06-10T10:06:12Z",
  "agent_version": "0.1.0",
  "state": "COMMITTED"
}
```

## 5. Manifest 写入要求

- `file_id` 唯一；
- `writeIfAbsent` 语义；
- 已存在且内容一致：幂等成功；
- 已存在但内容不一致：quarantine/报警；
- HDFS final 成功但 manifest 失败时，后续必须补写 manifest；
- manifest 成功后才允许本地 GC。

