package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HdfsConfig {
    private String implementation = "localfs";
    private String hadoopConfDir;
    private String nameService;
    private String rawBasePath;
    private String stagingBasePath;
    private String manifestBasePath;
    private String finalPathTemplate;
    private String stagingPathTemplate;
    private int bucketCount = 128;

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public String getHadoopConfDir() {
        return hadoopConfDir;
    }

    public void setHadoopConfDir(String hadoopConfDir) {
        this.hadoopConfDir = hadoopConfDir;
    }

    public String getNameService() {
        return nameService;
    }

    public void setNameService(String nameService) {
        this.nameService = nameService;
    }

    public String getRawBasePath() {
        return rawBasePath;
    }

    public void setRawBasePath(String rawBasePath) {
        this.rawBasePath = rawBasePath;
    }

    public String getStagingBasePath() {
        return stagingBasePath;
    }

    public void setStagingBasePath(String stagingBasePath) {
        this.stagingBasePath = stagingBasePath;
    }

    public String getManifestBasePath() {
        return manifestBasePath;
    }

    public void setManifestBasePath(String manifestBasePath) {
        this.manifestBasePath = manifestBasePath;
    }

    public String getFinalPathTemplate() {
        return finalPathTemplate;
    }

    public void setFinalPathTemplate(String finalPathTemplate) {
        this.finalPathTemplate = finalPathTemplate;
    }

    public String getStagingPathTemplate() {
        return stagingPathTemplate;
    }

    public void setStagingPathTemplate(String stagingPathTemplate) {
        this.stagingPathTemplate = stagingPathTemplate;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public void setBucketCount(int bucketCount) {
        this.bucketCount = bucketCount;
    }
}
