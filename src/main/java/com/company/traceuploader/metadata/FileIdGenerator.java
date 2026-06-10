package com.company.traceuploader.metadata;

public final class FileIdGenerator {
    public String generate(ParsedTraceFileName parsed, long sizeBytes, String checksum) {
        String material = String.join("|",
                parsed.app(), parsed.env(), parsed.region(), parsed.cluster(), parsed.host(), parsed.pid(), parsed.bootId(),
                parsed.startTs(), parsed.endTs(), parsed.seq(), parsed.fileName(), Long.toString(sizeBytes), checksum);
        return ChecksumService.sha256Hex(material);
    }

    public int bucket(String fileId, int bucketCount) {
        long value = Long.parseUnsignedLong(fileId.substring(0, 16), 16);
        return (int) Long.remainderUnsigned(value, bucketCount);
    }
}
