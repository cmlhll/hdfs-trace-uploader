package com.company.traceuploader.hdfs;

import com.company.traceuploader.config.HdfsConfig;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Factory for creating {@link HdfsClient} instances based on the implementation
 * type specified in the HDFS configuration.
 *
 * <p>Supported implementations:
 * <ul>
 *   <li>{@code localfs} — returns a {@link LocalFsHdfsClient} backed by the local filesystem</li>
 *   <li>{@code hadoop} — returns a {@link HadoopHdfsClient} backed by Hadoop FileSystem APIs</li>
 * </ul>
 */
public final class HdfsClientFactory {

    private HdfsClientFactory() {
        // utility class
    }

    /**
     * Creates an {@link HdfsClient} based on the implementation setting in the configuration.
     *
     * @param config   the full agent configuration (only hdfs section is used)
     * @param hdfsConfig the HDFS configuration section
     * @return a new HdfsClient instance
     * @throws IOException              if the client cannot be created
     * @throws IllegalArgumentException if the implementation is unknown
     */
    public static HdfsClient createHdfs(HdfsConfig hdfsConfig) throws IOException {
        String impl = hdfsConfig.implementation();
        if ("localfs".equalsIgnoreCase(impl)) {
            return new LocalFsHdfsClient(hdfsConfig.localRootForTestingPath());
        } else if ("hadoop".equalsIgnoreCase(impl)) {
            return new HadoopHdfsClient(hdfsConfig);
        } else {
            throw new IllegalArgumentException(
                    "Unknown HDFS implementation: '" + impl + "'. Supported: localfs, hadoop");
        }
    }
}
