package com.company.traceuploader.config;

import java.io.IOException;import java.nio.file.*;import java.util.*;

public class ConfigLoader {
    public AgentConfig load(Path path) throws IOException {
        AgentConfig c=new AgentConfig(); List<String> lines=Files.readAllLines(path); String section=""; String listKey=null;
        for(String raw: lines){ String line=raw.split("#",2)[0]; if(line.isBlank()) continue; int indent=countIndent(line); String t=line.trim(); if(!t.contains(":") && !t.startsWith("-")) continue; if(indent==0 && t.endsWith(":")){ section=t.substring(0,t.length()-1); listKey=null; continue; }
            if(t.startsWith("-")){ String v=unquote(t.substring(1).trim()); if("scanner.dataFileSuffixes".equals(listKey)) c.scanner.dataFileSuffixes.add(v); if("scanner.ignoredSuffixes".equals(listKey)) c.scanner.ignoredSuffixes.add(v); continue; }
            String[] kv=t.split(":",2); String key=kv[0].trim(); String val=kv.length>1?unquote(kv[1].trim()):""; if(val.equals("${HADOOP_CONF_DIR}")) val=System.getenv().getOrDefault("HADOOP_CONF_DIR",""); String fq=section+"."+key; if(val.isEmpty()){ listKey=fq; if(fq.equals("scanner.dataFileSuffixes")) c.scanner.dataFileSuffixes.clear(); if(fq.equals("scanner.ignoredSuffixes")) c.scanner.ignoredSuffixes.clear(); continue; } listKey=null; set(c,fq,val);
        }
        return c;
    }
    private static int countIndent(String s){int n=0; while(n<s.length() && s.charAt(n)==' ') n++; return n;}
    private static String unquote(String s){ if((s.startsWith("\"")&&s.endsWith("\""))||(s.startsWith("'")&&s.endsWith("'"))) return s.substring(1,s.length()-1); return s; }
    private static void set(AgentConfig c,String k,String v){ switch(k){
        case "agent.app" -> c.agent.app=v; case "agent.env" -> c.agent.env=v; case "agent.region" -> c.agent.region=v; case "agent.cluster" -> c.agent.cluster=v; case "agent.host" -> c.agent.host=v; case "agent.agentId" -> c.agent.agentId=v; case "agent.agentVersion" -> c.agent.agentVersion=v;
        case "localSpool.baseDir" -> c.localSpool.baseDir=v; case "localSpool.writingDir" -> c.localSpool.writingDir=v; case "localSpool.sealedDir" -> c.localSpool.sealedDir=v; case "localSpool.committedDir" -> c.localSpool.committedDir=v; case "localSpool.failedDir" -> c.localSpool.failedDir=v; case "localSpool.quarantineDir" -> c.localSpool.quarantineDir=v; case "localSpool.stateDir" -> c.localSpool.stateDir=v; case "localSpool.tmpDir" -> c.localSpool.tmpDir=v;
        case "scanner.markerSuffix" -> c.scanner.markerSuffix=v; case "scanner.minStableAgeSeconds" -> c.scanner.minStableAgeSeconds=Long.parseLong(v); case "scanner.maxFilesPerScan" -> c.scanner.maxFilesPerScan=Integer.parseInt(v); case "scanner.scanIntervalSeconds" -> c.scanner.scanIntervalSeconds=Long.parseLong(v);
        case "hdfs.implementation" -> c.hdfs.implementation=v; case "hdfs.localRootForTesting" -> c.hdfs.localRootForTesting=v; case "hdfs.rawBasePath" -> c.hdfs.rawBasePath=v; case "hdfs.stagingBasePath" -> c.hdfs.stagingBasePath=v; case "hdfs.manifestBasePath" -> c.hdfs.manifestBasePath=v; case "hdfs.finalPathTemplate" -> c.hdfs.finalPathTemplate=v; case "hdfs.stagingPathTemplate" -> c.hdfs.stagingPathTemplate=v; case "hdfs.bucketCount" -> c.hdfs.bucketCount=Integer.parseInt(v);
        case "upload.verifyChecksumAfterUpload" -> c.upload.verifyChecksumAfterUpload=Boolean.parseBoolean(v); case "upload.verifySizeAfterUpload" -> c.upload.verifySizeAfterUpload=Boolean.parseBoolean(v); case "upload.deleteStagingOnRetry" -> c.upload.deleteStagingOnRetry=Boolean.parseBoolean(v);
        case "manifest.type" -> c.manifest.type=v; case "manifest.localPath" -> c.manifest.localPath=v; case "manifest.writeIfAbsent" -> c.manifest.writeIfAbsent=Boolean.parseBoolean(v); default -> {}
    }}
}
