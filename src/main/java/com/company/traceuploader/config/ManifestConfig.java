package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ManifestConfig {
    private String type = "local_jsonl";
    private String localPath = "/tmp/trace_spool/state/manifest.jsonl";
    private boolean writeIfAbsent = true;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String localPath() {
        return localPath;
    }

    public boolean isWriteIfAbsent() {
        return writeIfAbsent;
    }

    public void setWriteIfAbsent(boolean writeIfAbsent) {
        this.writeIfAbsent = writeIfAbsent;
    }

    public boolean writeIfAbsent() {
        return writeIfAbsent;
    }

    void validate() {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("manifest.type is required");
        }
        if (localPath == null || localPath.isBlank()) {
            throw new IllegalArgumentException("manifest.localPath is required");
        }
    }

    @Override
    public String toString() {
        return "ManifestConfig{" +
                "type='" + type + '\'' +
                ", localPath='" + localPath + '\'' +
                ", writeIfAbsent=" + writeIfAbsent +
                '}';
    }
}
