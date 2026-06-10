# 08 - Codex MVP 任务拆解

## Task 1: 创建项目脚手架

请创建 Java 17 Maven 项目：

- groupId: `com.company`
- artifactId: `hdfs-trace-uploader`
- main class: `com.company.traceuploader.cli.TraceUploaderMain`
- 支持 `--config` 和 `--dry-run`
- 引入 JUnit 5
- 引入 YAML 配置解析库
- 引入 slf4j/logback

验收：`mvn test` 通过。

## Task 2: 配置模型

实现：

- `AgentConfig`
- `LocalSpoolConfig`
- `HdfsConfig`
- `ScannerConfig`
- `UploadConfig`
- `RetryConfig`
- `ManifestConfig`
- `GcConfig`

从 YAML 加载并校验必填项。

## Task 3: 文件扫描器

实现 `SealedFileScanner`：

- 扫描 sealedDir；
- 只返回有 `.done` marker 的数据文件；
- 忽略 tmp/part 文件；
- 支持文件稳定性检查；
- 返回 `DiscoveredFile`。

## Task 4: 元数据服务

实现：

- 文件名解析；
- file_id 生成；
- size 获取；
- checksum 计算；
- record_count 统计；
- HDFS final/staging path 规划。

## Task 5: 状态存储

实现 `UploadStateStore` 接口，MVP 用 SQLite 或文件 WAL。

方法：

- `upsertDiscovered`
- `transitionState`
- `findRetryable`
- `findUnfinished`
- `markQuarantined`
- `markManifestCommitted`

## Task 6: HDFS 客户端抽象

定义 `HdfsClient` 接口：

- `exists(Path)`
- `size(Path)`
- `upload(local, remote)`
- `rename(src, dst)`
- `delete(path)`
- `mkdirs(path)`
- `open(path)` 可选

先实现 `LocalFsHdfsClient` 便于本地测试，再实现 `HadoopHdfsClient`。

## Task 7: CommitProtocol

实现 staging/final 协议。

必须覆盖：

- 正常上传；
- final exists same；
- final exists mismatch；
- staging leftovers；
- rename uncertain。

## Task 8: ManifestWriter

MVP 实现 JSONL manifest writer：

- 每条 manifest 一行 JSON；
- 支持 file_id 去重；
- 支持本地 manifest 文件或 HDFS manifest path；
- 先实现 local JSONL，HDFS manifest 后续扩展。

## Task 9: Uploader Worker

把 scanner、metadata、state、commit、manifest 串起来。

支持：

- 单次 run once；
- daemon loop；
- dry-run；
- max concurrent uploads；
- retry backoff。

## Task 10: GC 和 Metrics

实现：

- committed 文件延迟删除；
- `.done` marker 一起删除；
- quarantine 文件移动；
- metrics counters/gauges 内部对象。

