package com.company.traceuploader.metadata;

import com.company.traceuploader.config.AgentConfig;import com.company.traceuploader.model.TraceFileMetadata;import com.company.traceuploader.scanner.SealedFile;
import java.io.IOException;import java.nio.file.*;import java.time.*;import java.time.format.DateTimeFormatter;import java.util.*;import java.util.regex.*;import java.util.stream.Stream;

public class MetadataService {
    private static final Pattern NAME = Pattern.compile("^trace-([^-]+)-([^-]+)-([^-]+)-([^-]+)-(.+)-pid([^-]+)-([^-]+)-(\\d{8}T\\d{6})-(\\d{8}T\\d{6})-seq(\\d+)\\.(.+)$");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").withZone(ZoneOffset.UTC);
    private final AgentConfig config; private final ChecksumService checksumService; private final FileIdGenerator fileIdGenerator;
    public MetadataService(AgentConfig config, ChecksumService checksumService, FileIdGenerator fileIdGenerator){this.config=config;this.checksumService=checksumService;this.fileIdGenerator=fileIdGenerator;}
    public TraceFileMetadata build(SealedFile file, int attempt) throws IOException {
        Path p=file.dataPath(); String fileName=p.getFileName().toString(); Matcher m=NAME.matcher(fileName); if(!m.matches()) throw new IllegalArgumentException("Unsupported trace file name: "+fileName);
        String app=m.group(1), env=m.group(2), region=m.group(3), cluster=m.group(4), host=m.group(5), pid=m.group(6), bootId=m.group(7), startRaw=m.group(8), endRaw=m.group(9), seqRaw=m.group(10);
        Instant start=LocalDateTime.parse(startRaw, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")).toInstant(ZoneOffset.UTC); Instant end=LocalDateTime.parse(endRaw, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")).toInstant(ZoneOffset.UTC);
        long size=Files.size(p); String checksum=checksumService.sha256(p); long records=countRecords(p); String fileId=fileIdGenerator.generate(app,env,region,cluster,host,pid,bootId,startRaw,endRaw,seqRaw,checksum,size); int bucket=fileIdGenerator.bucket(fileId, config.hdfs.bucketCount);
        LocalDate dt=LocalDateTime.ofInstant(start, ZoneOffset.UTC).toLocalDate(); int hour=LocalDateTime.ofInstant(start, ZoneOffset.UTC).getHour();
        String finalPath=render(config.hdfs.finalPathTemplate, config.hdfs.rawBasePath, config.hdfs.stagingBasePath, app, env, region, cluster, host, pid, bootId, dt, hour, bucket, fileName, fileId, attempt);
        String stagingPath=render(config.hdfs.stagingPathTemplate, config.hdfs.rawBasePath, config.hdfs.stagingBasePath, app, env, region, cluster, host, pid, bootId, dt, hour, bucket, fileName, fileId, attempt);
        return new TraceFileMetadata(fileId,app,env,region,cluster,host,pid,bootId,start,end,Integer.parseInt(seqRaw),p,file.donePath(),fileName,size,checksum,records,dt,hour,bucket,finalPath,stagingPath);
    }
    private long countRecords(Path p) throws IOException { try(Stream<String> lines=Files.lines(p)){return lines.count();} }
    private String render(String t,String raw,String staging,String app,String env,String region,String cluster,String host,String pid,String bootId,LocalDate dt,int hour,int bucket,String fileName,String fileId,int attempt){
        return t.replace("{rawBasePath}",raw).replace("{stagingBasePath}",staging).replace("{app}",app).replace("{env}",env).replace("{region}",region).replace("{cluster}",cluster).replace("{host}",host).replace("{pid}",pid).replace("{bootId}",bootId).replace("{dt}",dt.toString()).replace("{hour}",String.format("%02d",hour)).replace("{bucket}",String.format("%03d",bucket)).replace("{fileName}",fileName).replace("{fileId}",fileId).replace("{attempt}",String.valueOf(attempt));
    }
}
