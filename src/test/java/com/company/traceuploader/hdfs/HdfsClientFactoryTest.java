package com.company.traceuploader.hdfs;

import com.company.traceuploader.config.HdfsConfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link HdfsClientFactory}.
 */
class HdfsClientFactoryTest {

    @Test
    void localfsImplReturnsLocalFsHdfsClient() throws Exception {
        HdfsConfig config = new HdfsConfig();
        config.setImplementation("localfs");
        config.setLocalRootForTesting("/tmp/test-hdfs-factory-localfs");

        HdfsClient client = HdfsClientFactory.createHdfs(config);
        assertNotNull(client, "Expected a non-null HdfsClient");
        assertTrue(client instanceof LocalFsHdfsClient,
                "Expected LocalFsHdfsClient for implementation=localfs");
    }

    @Test
    void localfsImplCaseInsensitive() throws Exception {
        HdfsConfig config = new HdfsConfig();
        config.setImplementation("LocalFs");
        config.setLocalRootForTesting("/tmp/test-hdfs-factory-localfs-ci");

        HdfsClient client = HdfsClientFactory.createHdfs(config);
        assertNotNull(client);
        assertTrue(client instanceof LocalFsHdfsClient);
    }

    @Test
    void hadoopImplReturnsHadoopHdfsClient() throws Exception {
        HdfsConfig config = new HdfsConfig();
        config.setImplementation("hadoop");
        config.setHadoopConfDir("/nonexistent/hadoop/conf");
        config.setFsDefaultFS("file:///");
        config.setKerberosEnabled(false);

        // Hadoop client libraries are available via provided scope, so
        // HadoopHdfsClient initializes a local FileSystem successfully
        // even though the conf dir doesn't exist.
        HdfsClient client = HdfsClientFactory.createHdfs(config);
        assertNotNull(client);
        assertTrue(client instanceof HadoopHdfsClient,
                "Expected HadoopHdfsClient for implementation=hadoop");
    }

    @Test
    void hadoopImplCaseInsensitive() throws Exception {
        HdfsConfig config = new HdfsConfig();
        config.setImplementation("Hadoop");
        config.setHadoopConfDir("/nonexistent");
        config.setFsDefaultFS("file:///");
        config.setKerberosEnabled(false);

        HdfsClient client = HdfsClientFactory.createHdfs(config);
        assertNotNull(client);
        assertTrue(client instanceof HadoopHdfsClient);
    }

    @Test
    void unknownImplThrowsIllegalArgument() {
        HdfsConfig config = new HdfsConfig();
        config.setImplementation("unknown");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            HdfsClientFactory.createHdfs(config);
        });
        assertTrue(exception.getMessage().contains("unknown"),
                "Expected error message to mention the unknown implementation");
    }

    @Test
    void nullImplThrowsIllegalArgument() {
        HdfsConfig config = new HdfsConfig();
        config.setImplementation(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            HdfsClientFactory.createHdfs(config);
        });
        assertTrue(exception.getMessage().contains("null"),
                "Expected error message to mention null implementation");
    }
}
