package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ManifestConfig {
    private String type = "local_jsonl";
    private String localPath;
    private boolean writeIfAbsent = true;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public boolean isWriteIfAbsent() {
        return writeIfAbsent;
    }

    public void setWriteIfAbsent(boolean writeIfAbsent) {
        this.writeIfAbsent = writeIfAbsent;
    }
}
