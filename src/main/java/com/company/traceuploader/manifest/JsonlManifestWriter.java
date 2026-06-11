package com.company.traceuploader.manifest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class JsonlManifestWriter implements ManifestWriter {
    private final Path manifestPath;
    private final ObjectMapper mapper;

    public JsonlManifestWriter(Path manifestPath) {
        this(manifestPath, new ObjectMapper());
    }

    JsonlManifestWriter(Path manifestPath, ObjectMapper mapper) {
        this.manifestPath = Objects.requireNonNull(manifestPath, "manifestPath");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public synchronized WriteResult writeIfAbsent(ManifestRecord record) throws IOException {
        Objects.requireNonNull(record, "record");
        if (manifestPath.getParent() != null) {
            Files.createDirectories(manifestPath.getParent());
        }

        String recordJson = mapper.writeValueAsString(record);
        JsonNode recordNode = readJson(recordJson, "new manifest record");
        if (Files.exists(manifestPath)) {
            WriteResult existing = findExisting(record.fileId(), recordNode);
            if (existing != null) {
                return existing;
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(manifestPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC)) {
            writer.write(recordJson);
            writer.newLine();
            writer.flush();
        }
        return WriteResult.WRITTEN;
    }

    @Override
    public void close() throws IOException {
        // Writer handles are opened per append so every write can re-read the durable file first.
    }

    private WriteResult findExisting(String fileId, JsonNode recordNode) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                JsonNode existingNode = readJson(line, "manifest line " + lineNumber);
                JsonNode existingFileId = existingNode.get("file_id");
                if (existingFileId != null && fileId.equals(existingFileId.asText())) {
                    return existingNode.equals(recordNode)
                            ? WriteResult.ALREADY_EXISTS_MATCHING
                            : WriteResult.ALREADY_EXISTS_CONFLICT;
                }
            }
        }
        return null;
    }

    private JsonNode readJson(String json, String source) throws IOException {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IOException("Invalid JSON in " + source, e);
        }
    }
}
