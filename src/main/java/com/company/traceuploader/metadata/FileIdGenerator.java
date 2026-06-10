package com.company.traceuploader.metadata;

import com.company.traceuploader.model.TraceFileMetadata;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.HexFormat;
public class FileIdGenerator {
    public String generate(String app,String env,String region,String cluster,String host,String pid,String bootId,String start,String end,String seq,String checksum,long size){
        String base=String.join("|",app,env,region,cluster,host,pid,bootId,start,end,seq,checksum,String.valueOf(size));
        try { byte[] d=MessageDigest.getInstance("SHA-256").digest(base.getBytes(StandardCharsets.UTF_8)); return "trace-"+HexFormat.of().formatHex(d).substring(0,32); } catch(Exception e){ throw new IllegalStateException(e);}    }
    public int bucket(String fileId,int bucketCount){ return Math.floorMod(fileId.hashCode(), bucketCount); }
}
