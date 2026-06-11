package com.company.traceuploader.cli;

import com.company.traceuploader.config.AgentConfig;
import com.company.traceuploader.config.ConfigLoader;
import com.company.traceuploader.scanner.LocalSealedFileScanner;
import com.company.traceuploader.scanner.SealedFile;
import com.company.traceuploader.scanner.SealedFileScanner;
import com.company.traceuploader.worker.UploaderWorker;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

public final class TraceUploaderMain {
    private static final Logger LOG = System.getLogger(TraceUploaderMain.class.getName());

    private CliOptions options;
    private AgentConfig config;

    public static void main(String[] args) throws Exception {
        new TraceUploaderMain().run(args);
    }

    void run(String[] args) throws Exception {
        options = CliOptions.parse(args);
        if (options.help()) {
            System.out.println(CliOptions.usage());
            return;
        }

        config = new ConfigLoader().load(options.configPath());
        Files.createDirectories(config.localSpool().sealedDirPath());
        SealedFileScanner scanner = new LocalSealedFileScanner(
                config.localSpool().sealedDirPath(), config.scanner(), Clock.systemUTC());

        LOG.log(Level.INFO, "Loaded config {0}; dryRun={1}; once={2}",
                options.configPath(), options.dryRun(), options.once());

        if (options.dryRun()) {
            scanOnce(scanner, true);
            return;
        }
        if (options.once()) {
            scanOnce(scanner, false);
            return;
        }

        while (!Thread.currentThread().isInterrupted()) {
            scanOnce(scanner, false);
            Thread.sleep(config.scanner().scanIntervalMillis());
        }
    }

    private void scanOnce(SealedFileScanner scanner, boolean dryRun) throws Exception {
        List<SealedFile> files = scanner.scan();
        String mode = dryRun ? "Dry run" : "Scan";
        LOG.log(Level.INFO, "{0} discovered {1} sealed file(s)", mode, files.size());

        if (dryRun) {
            for (SealedFile file : files) {
                System.out.printf("Would process sealed file: %s (marker=%s, sizeBytes=%d)%n",
                        file.dataPath(), file.markerPath(), file.sizeBytes());
            }
            return;
        }

        if (files.isEmpty()) {
            return;
        }

        LOG.log(Level.INFO, "Processing {0} file(s) via UploaderWorker", files.size());
        new UploaderWorker(config, scanner).processOnce();
    }

    static final class CliOptions {
        private final Path configPath;
        private final boolean dryRun;
        private final boolean once;
        private final boolean help;

        CliOptions(Path configPath, boolean dryRun, boolean once, boolean help) {
            this.configPath = configPath;
            this.dryRun = dryRun;
            this.once = once;
            this.help = help;
        }

        Path configPath() { return configPath; }
        boolean dryRun() { return dryRun; }
        boolean once() { return once; }
        boolean help() { return help; }

        static CliOptions parse(String[] args) {
            Path configPath = null;
            boolean dryRun = false;
            boolean once = false;
            boolean help = false;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--config":
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--config requires a path");
                        }
                        configPath = Path.of(args[++i]);
                        break;
                    case "--dry-run":
                        dryRun = true;
                        break;
                    case "--once":
                        once = true;
                        break;
                    case "--help":
                    case "-h":
                        help = true;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown argument: " + args[i] + System.lineSeparator() + usage());
                }
            }
            if (!help && configPath == null) {
                throw new IllegalArgumentException("--config is required" + System.lineSeparator() + usage());
            }
            return new CliOptions(configPath, dryRun, once, help);
        }

        static String usage() {
            return "Usage: java -jar target/hdfs-trace-uploader.jar --config <path> [--dry-run] [--once]";
        }
    }
}
