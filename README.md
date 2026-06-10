# HDFS Trace Uploader Agent - Codex 开发包

这个目录是一套交给 Codex 的项目开发上下文，用于实现：

- Log4j 本地产生海量 trace log 文件；
- Uploader Agent 扫描 sealed 文件；
- 上传到 HDFS staging；
- 校验后 rename 到 final；
- 写 manifest / 对账表；
- 尽量做到文件级不重不丢；
- 后续给 Hive / Spark / Iceberg 做 Raw 入湖和后处理。

建议把本目录内容放入一个新的 Git 仓库根目录，然后让 Codex 先阅读：

1. `AGENTS.md`
2. `PLANS.md`
3. `docs/01_PRODUCT_REQUIREMENTS.md`
4. `docs/02_ARCHITECTURE_DESIGN.md`
5. `prompts/CODEX_MAIN_PROMPT.md`

推荐实现语言：Java 17。

推荐构建工具：Maven 或 Gradle，优先 Maven。

推荐 MVP：先实现本地文件扫描、SQLite 状态表、HDFS staging/final commit、manifest JSONL 输出、本地 dry-run mock HDFS 测试。后续再接入真实 Hadoop Client、Kerberos、Prometheus 指标和全局限流。



## 本地 HDFS 测试要求

本项目必须支持在开发者本机或单机伪分布式 HDFS 上做端到端测试。详见 `docs/10_LOCAL_HDFS_TEST_GUIDE.md` 和 `config/local-hdfs-agent.yaml`。

MVP 至少支持两种测试模式：

1. `LocalFsHdfsClient`：不依赖 Hadoop，用本地文件系统模拟 HDFS；
2. `HadoopHdfsClient`：连接本地伪分布式 HDFS，例如 `hdfs://localhost:9000`。

## Phase 0-1 当前实现

当前代码只实现第一轮范围：Java 17 + Maven 脚手架、CLI 参数解析、项目 YAML 配置加载、基础日志、JUnit 5 测试，以及本地 sealed 文件扫描器。

### 构建与测试

```bash
mvn test
mvn package
```

### Dry run 扫描示例

准备一个带 `.done` marker 的 sealed 文件：

```bash
mkdir -p /tmp/trace_spool/{writing,sealed,committed,failed,quarantine,state,tmp}
printf '{"message":"hello"}\n' > /tmp/trace_spool/sealed/trace-payment-dev-local-c1-host001-pid123-bootlocal-20260610T100000-20260610T100500-seq000001.jsonl
touch /tmp/trace_spool/sealed/trace-payment-dev-local-c1-host001-pid123-bootlocal-20260610T100000-20260610T100500-seq000001.jsonl.done
```

运行 dry-run：

```bash
java -jar target/hdfs-trace-uploader.jar \
  --config config/example-agent.yaml \
  --dry-run
```

预期输出会列出将被处理的 sealed 文件，但不会上传 HDFS、写 manifest 或删除本地文件。

### Phase 1 扫描规则

`LocalSealedFileScanner` 只扫描配置中的 `localSpool.sealedDir`，不会递归进入 `writing/` 子目录。候选数据文件必须同时满足：

- 数据文件本身是 regular file；
- 存在同名 `markerSuffix`（默认 `.done`）marker；
- 文件名不以 `.tmp`、`.part`、`.uploading` 等忽略后缀结尾；
- 数据文件和 marker 的 mtime 都达到 `scanner.minStableMillis`（或兼容的 `minStableAgeSeconds`）；
- 单轮最多返回 `scanner.maxFilesPerScan` 个文件。

## 暂未实现

后续 Phase 会继续实现 metadata/file_id、落盘状态机、LocalFs/Hadoop HDFS commit protocol、manifest、GC、指标和本地 HDFS E2E。当前 Phase 0-1 不会直接写 HDFS final path，也不会覆盖任何远端文件。
