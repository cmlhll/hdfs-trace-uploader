package com.company.traceuploader.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {
    @Test
    void loadsExampleAgentConfig() throws Exception {
        TraceUploaderConfig config = new ConfigLoader().load(Path.of("config/example-agent.yaml"));

        assertEquals("payment", config.getAgent().getApp());
        assertEquals("/data/trace_spool/sealed", config.getLocalSpool().getSealedDir());
        assertEquals(".done", config.getScanner().getMarkerSuffix());
        assertEquals(1000, config.getScanner().getMaxFilesPerScan());
        assertTrue(config.getScanner().getDataFileSuffixes().contains(".jsonl.zst"));
    }

    @Test
    void loadsConfigWithAsteriskValues(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("agent.yaml");
        Files.writeString(configPath,
                "agent:\n" +
                "  app: *trace\n" +
                "localSpool:\n" +
                "  sealedDir: /tmp/sealed\n" +
                "scanner:\n" +
                "  dataFileSuffixes:\n" +
                "    - .log.zst\n" +
                "  minStableAgeSeconds: 0\n" +
                "  maxFilesPerScan: 10\n");

        TraceUploaderConfig config = new ConfigLoader().load(configPath);

        assertEquals("trace", config.getAgent().getApp());
    }
}
