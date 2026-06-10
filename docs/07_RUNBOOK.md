# 07 - 运维 Runbook

## 1. 关键报警

- `oldest_uncommitted_file_age_seconds` 超阈值；
- `local_spool_disk_usage_percent` 超阈值；
- `upload_failure_rate` 升高；
- `hdfs_rename_latency_p99` 升高；
- `manifest_write_failure_count` > 0；
- `checksum_mismatch_count` > 0；
- `quarantine_file_count` > 0。

## 2. 上传积压

先止血：

1. 确认 HDFS 是否健康；
2. 降低 Agent 全局并发，避免补传风暴；
3. 对低优先级 app 暂停上传；
4. 检查本地磁盘水位；
5. 必要时通知业务降采样。

定位：

- 看 HDFS RPC QueueTime；
- 看 DataNode write throughput；
- 看 Agent error 分类；
- 看 Kerberos/token 是否异常；
- 看 manifest store 是否异常。

## 3. checksum mismatch

处理：

1. 禁止覆盖 final；
2. 本地文件进入 quarantine；
3. 保留 final 文件和 local 文件；
4. 比较 size/checksum/record_count；
5. 判断 file_id 冲突、文件损坏、重复命名或历史残留；
6. 人工修复后再恢复。

## 4. manifest 缺失

如果 HDFS final 已存在且 checksum 一致：

```text
补写 manifest，不重新上传。
```

## 5. staging 膨胀

清理策略：

- final 已存在且一致：删除 staging；
- staging older than threshold 且无 active state：删除；
- staging 与 local 一致且 final 不存在：可 retry rename；
- 不确定时先移动到 quarantine staging path。

