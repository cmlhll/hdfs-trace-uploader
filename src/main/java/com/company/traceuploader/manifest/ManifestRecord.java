package com.company.traceuploader.manifest;

import com.company.traceuploader.model.TraceFileMetadata;
import com.company.traceuploader.state.UploadFileRecord;

import java.time.Instant;

public record ManifestRecord(
        String fileId,
        String hdfsPath,
        long sizeBytes,
        String checksum,
        long recordCount,
        String state,
        String commitTime) {
    public static ManifestRecord fromMetadata(TraceFileMetadata metadata, Instant commitTime) {
        return new ManifestRecord(metadata.fileId(), metadata.hdfsFinalPath(), metadata.sizeBytes(), metadata.checksum(),
                metadata.recordCount(), "MANIFEST_COMMITTED", commitTime.toString());
    }

    public static ManifestRecord fromUploadRecord(UploadFileRecord record, Instant commitTime) {
        return new ManifestRecord(record.fileId(), record.hdfsFinalPath(), record.sizeBytes(), record.checksum(),
                record.recordCount(), "MANIFEST_COMMITTED", commitTime.toString());
    }
}
