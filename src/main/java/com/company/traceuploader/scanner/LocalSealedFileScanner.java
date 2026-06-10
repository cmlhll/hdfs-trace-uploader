package com.company.traceuploader.scanner;

import com.company.traceuploader.config.AgentConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Scans a sealed directory for stable data files that have matching done markers. */
public final class LocalSealedFileScanner implements SealedFileScanner {
    private final Path sealedDir;
    private final String markerSuffix;
    private final List<String> ignoredSuffixes;
    private final long minStableMillis;
    private final int maxFilesPerScan;
    private final Clock clock;

    public LocalSealedFileScanner(Path sealedDir, AgentConfig.Scanner scannerConfig, Clock clock) {
        this.sealedDir = Objects.requireNonNull(sealedDir, "sealedDir");
        Objects.requireNonNull(scannerConfig, "scannerConfig");
        this.markerSuffix = scannerConfig.markerSuffix();
        this.ignoredSuffixes = List.copyOf(scannerConfig.ignoredSuffixes());
        this.minStableMillis = scannerConfig.minStableMillis();
        this.maxFilesPerScan = scannerConfig.maxFilesPerScan();
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public List<SealedFile> scan() throws IOException {
        if (!Files.isDirectory(sealedDir)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(sealedDir)) {
            List<Path> dataFiles = entries
                    .filter(Files::isRegularFile)
                    .filter(this::isDataFileCandidate)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .filter(this::hasMarker)
                    .filter(this::isStable)
                    .limit(maxFilesPerScan)
                    .toList();
            return toSealedFiles(dataFiles);
        }
    }

    private boolean isDataFileCandidate(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(markerSuffix)) {
            return false;
        }
        return ignoredSuffixes.stream().noneMatch(fileName::endsWith);
    }

    private boolean hasMarker(Path path) {
        return Files.isRegularFile(markerPath(path));
    }

    private boolean isStable(Path dataPath) {
        try {
            long nowMillis = clock.millis();
            Path markerPath = markerPath(dataPath);
            return ageMillis(nowMillis, Files.getLastModifiedTime(dataPath)) >= minStableMillis
                    && ageMillis(nowMillis, Files.getLastModifiedTime(markerPath)) >= minStableMillis;
        } catch (IOException e) {
            return false;
        }
    }

    private long ageMillis(long nowMillis, FileTime fileTime) {
        return nowMillis - fileTime.toMillis();
    }

    private List<SealedFile> toSealedFiles(List<Path> dataFiles) throws IOException {
        java.util.ArrayList<SealedFile> sealedFiles = new java.util.ArrayList<>(dataFiles.size());
        for (Path dataPath : dataFiles) {
            sealedFiles.add(new SealedFile(dataPath, markerPath(dataPath), Files.size(dataPath)));
        }
        return List.copyOf(sealedFiles);
    }

    private Path markerPath(Path dataPath) {
        return dataPath.resolveSibling(dataPath.getFileName() + markerSuffix);
    }
}
