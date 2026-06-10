package com.company.traceuploader.worker;

import com.company.traceuploader.config.AgentConfig;
import com.company.traceuploader.config.ConfigLoader;
import com.company.traceuploader.hdfs.LocalFsHdfsClient;
import com.company.traceuploader.manifest.ManifestRecord;
import com.company.traceuploader.manifest.ManifestWriter;
import com.company.traceuploader.scanner.SealedFile;
import com.company.traceuploader.state.JsonlWalUploadStateStore;
import com.company.traceuploader.state.UploadState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploaderWorkerTest {
    @TempDir
    Path tempDir;

    @Test
    void commitsManifestAndMovesStateToLocalGcReadyIdempotently() throws Exception {
        Fixture fixture = fixture();
        UploaderWorker worker = new UploaderWorker(fixture.config, () -> List.of(fixture.sealedFile));

        worker.processOnce();
        worker.processOnce();

        assertEquals(1, Files.walk(fixture.fakeHdfs).filter(Files::isRegularFile).count());
        assertEquals(1, Files.readAllLines(fixture.manifestPath).size());
        try (JsonlWalUploadStateStore store = new JsonlWalUploadStateStore(fixture.config.localSpool().stateDir().resolve("upload-state.jsonl"))) {
            assertEquals(UploadState.LOCAL_GC_READY, store.getByFileId(readManifestFileId(fixture.manifestPath)).orElseThrow().state());
        }
    }

    @Test
    void leavesStateCommittedToHdfsWhenManifestWriteFailsAfterFinalCommit() throws Exception {
        Fixture fixture = fixture();
        ManifestWriter failingManifest = new ManifestWriter() {
            @Override
            public void writeIfAbsent(ManifestRecord record) throws IOException {
                throw new IOException("manifest unavailable");
            }
        };
        UploaderWorker worker = new UploaderWorker(fixture.config, () -> List.of(fixture.sealedFile),
                new LocalFsHdfsClient(fixture.fakeHdfs), failingManifest);

        worker.processOnce();

        assertEquals(1, Files.walk(fixture.fakeHdfs).filter(Files::isRegularFile).count());
        try (JsonlWalUploadStateStore store = new JsonlWalUploadStateStore(fixture.config.localSpool().stateDir().resolve("upload-state.jsonl"))) {
            assertEquals(UploadState.COMMITTED_TO_HDFS, store.findPending(10).stream().findFirst().orElseThrow().state());
        }
    }

    private String readManifestFileId(Path manifestPath) throws IOException {
        String line = Files.readString(manifestPath);
        String prefix = "\"file_id\":\"";
        int start = line.indexOf(prefix) + prefix.length();
        return line.substring(start, line.indexOf('"', start));
    }

    private Fixture fixture() throws Exception {
        Path sealedDir = tempDir.resolve("spool/sealed");
        Path stateDir = tempDir.resolve("spool/state");
        Path fakeHdfs = tempDir.resolve("fake-hdfs");
        Files.createDirectories(sealedDir);
        Files.createDirectories(stateDir);
        Files.createDirectories(fakeHdfs);
        Path data = sealedDir.resolve("trace-payment-dev-local-c1-host001-pid123-bootlocal-20260610T100000-20260610T100500-seq000001.jsonl");
        Files.writeString(data, "one\ntwo\n");
        Path marker = sealedDir.resolve(data.getFileName() + ".done");
        Files.createFile(marker);
        Path manifestPath = stateDir.resolve("manifest.jsonl");
        Path configPath = tempDir.resolve("agent.yaml");
        Files.writeString(configPath, """
                localSpool:
                  sealedDir: %s
                  stateDir: %s
                scanner:
                  minStableMillis: 0
                hdfs:
                  implementation: localfs
                  localRootForTesting: %s
                  rawBasePath: /warehouse/raw_trace
                  stagingBasePath: /warehouse/raw_trace/_staging
                  bucketCount: 16
                upload:
                  verifySizeAfterUpload: true
                  verifyChecksumAfterUpload: true
                manifest:
                  type: local_jsonl
                  localPath: %s
                """.formatted(sealedDir, stateDir, fakeHdfs, manifestPath));
        AgentConfig config = new ConfigLoader().load(configPath);
        return new Fixture(config, new SealedFile(data, marker, Files.size(data)), fakeHdfs, manifestPath);
    }

    private record Fixture(AgentConfig config, SealedFile sealedFile, Path fakeHdfs, Path manifestPath) {
    }
}
