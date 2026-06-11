package com.company.traceuploader.manifest;

import com.company.traceuploader.config.AgentConfig;
import com.company.traceuploader.model.TraceFileMetadata;
import com.company.traceuploader.state.UploadState;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonPropertyOrder({
        "file_id",
        "app",
        "env",
        "region",
        "cluster",
        "host",
        "pid",
        "boot_id",
        "dt",
        "hour",
        "bucket",
        "start_time",
        "end_time",
        "hdfs_path",
        "size_bytes",
        "checksum",
        "record_count",
        "raw_format",
        "compression",
        "upload_attempt",
        "commit_time",
        "agent_version",
        "state"
})
public final class ManifestRecord {
    private static final DateTimeFormatter TRACE_TS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final String fileId;
    private final String app;
    private final String env;
    private final String region;
    private final String cluster;
    private final String host;
    private final String pid;
    private final String bootId;
    private final String dt;
    private final int hour;
    private final int bucket;
    private final String startTime;
    private final String endTime;
    private final String hdfsPath;
    private final long sizeBytes;
    private final String checksum;
    private final long recordCount;
    private final String rawFormat;
    private final String compression;
    private final int uploadAttempt;
    private final String commitTime;
    private final String agentVersion;
    private final String state;

    @JsonCreator
    public ManifestRecord(
            @JsonProperty("file_id") String fileId,
            @JsonProperty("app") String app,
            @JsonProperty("env") String env,
            @JsonProperty("region") String region,
            @JsonProperty("cluster") String cluster,
            @JsonProperty("host") String host,
            @JsonProperty("pid") String pid,
            @JsonProperty("boot_id") String bootId,
            @JsonProperty("dt") String dt,
            @JsonProperty("hour") int hour,
            @JsonProperty("bucket") int bucket,
            @JsonProperty("start_time") String startTime,
            @JsonProperty("end_time") String endTime,
            @JsonProperty("hdfs_path") String hdfsPath,
            @JsonProperty("size_bytes") long sizeBytes,
            @JsonProperty("checksum") String checksum,
            @JsonProperty("record_count") long recordCount,
            @JsonProperty("raw_format") String rawFormat,
            @JsonProperty("compression") String compression,
            @JsonProperty("upload_attempt") int uploadAttempt,
            @JsonProperty("commit_time") String commitTime,
            @JsonProperty("agent_version") String agentVersion,
            @JsonProperty("state") String state) {
        this.fileId = fileId;
        this.app = app;
        this.env = env;
        this.region = region;
        this.cluster = cluster;
        this.host = host;
        this.pid = pid;
        this.bootId = bootId;
        this.dt = dt;
        this.hour = hour;
        this.bucket = bucket;
        this.startTime = startTime;
        this.endTime = endTime;
        this.hdfsPath = hdfsPath;
        this.sizeBytes = sizeBytes;
        this.checksum = checksum;
        this.recordCount = recordCount;
        this.rawFormat = rawFormat;
        this.compression = compression;
        this.uploadAttempt = uploadAttempt;
        this.commitTime = commitTime;
        this.agentVersion = agentVersion;
        this.state = state;
    }

    public static ManifestRecord fromMetadata(TraceFileMetadata metadata, AgentConfig config, Instant commitTime) {
        LocalDateTime start = LocalDateTime.parse(metadata.startTs(), TRACE_TS);
        return new ManifestRecord(
                metadata.fileId(),
                metadata.app(),
                metadata.env(),
                metadata.region(),
                metadata.cluster(),
                metadata.host(),
                metadata.pid(),
                metadata.bootId(),
                start.toLocalDate().toString(),
                start.getHour(),
                metadata.bucket(),
                toUtcInstantText(metadata.startTs()),
                toUtcInstantText(metadata.endTs()),
                metadata.hdfsFinalPath(),
                metadata.sizeBytes(),
                metadata.checksum(),
                metadata.recordCount(),
                rawFormat(metadata.fileName()),
                compression(metadata.fileName()),
                metadata.attempt(),
                commitTime.toString(),
                config.agentVersion(),
                UploadState.MANIFEST_COMMITTED.name());
    }

