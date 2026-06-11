package com.company.traceuploader.commit;

import com.company.traceuploader.state.UploadState;

public final class CommitResult {
    private final UploadState state;
    private final String message;

    public CommitResult(UploadState state, String message) {
        this.state = state;
        this.message = message;
    }

    public UploadState state() { return state; }
    public String message() { return message; }

    public UploadState getState() { return state; }
    public String getMessage() { return message; }
}
