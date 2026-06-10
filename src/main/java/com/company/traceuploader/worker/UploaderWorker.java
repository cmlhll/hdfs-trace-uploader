package com.company.traceuploader.worker;

import com.company.traceuploader.commit.CommitProtocol;
import com.company.traceuploader.commit.CommitResult;
import com.company.traceuploader.commit.LocalFsCommitProtocol;
import com.company.traceuploader.config.AgentConfig;
import com.company.traceuploader.hdfs.HdfsClient;
import com.company.traceuploader.hdfs.LocalFsHdfsClient;
import com.company.traceuploader.manifest.JsonlManifestWriter;
import com.company.traceuploader.manifest.ManifestRecord;
import com.company.traceuploader.manifest.ManifestWriter;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Coordinates Phase 2-4 processing without changing the Phase 0/1 scanner implementation. */
public final class UploaderWorker {
    private static final Logger LOG = System.getLogger(UploaderWorker.class.getName());

    private final AgentConfig config;
    private final SealedFileScanner scanner;
    private final HdfsClient hdfsOverride;
    private final ManifestWriter manifestWriterOverride;

    public UploaderWorker(AgentConfig config, SealedFileScanner scanner) {
        this(config, scanner, null, null);
    }

    public UploaderWorker(AgentConfig config, SealedFileScanner scanner, HdfsClient hdfsOverride,
                          ManifestWriter manifestWriterOverride) {
        this.config = config;
        this.scanner = scanner;
        this.hdfsOverride = hdfsOverride;
        this.manifestWriterOverride = manifestWriterOverride;
    }

    public void processOnce() throws Exception {
        Path walPath = config.localSpool().stateDir().resolve("upload-state.jsonl");
        try (JsonlWalUploadStateStore stateStore = new JsonlWalUploadStateStore(walPath)) {
            HdfsClient hdfs = hdfsOverride == null ? createHdfs() : hdfsOverride;
            MetadataService metadataService = new MetadataService(config, new ChecksumService(), new FileIdGenerator());
            CommitProtocol commitProtocol = new LocalFsCommitProtocol(hdfs, stateStore, config);
            ManifestWriter manifestWriter = manifestWriterOverride == null ? createManifestWriter() : manifestWriterOverride;
            List<SealedFile> sealedFiles = scanner.scan();
            LOG.log(Level.INFO, "Scan discovered {0} sealed file(s)", sealedFiles.size());
            for (SealedFile sealedFile : sealedFiles) {
                processFile(stateStore, metadataService, commitProtocol, manifestWriter, sealedFile);
            }
        }
    }

    private void processFile(JsonlWalUploadStateStore stateStore, MetadataService metadataService,
                             CommitProtocol commitProtocol, ManifestWriter manifestWriter, SealedFile sealedFile) {
        try {
            TraceFileMetadata metadata = metadataService.build(sealedFile, nextAttempt(stateStore, sealedFile, metadataService));
            Optional<UploadFileRecord> existing = stateStore.getByFileId(metadata.fileId());
            if (existing.isEmpty()) {
                stateStore.upsert(UploadFileRecord.fromMetadata(metadata, UploadState.DISCOVERED, System.currentTimeMillis()));
                stateStore.updateState(metadata.fileId(), UploadState.SEALED, null);
            } else {
                stateStore.upsert(UploadFileRecord.fromMetadata(metadata, existing.get().state(), System.currentTimeMillis()));
                if (shouldReschedule(existing.get().state())) {
                    stateStore.updateState(metadata.fileId(), UploadState.SEALED, null);
                }
            }
            CommitResult result = commitProtocol.commit(metadata);
            System.out.printf("Committed candidate %s -> %s (%s)%n", metadata.localPath(), result.state(), result.message());
            if (result.state() == UploadState.COMMITTED_TO_HDFS) {
                commitManifest(stateStore, manifestWriter, metadata.fileId());
            }
        } catch (Exception e) {
            LOG.log(Level.ERROR, "Failed to process sealed file " + sealedFile.dataPath(), e);
            System.err.printf("Failed to process %s: %s%n", sealedFile.dataPath(), e.getMessage());
        }
    }

    private boolean shouldReschedule(UploadState state) {
        return state != UploadState.COMMITTED_TO_HDFS
                && state != UploadState.MANIFEST_COMMITTED
                && state != UploadState.LOCAL_GC_READY
                && state != UploadState.LOCAL_GC_DONE
                && state != UploadState.QUARANTINED
                && state != UploadState.PERMANENT_FAILED;
    }

    private void commitManifest(JsonlWalUploadStateStore stateStore, ManifestWriter manifestWriter, String fileId) throws IOException {
        UploadFileRecord committed = stateStore.getByFileId(fileId)
                .orElseThrow(() -> new IllegalStateException("Missing state record for file_id=" + fileId));
        ManifestRecord record = ManifestRecord.fromUploadRecord(committed, Instant.ofEpochMilli(committed.createdAtMillis()));
        try {
            manifestWriter.writeIfAbsent(record);
            stateStore.updateState(fileId, UploadState.MANIFEST_COMMITTED, null);
            stateStore.updateState(fileId, UploadState.LOCAL_GC_READY, null);
            LOG.log(Level.INFO, "Manifest committed for file_id={0}; local file is now GC ready", fileId);
        } catch (IOException e) {
            stateStore.updateState(fileId, UploadState.COMMITTED_TO_HDFS, "manifest write failed: " + e.getMessage());
            LOG.log(Level.ERROR, "Manifest write failed after HDFS commit for file_id=" + fileId, e);
            throw e;
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
        return new LocalFsHdfsClient(config.hdfs().localRootForTesting());
    }

    private ManifestWriter createManifestWriter() throws IOException {
        if (!"local_jsonl".equalsIgnoreCase(config.manifest().type())) {
            throw new IllegalArgumentException("Only local_jsonl manifest is supported in Phase 5: " + config.manifest().type());
        }
        return new JsonlManifestWriter(config.manifest().localPath());
    }
}