    private static String toUtcInstantText(String traceTimestamp) {
        return LocalDateTime.parse(traceTimestamp, TRACE_TS).toInstant(ZoneOffset.UTC).toString();
    }

    private static String rawFormat(String fileName) {
        String withoutCompression = fileName.endsWith(".zst")
                ? fileName.substring(0, fileName.length() - ".zst".length())
                : fileName;
        if (withoutCompression.endsWith(".jsonl")) {
            return "jsonl";
        }
        if (withoutCompression.endsWith(".log")) {
            return "log";
        }
        return "unknown";
    }

    private static String compression(String fileName) {
        if (fileName.endsWith(".zst")) {
            return "zstd";
        }
        return "none";
    }

    public String fileId() { return fileId; }
    public String app() { return app; }
    public String env() { return env; }
    public String region() { return region; }
    public String cluster() { return cluster; }
    public String host() { return host; }
    public String pid() { return pid; }
    public String bootId() { return bootId; }
    public String dt() { return dt; }
    public int hour() { return hour; }
    public int bucket() { return bucket; }
    public String startTime() { return startTime; }
    public String endTime() { return endTime; }
    public String hdfsPath() { return hdfsPath; }
    public long sizeBytes() { return sizeBytes; }
    public String checksum() { return checksum; }
    public long recordCount() { return recordCount; }
    public String rawFormat() { return rawFormat; }
    public String compression() { return compression; }
    public int uploadAttempt() { return uploadAttempt; }
    public String commitTime() { return commitTime; }
    public String agentVersion() { return agentVersion; }
    public String state() { return state; }

    public String getFileId() { return fileId; }
    public String getApp() { return app; }
    public String getEnv() { return env; }
    public String getRegion() { return region; }
    public String getCluster() { return cluster; }
    public String getHost() { return host; }
    public String getPid() { return pid; }
    public String getBootId() { return bootId; }
    public String getDt() { return dt; }
    public int getHour() { return hour; }
    public int getBucket() { return bucket; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getHdfsPath() { return hdfsPath; }
    public long getSizeBytes() { return sizeBytes; }
    public String getChecksum() { return checksum; }
    public long getRecordCount() { return recordCount; }
    public String getRawFormat() { return rawFormat; }
    public String getCompression() { return compression; }
    public int getUploadAttempt() { return uploadAttempt; }
    public String getCommitTime() { return commitTime; }
    public String getAgentVersion() { return agentVersion; }
    public String getState() { return state; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ManifestRecord)) return false;
        ManifestRecord that = (ManifestRecord) o;
        return hour == that.hour
                && bucket == that.bucket
                && sizeBytes == that.sizeBytes
                && recordCount == that.recordCount
                && uploadAttempt == that.uploadAttempt
                && Objects.equals(fileId, that.fileId)
                && Objects.equals(app, that.app)
                && Objects.equals(env, that.env)
                && Objects.equals(region, that.region)
                && Objects.equals(cluster, that.cluster)
                && Objects.equals(host, that.host)
                && Objects.equals(pid, that.pid)
                && Objects.equals(bootId, that.bootId)
                && Objects.equals(dt, that.dt)
                && Objects.equals(startTime, that.startTime)
                && Objects.equals(endTime, that.endTime)
                && Objects.equals(hdfsPath, that.hdfsPath)
                && Objects.equals(checksum, that.checksum)
                && Objects.equals(rawFormat, that.rawFormat)
                && Objects.equals(compression, that.compression)
                && Objects.equals(commitTime, that.commitTime)
                && Objects.equals(agentVersion, that.agentVersion)
                && Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, app, env, region, cluster, host, pid, bootId, dt, hour, bucket, startTime,
                endTime, hdfsPath, sizeBytes, checksum, recordCount, rawFormat, compression, uploadAttempt,
                commitTime, agentVersion, state);
    }

    @Override
    public String toString() {
        return "ManifestRecord[" +
                "fileId=" + fileId +
                ", hdfsPath=" + hdfsPath +
                ", sizeBytes=" + sizeBytes +
                ", checksum=" + checksum +
                ", recordCount=" + recordCount +
                ", commitTime=" + commitTime +
                ", state=" + state +
                ']';
    }
}
