package me.justahuman.slimefun_essentials.utils;

import me.justahuman.slimefun_essentials.SlimefunEssentials;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SyncCache {
    private static final long MAX_FILE_BYTES = 67_108_864L;
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mona-slimefun-cache");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile Path activeDirectory;

    private SyncCache() {}

    public static void activate(String serverId) {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        String address = server == null ? "singleplayer" : server.ip;
        String key = CacheIdentity.forServer(address, serverId);
        activeDirectory = FabricLoader.getInstance().getConfigDir()
                .resolve("slimefun_essentials").resolve("cache").resolve(key);
    }

    public static void store(Class<?> payloadType, String hash, byte[] data) {
        Path root = activeDirectory;
        if (root == null || data.length > MAX_FILE_BYTES || !validHash(hash)) {
            return;
        }
        byte[] copy = data.clone();
        WRITER.execute(() -> {
            Path file = root.resolve(payloadType.getSimpleName()).resolve(hash + ".bin");
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try {
                Files.createDirectories(file.getParent());
                Files.write(temporary, copy);
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException unsupportedAtomicMove) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException exception) {
                SlimefunEssentials.LOGGER.warn("Could not write sync cache {}", file, exception);
            }
        });
    }

    public static List<byte[]> readAll(Class<?> payloadType, String manifest) {
        Path root = activeDirectory;
        if (root == null || manifest == null || manifest.isBlank()) {
            return List.of();
        }
        String[] hashes = manifest.split(":", -1);
        List<byte[]> result = new ArrayList<>(hashes.length);
        try {
            for (String hash : hashes) {
                if (!validHash(hash)) {
                    return List.of();
                }
                Path file = root.resolve(payloadType.getSimpleName()).resolve(hash + ".bin");
                if (!Files.isRegularFile(file) || Files.size(file) > MAX_FILE_BYTES) {
                    return List.of();
                }
                byte[] data = Files.readAllBytes(file);
                if (!MessageDigest.isEqual(sha256(data), HexFormat.of().parseHex(hash))) {
                    Files.deleteIfExists(file);
                    return List.of();
                }
                result.add(data);
            }
            return result;
        } catch (IOException | IllegalArgumentException exception) {
            SlimefunEssentials.LOGGER.warn("Could not read sync cache", exception);
            return List.of();
        }
    }

    private static boolean validHash(String hash) {
        return hash != null && hash.matches("[0-9a-f]{64}");
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
