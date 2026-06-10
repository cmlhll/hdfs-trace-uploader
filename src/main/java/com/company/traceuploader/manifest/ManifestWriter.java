package com.company.traceuploader.manifest;

import java.io.IOException;

public interface ManifestWriter {
    void writeIfAbsent(ManifestRecord record) throws IOException;
}
