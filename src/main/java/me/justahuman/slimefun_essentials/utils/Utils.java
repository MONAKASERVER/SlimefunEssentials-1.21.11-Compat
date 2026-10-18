package me.justahuman.slimefun_essentials.utils;

import me.justahuman.slimefun_essentials.client.SlimefunRecipe;
import me.justahuman.slimefun_essentials.client.SlimefunRegistry;
import me.justahuman.slimefun_essentials.mixins.minecraft.TextTooltipAccessor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[a-zA-Z_]+%");
    public static final Set<String> HIDDEN_SF_IDS = Set.of("_UI_BACKGROUND", "_UI_INPUT_SLOT", "_UI_OUTPUT_SLOT");

    public static boolean filterResources(Identifier identifier) {
        return identifier.getPath().endsWith(".json");
    }

    public static boolean filterVanillaItems(Identifier identifier) {
        if (!filterResources(identifier)) {
            return false;
        }

        final String path = identifier.getPath();
        final String item = getFileName(path);
        return SlimefunRegistry.getVanillaItems().contains(item);
    }

    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1, path.indexOf(".json"));
    }

    public static CompoundTag getPluginNbt(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return null;
        }
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag().getCompoundOrEmpty("PublicBukkitValues");
    }

    public static String getSlimefunId(@Nullable ItemStack itemStack) {
        final CompoundTag pluginNbt = getPluginNbt(itemStack);
        if (pluginNbt == null || !pluginNbt.contains("slimefun:slimefun_item")) {
            return null;
        }
        return pluginNbt.getString("slimefun:slimefun_item").orElse(null);
    }

    public static String getGuideMode(@Nullable ItemStack itemStack) {
        final CompoundTag pluginNbt = getPluginNbt(itemStack);
        if (pluginNbt == null || !pluginNbt.contains("slimefun:slimefun_guide_mode")) {
            return null;
        }
        return pluginNbt.getString("slimefun:slimefun_guide_mode").orElse(null);
    }

    public static String fillPlaceholders(String string, SlimefunRecipe recipe) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(string);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String placeholder = matcher.group();
            Object value = resolvePlaceholder(placeholder, recipe);
            matcher.appendReplacement(builder, value != null ? value.toString() : "");
        }
        return matcher.appendTail(builder).toString();
    }

    public static Object resolvePlaceholder(String string, SlimefunRecipe recipe) {
        return resolveNumberPlaceholder(string, recipe);
    }

    public static Number resolveNumberPlaceholder(String string, SlimefunRecipe recipe) {
        return resolveNumberPlaceholder(string, recipe, 0);
    }

    public static Number resolveNumberPlaceholder(String string, SlimefunRecipe recipe, Number defaultValue) {
        Number number = switch (string) {
            case "%ticks%" -> recipe.ticks();
            case "%sf_ticks%" -> recipe.sfTicks();
            case "%seconds%" -> recipe.seconds();
            case "%millis%" -> recipe.millis();
            case "%energy%" -> recipe.energy();
            case "%total_energy%" -> recipe.totalEnergy();
            case "%abs_total_energy%" -> recipe.totalEnergy() == null ? null : Math.abs(recipe.totalEnergy());
            case "%inputs%" -> recipe.inputs().size();
            case "%outputs%" -> recipe.outputs().size();
            default -> {
                Number value = defaultValue;
                try {
                    value = Double.parseDouble(string);
                } catch (NumberFormatException ignored) {}
                yield value != null && value.doubleValue() == value.intValue() ? value.intValue() : value;
            }
        };
        return number == null ? defaultValue : number;
    }

    public static List<ClientTooltipComponent> updateTooltip(List<ClientTooltipComponent> tooltip, SlimefunRecipe recipe) {
        if (tooltip.isEmpty()) {
            return tooltip;
        }

        List<String> strings = tooltip.stream().map(component -> from(((TextTooltipAccessor) component).getText())).map(string -> fillPlaceholders(string, recipe)).toList();
        List<ClientTooltipComponent> newTooltip = new ArrayList<>();
        strings.forEach(string -> newTooltip.add(ClientTooltipComponent.create(Component.literal(string).getVisualOrderText())));
        return newTooltip;
    }

    private static String from(FormattedCharSequence text) {
        final StringBuilder builder = new StringBuilder();
        text.accept((index, style, c) -> {
            builder.appendCodePoint(c);
            return true;
        });
        return builder.toString();
    }
}
