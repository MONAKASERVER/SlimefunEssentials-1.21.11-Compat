package me.justahuman.slimefun_essentials.compat.jei;

import me.justahuman.slimefun_essentials.client.SlimefunRecipe;
import me.justahuman.slimefun_essentials.client.payload.RecipeTransferPayload;
import me.justahuman.slimefun_essentials.utils.Payloads;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Adds JEI's + button to the 3x3 crafting area inside Networks' 6-row grid GUI. */
public final class NetworksCraftingGridTransferHandler implements IRecipeTransferHandler<ChestMenu, SlimefunRecipe> {
    private static final int CRAFT_BUTTON_SLOT = 34;

    private final IRecipeType<SlimefunRecipe> recipeType;
    private final IRecipeTransferHandlerHelper helper;

    public NetworksCraftingGridTransferHandler(IRecipeType<SlimefunRecipe> recipeType,
                                                IRecipeTransferHandlerHelper helper) {
        this.recipeType = recipeType;
        this.helper = helper;
    }

    @Override
    public Class<? extends ChestMenu> getContainerClass() {
        return ChestMenu.class;
    }

    @Override
    public Optional<MenuType<ChestMenu>> getMenuType() {
        return Optional.of(MenuType.GENERIC_9x6);
    }

    @Override
    public IRecipeType<SlimefunRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(ChestMenu container,
                                                          SlimefunRecipe recipe,
                                                          IRecipeSlotsView recipeSlots,
                                                          Player player,
                                                          boolean maxTransfer,
                                                          boolean doTransfer) {
        if (container.slots.size() <= CRAFT_BUTTON_SLOT
                || !container.getSlot(CRAFT_BUTTON_SLOT).getItem().is(Items.CRAFTING_TABLE)) {
            return helper.createUserErrorWithTooltip(Component.literal("Networks Crafting Gridを開いてください"));
        }
        int width = recipe.width() == null ? 3 : recipe.width();
        int height = recipe.height() == null ? 3 : recipe.height();
        if (width > 3 || height > 3 || recipe.inputs().size() > 9) {
            return helper.createUserErrorWithTooltip(Component.literal("このレシピは3x3材料枠には入りません"));
        }
        if (!ClientPlayNetworking.canSend(Payloads.RECIPE_TRANSFER_CHANNEL)) {
            return helper.createUserErrorWithTooltip(Component.literal("サーバーが自動配置に対応していません"));
        }

        int recipeIndex = recipe.parent().childRecipes().indexOf(recipe);
        if (recipeIndex < 0) {
            return helper.createInternalError();
        }
        if (doTransfer) {
            ClientPlayNetworking.send(new RecipeTransferPayload(recipe.parent().id(), recipeIndex, maxTransfer));
        }
        return null;
    }
}
