package com.company.traceuploader.model;

import java.nio.file.Path;
import java.util.Objects;

public final class TraceFileMetadata {
    private final Path localPath;
    private final String fileName;
    private final String app;
    private final String env;
    private final String region;
    private final String cluster;
    private final String host;
    private final String pid;
    private final String bootId;
    private final String startTs;
    private final String endTs;
    private final String seq;
    private final long sizeBytes;
    private final String checksum;
    private final long recordCount;
    private final String fileId;
    private final int bucket;
    private final String hdfsFinalPath;
    private final String hdfsStagingPath;
    private final int attempt;

    public TraceFileMetadata(
            Path localPath,
            String fileName,
            String app,
            String env,
            String region,
            String cluster,
            String host,
            String pid,
            String bootId,
            String startTs,
            String endTs,
            String seq,
            long sizeBytes,
            String checksum,
            long recordCount,
            String fileId,
            int bucket,
            String hdfsFinalPath,
            String hdfsStagingPath,
            int attempt) {
        this.localPath = localPath;
        this.fileName = fileName;
        this.app = app;
        this.env = env;
        this.region = region;
        this.cluster = cluster;
        this.host = host;
        this.pid = pid;
        this.bootId = bootId;
        this.startTs = startTs;
        this.endTs = endTs;
        this.seq = seq;
        this.sizeBytes = sizeBytes;
        this.checksum = checksum;
        this.recordCount = recordCount;
        this.fileId = fileId;
        this.bucket = bucket;
        this.hdfsFinalPath = hdfsFinalPath;
        this.hdfsStagingPath = hdfsStagingPath;
        this.attempt = attempt;
    }

    public Path localPath() { return localPath; }
    public String fileName() { return fileName; }
    public String app() { return app; }
    public String env() { return env; }
    public String region() { return region; }
    public String cluster() { return cluster; }
    public String host() { return host; }
    public String pid() { return pid; }
    public String bootId() { return bootId; }
    public String startTs() { return startTs; }
    public String endTs() { return endTs; }
    public String seq() { return seq; }
    public long sizeBytes() { return sizeBytes; }
    public String checksum() { return checksum; }
    public long recordCount() { return recordCount; }
    public String fileId() { return fileId; }
    public int bucket() { return bucket; }
    public String hdfsFinalPath() { return hdfsFinalPath; }
    public String hdfsStagingPath() { return hdfsStagingPath; }
    public int attempt() { return attempt; }

    public Path getLocalPath() { return localPath; }
    public String getFileName() { return fileName; }
    public String getApp() { return app; }
    public String getEnv() { return env; }
    public String getRegion() { return region; }
    public String getCluster() { return cluster; }
    public String getHost() { return host; }
    public String getPid() { return pid; }
    public String getBootId() { return bootId; }
    public String getStartTs() { return startTs; }
    public String getEndTs() { return endTs; }
    public String getSeq() { return seq; }
    public long getSizeBytes() { return sizeBytes; }
    public String getChecksum() { return checksum; }
    public long getRecordCount() { return recordCount; }
    public String getFileId() { return fileId; }
    public int getBucket() { return bucket; }
    public String getHdfsFinalPath() { return hdfsFinalPath; }
    public String getHdfsStagingPath() { return hdfsStagingPath; }
    public int getAttempt() { return attempt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TraceFileMetadata)) return false;
        TraceFileMetadata that = (TraceFileMetadata) o;
        return sizeBytes == that.sizeBytes
                && recordCount == that.recordCount
                && bucket == that.bucket
                && attempt == that.attempt
                && Objects.equals(localPath, that.localPath)
                && Objects.equals(fileName, that.fileName)
                && Objects.equals(app, that.app)
                && Objects.equals(env, that.env)
                && Objects.equals(region, that.region)
                && Objects.equals(cluster, that.cluster)
                && Objects.equals(host, that.host)
                && Objects.equals(pid, that.pid)
                && Objects.equals(bootId, that.bootId)
                && Objects.equals(startTs, that.startTs)
                && Objects.equals(endTs, that.endTs)
                && Objects.equals(seq, that.seq)
                && Objects.equals(checksum, that.checksum)
                && Objects.equals(fileId, that.fileId)
                && Objects.equals(hdfsFinalPath, that.hdfsFinalPath)
                && Objects.equals(hdfsStagingPath, that.hdfsStagingPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localPath, fileName, app, env, region, cluster, host, pid, bootId, startTs, endTs, seq,
                sizeBytes, checksum, recordCount, fileId, bucket, hdfsFinalPath, hdfsStagingPath, attempt);
    }

    @Override
    public String toString() {
        return "TraceFileMetadata[" +
                "localPath=" + localPath +
                ", fileName=" + fileName +
                ", app=" + app +
                ", env=" + env +
                ", region=" + region +
                ", cluster=" + cluster +
                ", host=" + host +
                ", pid=" + pid +
                ", bootId=" + bootId +
                ", startTs=" + startTs +
                ", endTs=" + endTs +
                ", seq=" + seq +
                ", sizeBytes=" + sizeBytes +
                ", checksum=" + checksum +
                ", recordCount=" + recordCount +
                ", fileId=" + fileId +
                ", bucket=" + bucket +
                ", hdfsFinalPath=" + hdfsFinalPath +
                ", hdfsStagingPath=" + hdfsStagingPath +
                ", attempt=" + attempt +
                ']';
    }
}
