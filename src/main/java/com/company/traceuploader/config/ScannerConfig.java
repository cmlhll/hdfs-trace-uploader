package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScannerConfig {
    private String markerSuffix = ".done";
    private List<String> dataFileSuffixes = new ArrayList<>(List.of(".jsonl.zst", ".log.zst"));
    private List<String> ignoredSuffixes = new ArrayList<>(List.of(".tmp", ".part", ".uploading"));
    private long minStableAgeSeconds = 30;
    private int maxFilesPerScan = 1000;
    private int scanIntervalSeconds = 10;

    public String getMarkerSuffix() {
        return markerSuffix;
    }

    public void setMarkerSuffix(String markerSuffix) {
        this.markerSuffix = markerSuffix;
    }

    public List<String> getDataFileSuffixes() {
        return dataFileSuffixes;
    }

    public void setDataFileSuffixes(List<String> dataFileSuffixes) {
        this.dataFileSuffixes = dataFileSuffixes == null ? new ArrayList<>() : new ArrayList<>(dataFileSuffixes);
    }

    public List<String> getIgnoredSuffixes() {
        return ignoredSuffixes;
    }

    public void setIgnoredSuffixes(List<String> ignoredSuffixes) {
        this.ignoredSuffixes = ignoredSuffixes == null ? new ArrayList<>() : new ArrayList<>(ignoredSuffixes);
    }

    public long getMinStableAgeSeconds() {
        return minStableAgeSeconds;
    }

    public void setMinStableAgeSeconds(long minStableAgeSeconds) {
        this.minStableAgeSeconds = minStableAgeSeconds;
    }

    public int getMaxFilesPerScan() {
        return maxFilesPerScan;
    }

    public void setMaxFilesPerScan(int maxFilesPerScan) {
        this.maxFilesPerScan = maxFilesPerScan;
    }

    public int getScanIntervalSeconds() {
        return scanIntervalSeconds;
    }

    public void setScanIntervalSeconds(int scanIntervalSeconds) {
        this.scanIntervalSeconds = scanIntervalSeconds;
    }
}
