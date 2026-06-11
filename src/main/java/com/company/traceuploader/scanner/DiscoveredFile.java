package com.company.traceuploader.scanner;

import java.nio.file.Path;
import java.util.Objects;

public final class DiscoveredFile {
    private final Path localPath;
    private final Path donePath;
    private final long sizeBytes;
    private final long lastModifiedTimeMs;

    public DiscoveredFile(Path localPath, Path donePath, long sizeBytes, long lastModifiedTimeMs) {
        this.localPath = Objects.requireNonNull(localPath, "localPath");
        this.donePath = Objects.requireNonNull(donePath, "donePath");
        this.sizeBytes = sizeBytes;
        this.lastModifiedTimeMs = lastModifiedTimeMs;
    }

    public Path getLocalPath() {
        return localPath;
    }

    public Path getDonePath() {
        return donePath;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public long getLastModifiedTimeMs() {
        return lastModifiedTimeMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DiscoveredFile)) {
            return false;
        }
        DiscoveredFile that = (DiscoveredFile) o;
        return sizeBytes == that.sizeBytes
                && lastModifiedTimeMs == that.lastModifiedTimeMs
                && localPath.equals(that.localPath)
                && donePath.equals(that.donePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localPath, donePath, sizeBytes, lastModifiedTimeMs);
    }

    @Override
    public String toString() {
        return "DiscoveredFile{"
                + "localPath=" + localPath
                + ", donePath=" + donePath
                + ", sizeBytes=" + sizeBytes
                + ", lastModifiedTimeMs=" + lastModifiedTimeMs
                + '}';
    }
}
