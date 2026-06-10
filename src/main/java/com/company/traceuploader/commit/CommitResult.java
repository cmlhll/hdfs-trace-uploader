package com.company.traceuploader.commit;
import com.company.traceuploader.state.UploadState;
public record CommitResult(UploadState state, String message) {}
