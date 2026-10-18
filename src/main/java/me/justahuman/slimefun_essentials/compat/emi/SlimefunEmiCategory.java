package me.justahuman.slimefun_essentials.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class SlimefunEmiCategory extends EmiRecipeCategory {
    private final Component displayName;
    
    public SlimefunEmiCategory(Identifier id, EmiStack workstation) {
        super(id, workstation);
        this.displayName = workstation.getItemStack().getName();
    }

    @Override
    public Component getName() {
        return this.displayName;
    }
}
