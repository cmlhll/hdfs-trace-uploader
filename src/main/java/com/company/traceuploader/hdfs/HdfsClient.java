package com.company.traceuploader.hdfs;
import java.io.IOException;import java.nio.file.Path;
public interface HdfsClient {
    boolean exists(String remotePath) throws IOException;
    void upload(Path localPath, String remotePath) throws IOException;
    boolean rename(String srcRemotePath, String dstRemotePath) throws IOException;
    void delete(String remotePath) throws IOException;
    long size(String remotePath) throws IOException;
    String checksumSha256(String remotePath) throws IOException;
}
