package com.company.traceuploader.model;

import java.nio.file.Path;

public record TraceFileMetadata(
        Path localPath,
        String fileName,
        String app,
        String env,
        String region,
        String cluster,
        String host,
        String pid,
        String bootId,
        String startTs,
        String endTs,
        String seq,
        long sizeBytes,
        String checksum,
        long recordCount,
        String fileId,
        int bucket,
        String hdfsFinalPath,
        String hdfsStagingPath,
        int attempt) {
}
