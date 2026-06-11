package com.company.traceuploader.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads the agent YAML configuration using Jackson YAML dataformat.
 *
 * <p>Internally deserializes the full YAML into a {@link TraceUploaderConfig} root object,
 * then extracts the {@link AgentConfig} identity fields and injects all sub-config sections
 * (localSpool, scanner, hdfs, upload, retry, manifest, gc, diskWatermark, metrics) into the
 * returned {@link AgentConfig}. This preserves backward compatibility with callers that use
 * {@code ConfigLoader.load()} returning an {@code AgentConfig} with delegate methods such as
 * {@code config.localSpool()}, {@code config.scanner()}, etc.</p>
 *
 * <p>Supports YAML anchors/aliases. Unknown properties are silently ignored.</p>
 */
public final class ConfigLoader {

    private final ObjectMapper mapper;

    /** Creates a ConfigLoader with a default ObjectMapper configured for YAML. */
    public ConfigLoader() {
        this.mapper = newMapper();
    }

    /**
     * Creates a ConfigLoader with a pre-configured ObjectMapper (useful for testing).
     *
     * @param mapper the ObjectMapper to use; must be configured for YAML reading
     */
    public ConfigLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Loads and validates the configuration from the given YAML file.
     *
     * <p>Returns an {@link AgentConfig} that contains both the agent identity fields
     * (app, env, region, etc.) and delegate accessors for all sub-config sections.</p>
     *
     * @param configPath path to the agent YAML configuration file
     * @return a fully populated and validated {@link AgentConfig}
     * @throws IOException if the file cannot be read or parsed
     * @throws IllegalArgumentException if validation fails
     */
    public AgentConfig load(Path configPath) throws IOException {
        // Step 1: Deserialize the full YAML into the root config object
        TraceUploaderConfig root = mapper.readValue(configPath.toFile(), TraceUploaderConfig.class);
        root.validate();

        // Step 2: Extract the AgentConfig identity fields from the root
        AgentConfig agent = root.getAgent();
        if (agent == null) {
            agent = new AgentConfig();
        }

        // Step 3: Inject all sub-config sections into the AgentConfig
        agent.injectSubConfigs(root);

        // Step 4: Validate the combined AgentConfig
        agent.validate();
        return agent;
    }

    // ---- internal helpers ----

    private static ObjectMapper newMapper() {
        YAMLFactory yamlFactory = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build();

        return new ObjectMapper(yamlFactory)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
