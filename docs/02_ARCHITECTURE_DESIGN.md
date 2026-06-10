# 02 - 架构设计

## 1. 总体链路

```text
Application
  |
  | Log4j async append
  v
/data/trace_spool/writing/trace.log
  |
  | rollover by size/time
  v
/data/trace_spool/sealed/trace-{file_id}.jsonl.zst
/data/trace_spool/sealed/trace-{file_id}.jsonl.zst.done
  |
  | scan by uploader agent
  v
Local state machine
  |
  | upload
  v
HDFS _staging path
  |
  | verify + rename
  v
HDFS final path
  |
  | manifest
  v
trace_file_manifest
```

## 2. 组件职责

### 2.1 Log4j

- 只负责写本地 active 文件；
- 按大小/时间 rollover；
- 生成唯一文件名；
- 不负责上传 HDFS；
- 不建议自动清理 sealed 文件。

### 2.2 Uploader Agent

- 扫描 sealed 文件；
- 检查 done marker；
- 计算 checksum/size/record_count；
- 维护本地状态；
- 上传 HDFS staging；
- 校验 staging；
- rename final；
- 写 manifest；
- 本地 GC；
- 指标和报警。

### 2.3 HDFS

- 提供 raw trace 目录；
- 提供 `_staging` 临时目录；
- final 文件不可覆盖；
- staging 和 final 必须在同一 namespace。

### 2.4 Manifest

- 作为文件提交账本；
- 支持 file_id 唯一；
- 支持对账、补传、GC 判断；
- MVP 可 JSONL，生产建议 JDBC/Iceberg/Hive 表。

## 3. 本地目录

```text
/data/trace_spool/
  writing/
  sealed/
  committed/
  failed/
  quarantine/
  state/
  tmp/
```

## 4. HDFS 目录

```text
/warehouse/raw_trace/
  _staging/
    app=payment/dt=2026-06-10/hour=10/region=us-east/bucket=003/file_id.attempt_1.tmp
  app=payment/dt=2026-06-10/hour=10/region=us-east/bucket=003/file_id.jsonl.zst
```

## 5. 文件命名

推荐：

```text
trace-{app}-{env}-{region}-{cluster}-{host}-{pid}-{bootId}-{startTs}-{endTs}-seq{seq}.jsonl.zst
```

要求：

- 全局唯一；
- 不复用；
- 可从文件名解析 app、host、time range；
- 最终 file_id 可由文件名、size、checksum 共同确定。

## 6. 数据格式

Raw 层：

```text
JSON Lines + zstd
```

或短期：

```text
plain text line + zstd
```

不建议 MVP 端侧直接写 Parquet/ORC。

## 7. 并发模型

- scanner 定期扫描；
- worker pool 处理上传；
- 单机默认并发 1~2；
- 每个 file_id 同时只能有一个 worker；
- backoff 避免失败风暴；
- 后续可接中心化限流。

