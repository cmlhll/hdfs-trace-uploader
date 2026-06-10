package com.company.traceuploader.state;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JsonlWalUploadStateStore implements UploadStateStore {
    private final Path walPath;
    private final Map<String, UploadFileRecord> records = new LinkedHashMap<>();
    private final BufferedWriter writer;

    public JsonlWalUploadStateStore(Path walPath) throws IOException {
        this.walPath = walPath;
        if (walPath.getParent() != null) Files.createDirectories(walPath.getParent());
        replay();
        this.writer = Files.newBufferedWriter(walPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
    }

    @Override
    public synchronized void upsert(UploadFileRecord record) throws IOException {
        UploadFileRecord existing = records.get(record.fileId());
        UploadFileRecord next = existing == null ? record : new UploadFileRecord(
                existing.fileId(), record.localPath(), record.hdfsFinalPath(), record.hdfsStagingPath(),
                record.sizeBytes(), record.checksum(), record.recordCount(), record.attempt(), existing.state(),
                existing.lastError(), existing.createdAtMillis(), System.currentTimeMillis());
        records.put(next.fileId(), next);
        append(next);
    }

    @Override
    public synchronized void updateState(String fileId, UploadState state, String lastError) throws IOException {
        UploadFileRecord existing = records.get(fileId);
        if (existing == null) throw new IllegalArgumentException("Unknown file_id: " + fileId);
        UploadFileRecord next = existing.withState(state, lastError, System.currentTimeMillis());
        records.put(fileId, next);
        append(next);
    }

    @Override
    public synchronized Optional<UploadFileRecord> getByFileId(String fileId) {
        return Optional.ofNullable(records.get(fileId));
    }

    @Override
    public synchronized List<UploadFileRecord> findPending(int limit) {
        return records.values().stream()
                .filter(record -> switch (record.state()) {
                    case LOCAL_GC_DONE, PERMANENT_FAILED, QUARANTINED, MANIFEST_COMMITTED, LOCAL_GC_READY -> false;
                    default -> true;
                })
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized void close() throws IOException {
        writer.close();
    }

    private void replay() throws IOException {
        if (!Files.exists(walPath)) return;
        for (String line : Files.readAllLines(walPath, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            UploadFileRecord record = fromJson(line);
            records.put(record.fileId(), record);
        }
    }

    private void append(UploadFileRecord record) throws IOException {
        writer.write(toJson(record));
        writer.newLine();
        writer.flush();
    }

    private static String toJson(UploadFileRecord r) {
        return "{" +
                field("file_id", r.fileId()) + "," +
                field("local_path", r.localPath()) + "," +
                field("hdfs_final_path", r.hdfsFinalPath()) + "," +
                field("hdfs_staging_path", r.hdfsStagingPath()) + "," +
                numberField("size_bytes", r.sizeBytes()) + "," +
                field("checksum", r.checksum()) + "," +
                numberField("record_count", r.recordCount()) + "," +
                numberField("attempt", r.attempt()) + "," +
                field("state", r.state().name()) + "," +
                field("last_error", r.lastError()) + "," +
                numberField("created_at_millis", r.createdAtMillis()) + "," +
                numberField("updated_at_millis", r.updatedAtMillis()) +
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

    private static UploadFileRecord fromJson(String json) {
        Map<String, String> map = parseFlatJson(json);
        return new UploadFileRecord(
                map.get("file_id"), map.get("local_path"), map.get("hdfs_final_path"), map.get("hdfs_staging_path"),
                Long.parseLong(map.get("size_bytes")), map.get("checksum"), Long.parseLong(map.get("record_count")),
                Integer.parseInt(map.get("attempt")), UploadState.valueOf(map.get("state")), map.get("last_error"),
                Long.parseLong(map.get("created_at_millis")), Long.parseLong(map.get("updated_at_millis")));
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
