package com.company.traceuploader.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsProjectYamlShape() throws Exception {
        Path configFile = tempDir.resolve("agent.yaml");
        Files.writeString(configFile, """
                localSpool:
                  writingDir: /tmp/trace_spool/writing
                  sealedDir: /tmp/trace_spool/sealed
                scanner:
                  markerSuffix: .done
                  ignoredSuffixes:
                    - .tmp
                    - .part
                  minStableMillis: 1500
                  maxFilesPerScan: 7
                  scanIntervalSeconds: 3
                manifest:
                  type: local_jsonl
                  localPath: /tmp/trace_spool/state/manifest.jsonl
                """);

        AgentConfig config = new ConfigLoader().load(configFile);

        assertEquals(Path.of("/tmp/trace_spool/sealed"), config.localSpool().sealedDir());
        assertEquals(Path.of("/tmp/trace_spool/writing"), config.localSpool().writingDir());
        assertEquals(".done", config.scanner().markerSuffix());
        assertEquals(1500, config.scanner().minStableMillis());
        assertEquals(7, config.scanner().maxFilesPerScan());
        assertEquals(3000, config.scanner().scanIntervalMillis());
        assertEquals("local_jsonl", config.manifest().type());
        assertEquals(Path.of("/tmp/trace_spool/state/manifest.jsonl"), config.manifest().localPath());
    }
}
