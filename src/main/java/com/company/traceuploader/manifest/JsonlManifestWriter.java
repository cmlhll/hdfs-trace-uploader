package com.company.traceuploader.manifest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local JSONL manifest with file_id based write-if-absent semantics. */
public final class JsonlManifestWriter implements ManifestWriter {
    private final Path manifestPath;
    private final Map<String, String> recordsByFileId = new LinkedHashMap<>();

    public JsonlManifestWriter(Path manifestPath) throws IOException {
        this.manifestPath = manifestPath;
        if (manifestPath.getParent() != null) Files.createDirectories(manifestPath.getParent());
        replay();
    }

    @Override
    public synchronized void writeIfAbsent(ManifestRecord record) throws IOException {
        String json = toJson(record);
        String existing = recordsByFileId.get(record.fileId());
        if (existing != null) {
            if (existing.equals(json)) {
                return;
            }
            throw new IOException("Manifest record conflict for file_id=" + record.fileId());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(manifestPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC)) {
            writer.write(json);
            writer.newLine();
        }
        recordsByFileId.put(record.fileId(), json);
    }

    private void replay() throws IOException {
        if (!Files.exists(manifestPath)) return;
        for (String line : Files.readAllLines(manifestPath, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            Map<String, String> values = parseFlatJson(line);
            String fileId = values.get("file_id");
            String existing = recordsByFileId.putIfAbsent(fileId, line);
            if (existing != null && !existing.equals(line)) {
                throw new IOException("Manifest file already contains conflicting rows for file_id=" + fileId);
            }
        }
    }

    private static String toJson(ManifestRecord record) {
        return "{" +
                field("file_id", record.fileId()) + "," +
                field("hdfs_path", record.hdfsPath()) + "," +
                numberField("size_bytes", record.sizeBytes()) + "," +
                field("checksum", record.checksum()) + "," +
                numberField("record_count", record.recordCount()) + "," +
                field("state", record.state()) + "," +
                field("commit_time", record.commitTime()) +
                "}";
    }

    private static String field(String key, String value) {
        if (value == null) return quote(key) + ":null";
        return quote(key) + ":" + quote(value);
    }

    private static String numberField(String key, long value) {
        return quote(key) + ":" + value;
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '"' || ch == '\\') out.append('\\').append(ch);
            else if (ch == '\n') out.append("\\n");
            else out.append(ch);
        }
        return out.append('"').toString();
    }

    private static Map<String, String> parseFlatJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        int i = 1;
        while (i < json.length() - 1) {
            while (i < json.length() && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) i++;
            String key = readString(json, i);
            i += encodedStringLength(json, i);
            while (json.charAt(i) != ':') i++;
            i++;
            String value;
            if (json.startsWith("null", i)) {
                value = null;
                i += 4;
            } else if (json.charAt(i) == '"') {
                value = readString(json, i);
                i += encodedStringLength(json, i);
            } else {
                int start = i;
                while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
                value = json.substring(start, i);
            }
            map.put(key, value);
        }
        return map;
    }

    private static String readString(String json, int startQuote) {
        StringBuilder out = new StringBuilder();
        for (int i = startQuote + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '"') return out.toString();
            if (ch == '\\') {
                char next = json.charAt(++i);
                out.append(next == 'n' ? '\n' : next);
            } else {
                out.append(ch);
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string");
    }

    private static int encodedStringLength(String json, int startQuote) {
        for (int i = startQuote + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '\\') i++;
            else if (ch == '"') return i - startQuote + 1;
        }
        throw new IllegalArgumentException("Unterminated JSON string");
    }
}
