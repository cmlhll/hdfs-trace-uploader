package com.company.traceuploader.metadata;
import java.io.*;import java.nio.file.*;import java.security.*;import java.util.HexFormat;
public class ChecksumService {
    public String sha256(Path path) throws IOException { try { MessageDigest md=MessageDigest.getInstance("SHA-256"); try(InputStream in=Files.newInputStream(path)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0) md.update(b,0,n);} return "sha256:"+HexFormat.of().formatHex(md.digest()); } catch(NoSuchAlgorithmException e){ throw new IllegalStateException(e);} }
}
