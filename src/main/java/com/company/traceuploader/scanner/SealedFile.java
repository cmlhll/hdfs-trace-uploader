package com.company.traceuploader.scanner;

import java.nio.file.Path;
import java.util.Objects;

public final class SealedFile {
    private final Path dataPath;
    private final Path markerPath;
    private final long sizeBytes;

    public SealedFile(Path dataPath, Path markerPath, long sizeBytes) {
        this.dataPath = dataPath;
        this.markerPath = markerPath;
        this.sizeBytes = sizeBytes;
    }

    public Path dataPath() { return dataPath; }
    public Path markerPath() { return markerPath; }
    public long sizeBytes() { return sizeBytes; }

    public Path getDataPath() { return dataPath; }
    public Path getMarkerPath() { return markerPath; }
    public long getSizeBytes() { return sizeBytes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SealedFile)) return false;
        SealedFile that = (SealedFile) o;
        return sizeBytes == that.sizeBytes
                && Objects.equals(dataPath, that.dataPath)
                && Objects.equals(markerPath, that.markerPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataPath, markerPath, sizeBytes);
    }

    @Override
    public String toString() {
        return "SealedFile[" +
                "dataPath=" + dataPath +
                ", markerPath=" + markerPath +
                ", sizeBytes=" + sizeBytes +
                ']';
    }
}
