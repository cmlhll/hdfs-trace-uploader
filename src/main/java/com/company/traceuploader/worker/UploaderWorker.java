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

/** Coordinates scanner, state, HDFS commit, manifest, and local-GC readiness. */
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
        try (JsonlWalUploadStateStore stateStore = new JsonlWalUploadStateStore(walPath);
             ManifestWriter manifestWriter = createManifestWriter()) {
            HdfsClient hdfs = createHdfs();
            MetadataService metadataService = new MetadataService(config, new ChecksumService(), new FileIdGenerator());
            CommitProtocol commitProtocol = new LocalFsCommitProtocol(hdfs, stateStore, config);
            List<SealedFile> sealedFiles = scanner.scan();
            LOG.log(Level.INFO, "Scan discovered {0} sealed file(s)", sealedFiles.size());
            for (SealedFile sealedFile : sealedFiles) {
                processFile(stateStore, metadataService, commitProtocol, manifestWriter, sealedFile);
            }
        }
    }

    private void processFile(JsonlWalUploadStateStore stateStore, MetadataService metadataService,
                             CommitProtocol commitProtocol, ManifestWriter manifestWriter, SealedFile sealedFile) {
        TraceFileMetadata metadata = null;
        try {
            metadata = buildMetadataForNextStep(stateStore, sealedFile, metadataService);
            Optional<UploadFileRecord> existing = stateStore.getByFileId(metadata.fileId());

            if (existing.isPresent() && shouldSkip(existing.get().state())) {
                LOG.log(Level.INFO, "Skipping {0}; state={1}", metadata.fileId(), existing.get().state());
                return;
            }
            if (existing.isPresent() && existing.get().state() == UploadState.MANIFEST_COMMITTED) {
                stateStore.updateState(metadata.fileId(), UploadState.LOCAL_GC_READY, null);
                LOG.log(Level.INFO, "Marked {0} LOCAL_GC_READY after manifest commit", metadata.fileId());
                return;
            }
            if (existing.isPresent() && existing.get().state() == UploadState.COMMITTED_TO_HDFS) {
                writeManifestAndMarkGcReady(stateStore, manifestWriter, metadata, existing.get());
                return;
            }
            if (existing.isPresent()
                    && existing.get().state() == UploadState.RETRYABLE_FAILED
                    && existing.get().attempt() >= config.retry().maxAttempts()) {
                stateStore.updateState(metadata.fileId(), UploadState.PERMANENT_FAILED,
                        "retry.maxAttempts exceeded: " + config.retry().maxAttempts());
                return;
            }

            if (existing.isEmpty()) {
                stateStore.upsert(UploadFileRecord.fromMetadata(metadata, UploadState.DISCOVERED, System.currentTimeMillis()));
                stateStore.updateState(metadata.fileId(), UploadState.SEALED, null);
            } else {
                stateStore.upsert(UploadFileRecord.fromMetadata(metadata, existing.get().state(), System.currentTimeMillis()));
                stateStore.updateState(metadata.fileId(), UploadState.SEALED, null);
            }

            CommitResult result = commitProtocol.commit(metadata);
            if (result.state() == UploadState.COMMITTED_TO_HDFS) {
                UploadFileRecord committed = stateStore.getByFileId(metadata.fileId()).orElseThrow();
                writeManifestAndMarkGcReady(stateStore, manifestWriter, metadata, committed);
            } else {
                System.out.printf("Processed candidate %s -> %s (%s)%n",
                        metadata.localPath(), result.state(), result.message());
            }
        } catch (Exception e) {
            LOG.log(Level.ERROR, "Failed to process sealed file " + sealedFile.dataPath(), e);
            markRetryableFailure(stateStore, metadata, e);
            System.err.printf("Failed to process %s: %s%n", sealedFile.dataPath(), e.getMessage());
        }
    }

    private TraceFileMetadata buildMetadataForNextStep(JsonlWalUploadStateStore stateStore, SealedFile sealedFile,
                                                       MetadataService metadataService) throws IOException {
        TraceFileMetadata probe = metadataService.build(sealedFile, 1);
        Optional<UploadFileRecord> existing = stateStore.getByFileId(probe.fileId());
        int attempt = existing
                .map(record -> record.state() == UploadState.RETRYABLE_FAILED ? record.attempt() + 1 : record.attempt())
                .orElse(1);
        if (attempt == 1) {
            return probe;
        }
        return metadataService.build(sealedFile, attempt);
    }

    private boolean shouldSkip(UploadState state) {
        return state == UploadState.QUARANTINED
                || state == UploadState.PERMANENT_FAILED
                || state == UploadState.LOCAL_GC_READY
                || state == UploadState.LOCAL_GC_DONE;
    }

    private void writeManifestAndMarkGcReady(JsonlWalUploadStateStore stateStore, ManifestWriter manifestWriter,
                                             TraceFileMetadata metadata, UploadFileRecord committedRecord) throws IOException {
        Instant commitTime = Instant.ofEpochMilli(committedRecord.updatedAtMillis());
        ManifestRecord record = ManifestRecord.fromMetadata(metadata, config, commitTime);
        ManifestWriter.WriteResult writeResult = manifestWriter.writeIfAbsent(record);
        if (writeResult == ManifestWriter.WriteResult.WRITTEN
                || writeResult == ManifestWriter.WriteResult.ALREADY_EXISTS_MATCHING) {
            stateStore.updateState(metadata.fileId(), UploadState.MANIFEST_COMMITTED, null);
            stateStore.updateState(metadata.fileId(), UploadState.LOCAL_GC_READY, null);
            System.out.printf("Committed candidate %s -> %s (manifest %s)%n",
                    metadata.localPath(), UploadState.LOCAL_GC_READY, writeResult);
            return;
        }

        String message = "manifest already contains different content for file_id";
        stateStore.updateState(metadata.fileId(), UploadState.QUARANTINED, message);
        System.err.printf("Quarantined %s: %s%n", metadata.localPath(), message);
    }

    private void markRetryableFailure(JsonlWalUploadStateStore stateStore, TraceFileMetadata metadata, Exception error) {
        if (metadata == null) {
            return;
        }
        try {
            Optional<UploadFileRecord> record = stateStore.getByFileId(metadata.fileId());
            if (record.isEmpty()) {
                return;
            }
            if (record.get().state() == UploadState.COMMITTED_TO_HDFS) {
                stateStore.updateState(metadata.fileId(), UploadState.COMMITTED_TO_HDFS, error.getMessage());
                return;
            }
            UploadState nextState = record.get().attempt() >= config.retry().maxAttempts()
                    ? UploadState.PERMANENT_FAILED
                    : UploadState.RETRYABLE_FAILED;
            stateStore.updateState(metadata.fileId(), nextState, error.getMessage());
        } catch (IOException stateError) {
            LOG.log(Level.ERROR, "Failed to record retryable failure for " + metadata.fileId(), stateError);
        }
    }

    private ManifestWriter createManifestWriter() {
        if (!"local_jsonl".equalsIgnoreCase(config.manifest().type())) {
            throw new IllegalArgumentException("Only local_jsonl manifest is supported in Phase 5: " + config.manifest().type());
        }
        return new JsonlManifestWriter(Path.of(config.manifest().localPath()));
    }

    private HdfsClient createHdfs() throws IOException {
        if (!"localfs".equalsIgnoreCase(config.hdfs().implementation())) {
            throw new IllegalArgumentException("Only localfs HDFS implementation is supported in Phase 4: " + config.hdfs().implementation());
        }
        return new LocalFsHdfsClient(config.hdfs().localRootForTestingPath());
    }
}
