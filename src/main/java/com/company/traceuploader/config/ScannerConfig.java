package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ScannerConfig {
    private String markerSuffix = ".done";
    private List<String> dataFileSuffixes = new ArrayList<>(List.of(".jsonl", ".jsonl.zst", ".log", ".log.zst"));
    private List<String> ignoredSuffixes = new ArrayList<>(List.of(".tmp", ".part", ".uploading"));
    private long minStableAgeSeconds = 0;
    private int maxFilesPerScan = 1000;
    private long scanIntervalSeconds = 10;

    public String getMarkerSuffix() {
        return markerSuffix;
    }

    public void setMarkerSuffix(String markerSuffix) {
        this.markerSuffix = markerSuffix;
    }

    public String markerSuffix() {
        return markerSuffix;
    }

    public List<String> getDataFileSuffixes() {
        return dataFileSuffixes;
    }

    public void setDataFileSuffixes(List<String> dataFileSuffixes) {
        this.dataFileSuffixes = dataFileSuffixes == null ? new ArrayList<>() : new ArrayList<>(dataFileSuffixes);
    }

    public List<String> dataFileSuffixes() {
        return dataFileSuffixes;
    }

    public List<String> getIgnoredSuffixes() {
        return ignoredSuffixes;
    }

    public void setIgnoredSuffixes(List<String> ignoredSuffixes) {
        this.ignoredSuffixes = ignoredSuffixes == null ? new ArrayList<>() : new ArrayList<>(ignoredSuffixes);
    }

    public List<String> ignoredSuffixes() {
        return ignoredSuffixes;
    }

    public long getMinStableAgeSeconds() {
        return minStableAgeSeconds;
    }

    public void setMinStableAgeSeconds(long minStableAgeSeconds) {
        this.minStableAgeSeconds = minStableAgeSeconds;
    }

    public long minStableAgeSeconds() {
        return minStableAgeSeconds;
    }

    public long minStableMillis() {
        if (minStableAgeSeconds <= 0) return 0L;
        return Math.multiplyExact(minStableAgeSeconds, 1000L);
    }

    public int getMaxFilesPerScan() {
        return maxFilesPerScan;
    }

    public void setMaxFilesPerScan(int maxFilesPerScan) {
        this.maxFilesPerScan = maxFilesPerScan;
    }

    public int maxFilesPerScan() {
        return maxFilesPerScan;
    }

    public long getScanIntervalSeconds() {
        return scanIntervalSeconds;
    }

    public void setScanIntervalSeconds(long scanIntervalSeconds) {
        this.scanIntervalSeconds = scanIntervalSeconds;
    }

    public long scanIntervalSeconds() {
        return scanIntervalSeconds;
    }

    public long scanIntervalMillis() {
        return Math.multiplyExact(scanIntervalSeconds, 1000L);
    }

    void validate() {
        if (markerSuffix == null || markerSuffix.isBlank()) {
            throw new IllegalArgumentException("scanner.markerSuffix must not be blank");
        }
        if (dataFileSuffixes == null || dataFileSuffixes.isEmpty()) {
            throw new IllegalArgumentException("scanner.dataFileSuffixes must not be empty");
        }
        if (ignoredSuffixes == null) {
            throw new IllegalArgumentException("scanner.ignoredSuffixes must not be null");
        }
        if (minStableAgeSeconds < 0) {
            throw new IllegalArgumentException("scanner.minStableAgeSeconds must be >= 0");
        }
        if (maxFilesPerScan <= 0) {
            throw new IllegalArgumentException("scanner.maxFilesPerScan must be > 0");
        }
        if (scanIntervalSeconds <= 0) {
            throw new IllegalArgumentException("scanner.scanIntervalSeconds must be > 0");
        }
    }

    @Override
    public String toString() {
        return "ScannerConfig{" +
                "markerSuffix='" + markerSuffix + '\'' +
                ", dataFileSuffixes=" + dataFileSuffixes +
                ", ignoredSuffixes=" + ignoredSuffixes +
                ", minStableAgeSeconds=" + minStableAgeSeconds +
                ", maxFilesPerScan=" + maxFilesPerScan +
                ", scanIntervalSeconds=" + scanIntervalSeconds +
                '}';
    }
}
