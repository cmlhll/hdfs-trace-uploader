package com.company.traceuploader.scanner;

import com.company.traceuploader.config.LocalSpoolConfig;
import com.company.traceuploader.config.ScannerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSealedFileScannerTest {
    private static final Instant NOW = Instant.parse("2026-06-11T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void fileWithoutDoneMarkerIsNotScanned() throws Exception {
        Path sealedDir = Files.createDirectory(tempDir.resolve("sealed"));
        createStableFile(sealedDir.resolve("trace-1.log.zst"), "payload");

        List<DiscoveredFile> discoveredFiles = scanner(sealedDir).scan();

        assertTrue(discoveredFiles.isEmpty());
    }

    @Test
    void fileWithDoneMarkerIsScanned() throws Exception {
        Path sealedDir = Files.createDirectory(tempDir.resolve("sealed"));
        Path dataFile = createStableFile(sealedDir.resolve("trace-1.log.zst"), "payload");
        Path doneFile = createStableFile(sealedDir.resolve("trace-1.log.zst.done"), "");

        List<DiscoveredFile> discoveredFiles = scanner(sealedDir).scan();

        assertEquals(1, discoveredFiles.size());
        DiscoveredFile discoveredFile = discoveredFiles.get(0);
        assertEquals(dataFile.toAbsolutePath().normalize(), discoveredFile.getLocalPath());
        assertEquals(doneFile.toAbsolutePath().normalize(), discoveredFile.getDonePath());
        assertEquals(7, discoveredFile.getSizeBytes());
    }

    @Test
    void ignoredSuffixesAreNeverScanned() throws Exception {
        Path sealedDir = Files.createDirectory(tempDir.resolve("sealed"));
        createStableFile(sealedDir.resolve("valid.log.zst"), "payload");
        createStableFile(sealedDir.resolve("valid.log.zst.done"), "");
        createStableFile(sealedDir.resolve("ignored.tmp"), "payload");
        createStableFile(sealedDir.resolve("ignored.tmp.done"), "");
        createStableFile(sealedDir.resolve("ignored.part"), "payload");
        createStableFile(sealedDir.resolve("ignored.part.done"), "");
        createStableFile(sealedDir.resolve("ignored.uploading"), "payload");
        createStableFile(sealedDir.resolve("ignored.uploading.done"), "");

        ScannerConfig config = scannerConfig();
        config.setDataFileSuffixes(List.of(".log.zst", ".tmp", ".part", ".uploading"));

        List<DiscoveredFile> discoveredFiles = scanner(sealedDir, config).scan();

        assertEquals(1, discoveredFiles.size());
        assertEquals("valid.log.zst", discoveredFiles.get(0).getLocalPath().getFileName().toString());
    }

    @Test
    void minStableAgeSecondsMustPassBeforeFileIsScanned() throws Exception {
        Path sealedDir = Files.createDirectory(tempDir.resolve("sealed"));
        Path dataFile = createFile(sealedDir.resolve("trace-1.log.zst"), "payload");
        Path doneFile = createFile(sealedDir.resolve("trace-1.log.zst.done"), "");
        setLastModified(dataFile, NOW.minusSeconds(5));
        setLastModified(doneFile, NOW.minusSeconds(5));

        ScannerConfig config = scannerConfig();
        config.setMinStableAgeSeconds(30);

        assertTrue(scanner(sealedDir, config).scan().isEmpty());

        setLastModified(dataFile, NOW.minusSeconds(31));
        setLastModified(doneFile, NOW.minusSeconds(31));

        assertEquals(1, scanner(sealedDir, config).scan().size());
    }

    @Test
    void maxFilesPerScanLimitsResults() throws Exception {
        Path sealedDir = Files.createDirectory(tempDir.resolve("sealed"));
        createStableFileWithDone(sealedDir.resolve("trace-1.log.zst"), "1");
        createStableFileWithDone(sealedDir.resolve("trace-2.log.zst"), "2");
        createStableFileWithDone(sealedDir.resolve("trace-3.log.zst"), "3");

        ScannerConfig config = scannerConfig();
        config.setMaxFilesPerScan(2);

        List<DiscoveredFile> discoveredFiles = scanner(sealedDir, config).scan();

        assertEquals(2, discoveredFiles.size());
    }

    @Test
    void writingDirectoryIsIgnored() throws Exception {
        Path sealedDir = Files.createDirectory(tempDir.resolve("sealed"));
        Path writingDir = Files.createDirectory(sealedDir.resolve("writing"));
        createStableFileWithDone(writingDir.resolve("active.log.zst"), "active");
        createStableFileWithDone(sealedDir.resolve("sealed.log.zst"), "sealed");

        List<DiscoveredFile> discoveredFiles = scanner(sealedDir, writingDir, scannerConfig()).scan();

        assertEquals(1, discoveredFiles.size());
        assertEquals("sealed.log.zst", discoveredFiles.get(0).getLocalPath().getFileName().toString());
    }

    private DefaultSealedFileScanner scanner(Path sealedDir) {
        return scanner(sealedDir, scannerConfig());
    }

    private DefaultSealedFileScanner scanner(Path sealedDir, ScannerConfig scannerConfig) {
        return scanner(sealedDir, sealedDir.resolve("writing"), scannerConfig);
    }

    private DefaultSealedFileScanner scanner(Path sealedDir, Path writingDir, ScannerConfig scannerConfig) {
        LocalSpoolConfig localSpoolConfig = new LocalSpoolConfig();
        localSpoolConfig.setSealedDir(sealedDir.toString());
        localSpoolConfig.setWritingDir(writingDir.toString());
        return new DefaultSealedFileScanner(localSpoolConfig, scannerConfig, FIXED_CLOCK);
    }

    private ScannerConfig scannerConfig() {
        ScannerConfig config = new ScannerConfig();
        config.setDataFileSuffixes(List.of(".log.zst", ".jsonl.zst"));
        config.setIgnoredSuffixes(List.of(".tmp", ".part", ".uploading"));
        config.setMinStableAgeSeconds(30);
        config.setMaxFilesPerScan(1000);
        return config;
    }

    private void createStableFileWithDone(Path dataPath, String content) throws Exception {
        createStableFile(dataPath, content);
        createStableFile(dataPath.resolveSibling(dataPath.getFileName().toString() + ".done"), "");
    }

    private Path createStableFile(Path path, String content) throws Exception {
        Path file = createFile(path, content);
        setLastModified(file, NOW.minusSeconds(31));
        return file;
    }

    private Path createFile(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content);
    }

    private void setLastModified(Path path, Instant instant) throws Exception {
        Files.setLastModifiedTime(path, FileTime.from(instant));
    }
}
