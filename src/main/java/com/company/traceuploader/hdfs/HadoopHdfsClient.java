package com.company.traceuploader.hdfs;

import com.company.traceuploader.config.HdfsConfig;
import com.company.traceuploader.metadata.ChecksumService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;

/**
 * Real Hadoop HDFS client implementation backed by {@link FileSystem}.
 *
 * <p>Loads Hadoop configuration from the directory specified in {@link HdfsConfig#hadoopConfDir()},
 * loading {@code core-site.xml} and {@code hdfs-site.xml} from that directory. If
 * {@link HdfsConfig#fsDefaultFS()} is set, it overrides {@code fs.defaultFS} in the
 * Configuration. Kerberos is supported via {@link HdfsConfig#kerberosEnabled()}.
 *
 * <p>This class requires Hadoop client libraries on the classpath at runtime.
 */
public final class HadoopHdfsClient implements HdfsClient {

    private final FileSystem fs;
    private final ChecksumService checksumService = new ChecksumService();

    /**
     * Creates a new HadoopHdfsClient from the given HDFS configuration.
     *
     * @param hdfsConfig the HDFS configuration containing hadoopConfDir, fsDefaultFS,
     *                   and kerberosEnabled settings
     * @throws IOException if the FileSystem cannot be initialized
     */
    public HadoopHdfsClient(HdfsConfig hdfsConfig) throws IOException {
        Configuration conf = loadConfiguration(hdfsConfig);

        if (hdfsConfig.kerberosEnabled()) {
            conf.set("hadoop.security.authentication", "kerberos");
            org.apache.hadoop.security.UserGroupInformation.setConfiguration(conf);
        }

        this.fs = FileSystem.get(conf);
    }

    /**
     * Package-private constructor for testing that accepts a pre-configured FileSystem.
     *
     * @param fs the FileSystem instance to use
     */
    HadoopHdfsClient(FileSystem fs) {
        this.fs = fs;
    }

    @Override
    public boolean exists(String remotePath) throws IOException {
        return fs.exists(new org.apache.hadoop.fs.Path(remotePath));
    }

    @Override
    public void upload(Path localPath, String remotePath) throws IOException {
        org.apache.hadoop.fs.Path target = new org.apache.hadoop.fs.Path(remotePath);
        if (fs.exists(target)) {
            throw new FileAlreadyExistsException(remotePath);
        }
        fs.copyFromLocalFile(false, true, new org.apache.hadoop.fs.Path(localPath.toString()), target);
    }

    @Override
    public boolean rename(String sourceRemotePath, String targetRemotePath) throws IOException {
        org.apache.hadoop.fs.Path src = new org.apache.hadoop.fs.Path(sourceRemotePath);
        org.apache.hadoop.fs.Path dst = new org.apache.hadoop.fs.Path(targetRemotePath);
        if (!fs.exists(src)) {
            return false;
        }
        if (fs.exists(dst)) {
            return false;
        }
        return fs.rename(src, dst);
    }

    @Override
    public void delete(String remotePath) throws IOException {
        org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(remotePath);
        fs.delete(path, false);
    }

    @Override
    public long size(String remotePath) throws IOException {
        return fs.getFileStatus(new org.apache.hadoop.fs.Path(remotePath)).getLen();
    }

    @Override
    public String checksumSha256(String remotePath) throws IOException {
        org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(remotePath);
        try (InputStream in = fs.open(path)) {
            return computeSha256Hex(in);
        }
    }

    // ---- internal helpers ----

    private static Configuration loadConfiguration(HdfsConfig hdfsConfig) throws IOException {
        Configuration conf = new Configuration(false);

        String confDir = hdfsConfig.hadoopConfDir();
        if (confDir != null && !confDir.isEmpty()) {
            java.nio.file.Path confDirPath = java.nio.file.Paths.get(confDir);
            java.nio.file.Path coreSite = confDirPath.resolve("core-site.xml");
            java.nio.file.Path hdfsSite = confDirPath.resolve("hdfs-site.xml");

            if (java.nio.file.Files.exists(coreSite)) {
                conf.addResource(coreSite.toUri().toURL());
            }
            if (java.nio.file.Files.exists(hdfsSite)) {
                conf.addResource(hdfsSite.toUri().toURL());
            }
        }

        String fsDefaultFS = hdfsConfig.fsDefaultFS();
        if (fsDefaultFS != null && !fsDefaultFS.isEmpty()) {
            conf.set("fs.defaultFS", fsDefaultFS);
        }

        return conf;
    }

    private static String computeSha256Hex(InputStream in) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not available", e);
        }

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }

        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder(64);
        for (byte b : hash) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
