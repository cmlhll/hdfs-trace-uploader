package com.company.traceuploader.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Configuration used by the uploader. */
public final class AgentConfig {
    private final LocalSpool localSpool;
    private final Scanner scanner;
    private final Hdfs hdfs;
    private final Upload upload;
    private final Manifest manifest;

    public AgentConfig(LocalSpool localSpool, Scanner scanner, Hdfs hdfs, Upload upload, Manifest manifest) {
        this.localSpool = Objects.requireNonNull(localSpool, "localSpool");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.hdfs = Objects.requireNonNull(hdfs, "hdfs");
        this.upload = Objects.requireNonNull(upload, "upload");
        this.manifest = Objects.requireNonNull(manifest, "manifest");
    }

    public LocalSpool localSpool() { return localSpool; }
    public Scanner scanner() { return scanner; }
    public Hdfs hdfs() { return hdfs; }
    public Upload upload() { return upload; }
    public Manifest manifest() { return manifest; }

    public static final class LocalSpool {
        private Path sealedDir;
        private Path writingDir;
        private Path stateDir;
        private Path quarantineDir;

        public Path sealedDir() { return sealedDir; }
        public void setSealedDir(Path sealedDir) { this.sealedDir = sealedDir; }
        public Path writingDir() { return writingDir; }
        public void setWritingDir(Path writingDir) { this.writingDir = writingDir; }
        public Path stateDir() { return stateDir; }
        public void setStateDir(Path stateDir) { this.stateDir = stateDir; }
        public Path quarantineDir() { return quarantineDir; }
        public void setQuarantineDir(Path quarantineDir) { this.quarantineDir = quarantineDir; }

        void validate() {
            if (sealedDir == null) throw new IllegalArgumentException("localSpool.sealedDir is required");
            if (stateDir == null) stateDir = sealedDir.resolveSibling("state");
        }
    }

    public static final class Scanner {
        private String markerSuffix = ".done";
        private final List<String> ignoredSuffixes = new ArrayList<>(List.of(".tmp", ".part", ".uploading"));
        private long minStableMillis = 0;
        private int maxFilesPerScan = 1000;
        private long scanIntervalMillis = 10_000;

        public String markerSuffix() { return markerSuffix; }
        public void setMarkerSuffix(String markerSuffix) { this.markerSuffix = markerSuffix; }
        public List<String> ignoredSuffixes() { return ignoredSuffixes; }
        public long minStableMillis() { return minStableMillis; }
        public void setMinStableMillis(long minStableMillis) { this.minStableMillis = minStableMillis; }
        public int maxFilesPerScan() { return maxFilesPerScan; }
        public void setMaxFilesPerScan(int maxFilesPerScan) { this.maxFilesPerScan = maxFilesPerScan; }
        public long scanIntervalMillis() { return scanIntervalMillis; }
        public void setScanIntervalMillis(long scanIntervalMillis) { this.scanIntervalMillis = scanIntervalMillis; }

        void validate() {
            if (markerSuffix == null || markerSuffix.isBlank()) throw new IllegalArgumentException("scanner.markerSuffix must not be blank");
            if (minStableMillis < 0) throw new IllegalArgumentException("scanner.minStableMillis must be >= 0");
            if (maxFilesPerScan <= 0) throw new IllegalArgumentException("scanner.maxFilesPerScan must be > 0");
            if (scanIntervalMillis <= 0) throw new IllegalArgumentException("scanner.scanIntervalMillis must be > 0");
        }
    }

    public static final class Hdfs {
        private String implementation = "localfs";
        private Path localRootForTesting = Path.of("/tmp/fake_hdfs");
        private String rawBasePath = "/warehouse/raw_trace";
        private String stagingBasePath = "/warehouse/raw_trace/_staging";
        private String finalPathTemplate = "{rawBasePath}/app={app}/dt={dt}/hour={hour}/region={region}/bucket={bucket}/{fileName}";
        private String stagingPathTemplate = "{stagingBasePath}/app={app}/dt={dt}/hour={hour}/region={region}/bucket={bucket}/{fileId}.attempt_{attempt}.tmp";
        private int bucketCount = 16;

        public String implementation() { return implementation; }
        public void setImplementation(String implementation) { this.implementation = implementation; }
        public Path localRootForTesting() { return localRootForTesting; }
        public void setLocalRootForTesting(Path localRootForTesting) { this.localRootForTesting = localRootForTesting; }
        public String rawBasePath() { return rawBasePath; }
        public void setRawBasePath(String rawBasePath) { this.rawBasePath = rawBasePath; }
        public String stagingBasePath() { return stagingBasePath; }
        public void setStagingBasePath(String stagingBasePath) { this.stagingBasePath = stagingBasePath; }
        public String finalPathTemplate() { return finalPathTemplate; }
        public void setFinalPathTemplate(String finalPathTemplate) { this.finalPathTemplate = finalPathTemplate; }
        public String stagingPathTemplate() { return stagingPathTemplate; }
        public void setStagingPathTemplate(String stagingPathTemplate) { this.stagingPathTemplate = stagingPathTemplate; }
        public int bucketCount() { return bucketCount; }
        public void setBucketCount(int bucketCount) { this.bucketCount = bucketCount; }

        void validate() {
            if (implementation == null || implementation.isBlank()) throw new IllegalArgumentException("hdfs.implementation is required");
            if (localRootForTesting == null) throw new IllegalArgumentException("hdfs.localRootForTesting is required for localfs");
            if (bucketCount <= 0) throw new IllegalArgumentException("hdfs.bucketCount must be > 0");
        }
    }

    public static final class Upload {
        private boolean verifySizeAfterUpload = true;
        private boolean verifyChecksumAfterUpload = true;
        private boolean deleteStagingOnRetry = true;

        public boolean verifySizeAfterUpload() { return verifySizeAfterUpload; }
        public void setVerifySizeAfterUpload(boolean verifySizeAfterUpload) { this.verifySizeAfterUpload = verifySizeAfterUpload; }
        public boolean verifyChecksumAfterUpload() { return verifyChecksumAfterUpload; }
        public void setVerifyChecksumAfterUpload(boolean verifyChecksumAfterUpload) { this.verifyChecksumAfterUpload = verifyChecksumAfterUpload; }
        public boolean deleteStagingOnRetry() { return deleteStagingOnRetry; }
        public void setDeleteStagingOnRetry(boolean deleteStagingOnRetry) { this.deleteStagingOnRetry = deleteStagingOnRetry; }
    }

    public static final class Manifest {
        private String type = "local_jsonl";
        private Path localPath;

        public String type() { return type; }
        public void setType(String type) { this.type = type; }
        public Path localPath() { return localPath; }
        public void setLocalPath(Path localPath) { this.localPath = localPath; }

        void validate(LocalSpool localSpool) {
            if (type == null || type.isBlank()) throw new IllegalArgumentException("manifest.type is required");
            if (localPath == null) localPath = localSpool.stateDir().resolve("manifest.jsonl");
        }
    }

    public void validate() {
        localSpool.validate();
        scanner.validate();
        hdfs.validate();
        manifest.validate(localSpool);
    }
}
