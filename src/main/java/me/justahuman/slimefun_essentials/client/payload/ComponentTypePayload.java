package me.justahuman.slimefun_essentials.client.payload;

import com.google.common.io.ByteStreams;
import me.justahuman.slimefun_essentials.api.DisplayComponentType;
import me.justahuman.slimefun_essentials.utils.Payloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ComponentTypePayload(byte[] data, String hash) implements SplitPayload {
    private static final ComponentTypePayload EMPTY = new ComponentTypePayload(null, "");
    public static final StreamCodec<FriendlyByteBuf, ComponentTypePayload> CODEC =
            Payloads.newSplitCodec(ComponentTypePayload::new, EMPTY);

    @Override
    public void apply() {
        if (data == null) {
            return;
        }
        var input = ByteStreams.newDataInput(data);
        int dataVersion = input.readInt();
        if (dataVersion < 1 || dataVersion > 2) {
            throw new IllegalArgumentException("Unsupported component data version: " + dataVersion);
        }
        DisplayComponentType.deserialize(input);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return Payloads.COMPONENT_TYPE_CHANNEL;
    }
}
