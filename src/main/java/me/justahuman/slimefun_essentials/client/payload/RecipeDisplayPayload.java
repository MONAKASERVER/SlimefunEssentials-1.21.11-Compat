package me.justahuman.slimefun_essentials.client.payload;

import com.google.common.io.ByteStreams;
import me.justahuman.slimefun_essentials.client.display.BasicDisplay;
import me.justahuman.slimefun_essentials.utils.Payloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RecipeDisplayPayload(byte[] data, String hash) implements SplitPayload {
    private static final RecipeDisplayPayload EMPTY = new RecipeDisplayPayload(null, "");
    public static final StreamCodec<FriendlyByteBuf, RecipeDisplayPayload> CODEC =
            Payloads.newSplitCodec(RecipeDisplayPayload::new, EMPTY);

    @Override
    public void apply() {
        if (data == null) {
            return;
        }
        var input = ByteStreams.newDataInput(data);
        int dataVersion = input.readInt();
        if (dataVersion < 1 || dataVersion > 2) {
            throw new IllegalArgumentException("Unsupported display data version: " + dataVersion);
        }
        BasicDisplay.deserialize(input);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return Payloads.RECIPE_DISPLAY_CHANNEL;
    }
}
