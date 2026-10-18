package me.justahuman.slimefun_essentials.client.payload;

import com.google.common.io.ByteStreams;
import me.justahuman.slimefun_essentials.client.RecipeCategory;
import me.justahuman.slimefun_essentials.utils.Payloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RecipeCategoriesPayload(byte[] data, String hash) implements SplitPayload {
    private static final RecipeCategoriesPayload EMPTY = new RecipeCategoriesPayload(null, "");
    public static final StreamCodec<FriendlyByteBuf, RecipeCategoriesPayload> CODEC =
            Payloads.newSplitCodec(RecipeCategoriesPayload::new, EMPTY);

    @Override
    public void apply() {
        if (data == null) {
            return;
        }
        var input = ByteStreams.newDataInput(data);
        int dataVersion = input.readInt();
        if (dataVersion < 1 || dataVersion > 2) {
            throw new IllegalArgumentException("Unsupported recipe data version: " + dataVersion);
        }
        int size = input.readInt();
        for (int i = 0; i < size; i++) {
            RecipeCategory.deserialize(input, dataVersion);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return Payloads.RECIPE_CATEGORIES_CHANNEL;
    }
}
