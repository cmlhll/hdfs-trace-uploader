package com.company.traceuploader.manifest;
import com.company.traceuploader.model.TraceFileMetadata;import java.io.IOException;
public interface ManifestWriter { void writeIfAbsent(TraceFileMetadata metadata) throws IOException; }
