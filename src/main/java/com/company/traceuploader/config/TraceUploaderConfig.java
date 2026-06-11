package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceUploaderConfig {
    private AgentConfig agent = new AgentConfig();
    private LocalSpoolConfig localSpool = new LocalSpoolConfig();
    private ScannerConfig scanner = new ScannerConfig();
    private HdfsConfig hdfs = new HdfsConfig();
    private UploadConfig upload = new UploadConfig();
    private RetryConfig retry = new RetryConfig();
    private ManifestConfig manifest = new ManifestConfig();
    private GcConfig gc = new GcConfig();
    private DiskWatermarkConfig diskWatermark = new DiskWatermarkConfig();
    private MetricsConfig metrics = new MetricsConfig();

    public AgentConfig getAgent() {
        return agent;
    }

    public void setAgent(AgentConfig agent) {
        this.agent = agent;
    }

    public LocalSpoolConfig getLocalSpool() {
        return localSpool;
    }

    public void setLocalSpool(LocalSpoolConfig localSpool) {
        this.localSpool = localSpool;
    }

    public ScannerConfig getScanner() {
        return scanner;
    }

    public void setScanner(ScannerConfig scanner) {
        this.scanner = scanner;
    }

    public HdfsConfig getHdfs() {
        return hdfs;
    }

    public void setHdfs(HdfsConfig hdfs) {
        this.hdfs = hdfs;
    }

    public UploadConfig getUpload() {
        return upload;
    }

    public void setUpload(UploadConfig upload) {
        this.upload = upload;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }

    public ManifestConfig getManifest() {
        return manifest;
    }

    public void setManifest(ManifestConfig manifest) {
        this.manifest = manifest;
    }

    public GcConfig getGc() {
        return gc;
    }

    public void setGc(GcConfig gc) {
        this.gc = gc;
    }

    public DiskWatermarkConfig getDiskWatermark() {
        return diskWatermark;
    }

    public void setDiskWatermark(DiskWatermarkConfig diskWatermark) {
        this.diskWatermark = diskWatermark;
    }

    public MetricsConfig getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics;
    }

    public void applyDefaults() {
        if (agent == null) {
            agent = new AgentConfig();
        }
        if (localSpool == null) {
            localSpool = new LocalSpoolConfig();
        }
        if (scanner == null) {
            scanner = new ScannerConfig();
        }
        if (hdfs == null) {
            hdfs = new HdfsConfig();
        }
        if (upload == null) {
            upload = new UploadConfig();
        }
        if (retry == null) {
            retry = new RetryConfig();
        }
        if (manifest == null) {
            manifest = new ManifestConfig();
        }
        if (gc == null) {
            gc = new GcConfig();
        }
        if (diskWatermark == null) {
            diskWatermark = new DiskWatermarkConfig();
        }
        if (metrics == null) {
            metrics = new MetricsConfig();
        }
        if (scanner.getDataFileSuffixes() == null) {
            scanner.setDataFileSuffixes(new ArrayList<>());
        }
        if (scanner.getIgnoredSuffixes() == null) {
            scanner.setIgnoredSuffixes(new ArrayList<>());
        }
    }

    public void validate() {
        requireNonBlank(localSpool.getSealedDir(), "localSpool.sealedDir");
        requireNonBlank(scanner.getMarkerSuffix(), "scanner.markerSuffix");
        if (scanner.getDataFileSuffixes().isEmpty()) {
            throw new IllegalArgumentException("scanner.dataFileSuffixes must contain at least one suffix");
        }
        if (scanner.getMinStableAgeSeconds() < 0) {
            throw new IllegalArgumentException("scanner.minStableAgeSeconds must be >= 0");
        }
        if (scanner.getMaxFilesPerScan() <= 0) {
            throw new IllegalArgumentException("scanner.maxFilesPerScan must be > 0");
        }
        if (scanner.getScanIntervalSeconds() < 0) {
            throw new IllegalArgumentException("scanner.scanIntervalSeconds must be >= 0");
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
