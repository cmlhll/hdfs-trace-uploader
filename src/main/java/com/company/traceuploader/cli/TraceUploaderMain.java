package com.company.traceuploader.cli;

import com.company.traceuploader.commit.*;import com.company.traceuploader.config.*;import com.company.traceuploader.hdfs.*;import com.company.traceuploader.manifest.*;import com.company.traceuploader.metadata.*;import com.company.traceuploader.model.TraceFileMetadata;import com.company.traceuploader.scanner.*;import com.company.traceuploader.state.*;
import java.nio.file.*;import java.time.Clock;import java.util.*;

public class TraceUploaderMain {
    public static void main(String[] args) throws Exception { new TraceUploaderMain().run(args); }
    void run(String[] args) throws Exception {
        Cli cli=Cli.parse(args); AgentConfig config=new ConfigLoader().load(cli.configPath);
        System.out.printf("Loaded config %s dryRun=%s once=%s%n", cli.configPath, cli.dryRun, cli.once);
        Files.createDirectories(Path.of(config.localSpool.sealedDir)); Files.createDirectories(Path.of(config.localSpool.stateDir));
        SealedFileScanner scanner=new LocalSealedFileScanner(Path.of(config.localSpool.sealedDir), config.scanner, Clock.systemUTC());
        if(cli.dryRun){ List<SealedFile> files=scanner.scan(); System.out.printf("Dry run discovered %d sealed files%n", files.size()); for(SealedFile f:files) System.out.println("Would process "+f.dataPath()); return; }
        do { processOnce(config, scanner); if(cli.once) break; Thread.sleep(config.scanner.scanIntervalSeconds*1000); } while(true);
    }
    private void processOnce(AgentConfig config, SealedFileScanner scanner) throws Exception {
        Path wal=Path.of(config.localSpool.stateDir,"upload-state.jsonl"); try(JsonlWalUploadStateStore store=new JsonlWalUploadStateStore(wal)){
            HdfsClient hdfs=createHdfs(config); ManifestWriter manifest=new LocalJsonlManifestWriter(Path.of(config.manifest.localPath), config); CommitProtocol commit=new LocalFsCommitProtocol(hdfs, store, manifest, config); MetadataService metadata=new MetadataService(config, new ChecksumService(), new FileIdGenerator());
            for(SealedFile f:scanner.scan()) { try { TraceFileMetadata m=metadata.build(f,1); UploadFileRecord r=UploadFileRecordFactory.fromMetadata(m, UploadState.DISCOVERED, 0); store.upsert(r); store.updateState(m.fileId(), UploadState.SEALED, null); CommitResult result=commit.commit(m); System.out.printf("%s -> %s (%s)%n", m.localPath(), result.state(), result.message()); } catch(Exception e){ System.err.println("Failed to process "+f.dataPath()+": "+e); e.printStackTrace(System.err); } }
        }
    }
    private HdfsClient createHdfs(AgentConfig config) throws Exception { if(!"localfs".equalsIgnoreCase(config.hdfs.implementation)) throw new IllegalArgumentException("Only localfs HDFS implementation is supported in this phase: "+config.hdfs.implementation); return new LocalFsHdfsClient(Path.of(config.hdfs.localRootForTesting)); }
    static class Cli { Path configPath; boolean dryRun, once; static Cli parse(String[] args){ Cli c=new Cli(); for(int i=0;i<args.length;i++){ switch(args[i]){ case "--config" -> c.configPath=Path.of(args[++i]); case "--dry-run" -> c.dryRun=true; case "--once" -> c.once=true; default -> throw new IllegalArgumentException("Unknown argument: "+args[i]); } } if(c.configPath==null) throw new IllegalArgumentException("--config is required"); return c; } }
}
