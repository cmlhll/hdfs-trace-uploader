package com.company.traceuploader.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ChecksumService {
    public String sha256(Path path) throws IOException {
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[1024 * 1024];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return "sha256:" + bytesToHex(digest.digest());
    }

    public static String sha256Hex(String value) {
        MessageDigest digest = newDigest();
        return bytesToHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
