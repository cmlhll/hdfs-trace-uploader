package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class LocalSpoolConfig {
    private String baseDir = "/tmp/trace_spool";
    private String writingDir = "/tmp/trace_spool/writing";
    private String sealedDir = "/tmp/trace_spool/sealed";
    private String committedDir = "/tmp/trace_spool/committed";
    private String failedDir = "/tmp/trace_spool/failed";
    private String quarantineDir = "/tmp/trace_spool/quarantine";
    private String stateDir = "/tmp/trace_spool/state";
    private String tmpDir = "/tmp/trace_spool/tmp";

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String baseDir() {
        return baseDir;
    }

    public Path baseDirPath() {
        return Path.of(baseDir);
    }

    public String getWritingDir() {
        return writingDir;
    }

    public void setWritingDir(String writingDir) {
        this.writingDir = writingDir;
    }

    public String writingDir() {
        return writingDir;
    }

    public Path writingDirPath() {
        return Path.of(writingDir);
    }

    public String getSealedDir() {
        return sealedDir;
    }

    public void setSealedDir(String sealedDir) {
        this.sealedDir = sealedDir;
    }

    public String sealedDir() {
        return sealedDir;
    }

    public Path sealedDirPath() {
        return Path.of(sealedDir);
    }

    public String getCommittedDir() {
        return committedDir;
    }

    public void setCommittedDir(String committedDir) {
        this.committedDir = committedDir;
    }

    public String committedDir() {
        return committedDir;
    }

    public Path committedDirPath() {
        return Path.of(committedDir);
    }

    public String getFailedDir() {
        return failedDir;
    }

    public void setFailedDir(String failedDir) {
        this.failedDir = failedDir;
    }

    public String failedDir() {
        return failedDir;
    }

    public Path failedDirPath() {
        return Path.of(failedDir);
    }

    public String getQuarantineDir() {
        return quarantineDir;
    }

    public void setQuarantineDir(String quarantineDir) {
        this.quarantineDir = quarantineDir;
    }

    public String quarantineDir() {
        return quarantineDir;
    }

    public Path quarantineDirPath() {
        return Path.of(quarantineDir);
    }

    public String getStateDir() {
        return stateDir;
    }

    public void setStateDir(String stateDir) {
        this.stateDir = stateDir;
    }

    public String stateDir() {
        return stateDir;
    }

    public Path stateDirPath() {
        return Path.of(stateDir);
    }

    public String getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(String tmpDir) {
        this.tmpDir = tmpDir;
    }

    public String tmpDir() {
        return tmpDir;
    }

    public Path tmpDirPath() {
        return Path.of(tmpDir);
    }

    void validate() {
        requirePath("localSpool.sealedDir", sealedDir);
        requirePath("localSpool.stateDir", stateDir);
    }

    private static void requirePath(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    @Override
    public String toString() {
        return "LocalSpoolConfig{" +
                "baseDir='" + baseDir + '\'' +
                ", writingDir='" + writingDir + '\'' +
                ", sealedDir='" + sealedDir + '\'' +
                ", committedDir='" + committedDir + '\'' +
                ", failedDir='" + failedDir + '\'' +
                ", quarantineDir='" + quarantineDir + '\'' +
                ", stateDir='" + stateDir + '\'' +
                ", tmpDir='" + tmpDir + '\'' +
                '}';
    }
}
