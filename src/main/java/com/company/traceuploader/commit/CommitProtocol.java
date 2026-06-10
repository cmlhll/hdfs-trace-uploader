package com.company.traceuploader.commit;
import com.company.traceuploader.model.TraceFileMetadata;import java.io.IOException;
public interface CommitProtocol { CommitResult commit(TraceFileMetadata metadata) throws IOException; }
