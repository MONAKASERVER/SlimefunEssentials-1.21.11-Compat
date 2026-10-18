package me.justahuman.slimefun_essentials.client.payload;

import me.justahuman.slimefun_essentials.api.DisplayComponentType;
import me.justahuman.slimefun_essentials.utils.Payloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class ComponentTypePayload implements CustomPacketPayload {
    private static final ComponentTypePayload EMPTY = new ComponentTypePayload();
    public static final StreamCodec<FriendlyByteBuf, ComponentTypePayload> CODEC =
            Payloads.newSplitCodec(input -> {
                int dataVersion = input.readInt();
                if (dataVersion < 1 || dataVersion > 2) {
                    throw new IllegalArgumentException("Unsupported component data version: " + dataVersion);
                }
                DisplayComponentType.deserialize(input);
                Payloads.checkMetExpected();
                return EMPTY;
            }, EMPTY);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return Payloads.COMPONENT_TYPE_CHANNEL;
    }
}
