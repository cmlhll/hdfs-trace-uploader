package com.company.traceuploader.scanner;

import com.company.traceuploader.config.LocalSpoolConfig;
import com.company.traceuploader.config.ScannerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class DefaultSealedFileScanner implements SealedFileScanner {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultSealedFileScanner.class);

    private final Path sealedDir;
    private final Path writingDir;
    private final ScannerConfig scannerConfig;
    private final Clock clock;

    public DefaultSealedFileScanner(LocalSpoolConfig localSpoolConfig, ScannerConfig scannerConfig) {
        this(localSpoolConfig, scannerConfig, Clock.systemUTC());
    }

    public DefaultSealedFileScanner(LocalSpoolConfig localSpoolConfig, ScannerConfig scannerConfig, Clock clock) {
        Objects.requireNonNull(localSpoolConfig, "localSpoolConfig");
        this.scannerConfig = Objects.requireNonNull(scannerConfig, "scannerConfig");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sealedDir = Paths.get(requireNonBlank(localSpoolConfig.getSealedDir(), "localSpool.sealedDir"))
                .toAbsolutePath()
                .normalize();
        this.writingDir = normalizeOptionalPath(localSpoolConfig.getWritingDir());
    }

    @Override
    public List<DiscoveredFile> scan() throws IOException {
        if (!Files.isDirectory(sealedDir)) {
            LOG.warn("Sealed directory does not exist or is not a directory: {}", sealedDir);
            return List.of();
        }

        List<DiscoveredFile> discoveredFiles = new ArrayList<>();
        Files.walkFileTree(sealedDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (shouldSkipDirectory(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    maybeDiscover(file, attrs).ifPresent(discoveredFiles::add);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        discoveredFiles.sort(Comparator
                .comparingLong(DiscoveredFile::getLastModifiedTimeMs)
                .thenComparing(discoveredFile -> discoveredFile.getLocalPath().toString()));

        int maxFilesPerScan = scannerConfig.getMaxFilesPerScan();
        if (discoveredFiles.size() > maxFilesPerScan) {
            return List.copyOf(discoveredFiles.subList(0, maxFilesPerScan));
        }
        return List.copyOf(discoveredFiles);
    }

    private java.util.Optional<DiscoveredFile> maybeDiscover(Path file, BasicFileAttributes attrs) {
        Path normalizedFile = file.toAbsolutePath().normalize();
        String fileName = normalizedFile.getFileName().toString();

        if (fileName.endsWith(scannerConfig.getMarkerSuffix())) {
            return java.util.Optional.empty();
        }
        if (hasIgnoredSuffix(fileName) || !hasAllowedDataSuffix(fileName)) {
            return java.util.Optional.empty();
        }
        if (!isStable(attrs.lastModifiedTime().toMillis())) {
            return java.util.Optional.empty();
        }

        Path donePath = normalizedFile.resolveSibling(fileName + scannerConfig.getMarkerSuffix());
        if (!Files.isRegularFile(donePath)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new DiscoveredFile(
                normalizedFile,
                donePath,
                attrs.size(),
                attrs.lastModifiedTime().toMillis()));
    }

    private boolean shouldSkipDirectory(Path dir) {
        Path normalizedDir = dir.toAbsolutePath().normalize();
        if (normalizedDir.equals(sealedDir)) {
            return false;
        }
        if (writingDir != null && normalizedDir.startsWith(writingDir)) {
            return true;
        }
        Path fileName = normalizedDir.getFileName();
        return fileName != null && "writing".equals(fileName.toString());
    }

    private boolean hasIgnoredSuffix(String fileName) {
        return scannerConfig.getIgnoredSuffixes().stream().anyMatch(fileName::endsWith);
    }

    private boolean hasAllowedDataSuffix(String fileName) {
        return scannerConfig.getDataFileSuffixes().stream().anyMatch(fileName::endsWith);
    }

    private boolean isStable(long lastModifiedTimeMs) {
        long minStableAgeMs = scannerConfig.getMinStableAgeSeconds() * 1000L;
        return clock.millis() - lastModifiedTimeMs >= minStableAgeMs;
    }

    private static Path normalizeOptionalPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return Paths.get(path).toAbsolutePath().normalize();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
