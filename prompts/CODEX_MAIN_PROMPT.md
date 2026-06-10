# Codex 主提示词

你现在要开发一个新的 Java 项目：`hdfs-trace-uploader`。

请先阅读仓库根目录的：

1. `AGENTS.md`
2. `PLANS.md`
3. `docs/01_PRODUCT_REQUIREMENTS.md`
4. `docs/02_ARCHITECTURE_DESIGN.md`
5. `docs/03_HDFS_COMMIT_PROTOCOL.md`
6. `docs/10_LOCAL_HDFS_TEST_GUIDE.md`
6. `docs/04_STATE_MACHINE_AND_MANIFEST.md`
7. `docs/08_MVP_TASKS.md`
8. `docs/09_API_AND_SCHEMA.md`

然后按照以下原则开始实现 MVP：

## 目标

实现一个本地 HDFS Trace Uploader Agent，用于扫描 Log4j rollover 后生成的 sealed 文件，通过 HDFS staging/final commit 协议上传到 HDFS，并写 manifest。

## 第一轮开发范围

请完成 Phase 0 到 Phase 3 的最小可运行版本：

1. Java 17 Maven 项目脚手架；
2. YAML 配置加载；
3. sealed 文件扫描器；
4. file_id / metadata 生成；
5. 本地状态存储接口和一个可用实现；
6. `HdfsClient` 抽象和 `LocalFsHdfsClient` 实现；
7. `CommitProtocol` 实现，支持 staging -> verify -> rename；
8. 单元测试覆盖核心异常。

## 约束

- 不要直接写 HDFS final path；
- 不要覆盖 final 文件；
- final 已存在且一致要幂等成功；
- final 已存在但不一致要 quarantine；
- 状态必须落盘；
- 单元测试必须覆盖状态恢复和 commit 协议；
- 先用 LocalFsHdfsClient 模拟 HDFS，后续再接 HadoopHdfsClient。

## 交付

完成后请输出：

1. 你创建/修改的文件列表；
2. 如何运行测试；
3. 如何本地 dry-run；
4. 当前未完成事项；
5. 下一步建议。



## 本地 HDFS 测试要求

请确保项目可以在本机伪分布式 HDFS 上做 E2E 测试。实现 `HadoopHdfsClient`、`--once` 模式、`config/local-hdfs-agent.yaml` 支持，并让 `scripts/local_hdfs_e2e_test_example.sh` 的流程可以跑通。
