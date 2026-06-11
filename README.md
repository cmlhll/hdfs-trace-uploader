# HDFS Trace Uploader Agent

可靠地把 Log4j 产生的 sealed trace log 文件上传到 HDFS，实现文件级不重不丢。

```
Log4j sealed file → 本地扫描 → state machine (JSONL WAL)
  → HDFS staging → verify checksum → atomic rename → final
  → manifest ledger → LOCAL_GC_READY
```

## 架构概要

```
src/main/java/com/company/traceuploader/
├── cli/          TraceUploaderMain       CLI 入口 (--config / --once / --dry-run)
├── config/       10 个 YAML 配置类       Jackson 反序列化
├── scanner/      SealedFileScanner       扫描 .done marker 对应文件
├── metadata/     MetadataService         文件名解析 / SHA-256 / file_id / bucket
├── state/        JsonlWalUploadStateStore JSONL WAL 状态机 (15 个状态)
├── commit/       LocalFsCommitProtocol    staging→verify→rename→final
├── hdfs/         HdfsClient 双实现        LocalFsHdfsClient / HadoopHdfsClient
├── manifest/     JsonlManifestWriter      JSONL 幂等记账 (23 字段)
├── model/        TraceFileMetadata        不可变元数据模型
└── worker/       UploaderWorker           串联所有模块的主 Worker
```

## 构建

```bash
mvn clean test
mvn package -DskipTests
```

目标 jar: `target/hdfs-trace-uploader.jar`（shaded uberjar，约 3.7 MB）

## 快速 LocalFs E2E 测试

无需 Hadoop，用本地文件系统模拟 HDFS 验证完整流程。

### 1. 准备测试数据

```bash
rm -rf /tmp/trace_spool /tmp/fake_hdfs
mkdir -p /tmp/trace_spool/{writing,sealed,committed,failed,quarantine,state,tmp}

cat > /tmp/trace_spool/sealed/trace-payment-dev-local-c1-host001-pid123-bootlocal-20260610T100000-20260610T100500-seq000001.jsonl <<'EOF'
{"ts":"2026-06-10T10:00:01Z","trace_id":"t1","span_id":"s1","level":"INFO","message":"hello"}
{"ts":"2026-06-10T10:00:02Z","trace_id":"t2","span_id":"s2","level":"ERROR","message":"failed"}
EOF

touch /tmp/trace_spool/sealed/trace-payment-dev-local-c1-host001-pid123-bootlocal-20260610T100000-20260610T100500-seq000001.jsonl.done
```

### 2. 首次运行

```bash
java -jar target/hdfs-trace-uploader.jar \
  --config config/example-agent.yaml \
  --once
```

预期输出：`Committed candidate ... -> LOCAL_GC_READY (manifest WRITTEN)`

### 3. 验证 HDFS final 文件

```bash
find /tmp/fake_hdfs -type f
```

预期看到：
```
/tmp/fake_hdfs/warehouse/raw_trace/app=payment/dt=2026-06-10/hour=10/region=local/bucket=006/trace-payment-...seq000001.jsonl
```

### 4. 验证 manifest

```bash
cat /tmp/trace_spool/state/manifest.jsonl | python3 -m json.tool
```

显示 23 个字段：file_id、checksum、hdfs_path、commit_time、state=MANIFEST_COMMITTED 等。

### 5. 重复运行验证幂等

```bash
java -jar target/hdfs-trace-uploader.jar \
  --config config/example-agent.yaml \
  --once
```

预期输出：`Skipping ...; state=LOCAL_GC_READY`

验证：
```bash
echo "HDFS files: $(find /tmp/fake_hdfs -type f | wc -l)"    # 仍为 1
echo "Manifest lines: $(wc -l < /tmp/trace_spool/state/manifest.jsonl)"  # 仍为 1
```

### 6. 多文件测试

```bash
cat > /tmp/trace_spool/sealed/trace-order-prod-local-c2-host002-pid456-bootlocal-20260610T110000-20260610T110500-seq000001.jsonl <<'EOF'
{"ts":"2026-06-10T11:00:01Z","trace_id":"t3","level":"WARN","message":"new"}
EOF
touch /tmp/trace_spool/sealed/trace-order-prod-local-c2-host002-pid456-bootlocal-20260610T110000-20260610T110500-seq000001.jsonl.done
java -jar target/hdfs-trace-uploader.jar --config config/example-agent.yaml --once
```

第二个文件进入不同 app 分区，已有文件自动跳过。

## 本机伪分布式 HDFS 测试

需要 Hadoop 3.x 伪分布式 HDFS 环境。

