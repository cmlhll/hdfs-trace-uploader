# 10 - 本地 HDFS 测试指南

## 1. 目标

本项目必须支持在开发者本机或单机 Hadoop 伪分布式 HDFS 上完成端到端测试。

本地测试要覆盖：

```text
Log4j / 脚本生成 sealed 文件
  -> .done marker
  -> Uploader Agent 扫描
  -> HDFS _staging 上传
  -> size/checksum 校验
  -> rename 到 final
  -> 写 manifest
  -> 本地 committed / GC 状态
```

Codex 实现时必须保证：

1. 不依赖生产 HDFS；
2. 不依赖 Kerberos；
3. 可以通过 `LocalFsHdfsClient` 做快速单测；
4. 可以通过单机伪分布式 HDFS 做真实 HDFS API 集成测试；
5. 后续可以无缝切换到生产 Hadoop HA / RBF 配置。

---

## 2. 测试层级

### 2.1 Level 0: LocalFs 模拟 HDFS

用途：快速单元测试和 CI。

配置：

```yaml
hdfs:
  implementation: localfs
  localRootForTesting: /tmp/trace-uploader-localfs-hdfs
```

LocalFs 模式要模拟以下行为：

- `exists(remotePath)`；
- `upload(localPath, remotePath)`；
- `rename(src, dst)`；
- `delete(path)`；
- `size(path)`；
- `open/read`，用于 checksum 验证；
- final 文件已存在时禁止覆盖。

### 2.2 Level 1: MiniDFSCluster 集成测试

用途：JUnit 集成测试，验证 Hadoop HDFS client 的基础行为。

要求：

- 使用 Hadoop `MiniDFSCluster` 启动临时 NameNode/DataNode；
- 测试 staging -> final rename；
- 测试 final 已存在时幂等判断；
- 测试 staging 残留恢复；
- 测试 Agent kill/restart 后继续提交。

### 2.3 Level 2: 本地伪分布式 HDFS 测试

用途：开发者本机真实 HDFS 测试，验证 `hdfs dfs -ls`、`hdfs dfs -cat`、NameNode WebUI 中可见的真实文件提交行为。

要求：

- 使用 `hdfs://localhost:9000` 或本机 `fs.defaultFS`；
- `hadoopConfDir` 指向本地 Hadoop conf；
- Agent 使用 `HadoopHdfsClient`；
- 测试完成后可以通过 `hdfs dfs -ls` 查看 final 和 manifest。

### 2.4 Level 3: 测试/预发 HDFS

用途：上线前验证 HA、RBF、Kerberos、权限、限流和真实 HDFS 压力。

---

## 3. 本地 HDFS 准备

如果本机已经有伪分布式 HDFS，可直接跳到第 4 节。

典型检查命令：

```bash
hdfs getconf -confKey fs.defaultFS
hdfs dfsadmin -report
hdfs dfs -ls /
```

如果 `fs.defaultFS` 输出类似以下内容，说明可以作为本地 HDFS 测试目标：

```text
hdfs://localhost:9000
```

或：

```text
hdfs://127.0.0.1:9000
```

---

## 4. 创建本地测试目录

```bash
hdfs dfs -mkdir -p /warehouse/raw_trace
hdfs dfs -mkdir -p /warehouse/raw_trace/_staging
hdfs dfs -mkdir -p /warehouse/raw_trace_manifest
hdfs dfs -chmod -R 777 /warehouse/raw_trace /warehouse/raw_trace_manifest
```

本地 spool 目录：

```bash
mkdir -p /tmp/trace_spool/{writing,sealed,committed,failed,quarantine,state,tmp}
```

---

## 5. 本地 HDFS 配置样例

参考 `config/local-hdfs-agent.yaml`。

关键配置：

```yaml
hdfs:
  implementation: hadoop
  hadoopConfDir: /path/to/hadoop/etc/hadoop
  fsDefaultFS: hdfs://localhost:9000
  rawBasePath: /warehouse/raw_trace
  stagingBasePath: /warehouse/raw_trace/_staging
  manifestBasePath: /warehouse/raw_trace_manifest
```

Codex 实现时要求：

