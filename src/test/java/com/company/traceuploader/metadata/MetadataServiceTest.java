package com.company.traceuploader.metadata;

import com.company.traceuploader.config.AgentConfig;
import com.company.traceuploader.config.ConfigLoader;
import com.company.traceuploader.model.TraceFileMetadata;
import com.company.traceuploader.scanner.SealedFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesTraceFileAndBuildsStableMetadataAndPaths() throws Exception {
        Path file = tempDir.resolve("trace-payment-dev-local-c1-host001-pid123-bootlocal-20260610T100000-20260610T100500-seq000001.jsonl");
        Files.writeString(file, "one\ntwo\n");
        Path marker = tempDir.resolve(file.getFileName() + ".done");
        Files.createFile(marker);
        AgentConfig config = config();

        TraceFileMetadata metadata = new MetadataService(config, new ChecksumService(), new FileIdGenerator())
                .build(new SealedFile(file, marker, Files.size(file)), 2);

        assertEquals("payment", metadata.app());
        assertEquals("dev", metadata.env());
        assertEquals("local", metadata.region());
        assertEquals("c1", metadata.cluster());
        assertEquals("host001", metadata.host());
        assertEquals("pid123", metadata.pid());
        assertEquals("bootlocal", metadata.bootId());
        assertEquals("20260610T100000", metadata.startTs());
        assertEquals("20260610T100500", metadata.endTs());
        assertEquals("000001", metadata.seq());
        assertEquals(8, metadata.sizeBytes());
        assertEquals(2, metadata.recordCount());
        assertTrue(metadata.checksum().startsWith("sha256:"));
        assertEquals(64, metadata.fileId().length());
        assertTrue(metadata.hdfsFinalPath().contains("/warehouse/raw_trace/app=payment/dt=2026-06-10/hour=10/region=local/bucket="));
        assertTrue(metadata.hdfsFinalPath().endsWith(file.getFileName().toString()));
        assertTrue(metadata.hdfsStagingPath().contains("/warehouse/raw_trace/_staging/app=payment/dt=2026-06-10/hour=10/region=local/bucket="));
        assertTrue(metadata.hdfsStagingPath().endsWith(metadata.fileId() + ".attempt_2.tmp"));
    }

    @Test
    void rejectsReusableOrUnexpectedFileNames() throws Exception {
        Path file = tempDir.resolve("trace.log.1");
        Files.writeString(file, "bad\n");
        Path marker = tempDir.resolve("trace.log.1.done");
        Files.createFile(marker);
        MetadataService service = new MetadataService(config(), new ChecksumService(), new FileIdGenerator());

        assertThrows(IllegalArgumentException.class, () -> service.build(new SealedFile(file, marker, Files.size(file)), 1));
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
                "  bucketCount: 16\n");
        return new ConfigLoader().load(configFile);
    }
}
