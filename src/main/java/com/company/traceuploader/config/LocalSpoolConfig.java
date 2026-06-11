package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalSpoolConfig {
    private String baseDir;
    private String writingDir;
    private String sealedDir;
    private String committedDir;
    private String failedDir;
    private String quarantineDir;
    private String stateDir;
    private String tmpDir;

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getWritingDir() {
        return writingDir;
    }

    public void setWritingDir(String writingDir) {
        this.writingDir = writingDir;
    }

    public String getSealedDir() {
        return sealedDir;
    }

    public void setSealedDir(String sealedDir) {
        this.sealedDir = sealedDir;
    }

    public String getCommittedDir() {
        return committedDir;
    }

    public void setCommittedDir(String committedDir) {
        this.committedDir = committedDir;
    }

    public String getFailedDir() {
        return failedDir;
    }

    public void setFailedDir(String failedDir) {
        this.failedDir = failedDir;
    }

    public String getQuarantineDir() {
        return quarantineDir;
    }

    public void setQuarantineDir(String quarantineDir) {
        this.quarantineDir = quarantineDir;
    }

    public String getStateDir() {
        return stateDir;
    }

    public void setStateDir(String stateDir) {
        this.stateDir = stateDir;
    }

    public String getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(String tmpDir) {
        this.tmpDir = tmpDir;
    }
}
