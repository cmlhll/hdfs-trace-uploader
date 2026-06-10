package com.company.traceuploader.model;

import java.nio.file.Path;import java.time.Instant;import java.time.LocalDate;
public record TraceFileMetadata(
        String fileId,String app,String env,String region,String cluster,String host,String pid,String bootId,
        Instant startTime,Instant endTime,int seq,Path localPath,Path donePath,String fileName,
        long sizeBytes,String checksum,long recordCount,LocalDate dt,int hour,int bucket,
        String hdfsFinalPath,String hdfsStagingPath) {}
