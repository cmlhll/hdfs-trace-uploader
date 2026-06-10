# 03 - HDFS Commit 协议

## 1. 核心原则

1. 永远不直接写 final path；
2. 永远不覆盖 final path；
3. 先写 staging，再校验，再 rename；
4. rename 后写 manifest；
5. manifest 成功前不能本地 GC；
6. final 已存在且 checksum 一致视为幂等成功；
7. final 已存在但 checksum 不一致必须 quarantine。

## 2. 标准流程

```text
local sealed file
  -> calculate metadata
  -> check final path
  -> upload staging
  -> verify staging
  -> rename staging to final
  -> write manifest
  -> mark local committed
  -> delayed local GC
```

## 3. final path 检查

### Case A: final 不存在

执行正常上传。

### Case B: final 存在，size/checksum 一致

说明之前已经提交成功，当前上传任务幂等成功。需要：

- 补写 manifest，如果缺失；
- 本地状态推进到 committed；
- 进入 GC 等待。

### Case C: final 存在，size/checksum 不一致

说明 file_id 冲突或数据损坏。必须：

- 不覆盖；
- 本地文件进入 quarantine；
- 记录 error；
- 触发报警。

## 4. staging 残留处理

如果发现 staging path 存在：

1. final 已存在且一致：删除 staging 残留；
2. final 不存在：删除 staging 后重新上传；
3. staging 与 local 一致且上传完整：可尝试 rename；
4. staging 不完整：删除并重传。

## 5. rename 结果不确定

rename 可能因为客户端超时导致结果不确定。处理逻辑：

```text
rename timeout
  -> check final path
      -> exists and checksum match: success
      -> not exists: check staging
          -> exists: retry rename or reupload
          -> not exists: mark retryable failure
      -> exists but mismatch: quarantine
```

## 6. 校验策略

MVP：

- size 校验；
- checksum 校验可用本地计算 checksum 写入 manifest，HDFS 文件重新读取计算成本高，可以提供可配置开关。

生产建议：

- 上传前计算本地 checksum；
- 上传后至少校验 HDFS size；
- 对低比例文件抽样回读 checksum；
- 对关键应用全量 checksum 校验；
- final 冲突时必须 checksum 比对。

## 7. 伪代码

```java
UploadFile file = scanner.next();
FileMeta meta = metadataService.build(file);

if (hdfs.exists(meta.finalPath())) {
    if (verifier.same(meta.localPath(), meta.finalPath())) {
        manifestWriter.writeIfAbsent(meta);
        stateStore.markManifestCommitted(meta.fileId());
        return;
    }
    quarantineService.quarantine(meta, "final exists but checksum mismatch");
    return;
}

Path staging = pathPlanner.stagingPath(meta, attempt);
cleanupStagingIfNeeded(staging, meta.finalPath());

stateStore.markUploading(meta.fileId(), staging);
hdfs.upload(meta.localPath(), staging);
stateStore.markUploadedToStaging(meta.fileId());

verifier.verifyStaging(meta.localPath(), staging);
stateStore.markVerifyingDone(meta.fileId());

try {
    hdfs.rename(staging, meta.finalPath());
} catch (TimeoutException e) {
    resolveRenameUncertainty(meta, staging);
}

stateStore.markCommittedToHdfs(meta.fileId());
manifestWriter.writeIfAbsent(meta);
stateStore.markManifestCommitted(meta.fileId());
```

