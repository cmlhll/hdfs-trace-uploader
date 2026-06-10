package com.company.traceuploader.scanner;

import com.company.traceuploader.config.AgentConfig;
import java.io.IOException;import java.nio.file.*;import java.time.Clock;import java.util.*;import java.util.stream.Stream;

public class LocalSealedFileScanner implements SealedFileScanner {
    private final Path sealedDir; private final AgentConfig.Scanner config; private final Clock clock;
    public LocalSealedFileScanner(Path sealedDir, AgentConfig.Scanner config, Clock clock){this.sealedDir=sealedDir;this.config=config;this.clock=clock;}
    public List<SealedFile> scan() throws IOException {
        if (!Files.isDirectory(sealedDir)) return List.of();
        List<SealedFile> out = new ArrayList<>(); long now = clock.millis();
        try (Stream<Path> s = Files.list(sealedDir)) {
            for (Path p : s.sorted().toList()) {
                if (out.size() >= config.maxFilesPerScan) break;
                if (!Files.isRegularFile(p)) continue;
                String name = p.getFileName().toString();
                if (name.endsWith(config.markerSuffix) || hasIgnoredSuffix(name) || !hasDataSuffix(name)) continue;
                Path done = p.resolveSibling(name + config.markerSuffix);
                if (!Files.isRegularFile(done)) continue;
                long age = now - Files.getLastModifiedTime(p).toMillis();
                if (age < config.minStableAgeSeconds * 1000) continue;
                out.add(new SealedFile(p, done));
            }
        }
        return out;
    }
    private boolean hasIgnoredSuffix(String name){return config.ignoredSuffixes.stream().anyMatch(name::endsWith);}    
    private boolean hasDataSuffix(String name){return config.dataFileSuffixes.stream().anyMatch(name::endsWith);}    
}
