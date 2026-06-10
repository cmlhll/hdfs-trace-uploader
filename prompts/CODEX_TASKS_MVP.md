# Codex 分任务提示词

## Prompt 1: 项目脚手架

请基于 `AGENTS.md` 和 `docs/08_MVP_TASKS.md` 创建 Java 17 Maven 项目脚手架，实现配置加载、main class、基础日志和 JUnit 5 测试。不要实现上传逻辑。

## Prompt 2: 文件扫描器

请实现 `SealedFileScanner`。它应该扫描配置里的 sealedDir，只返回同时存在数据文件和 `.done` marker 的文件，忽略 `.tmp`、`.part`、`.uploading`，支持 size/mtime 稳定性检查，并补充完整单元测试。

## Prompt 3: 元数据与 file_id

请实现 `MetadataService` 和 `FileIdGenerator`，从文件名解析 app/env/region/cluster/host/pid/bootId/start/end/seq，计算 size、checksum、record_count，并规划 HDFS staging/final path。补充非法文件名测试。

## Prompt 4: 状态存储

请实现 `UploadStateStore`。MVP 可以用 SQLite；如果依赖复杂，可先实现 durable JSONL WAL，但接口要保持可替换。要求状态变更持久化，Agent 重启后可恢复。

## Prompt 5: HDFS Client 抽象和 LocalFs 实现

请定义 `HdfsClient` 接口并实现 `LocalFsHdfsClient`，用于本地集成测试。LocalFsHdfsClient 要模拟 exists/upload/rename/delete/size。

## Prompt 6: CommitProtocol

请实现 `CommitProtocol`，严格按 `docs/03_HDFS_COMMIT_PROTOCOL.md`：final 不存在则上传 staging 并 rename，final 已存在且一致幂等成功，final 已存在但不一致 quarantine，staging 残留可恢复。

## Prompt 7: ManifestWriter

请实现本地 JSONL `ManifestWriter`，支持 writeIfAbsent。重复写一致时成功，重复写不一致时报错。

## Prompt 8: UploaderWorker

请把 scanner、metadata、state、commit、manifest 串成一个可运行流程，支持 `--dry-run` 和 `--once`。



## Prompt 9: 本地 HDFS E2E

请实现 `HadoopHdfsClient`，支持从 `hadoopConfDir` 读取 Hadoop 配置，并允许用 `fsDefaultFS` 覆盖本地测试目标。请实现 CLI `--once` 模式，并确保 `config/local-hdfs-agent.yaml` 可以连接本机伪分布式 HDFS。按照 `docs/10_LOCAL_HDFS_TEST_GUIDE.md` 跑通 sealed 文件上传、staging rename final、manifest 写入、重复运行幂等、final 冲突 quarantine。
