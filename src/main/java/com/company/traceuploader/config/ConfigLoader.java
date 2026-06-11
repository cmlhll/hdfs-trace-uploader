package com.company.traceuploader.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigLoader {
    private static final Pattern MAPPING_ALIAS_VALUE =
            Pattern.compile("^(\\s*[^#:\\n][^:\\n]*:\\s*)(\\*[^#\\s]*)(\\s*(?:#.*)?)$");
    private static final Pattern LIST_ALIAS_VALUE =
            Pattern.compile("^(\\s*-\\s*)(\\*[^#\\s]*)(\\s*(?:#.*)?)$");

    private final ObjectMapper objectMapper;

    public ConfigLoader() {
        this.objectMapper = new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    }

    public TraceUploaderConfig load(Path configPath) throws IOException {
        String yaml = Files.readString(configPath);
        TraceUploaderConfig config;
        try {
            config = read(yaml);
        } catch (IOException firstFailure) {
            if (!yaml.contains("*")) {
                throw firstFailure;
            }
            config = read(quoteAliasLikeScalars(yaml));
        }
        config.applyDefaults();
        config.validate();
        return config;
    }

    private TraceUploaderConfig read(String yaml) throws IOException {
        return objectMapper.readValue(yaml, TraceUploaderConfig.class);
    }

    private static String quoteAliasLikeScalars(String yaml) {
        return yaml.lines()
                .map(ConfigLoader::quoteAliasLikeScalarLine)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static String quoteAliasLikeScalarLine(String line) {
        Matcher mappingMatcher = MAPPING_ALIAS_VALUE.matcher(line);
        if (mappingMatcher.matches()) {
            return mappingMatcher.group(1) + "\"" + mappingMatcher.group(2) + "\"" + mappingMatcher.group(3);
        }

        Matcher listMatcher = LIST_ALIAS_VALUE.matcher(line);
        if (listMatcher.matches()) {
            return listMatcher.group(1) + "\"" + listMatcher.group(2) + "\"" + listMatcher.group(3);
        }

        return line;
    }
}
