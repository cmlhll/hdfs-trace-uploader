package com.company.traceuploader.metadata;

import java.util.Objects;

public final class ParsedTraceFileName {
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

    public ParsedTraceFileName(String fileName, String app, String env, String region, String cluster, String host,
                               String pid, String bootId, String startTs, String endTs, String seq) {
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
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParsedTraceFileName)) return false;
        ParsedTraceFileName that = (ParsedTraceFileName) o;
        return Objects.equals(fileName, that.fileName)
                && Objects.equals(app, that.app)
                && Objects.equals(env, that.env)
                && Objects.equals(region, that.region)
                && Objects.equals(cluster, that.cluster)
                && Objects.equals(host, that.host)
                && Objects.equals(pid, that.pid)
                && Objects.equals(bootId, that.bootId)
                && Objects.equals(startTs, that.startTs)
                && Objects.equals(endTs, that.endTs)
                && Objects.equals(seq, that.seq);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, app, env, region, cluster, host, pid, bootId, startTs, endTs, seq);
    }

    @Override
    public String toString() {
        return "ParsedTraceFileName[" +
                "fileName=" + fileName +
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
                ']';
    }
}
