package me.justahuman.slimefun_essentials.client.payload;

import me.justahuman.slimefun_essentials.utils.Payloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RecipeTransferPayload(String categoryId, int recipeIndex, boolean maxTransfer)
        implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, RecipeTransferPayload> CODEC = StreamCodec.ofMember(
            (payload, buffer) -> {
                buffer.writeUtf(payload.categoryId, 128);
                buffer.writeVarInt(payload.recipeIndex);
                buffer.writeBoolean(payload.maxTransfer);
            },
            buffer -> new RecipeTransferPayload(buffer.readUtf(128), buffer.readVarInt(), buffer.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return Payloads.RECIPE_TRANSFER_CHANNEL;
    }
}
