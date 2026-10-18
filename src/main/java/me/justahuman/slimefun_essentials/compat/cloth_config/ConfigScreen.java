package me.justahuman.slimefun_essentials.compat.cloth_config;

import me.justahuman.slimefun_essentials.config.ModConfig;
import me.justahuman.slimefun_essentials.utils.CompatUtils;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen {
    public static Screen buildScreen(Screen parent) {
        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("slimefun_essentials.title"));

        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        final ConfigCategory generalCategory = builder.getOrCreateCategory(Component.translatable("slimefun_essentials.config.category.general"));
        final ConfigCategory visualCategory = builder.getOrCreateCategory(Component.translatable("slimefun_essentials.config.category.visual"));

        /* General Config Options */

        generalCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("slimefun_essentials.config.option.recipe_features"), ModConfig.recipeFeatures())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("slimefun_essentials.config.option.recipe_features.tooltip"))
                .setRequirement(CompatUtils::isRecipeModLoaded)
                .setSaveConsumer(ModConfig::setRecipeFeatures)
                .build());

        /* Visual */

        visualCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("slimefun_essentials.config.option.replace_item_identifiers"), ModConfig.replaceItemIdentifiers())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("slimefun_essentials.config.option.replace_item_identifiers.tooltip"))
                .setSaveConsumer(ModConfig::setReplaceItemIdentifiers)
                .build());

        visualCategory.addEntry(entryBuilder.startBooleanToggle(Component.translatable("slimefun_essentials.config.option.hide_background_tooltips"), ModConfig.hideBackgroundTooltips())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("slimefun_essentials.config.option.hide_background_tooltips.tooltip"))
                .setSaveConsumer(ModConfig::setHideBackgroundTooltips)
                .build());

        builder.setSavingRunnable(ModConfig::saveConfig);

        return builder.build();
    }
}