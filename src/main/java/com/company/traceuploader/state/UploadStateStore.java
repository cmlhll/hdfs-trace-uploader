package com.company.traceuploader.state;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface UploadStateStore extends AutoCloseable {
    void upsert(UploadFileRecord record) throws IOException;
    void updateState(String fileId, UploadState state, String lastError) throws IOException;
    Optional<UploadFileRecord> getByFileId(String fileId);
    List<UploadFileRecord> findPending(int limit);
    @Override void close() throws IOException;
}
