package com.company.traceuploader.config;

import java.util.ArrayList;
import java.util.List;

public class AgentConfig {
    public Agent agent = new Agent();
    public LocalSpool localSpool = new LocalSpool();
    public Scanner scanner = new Scanner();
    public Hdfs hdfs = new Hdfs();
    public Manifest manifest = new Manifest();
    public Upload upload = new Upload();

    public static class Agent { public String app, env, region, cluster, host, agentId, agentVersion = "0.1.0"; }
    public static class LocalSpool { public String baseDir, writingDir, sealedDir, committedDir, failedDir, quarantineDir, stateDir, tmpDir; }
    public static class Scanner {
        public String markerSuffix = ".done";
        public List<String> dataFileSuffixes = new ArrayList<>(List.of(".jsonl", ".jsonl.zst", ".log", ".log.zst"));
        public List<String> ignoredSuffixes = new ArrayList<>(List.of(".tmp", ".part", ".uploading"));
        public long minStableAgeSeconds = 30;
        public int maxFilesPerScan = 1000;
        public long scanIntervalSeconds = 10;
    }
    public static class Hdfs {
        public String implementation = "localfs";
        public String localRootForTesting = "/tmp/fake_hdfs";
        public String rawBasePath = "/warehouse/raw_trace";
        public String stagingBasePath = "/warehouse/raw_trace/_staging";
        public String manifestBasePath = "/warehouse/raw_trace_manifest";
        public String finalPathTemplate = "{rawBasePath}/app={app}/dt={dt}/hour={hour}/region={region}/bucket={bucket}/{fileName}";
        public String stagingPathTemplate = "{stagingBasePath}/app={app}/dt={dt}/hour={hour}/region={region}/bucket={bucket}/{fileId}.attempt_{attempt}.tmp";
        public int bucketCount = 128;
    }
    public static class Manifest { public String type = "local_jsonl"; public String localPath; public boolean writeIfAbsent = true; }
    public static class Upload { public boolean verifyChecksumAfterUpload = false; public boolean verifySizeAfterUpload = true; public boolean deleteStagingOnRetry = true; }
}
