package me.justahuman.slimefun_essentials.mixins.minecraft;

import me.justahuman.slimefun_essentials.config.ModConfig;
import me.justahuman.slimefun_essentials.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Locale;

@Mixin(value = ItemStack.class, priority = 100000)
public abstract class ItemStackMixin {
    @Shadow public abstract Item getItem();

    @Inject(method = "getTooltipLines", at = @At(value = "RETURN"))
    public void changeTooltip(Item.TooltipContext context, Player player, TooltipFlag type, CallbackInfoReturnable<List<Component>> cir) {
        final ItemStack stack = (ItemStack) (Object) this;
        final String guideMode = Utils.getGuideMode(stack);
        final String id = guideMode == null ? Utils.getSlimefunId(stack) : guideMode + "_guide";
        if (id == null) {
            return;
        }

        final List<Component> lore = cir.getReturnValue();
        if (ModConfig.hideBackgroundTooltips() && Utils.HIDDEN_SF_IDS.contains(id)) {
            lore.clear();
            return;
        }

        if (!ModConfig.replaceItemIdentifiers()) {
            return;
        }

        final Identifier identifier = BuiltInRegistries.ITEM.getKey(getItem());
        final String idLine = identifier.toString();
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i).getString();
            if (line.equals(idLine)) {
                lore.set(i, Component.literal("slimefun:" + id.toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_GRAY));
            } else if (line.equals("Minecraft")) {
                lore.set(i, Component.literal("Slimefun").withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC));
            }
        }
    }
}
