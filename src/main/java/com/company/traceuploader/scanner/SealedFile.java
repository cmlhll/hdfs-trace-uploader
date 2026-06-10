package com.company.traceuploader.scanner;
import java.nio.file.Path;
public record SealedFile(Path dataPath, Path donePath) {}
