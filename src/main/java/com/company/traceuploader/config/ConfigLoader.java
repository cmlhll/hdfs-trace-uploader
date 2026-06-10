package com.company.traceuploader.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Minimal YAML loader for the project-provided agent configuration shape. */
public final class ConfigLoader {
    public AgentConfig load(Path configPath) throws IOException {
        AgentConfig.LocalSpool localSpool = new AgentConfig.LocalSpool();
        AgentConfig.Scanner scanner = new AgentConfig.Scanner();

        String section = "";
        String listKey = "";
        List<String> lines = Files.readAllLines(configPath);
        for (String rawLine : lines) {
            String lineWithoutComment = stripComment(rawLine);
            if (lineWithoutComment.isBlank()) {
                continue;
            }
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
            if (colon < 0) {
                continue;
            }
            listKey = "";
            String key = trimmed.substring(0, colon).trim();
            String value = unquote(trimmed.substring(colon + 1).trim());
            applyScalar(localSpool, scanner, section, key, value);
        }

        localSpool.validate();
        scanner.validate();
        return new AgentConfig(localSpool, scanner);
    }

    private static void applyScalar(AgentConfig.LocalSpool localSpool, AgentConfig.Scanner scanner,
                                    String section, String key, String value) {
        if ("localSpool".equals(section)) {
            if ("sealedDir".equals(key)) {
                localSpool.setSealedDir(Path.of(value));
            } else if ("writingDir".equals(key)) {
                localSpool.setWritingDir(Path.of(value));
            }
            return;
        }
        if (!"scanner".equals(section)) {
            return;
        }
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

    private static void applyListValue(AgentConfig.Scanner scanner, String section, String listKey, String value) {
        if ("scanner".equals(section) && "ignoredSuffixes".equals(listKey)) {
            scanner.ignoredSuffixes().add(value);
        }
    }

    private static String stripComment(String line) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (ch == '#' && !inSingleQuote && !inDoubleQuote) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static int leadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
