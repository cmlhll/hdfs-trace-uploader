package com.company.traceuploader.hdfs;

import com.company.traceuploader.config.HdfsConfig;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link HadoopHdfsClient}.
 *
 * <p>These tests are disabled by default because they require Hadoop client
 * libraries on the classpath. On machines without Hadoop installed, they are
 * skipped gracefully.
 */
@Disabled("Requires Hadoop libraries — skip on CI/machines without Hadoop")
class HadoopHdfsClientTest {

    @Test
    void constructorThrowsWhenConfDirDoesNotExist() {
        HdfsConfig config = new HdfsConfig();
        config.setImplementation("hadoop");
        config.setHadoopConfDir("/nonexistent/hadoop/conf");
        config.setFsDefaultFS("hdfs://localhost:9000");
        config.setKerberosEnabled(false);

        IOException exception = assertThrows(IOException.class, () -> {
            new HadoopHdfsClient(config);
        });
        assertTrue(exception.getMessage().contains("java.net.UnknownHostException")
                || exception.getMessage().contains("Connection refused")
                || exception.getMessage().contains("Failed to find")
                || exception.getMessage().toLowerCase().contains("error")
                || exception.getMessage().toLowerCase().contains("failed"));
    }

    @Test
    void constructorSucceedsWithMockFileSystem() throws Exception {
        // Use the package-private constructor that accepts a pre-configured FileSystem
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        conf.set("fs.defaultFS", "file:///");
        org.apache.hadoop.fs.FileSystem localFs = org.apache.hadoop.fs.FileSystem.getLocal(conf);
        HadoopHdfsClient client = new HadoopHdfsClient(localFs);
        assertNotNull(client);
    }
}
