package com.company.traceuploader.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceUploaderMainTest {
    @TempDir
    Path tempDir;

    @Test
    void dryRunPrintsDiscoveredFilesWithoutUploading() throws Exception {
        Path sealedDir = tempDir.resolve("sealed");
        Files.createDirectories(sealedDir);
        Files.writeString(sealedDir.resolve("trace-a.jsonl"), "{}\n");
        Files.createFile(sealedDir.resolve("trace-a.jsonl.done"));
        Path config = tempDir.resolve("agent.yaml");
        Files.writeString(config,
                String.format("localSpool:\n" +
                "  sealedDir: %s\n" +
                "scanner:\n" +
                "  minStableMillis: 0\n" +
                "  maxFilesPerScan: 10\n", sealedDir.toString()));

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
            new TraceUploaderMain().run(new String[]{"--config", config.toString(), "--dry-run"});
        } finally {
            System.setOut(originalOut);
        }

        String output = stdout.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Would process sealed file:"));
        assertTrue(output.contains("trace-a.jsonl"));
    }
}
