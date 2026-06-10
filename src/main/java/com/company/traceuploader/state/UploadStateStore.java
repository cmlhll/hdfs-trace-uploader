package com.company.traceuploader.state;
import java.io.IOException;import java.util.*;
public interface UploadStateStore extends AutoCloseable {
    void upsert(UploadFileRecord record) throws IOException;
    void updateState(String fileId, UploadState state, String error) throws IOException;
    Optional<UploadFileRecord> getByFileId(String fileId);
    java.util.List<UploadFileRecord> findPending(int limit);
    void close() throws IOException;
}
