package com.company.traceuploader.hdfs;

import com.company.traceuploader.metadata.ChecksumService;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LocalFsHdfsClient implements HdfsClient {
    private final Path root;
    private final ChecksumService checksumService = new ChecksumService();

    public LocalFsHdfsClient(Path root) throws IOException {
        this.root = root;
        Files.createDirectories(root);
    }

    @Override
    public boolean exists(String remotePath) {
        return Files.exists(toLocalPath(remotePath));
    }

    @Override
    public void upload(Path localPath, String remotePath) throws IOException {
        Path target = toLocalPath(remotePath);
        if (Files.exists(target)) throw new FileAlreadyExistsException(remotePath);
        Files.createDirectories(target.getParent());
        Files.copy(localPath, target);
    }

    @Override
    public boolean rename(String sourceRemotePath, String targetRemotePath) throws IOException {
        Path source = toLocalPath(sourceRemotePath);
        Path target = toLocalPath(targetRemotePath);
        if (!Files.exists(source)) return false;
        if (Files.exists(target)) return false;
        Files.createDirectories(target.getParent());
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        return true;
    }

    @Override
    public void delete(String remotePath) throws IOException {
        Files.deleteIfExists(toLocalPath(remotePath));
    }

    @Override
    public long size(String remotePath) throws IOException {
        return Files.size(toLocalPath(remotePath));
    }

    @Override
    public String checksumSha256(String remotePath) throws IOException {
        return checksumService.sha256(toLocalPath(remotePath));
    }

    public Path toLocalPath(String remotePath) {
        String normalized = remotePath.startsWith("/") ? remotePath.substring(1) : remotePath;
        return root.resolve(normalized).normalize();
    }
}
