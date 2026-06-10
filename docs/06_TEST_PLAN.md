# 06 - 测试计划

## 1. 单元测试

### Scanner

- 无 done marker 不扫描；
- 有 done marker 才扫描；
- 忽略 tmp/part 文件；
- 文件 mtime/size 不稳定不扫描；
- max files per scan 生效。

### FileIdGenerator

- 同一输入生成同一 file_id；
- 不同 host/seq/checksum 生成不同 file_id；
- 非法文件名进入 failed/quarantine。

### StateStore

- insert/update 幂等；
- 状态变更持久化；
- 重启后恢复；
- retry time 查询正确。

### CommitProtocol

- final 不存在正常上传；
- final 已存在且一致幂等成功；
- final 已存在但不一致 quarantine；
- staging 残留清理；
- rename 超时后检查 final 成功。

### ManifestWriter

- writeIfAbsent 成功；
- 重复写一致成功；
- 重复写不一致报错；
- manifest 写失败可恢复。

## 2. 集成测试

使用 mock HDFS 或 MiniDFSCluster：

1. 正常上传；
2. 上传中断重试；
3. Agent 崩溃恢复；
4. staging 残留恢复；
5. final 冲突；
6. manifest 补写；
7. 本地 GC 延迟删除。


## 2.5 本地伪分布式 HDFS E2E 测试

必须支持在本机 HDFS 上验证真实 HDFS 行为。详见 `docs/10_LOCAL_HDFS_TEST_GUIDE.md`。

最低验收：

1. `HadoopHdfsClient` 可连接 `hdfs://localhost:9000`；
2. sealed + done 文件可以上传到 `/warehouse/raw_trace/_staging`，再 rename 到 final；
3. 可以通过 `hdfs dfs -ls -R /warehouse/raw_trace` 查看 final 文件；
4. 重复执行 `--once` 不产生重复文件；
5. final 已存在但 checksum 不一致时进入 quarantine；
6. manifest 写入本地 JSONL 或 HDFS manifest。

## 3. 压测

MVP 后执行：

- 单机 1 并发、2 并发、4 并发上传吞吐；
- 1000/10000 文件扫描性能；
- SQLite 状态表 100 万记录性能；
- HDFS create/rename 延迟分布；
- 本地 checksum CPU 开销。

## 4. 故障注入

- HDFS unavailable；
- NameNode slow；
- DataNode pipeline failure；
- manifest store unavailable；
- 本地文件被删除；
- checksum mismatch；
- 进程 kill -9；
- 本地磁盘满。

