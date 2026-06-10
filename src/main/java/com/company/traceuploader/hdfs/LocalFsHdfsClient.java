package com.company.traceuploader.hdfs;

import com.company.traceuploader.metadata.ChecksumService;import java.io.IOException;import java.nio.file.*;
public class LocalFsHdfsClient implements HdfsClient {
    private final Path root; private final ChecksumService checksumService = new ChecksumService();
    public LocalFsHdfsClient(Path root) throws IOException { this.root=root; Files.createDirectories(root); }
    public Path toLocalPath(String remotePath){ String r=remotePath.startsWith("/")?remotePath.substring(1):remotePath; return root.resolve(r).normalize(); }
    public boolean exists(String remotePath){ return Files.exists(toLocalPath(remotePath)); }
    public void upload(Path localPath,String remotePath) throws IOException { Path dst=toLocalPath(remotePath); Files.createDirectories(dst.getParent()); Files.copy(localPath,dst,StandardCopyOption.REPLACE_EXISTING); }
    public boolean rename(String srcRemotePath,String dstRemotePath) throws IOException { Path src=toLocalPath(srcRemotePath), dst=toLocalPath(dstRemotePath); if(Files.exists(dst)) return false; Files.createDirectories(dst.getParent()); try { Files.move(src,dst,StandardCopyOption.ATOMIC_MOVE); } catch(AtomicMoveNotSupportedException e) { Files.move(src,dst); } return true; }
    public void delete(String remotePath) throws IOException { Files.deleteIfExists(toLocalPath(remotePath)); }
    public long size(String remotePath) throws IOException { return Files.size(toLocalPath(remotePath)); }
    public String checksumSha256(String remotePath) throws IOException { return checksumService.sha256(toLocalPath(remotePath)); }
}
