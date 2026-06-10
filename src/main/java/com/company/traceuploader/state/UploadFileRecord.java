package com.company.traceuploader.state;

import com.company.traceuploader.model.TraceFileMetadata;

public final class UploadFileRecord {
    private final String fileId;
    private final String localPath;
    private final String hdfsFinalPath;
    private final String hdfsStagingPath;
    private final long sizeBytes;
    private final String checksum;
    private final long recordCount;
    private final int attempt;
    private final UploadState state;
    private final String lastError;
    private final long createdAtMillis;
    private final long updatedAtMillis;

    public UploadFileRecord(String fileId, String localPath, String hdfsFinalPath, String hdfsStagingPath,
                            long sizeBytes, String checksum, long recordCount, int attempt, UploadState state,
                            String lastError, long createdAtMillis, long updatedAtMillis) {
        this.fileId = fileId;
        this.localPath = localPath;
        this.hdfsFinalPath = hdfsFinalPath;
        this.hdfsStagingPath = hdfsStagingPath;
        this.sizeBytes = sizeBytes;
        this.checksum = checksum;
        this.recordCount = recordCount;
        this.attempt = attempt;
        this.state = state;
        this.lastError = lastError;
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }

    public static UploadFileRecord fromMetadata(TraceFileMetadata metadata, UploadState state, long nowMillis) {
        return new UploadFileRecord(metadata.fileId(), metadata.localPath().toString(), metadata.hdfsFinalPath(),
                metadata.hdfsStagingPath(), metadata.sizeBytes(), metadata.checksum(), metadata.recordCount(),
                metadata.attempt(), state, null, nowMillis, nowMillis);
    }

    public UploadFileRecord withState(UploadState nextState, String error, long nowMillis) {
        return new UploadFileRecord(fileId, localPath, hdfsFinalPath, hdfsStagingPath, sizeBytes, checksum, recordCount,
                attempt, nextState, error, createdAtMillis, nowMillis);
    }

    public UploadFileRecord withMetadata(TraceFileMetadata metadata, long nowMillis) {
        return new UploadFileRecord(fileId, metadata.localPath().toString(), metadata.hdfsFinalPath(), metadata.hdfsStagingPath(),
                metadata.sizeBytes(), metadata.checksum(), metadata.recordCount(), metadata.attempt(), state, lastError,
                createdAtMillis, nowMillis);
    }

    public String fileId() { return fileId; }
    public String localPath() { return localPath; }
    public String hdfsFinalPath() { return hdfsFinalPath; }
    public String hdfsStagingPath() { return hdfsStagingPath; }
    public long sizeBytes() { return sizeBytes; }
    public String checksum() { return checksum; }
    public long recordCount() { return recordCount; }
    public int attempt() { return attempt; }
    public UploadState state() { return state; }
    public String lastError() { return lastError; }
    public long createdAtMillis() { return createdAtMillis; }
    public long updatedAtMillis() { return updatedAtMillis; }
}
