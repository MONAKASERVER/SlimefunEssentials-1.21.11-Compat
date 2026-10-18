package me.justahuman.slimefun_essentials.client.payload;

import me.justahuman.slimefun_essentials.utils.Payloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LoadingStatePayload(int typePackets, int itemPackets, int categoryPackets, int displayPackets,
                                  int protocol, String serverId, String typeHash, String itemHash,
                                  String categoryHash, String displayHash) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, LoadingStatePayload> CODEC = Payloads.newCodec(input -> {
        int typePackets = input.readInt();
        int itemPackets = input.readInt();
        int categoryPackets = input.readInt();
        int displayPackets = input.readInt();
        try {
            int protocol = input.readInt();
            if (protocol >= 2) {
                return new LoadingStatePayload(typePackets, itemPackets, categoryPackets, displayPackets,
                        protocol, input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF());
            }
        } catch (RuntimeException ignored) {
            // Protocol v1 ended after the four packet counters.
        }
        return new LoadingStatePayload(typePackets, itemPackets, categoryPackets, displayPackets,
                1, "", "", "", "", "");
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return Payloads.LOADING_STATE_CHANNEL;
    }
}
