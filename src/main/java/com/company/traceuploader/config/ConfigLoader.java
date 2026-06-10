package com.company.traceuploader.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Minimal YAML loader for the project-provided flat section configuration shape. */
public final class ConfigLoader {
    public AgentConfig load(Path configPath) throws IOException {
        AgentConfig.LocalSpool localSpool = new AgentConfig.LocalSpool();
        AgentConfig.Scanner scanner = new AgentConfig.Scanner();
        AgentConfig.Hdfs hdfs = new AgentConfig.Hdfs();
        AgentConfig.Upload upload = new AgentConfig.Upload();

        String section = "";
        String listKey = "";
        List<String> lines = Files.readAllLines(configPath);
        for (String rawLine : lines) {
            String lineWithoutComment = stripComment(rawLine);
            if (lineWithoutComment.isBlank()) continue;
            int indent = leadingSpaces(lineWithoutComment);
            String trimmed = lineWithoutComment.trim();

            if (indent == 0 && trimmed.endsWith(":")) {
                section = trimmed.substring(0, trimmed.length() - 1);
                listKey = "";
                continue;
            }
            if (indent == 2 && trimmed.endsWith(":")) {
                listKey = trimmed.substring(0, trimmed.length() - 1);
                continue;
            }
            if (indent >= 4 && trimmed.startsWith("- ")) {
                applyListValue(scanner, section, listKey, unquote(trimmed.substring(2).trim()));
                continue;
            }

            int colon = trimmed.indexOf(':');
            if (colon < 0) continue;
            listKey = "";
            String key = trimmed.substring(0, colon).trim();
            String value = unquote(trimmed.substring(colon + 1).trim());
            applyScalar(localSpool, scanner, hdfs, upload, section, key, value);
        }

        AgentConfig config = new AgentConfig(localSpool, scanner, hdfs, upload);
        config.validate();
        return config;
    }

    private static void applyScalar(AgentConfig.LocalSpool localSpool, AgentConfig.Scanner scanner,
                                    AgentConfig.Hdfs hdfs, AgentConfig.Upload upload,
                                    String section, String key, String value) {
        switch (section) {
            case "localSpool" -> {
                switch (key) {
                    case "sealedDir" -> localSpool.setSealedDir(Path.of(value));
                    case "writingDir" -> localSpool.setWritingDir(Path.of(value));
                    case "stateDir" -> localSpool.setStateDir(Path.of(value));
                    case "quarantineDir" -> localSpool.setQuarantineDir(Path.of(value));
                    default -> { }
                }
            }
            case "scanner" -> {
                switch (key) {
                    case "markerSuffix" -> scanner.setMarkerSuffix(value);
                    case "minStableMillis" -> scanner.setMinStableMillis(Long.parseLong(value));
                    case "minStableAgeSeconds" -> scanner.setMinStableMillis(Math.multiplyExact(Long.parseLong(value), 1000L));
                    case "maxFilesPerScan" -> scanner.setMaxFilesPerScan(Integer.parseInt(value));
                    case "scanIntervalMillis" -> scanner.setScanIntervalMillis(Long.parseLong(value));
                    case "scanIntervalSeconds" -> scanner.setScanIntervalMillis(Math.multiplyExact(Long.parseLong(value), 1000L));
                    default -> { }
                }
            }
            case "hdfs" -> {
                switch (key) {
                    case "implementation" -> hdfs.setImplementation(value);
                    case "localRootForTesting" -> hdfs.setLocalRootForTesting(Path.of(value));
                    case "rawBasePath" -> hdfs.setRawBasePath(value);
                    case "stagingBasePath" -> hdfs.setStagingBasePath(value);
                    case "finalPathTemplate" -> hdfs.setFinalPathTemplate(value);
                    case "stagingPathTemplate" -> hdfs.setStagingPathTemplate(value);
                    case "bucketCount" -> hdfs.setBucketCount(Integer.parseInt(value));
                    default -> { }
                }
            }
            case "upload" -> {
                switch (key) {
                    case "verifySizeAfterUpload" -> upload.setVerifySizeAfterUpload(Boolean.parseBoolean(value));
                    case "verifyChecksumAfterUpload" -> upload.setVerifyChecksumAfterUpload(Boolean.parseBoolean(value));
                    case "deleteStagingOnRetry" -> upload.setDeleteStagingOnRetry(Boolean.parseBoolean(value));
                    default -> { }
                }
            }
            default -> { }
        }
    }

    private static void applyListValue(AgentConfig.Scanner scanner, String section, String listKey, String value) {
        if ("scanner".equals(section) && "ignoredSuffixes".equals(listKey) && !scanner.ignoredSuffixes().contains(value)) {
            scanner.ignoredSuffixes().add(value);
        }
    }

    private static String stripComment(String line) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\'' && !inDoubleQuote) inSingleQuote = !inSingleQuote;
            else if (ch == '"' && !inSingleQuote) inDoubleQuote = !inDoubleQuote;
            else if (ch == '#' && !inSingleQuote && !inDoubleQuote) return line.substring(0, i);
        }
        return line;
    }

    private static int leadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') count++;
        return count;
    }

    private static String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
