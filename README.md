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
