package com.company.traceuploader.metadata;

import com.company.traceuploader.config.AgentConfig;
import com.company.traceuploader.model.TraceFileMetadata;
import com.company.traceuploader.scanner.SealedFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MetadataService {
    private static final Pattern TRACE_NAME = Pattern.compile(
            "^trace-([^-]+)-([^-]+)-([^-]+)-([^-]+)-([^-]+)-([^-]+)-([^-]+)-(\\d{8}T\\d{6})-(\\d{8}T\\d{6})-seq(\\d+)\\..+$");
    private static final DateTimeFormatter INPUT_TS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AgentConfig config;
    private final ChecksumService checksumService;
    private final FileIdGenerator fileIdGenerator;

    public MetadataService(AgentConfig config, ChecksumService checksumService, FileIdGenerator fileIdGenerator) {
        this.config = config;
        this.checksumService = checksumService;
        this.fileIdGenerator = fileIdGenerator;
    }

    public TraceFileMetadata build(SealedFile sealedFile, int attempt) throws IOException {
        ParsedTraceFileName parsed = parse(sealedFile.dataPath().getFileName().toString());
        long sizeBytes = Files.size(sealedFile.dataPath());
        String checksum = checksumService.sha256(sealedFile.dataPath());
        long recordCount = countRecords(sealedFile);
        String fileId = fileIdGenerator.generate(parsed, sizeBytes, checksum);
        int bucket = fileIdGenerator.bucket(fileId, config.hdfs().bucketCount());
        String bucketString = String.format("%03d", bucket);
        LocalDateTime start = LocalDateTime.parse(parsed.startTs(), INPUT_TS);
        Map<String, String> values = Map.ofEntries(
                Map.entry("rawBasePath", config.hdfs().rawBasePath()),
                Map.entry("stagingBasePath", config.hdfs().stagingBasePath()),
                Map.entry("app", parsed.app()),
                Map.entry("env", parsed.env()),
                Map.entry("region", parsed.region()),
                Map.entry("cluster", parsed.cluster()),
                Map.entry("host", parsed.host()),
                Map.entry("pid", parsed.pid()),
                Map.entry("bootId", parsed.bootId()),
                Map.entry("startTs", parsed.startTs()),
                Map.entry("endTs", parsed.endTs()),
                Map.entry("seq", parsed.seq()),
                Map.entry("dt", DT.format(start.toLocalDate())),
                Map.entry("hour", String.format("%02d", start.getHour())),
                Map.entry("bucket", bucketString),
                Map.entry("fileName", parsed.fileName()),
                Map.entry("fileId", fileId),
                Map.entry("attempt", Integer.toString(attempt)));
        String finalPath = render(config.hdfs().finalPathTemplate(), values);
        String stagingPath = render(config.hdfs().stagingPathTemplate(), values);
        return new TraceFileMetadata(sealedFile.dataPath(), parsed.fileName(), parsed.app(), parsed.env(), parsed.region(),
                parsed.cluster(), parsed.host(), parsed.pid(), parsed.bootId(), parsed.startTs(), parsed.endTs(), parsed.seq(),
                sizeBytes, checksum, recordCount, fileId, bucket, finalPath, stagingPath, attempt);
    }

    private ParsedTraceFileName parse(String fileName) {
        Matcher matcher = TRACE_NAME.matcher(fileName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Trace file name does not match expected stable rollover format: " + fileName);
        }
        return new ParsedTraceFileName(fileName, matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4),
                matcher.group(5), matcher.group(6), matcher.group(7), matcher.group(8), matcher.group(9), matcher.group(10));
    }

    private long countRecords(SealedFile sealedFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(sealedFile.dataPath())) {
            long count = 0;
            while (reader.readLine() != null) count++;
            return count;
        }
    }

    private String render(String template, Map<String, String> values) {
        String rendered = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return rendered;
    }
}
