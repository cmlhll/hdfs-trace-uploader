package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadConfig {
    private int maxConcurrentUploads = 2;
    private long maxUploadBandwidthBytesPerSec;
    private boolean verifyChecksumAfterUpload;
    private boolean verifySizeAfterUpload = true;
    private boolean deleteStagingOnRetry = true;

    public int getMaxConcurrentUploads() {
        return maxConcurrentUploads;
    }

    public void setMaxConcurrentUploads(int maxConcurrentUploads) {
        this.maxConcurrentUploads = maxConcurrentUploads;
    }

    public long getMaxUploadBandwidthBytesPerSec() {
        return maxUploadBandwidthBytesPerSec;
    }

    public void setMaxUploadBandwidthBytesPerSec(long maxUploadBandwidthBytesPerSec) {
        this.maxUploadBandwidthBytesPerSec = maxUploadBandwidthBytesPerSec;
    }

    public boolean isVerifyChecksumAfterUpload() {
        return verifyChecksumAfterUpload;
    }

    public void setVerifyChecksumAfterUpload(boolean verifyChecksumAfterUpload) {
        this.verifyChecksumAfterUpload = verifyChecksumAfterUpload;
    }

    public boolean isVerifySizeAfterUpload() {
        return verifySizeAfterUpload;
    }

    public void setVerifySizeAfterUpload(boolean verifySizeAfterUpload) {
        this.verifySizeAfterUpload = verifySizeAfterUpload;
    }

    public boolean isDeleteStagingOnRetry() {
        return deleteStagingOnRetry;
    }

    public void setDeleteStagingOnRetry(boolean deleteStagingOnRetry) {
        this.deleteStagingOnRetry = deleteStagingOnRetry;
    }
}
