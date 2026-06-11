package com.company.traceuploader.commit;

import com.company.traceuploader.config.AgentConfig;
import com.company.traceuploader.config.ConfigLoader;
import com.company.traceuploader.hdfs.LocalFsHdfsClient;
import com.company.traceuploader.metadata.ChecksumService;
import com.company.traceuploader.metadata.FileIdGenerator;
import com.company.traceuploader.metadata.MetadataService;
import com.company.traceuploader.model.TraceFileMetadata;
import com.company.traceuploader.scanner.SealedFile;
import com.company.traceuploader.state.JsonlWalUploadStateStore;
import com.company.traceuploader.state.UploadFileRecord;
import com.company.traceuploader.state.UploadState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFsCommitProtocolTest {
    @TempDir
    Path tempDir;

    @Test
    void uploadsStagingVerifiesAndRenamesToFinalWithoutDirectFinalWrite() throws Exception {
        Fixture fixture = fixture();

        CommitResult result = fixture.protocol.commit(fixture.metadata);

        assertEquals(UploadState.COMMITTED_TO_HDFS, result.state());
        assertTrue(Files.exists(fixture.hdfs.toLocalPath(fixture.metadata.hdfsFinalPath())));
        assertFalse(Files.exists(fixture.hdfs.toLocalPath(fixture.metadata.hdfsStagingPath())));
        assertEquals(UploadState.COMMITTED_TO_HDFS, fixture.store.getByFileId(fixture.metadata.fileId()).orElseThrow().state());
    }

    @Test
    void treatsExistingMatchingFinalAsIdempotentSuccess() throws Exception {
        Fixture fixture = fixture();
        fixture.protocol.commit(fixture.metadata);

        CommitResult second = fixture.protocol.commit(fixture.metadata);

        assertEquals(UploadState.COMMITTED_TO_HDFS, second.state());
        assertTrue(second.message().contains("idempotent"));
    }

    @Test
    void quarantinesExistingFinalChecksumMismatchWithoutOverwrite() throws Exception {
        Fixture fixture = fixture();
        Path finalPath = fixture.hdfs.toLocalPath(fixture.metadata.hdfsFinalPath());
        Files.createDirectories(finalPath.getParent());
        Files.writeString(finalPath, "different\n");

        CommitResult result = fixture.protocol.commit(fixture.metadata);

        assertEquals(UploadState.QUARANTINED, result.state());
        assertEquals("different\n", Files.readString(finalPath));
    }

    @Test
    void recoversMatchingStagingResidueByRenamingIt() throws Exception {
        Fixture fixture = fixture();
        fixture.hdfs.upload(fixture.metadata.localPath(), fixture.metadata.hdfsStagingPath());

        CommitResult result = fixture.protocol.commit(fixture.metadata);

        assertEquals(UploadState.COMMITTED_TO_HDFS, result.state());
        assertTrue(Files.exists(fixture.hdfs.toLocalPath(fixture.metadata.hdfsFinalPath())));
        assertFalse(Files.exists(fixture.hdfs.toLocalPath(fixture.metadata.hdfsStagingPath())));
    }

    private Fixture fixture() throws Exception {
        Path file = tempDir.resolve("trace-payment-dev-local-c1-host001-pid123-bootlocal-20260610T100000-20260610T100500-seq000001.jsonl");
        Files.writeString(file, "one\ntwo\n");
        Path marker = tempDir.resolve(file.getFileName() + ".done");
        Files.createFile(marker);
        AgentConfig config = config();
        TraceFileMetadata metadata = new MetadataService(config, new ChecksumService(), new FileIdGenerator())
                .build(new SealedFile(file, marker, Files.size(file)), 1);
        LocalFsHdfsClient hdfs = new LocalFsHdfsClient(tempDir.resolve("fake-hdfs"));
        JsonlWalUploadStateStore store = new JsonlWalUploadStateStore(tempDir.resolve("state/upload-state.jsonl"));
        store.upsert(UploadFileRecord.fromMetadata(metadata, UploadState.DISCOVERED, System.currentTimeMillis()));
        LocalFsCommitProtocol protocol = new LocalFsCommitProtocol(hdfs, store, config);
        return new Fixture(metadata, hdfs, store, protocol);
    }

    private AgentConfig config() throws Exception {
        Path configFile = tempDir.resolve("agent.yaml");
        Files.writeString(configFile,
                "localSpool:\n" +
                "  sealedDir: /tmp/trace_spool/sealed\n" +
                "  stateDir: /tmp/trace_spool/state\n" +
                "hdfs:\n" +
                "  implementation: localfs\n" +
                "  localRootForTesting: /tmp/fake_hdfs\n" +
                "  rawBasePath: /warehouse/raw_trace\n" +
                "  stagingBasePath: /warehouse/raw_trace/_staging\n" +
                "  bucketCount: 16\n" +
                "upload:\n" +
                "  verifySizeAfterUpload: true\n" +
                "  verifyChecksumAfterUpload: true\n" +
                "  deleteStagingOnRetry: true\n");
        return new ConfigLoader().load(configFile);
    }

    private static final class Fixture {
        final TraceFileMetadata metadata;
        final LocalFsHdfsClient hdfs;
        final JsonlWalUploadStateStore store;
        final LocalFsCommitProtocol protocol;

        Fixture(TraceFileMetadata metadata, LocalFsHdfsClient hdfs, JsonlWalUploadStateStore store, LocalFsCommitProtocol protocol) {
            this.metadata = metadata;
            this.hdfs = hdfs;
            this.store = store;
            this.protocol = protocol;
        }
    }
}