```bash
export HADOOP_CONF_DIR=/path/to/hadoop/etc/hadoop

# 准备 HDFS 目录
hdfs dfs -mkdir -p /warehouse/raw_trace
hdfs dfs -mkdir -p /warehouse/raw_trace/_staging
hdfs dfs -mkdir -p /warehouse/raw_trace_manifest
hdfs dfs -chmod -R 777 /warehouse/raw_trace /warehouse/raw_trace_manifest

# 运行
java -jar target/hdfs-trace-uploader.jar \
  --config config/local-hdfs-agent.yaml \
  --once

# 验证
hdfs dfs -ls -R /warehouse/raw_trace
hdfs dfs -cat /warehouse/raw_trace/app=payment/dt=2026-06-10/hour=10/region=local/bucket=*/trace-payment-*seq000001.jsonl
```

`local-hdfs-agent.yaml` 使用 `implementation: hadoop`，从 `hadoopConfDir` 加载 `core-site.xml`/`hdfs-site.xml`，通过 Hadoop Java Client 连接真实 HDFS。

## 两种 HdfsClient 实现

| 实现 | 配置 `implementation` | 用途 | 依赖 |
|------|----------------------|------|------|
| `LocalFsHdfsClient` | `localfs` | 快速单测 / CI / 无 Hadoop 环境 | 无 |
| `HadoopHdfsClient` | `hadoop` | 本地伪分布式 / 生产 HDFS | Hadoop 3.3.6 (provided) |

通过 `HdfsClientFactory` 路由。

## 状态机

共 15 个状态，通过 JSONL WAL 持久化，重启可恢复：

```
DISCOVERED → SEALED → CHECKSUMED → UPLOADING → UPLOADED_TO_STAGING
  → VERIFYING → RENAMING → COMMITTED_TO_HDFS → MANIFEST_COMMITTED
  → LOCAL_GC_READY → LOCAL_GC_DONE
```

终止状态：`RETRYABLE_FAILED` / `PERMANENT_FAILED` / `QUARANTINED`

完整状态文件位置：`/tmp/trace_spool/state/upload-state.jsonl`

## HDFS Commit 协议

1. **staging 优先** — 不上传直接到 final 路径
2. **verify** — 上传后校验 size + checksum
3. **atomic rename** — `fs.rename()` staging → final
4. **幂等** — final 已存在 + checksum 一致 → 成功，不重复上传
5. **冲突保护** — final 已存在 + checksum 不一致 → QUARANTINE，不覆盖
6. **crash 恢复** — rename 结果不确定时重新检查 final/staging 状态

详见 `docs/03_HDFS_COMMIT_PROTOCOL.md`

## Manifest

JSONL 格式，每行 23 个字段（蛇形命名）：

```
file_id, app, env, region, cluster, host, pid, boot_id,
dt, hour, bucket, start_time, end_time, hdfs_path,
size_bytes, checksum, record_count, raw_format, compression,
upload_attempt, commit_time, agent_version, state
```

幂等策略：`writeIfAbsent(file_id)` — 每次写入前全量扫描，按 `file_id` 去重。

## 配置

`config/example-agent.yaml` — localfs 模式默认配置，包含 10 个配置段：

| 配置段 | 说明 |
|--------|------|
| `agent` | 身份标识 (app/env/region/cluster/host) |
| `localSpool` | 本地 spool 目录结构 |
| `scanner` | 扫描规则 (marker suffix / whitelist / interval) |
| `hdfs` | HDFS 路径模板 / 实现选择 |
| `upload` | 并发 / 带宽 / 校验策略 |
| `retry` | 指数退避 (max attempts / backoff) |
| `manifest` | manifest 位置和类型 |
| `gc` | 本地 GC 延迟策略 |
| `diskWatermark` | 磁盘水位阈值 |
| `metrics` | Prometheus 端口 |

## 测试

```bash
mvn test
```

23 个单元测试，覆盖 scanner / config / metadata / state / commit / manifest / factory / CLI 各模块。

2 个被 skip 的测试（`HadoopHdfsClientTest`）需要真实 Hadoop 运行时。

## 设计文档

```
docs/
├── 01_PRODUCT_REQUIREMENTS.md
├── 02_ARCHITECTURE_DESIGN.md
├── 03_HDFS_COMMIT_PROTOCOL.md
├── 04_STATE_MACHINE_AND_MANIFEST.md
├── 05_CONFIG_AND_DEPLOYMENT.md
├── 06_TEST_PLAN.md
├── 07_RUNBOOK.md
├── 08_MVP_TASKS.md
├── 09_API_AND_SCHEMA.md
└── 10_LOCAL_HDFS_TEST_GUIDE.md
```

## 暂未实现

详见 `docs/08_MVP_TASKS.md`。当前 MVP 已完成 Phase 0-7 全部功能，上生产前需补齐：

- 常驻扫描模式（当前仅支持 `--once`）
- 本地文件 GC（延迟删除）
- Prometheus metrics 暴露
- MiniDFSCluster 集成测试
- Kerberos 认证 E2E 验证
- 并发上传和限流
- 磁盘水位保护
