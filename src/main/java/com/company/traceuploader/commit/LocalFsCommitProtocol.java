package com.company.traceuploader.commit;

import com.company.traceuploader.config.AgentConfig;
import com.company.traceuploader.hdfs.HdfsClient;
import com.company.traceuploader.model.TraceFileMetadata;
import com.company.traceuploader.state.UploadState;
import com.company.traceuploader.state.UploadStateStore;

import java.io.IOException;

public final class LocalFsCommitProtocol implements CommitProtocol {
    private final HdfsClient hdfs;
    private final UploadStateStore stateStore;
    private final AgentConfig config;

    public LocalFsCommitProtocol(HdfsClient hdfs, UploadStateStore stateStore, AgentConfig config) {
        this.hdfs = hdfs;
        this.stateStore = stateStore;
        this.config = config;
    }

    @Override
    public CommitResult commit(TraceFileMetadata metadata) throws IOException {
        if (hdfs.exists(metadata.hdfsFinalPath())) {
            return handleExistingFinal(metadata);
        }
        stateStore.updateState(metadata.fileId(), UploadState.CHECKSUMED, null);

        if (hdfs.exists(metadata.hdfsStagingPath())) {
            CommitResult recovered = recoverStaging(metadata);
            if (recovered != null) return recovered;
        }

        stateStore.updateState(metadata.fileId(), UploadState.UPLOADING, null);
        hdfs.upload(metadata.localPath(), metadata.hdfsStagingPath());
        stateStore.updateState(metadata.fileId(), UploadState.UPLOADED_TO_STAGING, null);
        return verifyAndRename(metadata);
    }

    private CommitResult recoverStaging(TraceFileMetadata metadata) throws IOException {
        if (stagingMatches(metadata)) {
            return verifyAndRename(metadata);
        }
        if (config.upload().deleteStagingOnRetry()) {
            hdfs.delete(metadata.hdfsStagingPath());
            return null;
        }
        String message = "staging exists but does not match local file";
        stateStore.updateState(metadata.fileId(), UploadState.RETRYABLE_FAILED, message);
        return new CommitResult(UploadState.RETRYABLE_FAILED, message);
    }

    private CommitResult verifyAndRename(TraceFileMetadata metadata) throws IOException {
        stateStore.updateState(metadata.fileId(), UploadState.VERIFYING, null);
        if (!stagingMatches(metadata)) {
            String message = "staging verification failed";
            stateStore.updateState(metadata.fileId(), UploadState.RETRYABLE_FAILED, message);
            return new CommitResult(UploadState.RETRYABLE_FAILED, message);
        }

        stateStore.updateState(metadata.fileId(), UploadState.RENAMING, null);
        boolean renamed = hdfs.rename(metadata.hdfsStagingPath(), metadata.hdfsFinalPath());
        if (renamed) {
            stateStore.updateState(metadata.fileId(), UploadState.COMMITTED_TO_HDFS, null);
            return new CommitResult(UploadState.COMMITTED_TO_HDFS, "committed staging to final");
        }

        if (hdfs.exists(metadata.hdfsFinalPath())) {
            return handleExistingFinal(metadata);
        }
        if (hdfs.exists(metadata.hdfsStagingPath())) {
            String message = "rename result uncertain; staging still exists and final is missing";
            stateStore.updateState(metadata.fileId(), UploadState.RETRYABLE_FAILED, message);
            return new CommitResult(UploadState.RETRYABLE_FAILED, message);
        }

        String message = "rename result uncertain; neither staging nor final exists";
        stateStore.updateState(metadata.fileId(), UploadState.RETRYABLE_FAILED, message);
        return new CommitResult(UploadState.RETRYABLE_FAILED, message);
    }

    private CommitResult handleExistingFinal(TraceFileMetadata metadata) throws IOException {
        if (hdfs.size(metadata.hdfsFinalPath()) == metadata.sizeBytes()
                && hdfs.checksumSha256(metadata.hdfsFinalPath()).equals(metadata.checksum())) {
            stateStore.updateState(metadata.fileId(), UploadState.COMMITTED_TO_HDFS, null);
            if (hdfs.exists(metadata.hdfsStagingPath())) hdfs.delete(metadata.hdfsStagingPath());
            return new CommitResult(UploadState.COMMITTED_TO_HDFS, "final already exists with matching checksum; idempotent success");
        }
        String message = "final already exists with different checksum; quarantined without overwrite";
        stateStore.updateState(metadata.fileId(), UploadState.QUARANTINED, message);
        return new CommitResult(UploadState.QUARANTINED, message);
    }

    private boolean stagingMatches(TraceFileMetadata metadata) throws IOException {
        if (config.upload().verifySizeAfterUpload() && hdfs.size(metadata.hdfsStagingPath()) != metadata.sizeBytes()) {
            return false;
        }
        return !config.upload().verifyChecksumAfterUpload()
                || hdfs.checksumSha256(metadata.hdfsStagingPath()).equals(metadata.checksum());
    }
}
