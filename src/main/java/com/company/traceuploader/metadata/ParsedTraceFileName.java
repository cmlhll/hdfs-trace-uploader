package com.company.traceuploader.metadata;

record ParsedTraceFileName(String fileName, String app, String env, String region, String cluster, String host,
                           String pid, String bootId, String startTs, String endTs, String seq) {
}
