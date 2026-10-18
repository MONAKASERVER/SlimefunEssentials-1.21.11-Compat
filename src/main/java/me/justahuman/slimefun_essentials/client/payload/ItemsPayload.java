package me.justahuman.slimefun_essentials.client.payload;

import me.justahuman.slimefun_essentials.SlimefunEssentials;
import me.justahuman.slimefun_essentials.client.SlimefunRegistry;
import me.justahuman.slimefun_essentials.utils.DataUtils;
import me.justahuman.slimefun_essentials.utils.Payloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashMap;
import java.util.Map;

public class ItemsPayload implements CustomPacketPayload {
    public static final ItemsPayload EMPTY = new ItemsPayload();
    public static final StreamCodec<FriendlyByteBuf, ItemsPayload> CODEC =
            Payloads.newSplitCodec(input -> {
                int dataVersion = input.readInt();
                if (dataVersion < 1 || dataVersion > 2) {
                    throw new IllegalArgumentException("Unsupported items data version: " + dataVersion);
                }
                int size = input.readInt();
                Map<String, ItemStack> items = new LinkedHashMap<>(size);
                for (int i = 0; i < size; i++) {
                    String id = input.readUTF();
                    try {
                        items.put(id, DataUtils.get(input));
                    } catch (Exception e) {
                        SlimefunEssentials.LOGGER.error("Failed to deserialize slimefun item: {}", id, e);
                    }
                }
                SlimefunRegistry.addItems(items);
                return EMPTY;
            }, EMPTY);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return Payloads.ITEM_CHANNEL;
    }
}