1. `HadoopHdfsClient` 优先使用 `hadoopConfDir` 加载 `core-site.xml` 和 `hdfs-site.xml`；
2. 如果配置中显式传入 `fsDefaultFS`，允许覆盖本地测试的 `fs.defaultFS`；
3. 本地 HDFS 测试默认不启用 Kerberos；
4. 生产 Kerberos 配置预留，但不要阻塞本地测试。

---

## 6. 构造测试 sealed 文件

生成一个测试文件：

```bash
cat > /tmp/trace_spool/sealed/trace-payment-dev-local-c1-host001-pid123-bootlocal-20260610T100000-20260610T100500-seq000001.jsonl <<'DATA'
{"ts":"2026-06-10T10:00:01Z","trace_id":"t1","span_id":"s1","level":"INFO","message":"hello"}
{"ts":"2026-06-10T10:00:02Z","trace_id":"t2","span_id":"s2","level":"ERROR","message":"failed"}
DATA
```

如果 MVP 暂时不实现 zstd，可允许 `.jsonl` 作为测试后缀；生产建议 `.jsonl.zst`。

创建 done marker：

```bash
touch /tmp/trace_spool/sealed/trace-payment-dev-local-c1-host001-pid123-bootlocal-20260610T100000-20260610T100500-seq000001.jsonl.done
```

---

## 7. 运行 Agent

Codex 实现 CLI 时至少支持以下命令形态：

```bash
java -jar target/hdfs-trace-uploader.jar \
  --config config/local-hdfs-agent.yaml \
  --once
```

`--once` 表示扫描一轮、处理可上传文件后退出，方便本地测试和 CI。

也需要支持常驻模式：

```bash
java -jar target/hdfs-trace-uploader.jar \
  --config config/local-hdfs-agent.yaml
```

---

## 8. 验证 HDFS final 文件

查看 final 目录：

```bash
hdfs dfs -ls -R /warehouse/raw_trace/app=payment/dt=2026-06-10/hour=10
```

预期看到类似：

```text
/warehouse/raw_trace/app=payment/dt=2026-06-10/hour=10/region=local/bucket=xxx/trace-payment-...seq000001.jsonl
```

确认 staging 目录没有成功任务残留：

```bash
hdfs dfs -ls -R /warehouse/raw_trace/_staging
```

查看文件内容：

```bash
hdfs dfs -cat /warehouse/raw_trace/app=payment/dt=2026-06-10/hour=10/region=local/bucket=*/trace-payment-*seq000001.jsonl
```

---

## 9. 验证 manifest

如果使用本地 JSONL manifest：

```bash
cat /tmp/trace_spool/state/manifest.jsonl
```

如果使用 HDFS JSONL manifest：

```bash
hdfs dfs -ls -R /warehouse/raw_trace_manifest
hdfs dfs -cat /warehouse/raw_trace_manifest/dt=2026-06-10/hour=10/*.jsonl
```

manifest 中必须包含：

- `file_id`；
- `hdfs_path`；
- `size_bytes`；
- `checksum`；
- `record_count`；
- `state=MANIFEST_COMMITTED` 或等价成功状态；
- `commit_time`。

---

## 10. 本地 HDFS 必测用例

Codex 必须至少实现或预留以下测试：

1. 正常上传：sealed + done -> final + manifest；
2. 重复运行：第二次执行不重复上传，final 已存在且一致时幂等成功；
3. final 冲突：final 已存在但内容不同，进入 quarantine，不覆盖；
4. staging 残留：final 不存在，staging 残留时可清理并重传；
5. manifest 补写：final 已存在但 manifest 缺失时补写 manifest；
6. 无 done marker：不上传；
7. active/writing 文件：不上传；
8. Agent 重启：状态恢复后继续处理；
9. 本地文件缺失：进入 failed/quarantine，不能写假 manifest；
10. HDFS 不可用：保留本地文件，进入 retry，不删除。

---

## 11. 验收标准

本地 HDFS 测试通过标准：

```text
1. Agent 可以使用 HadoopHdfsClient 上传到本机 HDFS；
2. HDFS 上只有 final 成功文件，没有半成品暴露在 final 目录；
3. repeated run 幂等；
4. final 文件不被覆盖；
5. manifest 与 final 文件一一对应；
6. 本地文件只有 manifest 成功后才进入 committed/GC；
7. 可以通过 hdfs dfs 命令验证结果。
```
