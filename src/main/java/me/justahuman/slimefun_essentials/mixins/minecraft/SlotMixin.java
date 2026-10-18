package me.justahuman.slimefun_essentials.mixins.minecraft;

import me.justahuman.slimefun_essentials.config.ModConfig;
import me.justahuman.slimefun_essentials.utils.Utils;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow public abstract ItemStack getItem();

    @Inject(at = @At("HEAD"), method = "isHighlightable", cancellable = true)
    public void canBeHighlighted(CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.hideBackgroundTooltips()) {
            return;
        }

        final ItemStack itemStack = getItem();
        final String id = Utils.getSlimefunId(itemStack);
        if (id != null && Utils.HIDDEN_SF_IDS.contains(id)) {
            cir.setReturnValue(false);
        }
    }
}
