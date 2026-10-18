package me.justahuman.slimefun_essentials.utils;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import me.justahuman.slimefun_essentials.SlimefunEssentials;
import me.justahuman.slimefun_essentials.client.RecipeCategory;
import me.justahuman.slimefun_essentials.api.DisplayComponentType;
import me.justahuman.slimefun_essentials.api.RecipeDisplay;
import me.justahuman.slimefun_essentials.client.SlimefunRegistry;
import me.justahuman.slimefun_essentials.client.payload.CacheStatusPayload;
import me.justahuman.slimefun_essentials.client.payload.ComponentTypePayload;
import me.justahuman.slimefun_essentials.client.payload.ItemsPayload;
import me.justahuman.slimefun_essentials.client.payload.LoadingStatePayload;
import me.justahuman.slimefun_essentials.client.payload.RecipeCategoriesPayload;
import me.justahuman.slimefun_essentials.client.payload.RecipeDisplayPayload;
import me.justahuman.slimefun_essentials.client.payload.SplitPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.zip.GZIPInputStream;

public class Payloads {
    public static final String PLUGIN_ID = "slimefun_server_essentials";
    public static final CustomPacketPayload.Type<ComponentTypePayload> COMPONENT_TYPE_CHANNEL = newChannel("component_types");
    public static final CustomPacketPayload.Type<ItemsPayload> ITEM_CHANNEL = newChannel("items");
    public static final CustomPacketPayload.Type<RecipeDisplayPayload> RECIPE_DISPLAY_CHANNEL = newChannel("recipe_displays");
    public static final CustomPacketPayload.Type<RecipeCategoriesPayload> RECIPE_CATEGORIES_CHANNEL = newChannel("recipe_categories");
    public static final CustomPacketPayload.Type<LoadingStatePayload> LOADING_STATE_CHANNEL = newChannel("loading_state");
    public static final CustomPacketPayload.Type<CacheStatusPayload> CACHE_STATUS_CHANNEL = newChannel("cache_status");

    private static final Type RECEIVING_TOAST_TYPE = new Type("receiving_expected_packets");
    private static final Type RECEIVED_TOAST_TYPE = new Type("received_expected_packets");

    private static final int MAX_MESSAGE_SIZE = 32766;
    private static final int SPLIT_MESSAGE_SIZE = MAX_MESSAGE_SIZE - 4 - 4 - 4;
    private static final int V2_MAGIC = 0x4D534532;
    private static final short V2_PROTOCOL = 2;
    private static final byte FLAG_GZIP = 1;
    private static final String CHUNK_MARKER = "__mona_sync_chunk__:";
    private static final int HASH_SIZE = 32;
    private static final int V2_HEADER_SIZE = Integer.BYTES + Short.BYTES + Byte.BYTES + Integer.BYTES * 5 + HASH_SIZE;
    private static final int MAX_UNCOMPRESSED_BYTES = 67_108_864;
    private static final int MAX_COMPRESSED_BYTES = 33_554_432;
    private static final int MAX_CHUNKS = 4_096;
    private static final int MAX_IN_FLIGHT_MESSAGES = 64;
    private static final long ASSEMBLY_TIMEOUT_NANOS = Duration.ofSeconds(30).toNanos();
    private static final Map<Class<?>, Integer> PACKET_COUNTS = new HashMap<>();
    private static final Map<Class<?>, Integer> EXPECTED_PACKETS = new HashMap<>();
    private static final Map<Class<?>, BiFunction<byte[], String, ? extends SplitPayload>> PAYLOAD_FACTORIES = new HashMap<>();
    private static final Map<Class<?>, Set<String>> CACHED_HASHES = new HashMap<>();
    private static Runnable onMeetExpected = showToast(RECEIVED_TOAST_TYPE);
    private static boolean metExpected = false;
    private static String activeManifest = "";

    private static Runnable showToast(Type type) {
        return () -> {
            Component title = Component.translatable("slimefun_essentials.toast." + type.type);
            Component description = Component.translatable("slimefun_essentials.toast." + type.type + ".description",
                    PACKET_COUNTS.getOrDefault(ComponentTypePayload.class, 0),
                    EXPECTED_PACKETS.getOrDefault(ComponentTypePayload.class, 0),
                    PACKET_COUNTS.getOrDefault(ItemsPayload.class, 0),
                    EXPECTED_PACKETS.getOrDefault(ItemsPayload.class, 0),
                    PACKET_COUNTS.getOrDefault(RecipeCategoriesPayload.class, 0),
                    EXPECTED_PACKETS.getOrDefault(RecipeCategoriesPayload.class, 0),
                    PACKET_COUNTS.getOrDefault(RecipeDisplayPayload.class, 0),
                    EXPECTED_PACKETS.getOrDefault(RecipeDisplayPayload.class, 0));
            ToastManager manager = Minecraft.getInstance().getToastManager();
            SystemToast existing = manager.getToast(SystemToast.class, type);
            if (existing != null) {
                existing.reset(title, description);
            } else {
                manager.addToast(new SystemToast(type, title, description));
            }
        };
    }

