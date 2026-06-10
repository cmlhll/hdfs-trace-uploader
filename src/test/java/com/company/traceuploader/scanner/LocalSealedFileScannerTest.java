package com.company.traceuploader.scanner;

import com.company.traceuploader.config.AgentConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSealedFileScannerTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-10T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void scansOnlyDataFilesWithDoneMarkers() throws Exception {
        AgentConfig.Scanner config = scannerConfig(0, 10);
        Files.writeString(tempDir.resolve("ready.jsonl"), "ok\n");
        Files.createFile(tempDir.resolve("ready.jsonl.done"));
        Files.writeString(tempDir.resolve("missing-marker.jsonl"), "skip\n");

        List<SealedFile> files = new LocalSealedFileScanner(tempDir, config, CLOCK).scan();

        assertEquals(1, files.size());
        assertEquals("ready.jsonl", files.get(0).dataPath().getFileName().toString());
        assertEquals("ready.jsonl.done", files.get(0).markerPath().getFileName().toString());
    }

    @Test
    void ignoresWritingDirectoryAndTemporarySuffixes() throws Exception {
        AgentConfig.Scanner config = scannerConfig(0, 10);
        Path writing = tempDir.resolve("writing");
        Files.createDirectory(writing);
        Files.writeString(writing.resolve("active.jsonl"), "active\n");
        Files.createFile(writing.resolve("active.jsonl.done"));
        for (String suffix : List.of(".tmp", ".part", ".uploading")) {
            Files.writeString(tempDir.resolve("skip" + suffix), "skip\n");
            Files.createFile(tempDir.resolve("skip" + suffix + ".done"));
        }
        Files.writeString(tempDir.resolve("ready.jsonl"), "ok\n");
        Files.createFile(tempDir.resolve("ready.jsonl.done"));

        List<SealedFile> files = new LocalSealedFileScanner(tempDir, config, CLOCK).scan();

        assertEquals(1, files.size());
        assertEquals("ready.jsonl", files.get(0).dataPath().getFileName().toString());
    }

    @Test
    void respectsMinStableMillisForDataAndMarker() throws Exception {
        AgentConfig.Scanner config = scannerConfig(5_000, 10);
        Path oldData = tempDir.resolve("old.jsonl");
        Path oldMarker = tempDir.resolve("old.jsonl.done");
        Files.writeString(oldData, "old\n");
        Files.createFile(oldMarker);
        FileTime stableTime = FileTime.from(Instant.parse("2026-06-10T11:59:50Z"));
        Files.setLastModifiedTime(oldData, stableTime);
        Files.setLastModifiedTime(oldMarker, stableTime);

        Path newData = tempDir.resolve("new.jsonl");
        Path newMarker = tempDir.resolve("new.jsonl.done");
        Files.writeString(newData, "new\n");
        Files.createFile(newMarker);
        FileTime unstableTime = FileTime.from(Instant.parse("2026-06-10T11:59:58Z"));
        Files.setLastModifiedTime(newData, unstableTime);
        Files.setLastModifiedTime(newMarker, stableTime);

        List<SealedFile> files = new LocalSealedFileScanner(tempDir, config, CLOCK).scan();

        assertEquals(1, files.size());
        assertEquals("old.jsonl", files.get(0).dataPath().getFileName().toString());
    }

    @Test
    void respectsMaxFilesPerScanAndSortsByFileName() throws Exception {
        AgentConfig.Scanner config = scannerConfig(0, 2);
        for (String name : List.of("b.jsonl", "a.jsonl", "c.jsonl")) {
            Files.writeString(tempDir.resolve(name), name + "\n");
            Files.createFile(tempDir.resolve(name + ".done"));
        }

        List<SealedFile> files = new LocalSealedFileScanner(tempDir, config, CLOCK).scan();

        assertEquals(2, files.size());
        assertEquals("a.jsonl", files.get(0).dataPath().getFileName().toString());
        assertEquals("b.jsonl", files.get(1).dataPath().getFileName().toString());
    }

    @Test
    void returnsEmptyListWhenSealedDirDoesNotExist() throws Exception {
        AgentConfig.Scanner config = scannerConfig(0, 10);
        List<SealedFile> files = new LocalSealedFileScanner(tempDir.resolve("missing"), config, CLOCK).scan();
        assertTrue(files.isEmpty());
    }

    private static AgentConfig.Scanner scannerConfig(long minStableMillis, int maxFilesPerScan) {
        AgentConfig.Scanner config = new AgentConfig.Scanner();
        config.setMinStableMillis(minStableMillis);
        config.setMaxFilesPerScan(maxFilesPerScan);
        return config;
    }
}
