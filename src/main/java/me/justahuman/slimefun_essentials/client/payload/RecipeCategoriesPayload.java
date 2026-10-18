package me.justahuman.slimefun_essentials.client.payload;

import me.justahuman.slimefun_essentials.client.RecipeCategory;
import me.justahuman.slimefun_essentials.utils.Payloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class RecipeCategoriesPayload implements CustomPacketPayload {
    private static final RecipeCategoriesPayload EMPTY = new RecipeCategoriesPayload();
    public static final StreamCodec<FriendlyByteBuf, RecipeCategoriesPayload> CODEC =
            Payloads.newSplitCodec(input -> {
                int dataVersion = input.readInt();
                if (dataVersion < 1 || dataVersion > 2) {
                    throw new IllegalArgumentException("Unsupported recipe data version: " + dataVersion);
                }
                int size = input.readInt();
                for (int i = 0; i < size; i++) {
                    RecipeCategory.deserialize(input, dataVersion);
                }
                Payloads.checkMetExpected();
                return EMPTY;
            }, EMPTY);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return Payloads.RECIPE_CATEGORIES_CHANNEL;
    }
}
