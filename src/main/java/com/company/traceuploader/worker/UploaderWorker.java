package com.company.traceuploader.worker;

import com.company.traceuploader.commit.CommitProtocol;
import com.company.traceuploader.commit.CommitResult;
import com.company.traceuploader.commit.LocalFsCommitProtocol;
import com.company.traceuploader.config.AgentConfig;
import com.company.traceuploader.hdfs.HdfsClient;
import com.company.traceuploader.hdfs.LocalFsHdfsClient;
import com.company.traceuploader.metadata.ChecksumService;
import com.company.traceuploader.metadata.FileIdGenerator;
import com.company.traceuploader.metadata.MetadataService;
import com.company.traceuploader.model.TraceFileMetadata;
import com.company.traceuploader.scanner.SealedFile;
import com.company.traceuploader.scanner.SealedFileScanner;
import com.company.traceuploader.state.JsonlWalUploadStateStore;
import com.company.traceuploader.state.UploadFileRecord;
import com.company.traceuploader.state.UploadState;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Coordinates Phase 2-4 processing without changing the Phase 0/1 scanner implementation. */
public final class UploaderWorker {
    private static final Logger LOG = System.getLogger(UploaderWorker.class.getName());

    private final AgentConfig config;
    private final SealedFileScanner scanner;

    public UploaderWorker(AgentConfig config, SealedFileScanner scanner) {
        this.config = config;
        this.scanner = scanner;
    }

    public void processOnce() throws Exception {
        Path walPath = config.localSpool().stateDirPath().resolve("upload-state.jsonl");
        try (JsonlWalUploadStateStore stateStore = new JsonlWalUploadStateStore(walPath)) {
            HdfsClient hdfs = createHdfs();
            MetadataService metadataService = new MetadataService(config, new ChecksumService(), new FileIdGenerator());
            CommitProtocol commitProtocol = new LocalFsCommitProtocol(hdfs, stateStore, config);
            List<SealedFile> sealedFiles = scanner.scan();
            LOG.log(Level.INFO, "Scan discovered {0} sealed file(s)", sealedFiles.size());
            for (SealedFile sealedFile : sealedFiles) {
                processFile(stateStore, metadataService, commitProtocol, sealedFile);
            }
        }
    }

    private void processFile(JsonlWalUploadStateStore stateStore, MetadataService metadataService,
                             CommitProtocol commitProtocol, SealedFile sealedFile) {
        try {
            TraceFileMetadata metadata = metadataService.build(sealedFile, nextAttempt(stateStore, sealedFile, metadataService));
            Optional<UploadFileRecord> existing = stateStore.getByFileId(metadata.fileId());
            if (existing.isEmpty()) {
                stateStore.upsert(UploadFileRecord.fromMetadata(metadata, UploadState.DISCOVERED, System.currentTimeMillis()));
                stateStore.updateState(metadata.fileId(), UploadState.SEALED, null);
            } else {
                stateStore.upsert(UploadFileRecord.fromMetadata(metadata, existing.get().state(), System.currentTimeMillis()));
                if (existing.get().state() != UploadState.COMMITTED_TO_HDFS) {
                    stateStore.updateState(metadata.fileId(), UploadState.SEALED, null);
                }
            }
            CommitResult result = commitProtocol.commit(metadata);
            System.out.printf("Committed candidate %s -> %s (%s)%n", metadata.localPath(), result.state(), result.message());
        } catch (Exception e) {
            LOG.log(Level.ERROR, "Failed to process sealed file " + sealedFile.dataPath(), e);
            System.err.printf("Failed to process %s: %s%n", sealedFile.dataPath(), e.getMessage());
        }
    }

    private int nextAttempt(JsonlWalUploadStateStore stateStore, SealedFile sealedFile, MetadataService metadataService) throws IOException {
        TraceFileMetadata probe = metadataService.build(sealedFile, 1);
        return stateStore.getByFileId(probe.fileId())
                .map(record -> record.state() == UploadState.RETRYABLE_FAILED ? record.attempt() + 1 : record.attempt())
                .orElse(1);
    }

    private HdfsClient createHdfs() throws IOException {
        if (!"localfs".equalsIgnoreCase(config.hdfs().implementation())) {
            throw new IllegalArgumentException("Only localfs HDFS implementation is supported in Phase 4: " + config.hdfs().implementation());
        }
        return new LocalFsHdfsClient(config.hdfs().localRootForTestingPath());
    }
}
