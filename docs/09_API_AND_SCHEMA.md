# 09 - 接口与 Schema

## 1. 核心数据模型

### DiscoveredFile

```java
record DiscoveredFile(
    Path localPath,
    Path donePath,
    long sizeBytes,
    long lastModifiedTimeMs
) {}
```

### FileMeta

```java
record FileMeta(
    String fileId,
    String app,
    String env,
    String region,
    String cluster,
    String host,
    String pid,
    String bootId,
    Instant startTime,
    Instant endTime,
    String dt,
    int hour,
    int bucket,
    Path localPath,
    Path donePath,
    String hdfsStagingPath,
    String hdfsFinalPath,
    long sizeBytes,
    String checksum,
    long recordCount,
    String rawFormat,
    String compression
) {}
```

### UploadState

```java
enum UploadState {
    DISCOVERED,
    SEALED,
    CHECKSUMED,
    UPLOADING,
    UPLOADED_TO_STAGING,
    VERIFYING,
    RENAMING,
    COMMITTED_TO_HDFS,
    MANIFEST_COMMITTED,
    LOCAL_GC_READY,
    LOCAL_GC_DONE,
    RETRYABLE_FAILED,
    PERMANENT_FAILED,
    QUARANTINED
}
```

## 2. 核心接口

```java
interface SealedFileScanner {
    List<DiscoveredFile> scan();
}

interface MetadataService {
    FileMeta build(DiscoveredFile file);
}

interface UploadStateStore {
    void upsertDiscovered(FileMeta meta);
    Optional<UploadRecord> findByFileId(String fileId);
    void transition(String fileId, UploadState from, UploadState to);
    void markRetryableFailed(String fileId, Throwable error, Instant nextRetryAt);
    void markQuarantined(String fileId, String reason);
    List<UploadRecord> findWork(int limit);
}

interface HdfsClient {
    boolean exists(String path);
    long size(String path);
    void upload(Path localPath, String remotePath);
    boolean rename(String src, String dst);
    void delete(String path, boolean recursive);
    InputStream open(String path);
}

interface CommitProtocol {
    CommitResult commit(FileMeta meta, UploadRecord state);
}

interface ManifestWriter {
    ManifestWriteResult writeIfAbsent(FileMeta meta);
}
```

## 3. Manifest SQL

见 `schema/sql/trace_file_manifest.sql`。

