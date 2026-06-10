package com.company.traceuploader.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonlWalUploadStateStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsStateTransitionsAndRecoversLatestRecord() throws Exception {
        Path wal = tempDir.resolve("state/upload-state.jsonl");
        UploadFileRecord record = new UploadFileRecord("file-1", "/tmp/local", "/final", "/staging", 7,
                "sha256:abc", 1, 1, UploadState.DISCOVERED, null, 10, 10);

        try (JsonlWalUploadStateStore store = new JsonlWalUploadStateStore(wal)) {
            store.upsert(record);
            store.updateState("file-1", UploadState.UPLOADING, null);
            assertEquals(UploadState.UPLOADING, store.getByFileId("file-1").orElseThrow().state());
        }

        try (JsonlWalUploadStateStore recovered = new JsonlWalUploadStateStore(wal)) {
            assertEquals(UploadState.UPLOADING, recovered.getByFileId("file-1").orElseThrow().state());
            assertEquals(1, recovered.findPending(10).size());
            recovered.updateState("file-1", UploadState.COMMITTED_TO_HDFS, null);
            assertTrue(recovered.findPending(10).isEmpty());
        }
    }
}
