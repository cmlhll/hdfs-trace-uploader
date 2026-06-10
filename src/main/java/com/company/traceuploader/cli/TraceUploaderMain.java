package com.company.traceuploader.cli;

import com.company.traceuploader.commit.CommitProtocol;
import com.company.traceuploader.commit.CommitResult;
import com.company.traceuploader.commit.LocalFsCommitProtocol;
import com.company.traceuploader.config.AgentConfig;
import com.company.traceuploader.config.ConfigLoader;
import com.company.traceuploader.hdfs.HdfsClient;
import com.company.traceuploader.hdfs.LocalFsHdfsClient;
import com.company.traceuploader.metadata.ChecksumService;
import com.company.traceuploader.metadata.FileIdGenerator;
import com.company.traceuploader.metadata.MetadataService;
import com.company.traceuploader.model.TraceFileMetadata;
import com.company.traceuploader.scanner.LocalSealedFileScanner;
import com.company.traceuploader.scanner.SealedFile;
import com.company.traceuploader.scanner.SealedFileScanner;
import com.company.traceuploader.state.JsonlWalUploadStateStore;
import com.company.traceuploader.state.UploadFileRecord;
import com.company.traceuploader.state.UploadState;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

public final class TraceUploaderMain {
    private static final Logger LOG = System.getLogger(TraceUploaderMain.class.getName());

    public static void main(String[] args) throws Exception {
        new TraceUploaderMain().run(args);
    }

    void run(String[] args) throws Exception {
        CliOptions options = CliOptions.parse(args);
        if (options.help()) {
            System.out.println(CliOptions.usage());
            return;
        }

        AgentConfig config = new ConfigLoader().load(options.configPath());
        Files.createDirectories(config.localSpool().sealedDir());
        Files.createDirectories(config.localSpool().stateDir());
        SealedFileScanner scanner = new LocalSealedFileScanner(config.localSpool().sealedDir(), config.scanner(), Clock.systemUTC());

        LOG.log(Level.INFO, "Loaded config {0}; dryRun={1}; once={2}", options.configPath(), options.dryRun(), options.once());
        if (options.dryRun()) {
            scanOnly(scanner, true);
            return;
        }
        if (options.once()) {
            processOnce(config, scanner);
            return;
        }

        while (!Thread.currentThread().isInterrupted()) {
            processOnce(config, scanner);
            Thread.sleep(config.scanner().scanIntervalMillis());
        }
    }

    private void scanOnly(SealedFileScanner scanner, boolean dryRun) throws IOException {
        List<SealedFile> files = scanner.scan();
        String mode = dryRun ? "Dry run" : "Scan";
        LOG.log(Level.INFO, "{0} discovered {1} sealed file(s)", mode, files.size());
        for (SealedFile file : files) {
            System.out.printf("Would process sealed file: %s (marker=%s, sizeBytes=%d)%n",
                    file.dataPath(), file.markerPath(), file.sizeBytes());
        }
    }

    private void processOnce(AgentConfig config, SealedFileScanner scanner) throws Exception {
        Path walPath = config.localSpool().stateDir().resolve("upload-state.jsonl");
        try (JsonlWalUploadStateStore stateStore = new JsonlWalUploadStateStore(walPath)) {
            HdfsClient hdfs = createHdfs(config);
            MetadataService metadataService = new MetadataService(config, new ChecksumService(), new FileIdGenerator());
            CommitProtocol commitProtocol = new LocalFsCommitProtocol(hdfs, stateStore, config);
            List<SealedFile> sealedFiles = scanner.scan();
            LOG.log(Level.INFO, "Scan discovered {0} sealed file(s)", sealedFiles.size());
            for (SealedFile sealedFile : sealedFiles) {
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
        }
    }

    private int nextAttempt(JsonlWalUploadStateStore stateStore, SealedFile sealedFile, MetadataService metadataService) throws IOException {
        TraceFileMetadata probe = metadataService.build(sealedFile, 1);
        return stateStore.getByFileId(probe.fileId())
                .map(record -> record.state() == UploadState.RETRYABLE_FAILED ? record.attempt() + 1 : record.attempt())
                .orElse(1);
    }

    private HdfsClient createHdfs(AgentConfig config) throws IOException {
        if (!"localfs".equalsIgnoreCase(config.hdfs().implementation())) {
            throw new IllegalArgumentException("Only localfs HDFS implementation is supported in Phase 4: " + config.hdfs().implementation());
        }
        return new LocalFsHdfsClient(config.hdfs().localRootForTesting());
    }

    record CliOptions(Path configPath, boolean dryRun, boolean once, boolean help) {
        static CliOptions parse(String[] args) {
            Path configPath = null;
            boolean dryRun = false;
            boolean once = false;
            boolean help = false;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--config" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("--config requires a path");
                        configPath = Path.of(args[++i]);
                    }
                    case "--dry-run" -> dryRun = true;
                    case "--once" -> once = true;
                    case "--help", "-h" -> help = true;
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i] + System.lineSeparator() + usage());
                }
            }
            if (!help && configPath == null) throw new IllegalArgumentException("--config is required" + System.lineSeparator() + usage());
            return new CliOptions(configPath, dryRun, once, help);
        }

        static String usage() {
            return "Usage: java -jar target/hdfs-trace-uploader.jar --config <path> [--dry-run] [--once]";
        }
    }
}
