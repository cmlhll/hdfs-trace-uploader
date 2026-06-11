package com.company.traceuploader.cli;

import com.company.traceuploader.config.ConfigLoader;
import com.company.traceuploader.config.TraceUploaderConfig;
import com.company.traceuploader.scanner.DefaultSealedFileScanner;
import com.company.traceuploader.scanner.DiscoveredFile;
import com.company.traceuploader.scanner.SealedFileScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class TraceUploaderMain {
    private static final Logger LOG = LoggerFactory.getLogger(TraceUploaderMain.class);

    public static void main(String[] args) {
        int exitCode = new TraceUploaderMain().run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int run(String[] args) {
        CommandLineOptions options;
        try {
            options = CommandLineOptions.parse(args);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            printUsage();
            return 2;
        }

        if (options.help) {
            printUsage();
            return 0;
        }

        try {
            TraceUploaderConfig config = new ConfigLoader().load(options.configPath);
            LOG.info("Loaded config from {}", options.configPath);

            if (options.dryRun) {
                LOG.info("Dry-run completed for agentId={} sealedDir={}",
                        config.getAgent().getAgentId(),
                        config.getLocalSpool().getSealedDir());
                System.out.println("Dry run completed: config loaded from " + options.configPath);
                return 0;
            }

            if (options.once) {
                SealedFileScanner scanner = new DefaultSealedFileScanner(config.getLocalSpool(), config.getScanner());
                List<DiscoveredFile> discoveredFiles = scanner.scan();
                LOG.info("Single scan completed. discoveredFiles={}", discoveredFiles.size());
                return 0;
            }

            LOG.info("Daemon mode is not implemented in Phase 0/1. Use --once for a single scan.");
            return 0;
        } catch (Exception exception) {
            LOG.error("Trace uploader failed", exception);
            System.err.println("Trace uploader failed: " + exception.getMessage());
            return 1;
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -jar hdfs-trace-uploader.jar --config <path> [--dry-run] [--once]");
    }

    private static final class CommandLineOptions {
        private final Path configPath;
        private final boolean dryRun;
        private final boolean once;
        private final boolean help;

        private CommandLineOptions(Path configPath, boolean dryRun, boolean once, boolean help) {
            this.configPath = configPath;
            this.dryRun = dryRun;
            this.once = once;
            this.help = help;
        }

        private static CommandLineOptions parse(String[] args) {
            Path configPath = null;
            boolean dryRun = false;
            boolean once = false;
            boolean help = false;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--help".equals(arg) || "-h".equals(arg)) {
                    help = true;
                } else if ("--dry-run".equals(arg)) {
                    dryRun = true;
                } else if ("--once".equals(arg)) {
                    once = true;
                } else if ("--config".equals(arg)) {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--config requires a path");
                    }
                    configPath = Path.of(args[++i]);
                } else if (arg.startsWith("--config=")) {
                    String value = arg.substring("--config=".length());
                    if (value.isBlank()) {
                        throw new IllegalArgumentException("--config requires a path");
                    }
                    configPath = Path.of(value);
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            if (!help && configPath == null) {
                throw new IllegalArgumentException("--config is required");
            }

            return new CommandLineOptions(configPath, dryRun, once, help);
        }
    }
}
