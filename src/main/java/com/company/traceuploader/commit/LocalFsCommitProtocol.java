package com.company.traceuploader.commit;

import com.company.traceuploader.config.AgentConfig;import com.company.traceuploader.hdfs.HdfsClient;import com.company.traceuploader.manifest.ManifestWriter;import com.company.traceuploader.model.TraceFileMetadata;import com.company.traceuploader.state.*;
import java.io.IOException;

public class LocalFsCommitProtocol implements CommitProtocol {
    private final HdfsClient hdfs; private final UploadStateStore store; private final ManifestWriter manifest; private final AgentConfig config;
    public LocalFsCommitProtocol(HdfsClient hdfs, UploadStateStore store, ManifestWriter manifest, AgentConfig config){this.hdfs=hdfs;this.store=store;this.manifest=manifest;this.config=config;}
    public CommitResult commit(TraceFileMetadata m) throws IOException {
        store.updateState(m.fileId(), UploadState.CHECKSUMED, null);
        if (hdfs.exists(m.hdfsFinalPath())) return handleExistingFinal(m);
        if (hdfs.exists(m.hdfsStagingPath())) {
            if (hdfs.size(m.hdfsStagingPath()) == m.sizeBytes() && (!config.upload.verifyChecksumAfterUpload || hdfs.checksumSha256(m.hdfsStagingPath()).equals(m.checksum()))) {
                return renameAndManifest(m);
            }
            hdfs.delete(m.hdfsStagingPath());
        }
        store.updateState(m.fileId(), UploadState.UPLOADING, null);
        hdfs.upload(m.localPath(), m.hdfsStagingPath());
        store.updateState(m.fileId(), UploadState.UPLOADED_TO_STAGING, null);
        store.updateState(m.fileId(), UploadState.VERIFYING, null);
        if (config.upload.verifySizeAfterUpload && hdfs.size(m.hdfsStagingPath()) != m.sizeBytes()) { store.updateState(m.fileId(), UploadState.RETRYABLE_FAILED, "staging size mismatch"); return new CommitResult(UploadState.RETRYABLE_FAILED,"staging size mismatch"); }
        if (config.upload.verifyChecksumAfterUpload && !hdfs.checksumSha256(m.hdfsStagingPath()).equals(m.checksum())) { store.updateState(m.fileId(), UploadState.RETRYABLE_FAILED, "staging checksum mismatch"); return new CommitResult(UploadState.RETRYABLE_FAILED,"staging checksum mismatch"); }
        return renameAndManifest(m);
    }
    private CommitResult renameAndManifest(TraceFileMetadata m) throws IOException { store.updateState(m.fileId(), UploadState.RENAMING, null); if(!hdfs.rename(m.hdfsStagingPath(), m.hdfsFinalPath())) return handleExistingFinal(m); store.updateState(m.fileId(), UploadState.COMMITTED_TO_HDFS, null); manifest.writeIfAbsent(m); store.updateState(m.fileId(), UploadState.MANIFEST_COMMITTED, null); return new CommitResult(UploadState.MANIFEST_COMMITTED,"committed"); }
    private CommitResult handleExistingFinal(TraceFileMetadata m) throws IOException { if(hdfs.size(m.hdfsFinalPath()) == m.sizeBytes() && hdfs.checksumSha256(m.hdfsFinalPath()).equals(m.checksum())) { if(hdfs.exists(m.hdfsStagingPath())) hdfs.delete(m.hdfsStagingPath()); store.updateState(m.fileId(), UploadState.COMMITTED_TO_HDFS, null); manifest.writeIfAbsent(m); store.updateState(m.fileId(), UploadState.MANIFEST_COMMITTED, null); return new CommitResult(UploadState.MANIFEST_COMMITTED,"idempotent final exists"); } store.updateState(m.fileId(), UploadState.QUARANTINED, "final exists but checksum mismatch"); return new CommitResult(UploadState.QUARANTINED,"final exists but checksum mismatch"); }
}
