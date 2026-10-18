package me.justahuman.slimefun_essentials.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class CacheIdentity {
    private CacheIdentity() {}

    public static String forServer(String address, String serverId) {
        return HexFormat.of().formatHex(sha256((address + '\0' + serverId).getBytes(StandardCharsets.UTF_8)));
    }

    public static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
