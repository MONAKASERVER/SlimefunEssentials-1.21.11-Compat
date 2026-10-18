package me.justahuman.slimefun_essentials.client.payload;

import me.justahuman.slimefun_essentials.utils.Payloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CacheStatusPayload(byte mask) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, CacheStatusPayload> CODEC = StreamCodec.ofMember(
            (payload, buffer) -> buffer.writeByte(payload.mask),
            buffer -> new CacheStatusPayload(buffer.readByte())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return Payloads.CACHE_STATUS_CHANNEL;
    }
}
