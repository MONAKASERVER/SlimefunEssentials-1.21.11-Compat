package me.justahuman.slimefun_essentials.client.payload;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A transport payload whose expensive registry deserialization must run on the
 * Minecraft client thread, not Netty's packet decoder thread.
 */
public interface SplitPayload extends CustomPacketPayload {
    byte[] data();

    String hash();

    void apply();
}
