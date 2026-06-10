package com.company.traceuploader.manifest;

import com.company.traceuploader.config.AgentConfig;import com.company.traceuploader.model.TraceFileMetadata;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.time.Instant;import java.util.*;
public class LocalJsonlManifestWriter implements ManifestWriter {
    private final Path path; private final AgentConfig config;
    public LocalJsonlManifestWriter(Path path, AgentConfig config){this.path=path;this.config=config;}
    public synchronized void writeIfAbsent(TraceFileMetadata m) throws IOException { Files.createDirectories(path.getParent()); if(Files.exists(path)){ for(String line:Files.readAllLines(path)){ if(line.contains("\"file_id\":\""+m.fileId()+"\"")) return; } }
        Map<String,Object> row=new LinkedHashMap<>(); row.put("file_id",m.fileId()); row.put("app",m.app()); row.put("env",m.env()); row.put("region",m.region()); row.put("cluster",m.cluster()); row.put("host",m.host()); row.put("pid",m.pid()); row.put("boot_id",m.bootId()); row.put("dt",m.dt().toString()); row.put("hour",m.hour()); row.put("bucket",m.bucket()); row.put("start_time",m.startTime().toString()); row.put("end_time",m.endTime().toString()); row.put("hdfs_path",m.hdfsFinalPath()); row.put("size_bytes",m.sizeBytes()); row.put("checksum",m.checksum()); row.put("record_count",m.recordCount()); row.put("raw_format",m.fileName().contains(".jsonl")?"jsonl":"log"); row.put("compression",m.fileName().endsWith(".zst")?"zstd":"none"); row.put("commit_time", Instant.now().toString()); row.put("agent_version", config.agent.agentVersion); row.put("state","MANIFEST_COMMITTED");
        try(BufferedWriter bw=Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)){ bw.write(toJson(row)); bw.newLine(); }
    }
    private static String toJson(Map<String,Object> m){ StringBuilder sb=new StringBuilder("{"); boolean first=true; for(var e:m.entrySet()){ if(!first) sb.append(','); first=false; sb.append('"').append(e.getKey()).append("\":"); Object v=e.getValue(); if(v instanceof Number) sb.append(v); else sb.append('"').append(String.valueOf(v).replace("\\","\\\\").replace("\"","\\\"")).append('"'); } return sb.append('}').toString(); }
}
