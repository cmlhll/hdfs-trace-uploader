package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class TraceUploaderConfig {
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

    public AgentConfig agent() {
        return agent;
    }

    public LocalSpoolConfig getLocalSpool() {
        return localSpool;
    }

    public void setLocalSpool(LocalSpoolConfig localSpool) {
        this.localSpool = localSpool;
    }

    public LocalSpoolConfig localSpool() {
        return localSpool;
    }

    public ScannerConfig getScanner() {
        return scanner;
    }

    public void setScanner(ScannerConfig scanner) {
        this.scanner = scanner;
    }

    public ScannerConfig scanner() {
        return scanner;
    }

    public HdfsConfig getHdfs() {
        return hdfs;
    }

    public void setHdfs(HdfsConfig hdfs) {
        this.hdfs = hdfs;
    }

    public HdfsConfig hdfs() {
        return hdfs;
    }

    public UploadConfig getUpload() {
        return upload;
    }

    public void setUpload(UploadConfig upload) {
        this.upload = upload;
    }

    public UploadConfig upload() {
        return upload;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }

    public RetryConfig retry() {
        return retry;
    }

    public ManifestConfig getManifest() {
        return manifest;
    }

    public void setManifest(ManifestConfig manifest) {
        this.manifest = manifest;
    }

    public ManifestConfig manifest() {
        return manifest;
    }

    public GcConfig getGc() {
        return gc;
    }

    public void setGc(GcConfig gc) {
        this.gc = gc;
    }

    public GcConfig gc() {
        return gc;
    }

    public DiskWatermarkConfig getDiskWatermark() {
        return diskWatermark;
    }

    public void setDiskWatermark(DiskWatermarkConfig diskWatermark) {
        this.diskWatermark = diskWatermark;
    }

    public DiskWatermarkConfig diskWatermark() {
        return diskWatermark;
    }

    public MetricsConfig getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics;
    }

    public MetricsConfig metrics() {
        return metrics;
    }

    public void validate() {
        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(localSpool, "localSpool").validate();
        Objects.requireNonNull(scanner, "scanner").validate();
        Objects.requireNonNull(hdfs, "hdfs").validate();
        Objects.requireNonNull(upload, "upload").validate();
        Objects.requireNonNull(retry, "retry").validate();
        Objects.requireNonNull(manifest, "manifest").validate();
        Objects.requireNonNull(gc, "gc").validate();
        Objects.requireNonNull(diskWatermark, "diskWatermark").validate();
        Objects.requireNonNull(metrics, "metrics").validate();
    }

    @Override
    public String toString() {
        return "TraceUploaderConfig{" +
                "agent=" + agent +
                ", localSpool=" + localSpool +
                ", scanner=" + scanner +
                ", hdfs=" + hdfs +
                ", upload=" + upload +
                ", retry=" + retry +
                ", manifest=" + manifest +
                ", gc=" + gc +
                ", diskWatermark=" + diskWatermark +
                ", metrics=" + metrics +
                '}';
    }
}
