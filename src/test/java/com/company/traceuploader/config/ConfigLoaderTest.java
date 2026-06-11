package com.company.traceuploader.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsFullProjectYaml() throws Exception {
        Path configFile = tempDir.resolve("agent.yaml");
        Files.writeString(configFile,
                "agent:\n" +
                "  app: myapp\n" +
                "  env: staging\n" +
                "  region: us-east-1\n" +
                "  cluster: c2\n" +
                "  host: host002\n" +
                "  agentId: host002-trace-uploader\n" +
                "  agentVersion: 0.2.0\n" +
                "localSpool:\n" +
                "  writingDir: /opt/spool/writing\n" +
                "  sealedDir: /opt/spool/sealed\n" +
                "scanner:\n" +
                "  markerSuffix: .done\n" +
                "  dataFileSuffixes:\n" +
                "    - .jsonl\n" +
                "    - .log\n" +
                "  maxFilesPerScan: 50\n" +
                "  minStableAgeSeconds: 5\n" +
                "  scanIntervalSeconds: 15\n" +
                "hdfs:\n" +
                "  implementation: localfs\n" +
                "  localRootForTesting: /tmp/test_hdfs\n" +
                "  rawBasePath: /warehouse/raw_trace\n" +
                "  stagingBasePath: /warehouse/raw_trace/_staging\n" +
                "  bucketCount: 8\n" +
                "upload:\n" +
                "  maxConcurrentUploads: 4\n" +
                "  verifyChecksumAfterUpload: true\n" +
                "  verifySizeAfterUpload: false\n" +
                "  deleteStagingOnRetry: true\n" +
                "retry:\n" +
                "  maxAttempts: 5\n" +
                "  initialBackoffSeconds: 10\n" +
                "  maxBackoffSeconds: 300\n" +
                "  backoffMultiplier: 2.0\n" +
                "manifest:\n" +
                "  type: local_jsonl\n" +
                "  localPath: /opt/spool/state/manifest.jsonl\n" +
                "gc:\n" +
                "  enabled: true\n" +
                "  delayHoursAfterManifestCommit: 48\n" +
                "diskWatermark:\n" +
                "  warnPercent: 60\n" +
                "  errorPercent: 80\n" +
                "  criticalPercent: 95\n" +
                "metrics:\n" +
                "  enabled: false\n" +
                "  prometheusPort: 9090\n");

        AgentConfig config = new ConfigLoader().load(configFile);

        // Agent identity
        assertEquals("myapp", config.app());
        assertEquals("staging", config.env());
        assertEquals("us-east-1", config.region());
        assertEquals("c2", config.cluster());
        assertEquals("host002", config.host());
        assertEquals("host002-trace-uploader", config.agentId());
        assertEquals("0.2.0", config.agentVersion());

        // LocalSpool
        assertEquals("/opt/spool/sealed", config.localSpool().sealedDir());
        assertEquals("/opt/spool/writing", config.localSpool().writingDir());
        assertEquals(Path.of("/opt/spool/sealed"), config.localSpool().sealedDirPath());
        assertEquals(Path.of("/opt/spool/writing"), config.localSpool().writingDirPath());

        // Scanner
        assertEquals(".done", config.scanner().markerSuffix());
        assertEquals(2, config.scanner().dataFileSuffixes().size());
        assertEquals(50, config.scanner().maxFilesPerScan());
        assertEquals(5, config.scanner().minStableAgeSeconds());
        assertEquals(5000, config.scanner().minStableMillis());
        assertEquals(15, config.scanner().scanIntervalSeconds());
        assertEquals(15000, config.scanner().scanIntervalMillis());

        // HDFS
        assertEquals("localfs", config.hdfs().implementation());
        assertEquals("/tmp/test_hdfs", config.hdfs().localRootForTesting());
        assertEquals("/warehouse/raw_trace", config.hdfs().rawBasePath());
        assertEquals(8, config.hdfs().bucketCount());

        // Upload
        assertEquals(4, config.upload().maxConcurrentUploads());
        assertEquals(false, config.upload().verifySizeAfterUpload());

        // Retry
        assertEquals(5, config.retry().maxAttempts());
        assertEquals(10, config.retry().initialBackoffSeconds());

        // Manifest
        assertEquals("local_jsonl", config.manifest().type());
        assertEquals("/opt/spool/state/manifest.jsonl", config.manifest().localPath());

        // GC
        assertEquals(true, config.gc().enabled());
        assertEquals(48, config.gc().delayHoursAfterManifestCommit());

        // Disk watermark
        assertEquals(60, config.diskWatermark().warnPercent());
        assertEquals(80, config.diskWatermark().errorPercent());
        assertEquals(95, config.diskWatermark().criticalPercent());

        // Metrics
        assertEquals(false, config.metrics().enabled());
        assertEquals(9090, config.metrics().prometheusPort());
    }

    @Test
    void loadsMinimalYamlWithDefaults() throws Exception {
        Path configFile = tempDir.resolve("minimal.yaml");
        Files.writeString(configFile,
                "localSpool:\n" +
                "  sealedDir: /data/sealed\n" +
                "scanner:\n" +
                "  maxFilesPerScan: 10\n");

        AgentConfig config = new ConfigLoader().load(configFile);

        // Agent identity defaults
        assertEquals("payment", config.app());
        assertEquals("dev", config.env());
        assertEquals("local", config.region());

        // LocalSpool
        assertEquals("/data/sealed", config.localSpool().sealedDir());
        assertEquals("/tmp/trace_spool/writing", config.localSpool().writingDir());

        // Scanner defaults
        assertEquals(".done", config.scanner().markerSuffix());
        assertEquals(10, config.scanner().maxFilesPerScan());
        assertEquals(0, config.scanner().minStableAgeSeconds());
        assertEquals(10, config.scanner().scanIntervalSeconds());
    }
}
