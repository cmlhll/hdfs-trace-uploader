package com.company.traceuploader.scanner;

import java.io.IOException;
import java.util.List;

public interface SealedFileScanner {
    List<DiscoveredFile> scan() throws IOException;
}
