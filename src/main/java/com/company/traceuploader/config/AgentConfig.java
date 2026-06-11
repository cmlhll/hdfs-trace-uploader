package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

/**
 * Agent identity configuration plus delegated access to all sub-config sections.
 *
 * <p>This class serves both as the {@code agent:} YAML section holder (identity fields)
 * <b>and</b> as the backward-compatible root config object expected by existing callers.
 * The {@link ConfigLoader} now returns {@link TraceUploaderConfig} from its {@code load()}
 * method, but this class retains {@code localSpool()}, {@code scanner()}, {@code hdfs()},
 * {@code upload()}, {@code retry()}, {@code manifest()}, {@code gc()},
 * {@code diskWatermark()}, and {@code metrics()} delegates for callers that used the
 * previous API.</p>
 *
 * <p>When populated via YAML deserialization under the {@code agent:} key, only the
 * identity fields are set. The sub-config references are set by
 * {@link #injectSubConfigs(TraceUploaderConfig)} after the full YAML is loaded.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AgentConfig {

    // ---- identity fields (from the agent: section) ----

    private String app = "payment";
    private String env = "dev";
    private String region = "local";
    private String cluster = "c1";
    private String host = "host001";
    private String agentId = "host001-trace-uploader";
    private String agentVersion = "0.1.0";

    // ---- delegated sub-config references (set by injectSubConfigs) ----
    // Package-private so TraceUploaderConfig can set them.

    private LocalSpoolConfig localSpool = new LocalSpoolConfig();
    private ScannerConfig scanner = new ScannerConfig();
    private HdfsConfig hdfs = new HdfsConfig();
    private UploadConfig upload = new UploadConfig();
    private RetryConfig retry = new RetryConfig();
    private ManifestConfig manifest = new ManifestConfig();
    private GcConfig gc = new GcConfig();
    private DiskWatermarkConfig diskWatermark = new DiskWatermarkConfig();
    private MetricsConfig metrics = new MetricsConfig();

    // ---- constructors ----

    /** Default constructor for Jackson deserialization. */
    public AgentConfig() {
    }

    // ---- Jackson getters/setters (agent: identity fields) ----

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    // ---- fluent accessors (backward compat) ----

    public String app() {
        return app;
    }

    public String env() {
        return env;
    }

    public String region() {
        return region;
    }

    public String cluster() {
        return cluster;
    }

    public String host() {
        return host;
    }

    public String agentId() {
        return agentId;
    }

    public String agentVersion() {
        return agentVersion;
    }

    // ---- sub-config delegate accessors (backward compat with old API) ----

    public LocalSpoolConfig localSpool() {
        return localSpool;
    }

    public ScannerConfig scanner() {
        return scanner;
    }

    public HdfsConfig hdfs() {
        return hdfs;
    }

    public UploadConfig upload() {
        return upload;
    }

    public RetryConfig retry() {
        return retry;
    }

    public ManifestConfig manifest() {
        return manifest;
    }

    public GcConfig gc() {
        return gc;
    }

    public DiskWatermarkConfig diskWatermark() {
        return diskWatermark;
    }

    public MetricsConfig metrics() {
        return metrics;
    }

    // ---- package-private setters for sub-config injection ----

    void setLocalSpool(LocalSpoolConfig localSpool) {
        this.localSpool = localSpool != null ? localSpool : new LocalSpoolConfig();
    }

    void setScanner(ScannerConfig scanner) {
        this.scanner = scanner != null ? scanner : new ScannerConfig();
    }

    void setHdfs(HdfsConfig hdfs) {
        this.hdfs = hdfs != null ? hdfs : new HdfsConfig();
    }

    void setUpload(UploadConfig upload) {
        this.upload = upload != null ? upload : new UploadConfig();
    }

    void setRetry(RetryConfig retry) {
        this.retry = retry != null ? retry : new RetryConfig();
    }

    void setManifest(ManifestConfig manifest) {
        this.manifest = manifest != null ? manifest : new ManifestConfig();
    }

    void setGc(GcConfig gc) {
        this.gc = gc != null ? gc : new GcConfig();
    }

    void setDiskWatermark(DiskWatermarkConfig diskWatermark) {
        this.diskWatermark = diskWatermark != null ? diskWatermark : new DiskWatermarkConfig();
    }

    void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics != null ? metrics : new MetricsConfig();
    }

    /**
     * Injects all sub-config sections from a fully-loaded {@link TraceUploaderConfig}
     * into this AgentConfig, overwriting the defaults.
     */
    void injectSubConfigs(TraceUploaderConfig root) {
        if (root == null) return;
        if (root.getLocalSpool() != null) setLocalSpool(root.getLocalSpool());
        if (root.getScanner() != null) setScanner(root.getScanner());
        if (root.getHdfs() != null) setHdfs(root.getHdfs());
        if (root.getUpload() != null) setUpload(root.getUpload());
        if (root.getRetry() != null) setRetry(root.getRetry());
        if (root.getManifest() != null) setManifest(root.getManifest());
        if (root.getGc() != null) setGc(root.getGc());
        if (root.getDiskWatermark() != null) setDiskWatermark(root.getDiskWatermark());
        if (root.getMetrics() != null) setMetrics(root.getMetrics());
    }

    // ---- validation ----

    void validate() {
        Objects.requireNonNull(app, "agent.app is required");
        Objects.requireNonNull(env, "agent.env is required");
        Objects.requireNonNull(region, "agent.region is required");
        Objects.requireNonNull(cluster, "agent.cluster is required");
        Objects.requireNonNull(host, "agent.host is required");
        Objects.requireNonNull(agentId, "agent.agentId is required");
        Objects.requireNonNull(agentVersion, "agent.agentVersion is required");
        Objects.requireNonNull(localSpool, "agent.localSpool is required").validate();
        Objects.requireNonNull(scanner, "agent.scanner is required").validate();
        Objects.requireNonNull(hdfs, "agent.hdfs is required").validate();
        Objects.requireNonNull(upload, "agent.upload is required").validate();
        Objects.requireNonNull(retry, "agent.retry is required").validate();
        Objects.requireNonNull(manifest, "agent.manifest is required").validate();
        Objects.requireNonNull(gc, "agent.gc is required").validate();
        Objects.requireNonNull(diskWatermark, "agent.diskWatermark is required").validate();
        Objects.requireNonNull(metrics, "agent.metrics is required").validate();
    }

    // ---- toString ----

    @Override
    public String toString() {
        return "AgentConfig{"
                + "app='" + app + '\''
                + ", env='" + env + '\''
                + ", region='" + region + '\''
                + ", cluster='" + cluster + '\''
                + ", host='" + host + '\''
                + ", agentId='" + agentId + '\''
                + ", agentVersion='" + agentVersion + '\''
                + ", localSpool=" + localSpool
                + ", scanner=" + scanner
                + ", hdfs=" + hdfs
                + ", upload=" + upload
                + ", retry=" + retry
                + ", manifest=" + manifest
                + ", gc=" + gc
                + ", diskWatermark=" + diskWatermark
                + ", metrics=" + metrics
                + '}';
    }
}
