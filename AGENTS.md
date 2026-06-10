# AGENTS.md - Codex 项目指导

你正在开发一个生产级 HDFS Trace Uploader Agent。请严格遵守本文件和 `docs/` 下的设计文档。

## 项目目标

实现一个本地常驻 Agent，用于把 Log4j rollover 后生成的 sealed trace log 文件可靠上传到 HDFS：

```text
Log4j sealed file
  -> local uploader state machine
  -> HDFS _staging path
  -> verify size/checksum
  -> atomic rename to final path
  -> manifest record
  -> delayed local GC
```

核心目标：

1. 文件级幂等提交；
2. 上传失败可重试；
3. final 文件禁止覆盖；
4. final 已存在且 checksum 一致时视为幂等成功；
5. final 已存在但 checksum 不一致时 quarantine；
6. manifest 是文件提交账本；
7. Agent 重启后能从本地状态恢复；
8. 上传逻辑不能影响业务进程；
9. MVP 先跑通本地、MiniDFSCluster/mock HDFS 测试，再逐步增强生产能力；
10. 必须支持连接本机伪分布式 HDFS 做端到端测试，详见 `docs/10_LOCAL_HDFS_TEST_GUIDE.md`。

## 技术偏好

- 语言：Java 17。
- 构建：Maven 优先。
- 配置：YAML。
- 状态存储：MVP 用 SQLite；如果依赖引入困难，可先使用单文件 JSONL WAL，但接口要保留可替换性。
- HDFS：通过 Hadoop Java Client 实现；MVP 可以先做 `LocalFileSystem` 或 mock 实现，但必须预留并实现 `HadoopHdfsClient`，支持本地伪分布式 HDFS 测试。
- 日志：slf4j + logback 或 log4j2 均可。
- 测试：JUnit 5。
- 指标：MVP 可以先暴露内部 metrics 对象，后续接 Prometheus。

## 代码设计要求

优先模块化，不要把所有逻辑写在一个类里。建议包结构：

```text
src/main/java/com/company/traceuploader/
  config/
  scanner/
  state/
  model/
  hdfs/
  commit/
  manifest/
  metrics/
  gc/
  quarantine/
  cli/
```

核心接口建议：

- `SealedFileScanner`
- `FileIdGenerator`
- `ChecksumService`
- `UploadStateStore`
- `HdfsClient`
- `CommitProtocol`
- `ManifestWriter`
- `LocalGcService`
- `UploaderWorker`
- `RateLimiter`

## 正确性优先级

正确性优先于吞吐。任何实现都不能直接覆盖 HDFS final 文件。

必须覆盖以下异常：

1. Agent 在上传中崩溃；
2. HDFS staging 残留；
3. rename 请求超时或结果不确定；
4. manifest 写失败但 HDFS final 已成功；
5. final 已存在且 checksum 一致；
6. final 已存在但 checksum 不一致；
7. 本地文件被删除或 size 改变；
8. 本地磁盘水位过高；
9. Agent 重启后恢复状态。

## 开发工作方式

每次开始改代码前：

1. 阅读 `PLANS.md`；
2. 确认当前阶段目标；
3. 小步提交；
4. 每完成一个模块就补充单元测试；
5. 不要为了赶进度跳过幂等和异常处理。

## 禁止事项

- 禁止直接上传到 HDFS final path；必须先 staging。
- 禁止覆盖 final 文件。
- 禁止只依赖内存状态。
- 禁止让业务 Log4j 进程参与 HDFS 上传。
- 禁止 uploader 读取 active/writing 文件。
- 禁止把 `trace.log.1`、`trace.log.2` 这种可复用文件名直接作为 final 文件名。
- 禁止未写 manifest 就删除本地文件。



## 本地 HDFS 测试硬性要求

Codex 实现时必须让项目能在本地 HDFS 上跑通 E2E：

```bash
java -jar target/hdfs-trace-uploader.jar --config config/local-hdfs-agent.yaml --once
```

验收方式包括：`hdfs dfs -ls -R /warehouse/raw_trace` 能看到 final 文件，重复运行不产生重复文件，final 已存在且一致时幂等成功，final 冲突时进入 quarantine。
