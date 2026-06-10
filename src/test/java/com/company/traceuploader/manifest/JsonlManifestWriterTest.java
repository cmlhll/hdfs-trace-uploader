package com.company.traceuploader.manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonlManifestWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writeIfAbsentIsIdempotentForIdenticalRecords() throws Exception {
        Path manifest = tempDir.resolve("manifest.jsonl");
        ManifestRecord record = new ManifestRecord("file-1", "/final/file", 10, "sha256:abc", 2,
                "MANIFEST_COMMITTED", "2026-06-10T10:00:00Z");
        JsonlManifestWriter writer = new JsonlManifestWriter(manifest);

        writer.writeIfAbsent(record);
        writer.writeIfAbsent(record);
        new JsonlManifestWriter(manifest).writeIfAbsent(record);

        assertEquals(1, Files.readAllLines(manifest).size());
    }

    @Test
    void writeIfAbsentRejectsConflictingRecordForSameFileId() throws Exception {
        Path manifest = tempDir.resolve("manifest.jsonl");
        JsonlManifestWriter writer = new JsonlManifestWriter(manifest);
        writer.writeIfAbsent(new ManifestRecord("file-1", "/final/file", 10, "sha256:abc", 2,
                "MANIFEST_COMMITTED", "2026-06-10T10:00:00Z"));

        IOException error = assertThrows(IOException.class, () -> writer.writeIfAbsent(
                new ManifestRecord("file-1", "/final/file", 11, "sha256:different", 2,
                        "MANIFEST_COMMITTED", "2026-06-10T10:00:00Z")));
        assertEquals("Manifest record conflict for file_id=file-1", error.getMessage());
    }
}
