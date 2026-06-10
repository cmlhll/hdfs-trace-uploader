package com.company.traceuploader.state;

public class UploadFileRecord {
    public String fileId, app, env, region, cluster, host, pid, bootId, localPath, donePath, hdfsStagingPath, hdfsFinalPath, dt, checksum, lastError;
    public int hour, bucket, attempt; public long startTimeMs, endTimeMs, sizeBytes, recordCount, firstSeenTimeMs, lastUpdateTimeMs, nextRetryTimeMs;
    public UploadState state;
    public UploadFileRecord() {}
}