    public static void onMeetExpected(Runnable runnable) {
        Runnable previous = onMeetExpected;
        onMeetExpected = () -> {
            previous.run();
            runnable.run();
        };
    }

    public static boolean metExpected() {
        return metExpected;
    }

    public static void reset() {
        PACKET_COUNTS.clear();
        EXPECTED_PACKETS.clear();
        CACHED_HASHES.clear();
        activeManifest = "";
        onMeetExpected = showToast(RECEIVED_TOAST_TYPE);
        metExpected = false;
    }

    public static void expect(LoadingStatePayload payload) {
        String manifest = payload.serverId() + '|' + payload.typeHash() + '|' + payload.itemHash()
                + '|' + payload.categoryHash() + '|' + payload.displayHash();
        EXPECTED_PACKETS.clear();
        EXPECTED_PACKETS.put(ComponentTypePayload.class, payload.typePackets());
        EXPECTED_PACKETS.put(ItemsPayload.class, payload.itemPackets());
        EXPECTED_PACKETS.put(RecipeCategoriesPayload.class, payload.categoryPackets());
        EXPECTED_PACKETS.put(RecipeDisplayPayload.class, payload.displayPackets());
        SlimefunEssentials.LOGGER.info("Expecting {} type packets, {} item packets, {} category packets, and {} display packets",
                EXPECTED_PACKETS.get(ComponentTypePayload.class),
                EXPECTED_PACKETS.get(ItemsPayload.class),
                EXPECTED_PACKETS.get(RecipeCategoriesPayload.class),
                EXPECTED_PACKETS.get(RecipeDisplayPayload.class));

        if (payload.protocol() >= 2) {
            if (manifest.equals(activeManifest) && metExpected) {
                sendCacheStatus((byte) 0x0F);
                return;
            }
            activeManifest = manifest;
            metExpected = false;
            PACKET_COUNTS.clear();
            CACHED_HASHES.clear();
            RecipeCategory.clear();
            DisplayComponentType.clear();
            RecipeDisplay.clear();
            SlimefunRegistry.reset();
            SyncCache.activate(payload.serverId());

            byte cacheMask = 0;
            if (restoreCached(ComponentTypePayload.class, payload.typeHash(), payload.typePackets())) cacheMask |= 1;
            if (restoreCached(ItemsPayload.class, payload.itemHash(), payload.itemPackets())) cacheMask |= 1 << 1;
            if (restoreCached(RecipeCategoriesPayload.class, payload.categoryHash(), payload.categoryPackets())) cacheMask |= 1 << 2;
            if (restoreCached(RecipeDisplayPayload.class, payload.displayHash(), payload.displayPackets())) cacheMask |= 1 << 3;
            sendCacheStatus(cacheMask);
        }
        showToast(RECEIVING_TOAST_TYPE).run();
        checkMetExpected();
    }

    private static boolean restoreCached(Class<?> payloadType, String manifest, int expectedPackets) {
        if (manifest == null || manifest.isBlank()) {
            return false;
        }
        List<byte[]> cached = SyncCache.readAll(payloadType, manifest);
        String[] hashes = manifest.split(":", -1);
        if (cached.size() != hashes.length) {
            return false;
        }
        BiFunction<byte[], String, ? extends SplitPayload> factory = PAYLOAD_FACTORIES.get(payloadType);
        if (factory == null) {
            return false;
        }
        try {
            for (int i = 0; i < cached.size(); i++) {
                factory.apply(cached.get(i), hashes[i]).apply();
            }
            PACKET_COUNTS.put(payloadType, expectedPackets);
            CACHED_HASHES.put(payloadType, new HashSet<>(Arrays.asList(hashes)));
            return true;
        } catch (RuntimeException exception) {
            SlimefunEssentials.LOGGER.warn("Could not restore cached {} data", payloadType.getSimpleName(), exception);
            return false;
        }
    }

    public static void accept(SplitPayload payload) {
        String transportHash = payload.hash() == null ? "" : payload.hash();
        boolean partialChunk = transportHash.startsWith(CHUNK_MARKER);
        String contentHash = partialChunk ? transportHash.substring(CHUNK_MARKER.length()) : transportHash;
        if (payload.data() == null && !partialChunk) {
            return;
        }
        if (!contentHash.isBlank()
                && CACHED_HASHES.getOrDefault(payload.getClass(), Set.of()).contains(contentHash)) {
            return;
        }
        PACKET_COUNTS.merge(payload.getClass(), 1, Integer::sum);
        if (payload.data() != null) {
            if (payload.hash() != null && !payload.hash().isBlank()) {
                SyncCache.store(payload.getClass(), payload.hash(), payload.data());
            }
            payload.apply();
        }
        checkMetExpected();
    }

