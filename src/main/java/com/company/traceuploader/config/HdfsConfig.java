package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class HdfsConfig {
    private String implementation = "localfs";
    private String localRootForTesting = "/tmp/fake_hdfs";
    private String hadoopConfDir = "/etc/hadoop/conf";
    private String fsDefaultFS = "";
    private boolean kerberosEnabled = false;
    private String nameService = "";
    private String rawBasePath = "/warehouse/raw_trace";
    private String stagingBasePath = "/warehouse/raw_trace/_staging";
    private String manifestBasePath = "/warehouse/raw_trace_manifest";
    private String finalPathTemplate = "{rawBasePath}/app={app}/dt={dt}/hour={hour}/region={region}/bucket={bucket}/{fileName}";
    private String stagingPathTemplate = "{stagingBasePath}/app={app}/dt={dt}/hour={hour}/region={region}/bucket={bucket}/{fileId}.attempt_{attempt}.tmp";
    private int bucketCount = 16;

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public String implementation() {
        return implementation;
    }

    public String getLocalRootForTesting() {
        return localRootForTesting;
    }

    public void setLocalRootForTesting(String localRootForTesting) {
        this.localRootForTesting = localRootForTesting;
    }

    public String localRootForTesting() {
        return localRootForTesting;
    }

    public Path localRootForTestingPath() {
        return Path.of(localRootForTesting);
    }

    public String getHadoopConfDir() {
        return hadoopConfDir;
    }

    public void setHadoopConfDir(String hadoopConfDir) {
        this.hadoopConfDir = hadoopConfDir;
    }

    public String hadoopConfDir() {
        return hadoopConfDir;
    }

    public String getFsDefaultFS() {
        return fsDefaultFS;
    }

    public void setFsDefaultFS(String fsDefaultFS) {
        this.fsDefaultFS = fsDefaultFS;
    }

    public String fsDefaultFS() {
        return fsDefaultFS;
    }

    public boolean isKerberosEnabled() {
        return kerberosEnabled;
    }

    public void setKerberosEnabled(boolean kerberosEnabled) {
        this.kerberosEnabled = kerberosEnabled;
    }

    public boolean kerberosEnabled() {
        return kerberosEnabled;
    }

    public String getNameService() {
        return nameService;
    }

    public void setNameService(String nameService) {
        this.nameService = nameService;
    }

    public String nameService() {
        return nameService;
    }

    public String getRawBasePath() {
        return rawBasePath;
    }

    public void setRawBasePath(String rawBasePath) {
        this.rawBasePath = rawBasePath;
    }

    public String rawBasePath() {
        return rawBasePath;
    }

    public String getStagingBasePath() {
        return stagingBasePath;
    }

    public void setStagingBasePath(String stagingBasePath) {
        this.stagingBasePath = stagingBasePath;
    }

    public String stagingBasePath() {
        return stagingBasePath;
    }

    public String getManifestBasePath() {
        return manifestBasePath;
    }

    public void setManifestBasePath(String manifestBasePath) {
        this.manifestBasePath = manifestBasePath;
    }

    public String manifestBasePath() {
        return manifestBasePath;
    }

    public String getFinalPathTemplate() {
        return finalPathTemplate;
    }

    public void setFinalPathTemplate(String finalPathTemplate) {
        this.finalPathTemplate = finalPathTemplate;
    }

    public String finalPathTemplate() {
        return finalPathTemplate;
    }

    public String getStagingPathTemplate() {
        return stagingPathTemplate;
    }

    public void setStagingPathTemplate(String stagingPathTemplate) {
        this.stagingPathTemplate = stagingPathTemplate;
    }

    public String stagingPathTemplate() {
        return stagingPathTemplate;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public void setBucketCount(int bucketCount) {
        this.bucketCount = bucketCount;
    }

    public int bucketCount() {
        return bucketCount;
    }

    void validate() {
        require("hdfs.implementation", implementation);
        require("hdfs.localRootForTesting", localRootForTesting);
        require("hdfs.rawBasePath", rawBasePath);
        require("hdfs.stagingBasePath", stagingBasePath);
        require("hdfs.finalPathTemplate", finalPathTemplate);
        require("hdfs.stagingPathTemplate", stagingPathTemplate);
        if (bucketCount <= 0) {
            throw new IllegalArgumentException("hdfs.bucketCount must be > 0");
        }
    }

    private static void require(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    @Override
    public String toString() {
        return "HdfsConfig{" +
                "implementation='" + implementation + '\'' +
                ", localRootForTesting='" + localRootForTesting + '\'' +
                ", hadoopConfDir='" + hadoopConfDir + '\'' +
                ", fsDefaultFS='" + fsDefaultFS + '\'' +
                ", kerberosEnabled=" + kerberosEnabled +
                ", nameService='" + nameService + '\'' +
                ", rawBasePath='" + rawBasePath + '\'' +
                ", stagingBasePath='" + stagingBasePath + '\'' +
                ", manifestBasePath='" + manifestBasePath + '\'' +
                ", finalPathTemplate='" + finalPathTemplate + '\'' +
                ", stagingPathTemplate='" + stagingPathTemplate + '\'' +
                ", bucketCount=" + bucketCount +
                '}';
    }
}
