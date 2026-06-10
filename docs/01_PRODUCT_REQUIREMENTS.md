# 01 - 产品需求说明 PRD

## 1. 背景

当前 trace log 生产速度极高，传统链路：

```text
filebeat -> Kafka -> Flink -> HDFS
```

在 filebeat、Kafka、Flink 上都产生了较高成本和资源压力。trace log 的主要目标是最终低成本进入 HDFS，后续由 Hive/Spark/Iceberg 查询或加工。因此需要设计一个本地文件级上传系统。

## 2. 目标

实现一个本地 Uploader Agent：

```text
Log4j sealed file -> Agent -> HDFS staging -> final -> manifest
```

目标：

1. 支持海量 trace log 文件归档到 HDFS；
2. 避免全量 trace 经过 Kafka/Flink；
3. 文件级尽量不重不丢；
4. 支持失败重试和幂等提交；
5. 支持 HDFS 压力控制；
6. 支持本地磁盘水位保护；
7. 支持 manifest 对账；
8. 支持后续 Spark/Hive/Iceberg 读取。

## 3. 非目标

MVP 不做：

1. 实时 trace 检索；
2. 记录级 exactly-once；
3. 端侧直接写 Iceberg；
4. 端侧直接写 Parquet/ORC；
5. 全局控制平台；
6. Web UI；
7. 多租户权限管理平台。

## 4. 可靠性边界

Agent 保证：

```text
只要 sealed 文件已经稳定落到本地磁盘，且机器/磁盘没有永久丢失，Agent 应尽最大努力最终上传到 HDFS final，并写入 manifest。
```

Agent 不保证：

1. 应用尚未 flush 的日志；
2. Log4j 正在写 active 文件中的日志；
3. 本地磁盘永久损坏且文件尚未上传；
4. Log4j 自己提前删除的 sealed 文件；
5. 业务重复打印导致的 record-level 重复。

## 5. 核心用户故事

### US-1: 正常上传

作为平台工程师，我希望 Agent 能发现 sealed 文件并上传到 HDFS final 目录，写入 manifest，最后本地延迟删除。

### US-2: 失败重试

作为 on-call，我希望 HDFS 暂时不可用时，Agent 不丢本地文件，并在 HDFS 恢复后自动重试。

### US-3: 幂等提交

作为数据平台负责人，我希望 Agent 崩溃重启后不会重复提交文件。

### US-4: 对账

作为数据治理负责人，我希望通过 manifest 判断每小时哪些文件已提交、哪些缺失、哪些异常。

### US-5: HDFS 保护

作为 HDFS owner，我希望 Agent 不会在 HDFS 故障恢复后无限并发补传，导致 NameNode/DataNode 被打挂。

## 6. 成功指标

- 文件重复率：接近 0；
- sealed 文件丢失率：接近 0，明确排除本地磁盘永久丢失；
- P95 上传延迟：可配置，例如小于 30 分钟；
- HDFS final checksum mismatch：0；
- manifest missing after final committed：自动补偿；
- 本地 spool 水位：低于告警阈值；
- NameNode RPC QueueTime 无明显恶化；
- DataNode 写入 IO 无明显打满。

