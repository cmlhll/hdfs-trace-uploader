package com.company.traceuploader.manifest;

import java.io.IOException;

public interface ManifestWriter extends AutoCloseable {
    enum WriteResult {
        WRITTEN,
        ALREADY_EXISTS_MATCHING,
        ALREADY_EXISTS_CONFLICT
    }

    WriteResult writeIfAbsent(ManifestRecord record) throws IOException;

    @Override
    void close() throws IOException;
}
