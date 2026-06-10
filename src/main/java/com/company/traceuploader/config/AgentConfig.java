package com.company.traceuploader.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Configuration used by the Phase 0/1 uploader scaffold. */
public final class AgentConfig {
    private final LocalSpool localSpool;
    private final Scanner scanner;

    public AgentConfig(LocalSpool localSpool, Scanner scanner) {
        this.localSpool = Objects.requireNonNull(localSpool, "localSpool");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    public LocalSpool localSpool() {
        return localSpool;
    }

    public Scanner scanner() {
        return scanner;
    }

    public static final class LocalSpool {
        private Path sealedDir;
        private Path writingDir;

        public Path sealedDir() {
            return sealedDir;
        }

        public void setSealedDir(Path sealedDir) {
            this.sealedDir = sealedDir;
        }

        public Path writingDir() {
            return writingDir;
        }

        public void setWritingDir(Path writingDir) {
            this.writingDir = writingDir;
        }

        void validate() {
            if (sealedDir == null) {
                throw new IllegalArgumentException("localSpool.sealedDir is required");
            }
        }
    }

    public static final class Scanner {
        private String markerSuffix = ".done";
        private final List<String> ignoredSuffixes = new ArrayList<>(List.of(".tmp", ".part", ".uploading"));
        private long minStableMillis = 0;
        private int maxFilesPerScan = 1000;
        private long scanIntervalMillis = 10_000;

        public String markerSuffix() {
            return markerSuffix;
        }

        public void setMarkerSuffix(String markerSuffix) {
            this.markerSuffix = markerSuffix;
        }

        public List<String> ignoredSuffixes() {
            return ignoredSuffixes;
        }

        public long minStableMillis() {
            return minStableMillis;
        }

        public void setMinStableMillis(long minStableMillis) {
            this.minStableMillis = minStableMillis;
        }

        public int maxFilesPerScan() {
            return maxFilesPerScan;
        }

        public void setMaxFilesPerScan(int maxFilesPerScan) {
            this.maxFilesPerScan = maxFilesPerScan;
        }

        public long scanIntervalMillis() {
            return scanIntervalMillis;
        }

        public void setScanIntervalMillis(long scanIntervalMillis) {
            this.scanIntervalMillis = scanIntervalMillis;
        }

        void validate() {
            if (markerSuffix == null || markerSuffix.isBlank()) {
                throw new IllegalArgumentException("scanner.markerSuffix must not be blank");
            }
            if (minStableMillis < 0) {
                throw new IllegalArgumentException("scanner.minStableMillis must be >= 0");
            }
            if (maxFilesPerScan <= 0) {
                throw new IllegalArgumentException("scanner.maxFilesPerScan must be > 0");
            }
            if (scanIntervalMillis <= 0) {
                throw new IllegalArgumentException("scanner.scanIntervalMillis must be > 0");
            }
        }
    }
}
