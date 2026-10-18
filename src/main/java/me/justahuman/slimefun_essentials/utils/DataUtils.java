package me.justahuman.slimefun_essentials.utils;

import com.google.common.io.ByteArrayDataInput;
import com.mojang.serialization.DynamicOps;
import me.justahuman.slimefun_essentials.SlimefunEssentials;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class DataUtils {
    public static ItemStack get(ByteArrayDataInput input) {
        final ItemStack itemStack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.tryParse(input.readUTF())));
        itemStack.setCount(input.readInt());

        try {
            if (input.readBoolean() && itemStack.getComponents() instanceof PatchedDataComponentMap components) {
                components.restorePatch(DataComponentPatch.CODEC.decode(withRegistryAccess(NbtOps.INSTANCE), TagParser.create(NbtOps.INSTANCE).parseFully(input.readUTF())).getOrThrow().getFirst());
            }
        } catch (Exception e) {
            SlimefunEssentials.LOGGER.error("Failed to deserialize item components", e);
        }
        return itemStack;
    }

    public static ItemStack get(ByteArrayDataInput input, ItemStack def) {
        if (!input.readBoolean()) {
            return def;
        }
        return get(input);
    }

    public static String get(ByteArrayDataInput input, String def) {
        return input.readBoolean() ? input.readUTF() : def;
    }

    public static Boolean get(ByteArrayDataInput input, Boolean def) {
        return input.readBoolean() ? input.readBoolean() : def;
    }

    public static Long get(ByteArrayDataInput input, Long def) {
        return input.readBoolean() ? Long.valueOf(input.readLong()) : def;
    }

    public static Integer get(ByteArrayDataInput input, Integer def) {
        return input.readBoolean() ? Integer.valueOf(input.readInt()) : def;
    }

    public static List<ClientTooltipComponent> getTooltip(ByteArrayDataInput input) {
        int size = input.readInt();
        if (size <= 0) {
            return List.of();
        }

        final List<ClientTooltipComponent> tooltip = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            tooltip.add(ClientTooltipComponent.create(Component.literal(input.readUTF()).getVisualOrderText()));
        }
        return tooltip;
    }

    private static <T> DynamicOps<T> withRegistryAccess(DynamicOps<T> ops) {
        Minecraft instance = Minecraft.getInstance();
        if (instance == null || instance.level == null) {
            return ops;
        }
        return instance.level.registryAccess().createSerializationContext(ops);
    }
}
