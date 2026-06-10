# PLANS.md - 分阶段开发计划

## Phase 0: 项目脚手架

目标：创建可编译、可测试、可配置的 Java 项目。

交付物：

- Maven 项目结构；
- main class；
- YAML 配置加载；
- 基础日志；
- 单元测试框架；
- README 运行命令。

验收：

```bash
mvn test
java -jar target/trace-uploader-agent.jar --config config/example-agent.yaml --dry-run
```

## Phase 1: 本地 sealed 文件扫描

目标：扫描 `sealedDir` 中符合规则的文件，只处理 `.done` marker 对应的数据文件。

要求：

- 忽略 writing 目录；
- 忽略 `.tmp`、`.part`、`.uploading`；
- 只有 `file.zst` 与 `file.zst.done` 同时存在才进入 DISCOVERED；
- 支持文件 size/mtime 稳定性检查；
- 支持 max files per scan。

验收：

- 构造临时目录测试；
- 没有 done marker 的文件不会被上传；
- active 文件不会被扫描。

## Phase 2: 本地状态机

目标：实现状态落盘。

状态：

- DISCOVERED
- SEALED
- CHECKSUMED
- UPLOADING
- UPLOADED_TO_STAGING
- VERIFYING
- RENAMING
- COMMITTED_TO_HDFS
- MANIFEST_COMMITTED
- LOCAL_GC_READY
- LOCAL_GC_DONE
- RETRYABLE_FAILED
- PERMANENT_FAILED
- QUARANTINED

要求：

- 状态变更原子落盘；
- Agent 重启后可恢复；
- 按 file_id 幂等 upsert；
- 保存 attempt、last_error、timestamps。

## Phase 3: HDFS commit 协议

目标：实现 staging -> verify -> rename -> final。

要求：

- final 不存在：上传 staging，校验，rename；
- final 已存在且 checksum 一致：幂等成功；
- final 已存在但 checksum 不一致：quarantine；
- staging 残留：按策略清理或重传；
- rename 结果不确定：重新检查 final/staging 状态。

## Phase 4: Manifest

目标：写 manifest 账本。

MVP：写 HDFS/local JSONL manifest。

后续：支持 JDBC/Iceberg manifest table。

要求：

- manifest 以 file_id 幂等；
- final 已成功但 manifest 失败时可补写；
- manifest 写成功后才允许进入 local GC。

## Phase 5: GC、限流、指标

目标：生产可用的资源保护。

要求：

- 本地 GC 延迟删除；
- spool 水位检查；
- 单机并发限制；
- 指数退避；
- 关键 metrics 暴露。

## Phase 6: 集成测试和 MiniDFSCluster

目标：用本地集成测试覆盖异常场景。

要求：

- mock HDFS 或 MiniDFSCluster；
- 崩溃恢复测试；
- staging 残留测试；
- final 冲突测试；
- manifest 补写测试。