    private static void sendCacheStatus(byte mask) {
        if (mask != 0 && ClientPlayNetworking.canSend(CACHE_STATUS_CHANNEL)) {
            ClientPlayNetworking.send(new CacheStatusPayload(mask));
        }
    }

    public static void checkMetExpected() {
        showToast(RECEIVING_TOAST_TYPE).run();
        if (metExpected) {
            return;
        }
        for (Map.Entry<Class<?>, Integer> expected : EXPECTED_PACKETS.entrySet()) {
            if (PACKET_COUNTS.getOrDefault(expected.getKey(), 0) < expected.getValue()) {
                return;
            } else if (PACKET_COUNTS.getOrDefault(expected.getKey(), 0) > expected.getValue()) {
                SlimefunEssentials.LOGGER.warn("Received more packets than expected for {}", expected.getKey().getSimpleName());
            }
        }

        metExpected = !EXPECTED_PACKETS.isEmpty();
        if (metExpected) {
            SlimefunEssentials.LOGGER.info("Received every expected packet");
            RecipeCategory.finalizeCategories();
            onMeetExpected.run();
            onMeetExpected = showToast(RECEIVED_TOAST_TYPE);
        }
    }

    public static <P extends CustomPacketPayload> CustomPacketPayload.Type<P> newChannel(String channel) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(PLUGIN_ID, channel));
    }

    public static <P extends SplitPayload> StreamCodec<FriendlyByteBuf, P> newSplitCodec(
            BiFunction<byte[], String, P> factory, P empty) {
        PAYLOAD_FACTORIES.put(empty.getClass(), factory);
        return new StreamCodec<>() {
            private final Map<Integer, Assembly> received = new HashMap<>();

            @Override
            public void encode(FriendlyByteBuf buf, P value) {}

            @Override
            public P decode(FriendlyByteBuf buf) {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                try {
                    if (bytes.length >= V2_HEADER_SIZE && ByteBuffer.wrap(bytes).getInt() == V2_MAGIC) {
                        return decodeV2(bytes, factory, empty);
                    }
                    return decodeLegacy(bytes, factory, empty);
                } catch (RuntimeException exception) {
                    SlimefunEssentials.LOGGER.warn("Rejected invalid Mona Slimefun sync packet: {}", exception.getMessage());
                    return empty;
                }
            }

            private P decodeV2(byte[] bytes, BiFunction<byte[], String, P> factory, P empty) {
                cleanupExpired();
                ByteBuffer header = ByteBuffer.wrap(bytes);
                header.getInt();
                short protocol = header.getShort();
                byte flags = header.get();
                int id = header.getInt();
                int piece = header.getInt();
                int pieces = header.getInt();
                int rawLength = header.getInt();
                int encodedLength = header.getInt();
                byte[] expectedHash = new byte[HASH_SIZE];
                header.get(expectedHash);
                String hashString = HexFormat.of().formatHex(expectedHash);

                if (protocol != V2_PROTOCOL || (flags & ~FLAG_GZIP) != 0) {
                    throw new IllegalArgumentException("unsupported protocol or flags");
                }
                if (pieces < 1 || pieces > MAX_CHUNKS || piece < 0 || piece >= pieces) {
                    throw new IllegalArgumentException("invalid chunk index/count");
                }
                if (rawLength < 0 || rawLength > MAX_UNCOMPRESSED_BYTES
                        || encodedLength < 0 || encodedLength > MAX_COMPRESSED_BYTES) {
                    throw new IllegalArgumentException("payload length exceeds client limits");
                }
                if (received.size() >= MAX_IN_FLIGHT_MESSAGES && !received.containsKey(id)) {
                    throw new IllegalArgumentException("too many in-flight messages");
                }
                Assembly assembly = received.computeIfAbsent(id,
                        ignored -> new Assembly(flags, pieces, rawLength, encodedLength, expectedHash));
                if (!assembly.matches(flags, pieces, rawLength, encodedLength, expectedHash)) {
                    received.remove(id);
                    throw new IllegalArgumentException("chunk metadata mismatch");
                }
                assembly.put(piece, Arrays.copyOfRange(bytes, V2_HEADER_SIZE, bytes.length));
                if (!assembly.complete()) {
                    return factory.apply(null, CHUNK_MARKER + hashString);
                }

                received.remove(id);
                byte[] encoded = assembly.join();
                byte[] raw = (flags & FLAG_GZIP) != 0 ? gunzip(encoded, rawLength) : encoded;
                if (raw.length != rawLength || !MessageDigest.isEqual(sha256(raw), expectedHash)) {
                    throw new IllegalArgumentException("payload size or SHA-256 mismatch");
                }
                return factory.apply(raw, hashString);
            }

            private P decodeLegacy(byte[] bytes, BiFunction<byte[], String, P> factory, P empty) {
                if (bytes.length < 12) {
                    throw new IllegalArgumentException("legacy packet is shorter than its header");
                }
                ByteBuffer header = ByteBuffer.wrap(bytes);
                int id = header.getInt();
                int pieces = header.getInt();
                int piece = header.getInt();
                if (pieces < 1 || pieces > MAX_CHUNKS || piece < 0 || piece >= pieces) {
                    throw new IllegalArgumentException("invalid legacy chunk index/count");
                }

                Assembly assembly = received.computeIfAbsent(id,
                        ignored -> new Assembly((byte) 0, pieces, -1, -1, new byte[HASH_SIZE]));
                if (assembly.pieces.length != pieces) {
                    received.remove(id);
                    throw new IllegalArgumentException("legacy chunk count mismatch");
                }
                assembly.put(piece, Arrays.copyOfRange(bytes, 12, bytes.length));
                if (!assembly.complete()) {
                    return factory.apply(null, CHUNK_MARKER);
                }
                received.remove(id);
                return factory.apply(assembly.join(), "");
            }

            private void cleanupExpired() {
                long threshold = System.nanoTime() - ASSEMBLY_TIMEOUT_NANOS;
                received.entrySet().removeIf(entry -> entry.getValue().lastUpdateNanos < threshold);
            }

            private byte[] gunzip(byte[] encoded, int expectedLength) {
                try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(encoded));
                     ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(32, expectedLength))) {
                    byte[] buffer = new byte[8_192];
                    int total = 0;
                    for (int read; (read = gzip.read(buffer)) != -1;) {
                        total += read;
                        if (total > expectedLength || total > MAX_UNCOMPRESSED_BYTES) {
                            throw new IllegalArgumentException("decompressed payload exceeds declared size");
                        }
                        output.write(buffer, 0, read);
                    }
                    return output.toByteArray();
                } catch (IOException exception) {
                    throw new IllegalArgumentException("invalid gzip payload", exception);
                }
            }

            private byte[] sha256(byte[] data) {
                try {
                    return MessageDigest.getInstance("SHA-256").digest(data);
                } catch (NoSuchAlgorithmException impossible) {
                    throw new IllegalStateException("SHA-256 is unavailable", impossible);
                }
            }

            private final class Assembly {
                private final byte flags;
                private final byte[][] pieces;
                private final int rawLength;
                private final int encodedLength;
                private final byte[] hash;
                private long lastUpdateNanos = System.nanoTime();

                private Assembly(byte flags, int pieceCount, int rawLength, int encodedLength, byte[] hash) {
                    this.flags = flags;
                    this.pieces = new byte[pieceCount][];
                    this.rawLength = rawLength;
                    this.encodedLength = encodedLength;
                    this.hash = hash.clone();
                }

                private boolean matches(byte flags, int pieceCount, int rawLength, int encodedLength, byte[] hash) {
                    return this.flags == flags && this.pieces.length == pieceCount && this.rawLength == rawLength
                            && this.encodedLength == encodedLength && MessageDigest.isEqual(this.hash, hash);
                }

                private void put(int index, byte[] data) {
                    if (pieces[index] != null && !Arrays.equals(pieces[index], data)) {
                        throw new IllegalArgumentException("conflicting duplicate chunk");
                    }
                    pieces[index] = data;
                    lastUpdateNanos = System.nanoTime();
                }

                private boolean complete() {
                    return Arrays.stream(pieces).allMatch(piece -> piece != null);
                }

                private byte[] join() {
                    int length = Arrays.stream(pieces).mapToInt(piece -> piece.length).sum();
                    if (encodedLength >= 0 && length != encodedLength) {
                        throw new IllegalArgumentException("encoded payload length mismatch");
                    }
                    ByteArrayOutputStream output = new ByteArrayOutputStream(length);
                    for (byte[] piece : pieces) {
                        output.writeBytes(piece);
                    }
                    return output.toByteArray();
                }
            }
        };
    }

    public static <P extends CustomPacketPayload>StreamCodec<FriendlyByteBuf, P> newCodec(Function<ByteArrayDataInput, P> decoder) {
        return StreamCodec.ofMember((value, buf) -> {}, buf -> {
            byte[] bytes = new byte[buf.readableBytes()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = buf.readByte();
            }
            return decoder.apply(ByteStreams.newDataInput(bytes));
        });
    }

    private static class Type extends SystemToast.SystemToastId {
        final String type;

        public Type(String type) {
            this.type = type;
        }
    }
}
