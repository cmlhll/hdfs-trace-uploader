package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class UploadConfig {
    private int maxConcurrentUploads = 2;
    private long maxUploadBandwidthBytesPerSec = 0;
    private boolean verifyChecksumAfterUpload = true;
    private boolean verifySizeAfterUpload = true;
    private boolean deleteStagingOnRetry = true;

    public int getMaxConcurrentUploads() {
        return maxConcurrentUploads;
    }

    public void setMaxConcurrentUploads(int maxConcurrentUploads) {
        this.maxConcurrentUploads = maxConcurrentUploads;
    }

    public int maxConcurrentUploads() {
        return maxConcurrentUploads;
    }

    public long getMaxUploadBandwidthBytesPerSec() {
        return maxUploadBandwidthBytesPerSec;
    }

    public void setMaxUploadBandwidthBytesPerSec(long maxUploadBandwidthBytesPerSec) {
        this.maxUploadBandwidthBytesPerSec = maxUploadBandwidthBytesPerSec;
    }

    public long maxUploadBandwidthBytesPerSec() {
        return maxUploadBandwidthBytesPerSec;
    }

    public boolean isVerifyChecksumAfterUpload() {
        return verifyChecksumAfterUpload;
    }

    public void setVerifyChecksumAfterUpload(boolean verifyChecksumAfterUpload) {
        this.verifyChecksumAfterUpload = verifyChecksumAfterUpload;
    }

    public boolean verifyChecksumAfterUpload() {
        return verifyChecksumAfterUpload;
    }

    public boolean isVerifySizeAfterUpload() {
        return verifySizeAfterUpload;
    }

    public void setVerifySizeAfterUpload(boolean verifySizeAfterUpload) {
        this.verifySizeAfterUpload = verifySizeAfterUpload;
    }

    public boolean verifySizeAfterUpload() {
        return verifySizeAfterUpload;
    }

    public boolean isDeleteStagingOnRetry() {
        return deleteStagingOnRetry;
    }

    public void setDeleteStagingOnRetry(boolean deleteStagingOnRetry) {
        this.deleteStagingOnRetry = deleteStagingOnRetry;
    }

    public boolean deleteStagingOnRetry() {
        return deleteStagingOnRetry;
    }

    void validate() {
        if (maxConcurrentUploads <= 0) {
            throw new IllegalArgumentException("upload.maxConcurrentUploads must be > 0");
        }
        if (maxUploadBandwidthBytesPerSec < 0) {
            throw new IllegalArgumentException("upload.maxUploadBandwidthBytesPerSec must be >= 0");
        }
    }

    @Override
    public String toString() {
        return "UploadConfig{" +
                "maxConcurrentUploads=" + maxConcurrentUploads +
                ", maxUploadBandwidthBytesPerSec=" + maxUploadBandwidthBytesPerSec +
                ", verifyChecksumAfterUpload=" + verifyChecksumAfterUpload +
                ", verifySizeAfterUpload=" + verifySizeAfterUpload +
                ", deleteStagingOnRetry=" + deleteStagingOnRetry +
                '}';
    }
}
