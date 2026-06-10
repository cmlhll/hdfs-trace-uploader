package com.company.traceuploader.scanner;
import com.company.traceuploader.config.AgentConfig;import org.junit.jupiter.api.*;import java.nio.file.*;import java.time.Clock;import java.util.List;import static org.junit.jupiter.api.Assertions.*;
class LocalSealedFileScannerTest {
 @TempDir Path dir;
 @Test void scansOnlyStableFilesWithDoneMarker() throws Exception { AgentConfig.Scanner c=new AgentConfig.Scanner(); c.minStableAgeSeconds=0; c.maxFilesPerScan=10; Files.writeString(dir.resolve("a.jsonl"),"x\n"); Files.createFile(dir.resolve("a.jsonl.done")); Files.writeString(dir.resolve("b.jsonl"),"x\n"); Files.writeString(dir.resolve("c.jsonl.tmp"),"x\n"); Files.createFile(dir.resolve("c.jsonl.tmp.done")); List<SealedFile> got=new LocalSealedFileScanner(dir,c,Clock.systemUTC()).scan(); assertEquals(1, got.size()); assertEquals("a.jsonl", got.get(0).dataPath().getFileName().toString()); }
 @Test void respectsMaxFilesPerScan() throws Exception { AgentConfig.Scanner c=new AgentConfig.Scanner(); c.minStableAgeSeconds=0; c.maxFilesPerScan=1; Files.writeString(dir.resolve("a.jsonl"),"x"); Files.createFile(dir.resolve("a.jsonl.done")); Files.writeString(dir.resolve("b.jsonl"),"x"); Files.createFile(dir.resolve("b.jsonl.done")); assertEquals(1, new LocalSealedFileScanner(dir,c,Clock.systemUTC()).scan().size()); }
}
