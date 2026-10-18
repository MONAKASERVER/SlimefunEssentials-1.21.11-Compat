package me.justahuman.slimefun_essentials;

import me.justahuman.slimefun_essentials.api.DisplayComponentType;
import me.justahuman.slimefun_essentials.api.RecipeDisplay;
import me.justahuman.slimefun_essentials.client.RecipeCategory;
import me.justahuman.slimefun_essentials.client.SlimefunRegistry;
import me.justahuman.slimefun_essentials.client.payload.ComponentTypePayload;
import me.justahuman.slimefun_essentials.client.payload.CacheStatusPayload;
import me.justahuman.slimefun_essentials.client.payload.ItemsPayload;
import me.justahuman.slimefun_essentials.client.payload.LoadingStatePayload;
import me.justahuman.slimefun_essentials.client.payload.RecipeCategoriesPayload;
import me.justahuman.slimefun_essentials.client.payload.RecipeDisplayPayload;
import me.justahuman.slimefun_essentials.client.payload.SplitPayload;
import me.justahuman.slimefun_essentials.compat.cloth_config.ConfigScreen;
import me.justahuman.slimefun_essentials.config.ModConfig;
import me.justahuman.slimefun_essentials.utils.Payloads;
import me.justahuman.slimefun_essentials.utils.CompatUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class SlimefunEssentials implements ClientModInitializer {
    public static final String MOD_ID = "slimefun_essentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.clientboundPlay().register(Payloads.LOADING_STATE_CHANNEL, LoadingStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.ITEM_CHANNEL, ItemsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.COMPONENT_TYPE_CHANNEL, ComponentTypePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.RECIPE_CATEGORIES_CHANNEL, RecipeCategoriesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.RECIPE_DISPLAY_CHANNEL, RecipeDisplayPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Payloads.CACHE_STATUS_CHANNEL, CacheStatusPayload.CODEC);
        ModConfig.loadConfig();

        ClientPlayNetworking.registerGlobalReceiver(Payloads.LOADING_STATE_CHANNEL,
                (payload, context) -> context.client().execute(() -> Payloads.expect(payload)));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ITEM_CHANNEL, SlimefunEssentials::receive);
        ClientPlayNetworking.registerGlobalReceiver(Payloads.COMPONENT_TYPE_CHANNEL, SlimefunEssentials::receive);
        ClientPlayNetworking.registerGlobalReceiver(Payloads.RECIPE_CATEGORIES_CHANNEL, SlimefunEssentials::receive);
        ClientPlayNetworking.registerGlobalReceiver(Payloads.RECIPE_DISPLAY_CHANNEL, SlimefunEssentials::receive);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RecipeCategory.clear();
            DisplayComponentType.clear();
            RecipeDisplay.clear();
            SlimefunRegistry.reset();
            Payloads.reset();
        });

        if (CompatUtils.isClothConfigLoaded()) {
            final KeyMapping.Category category = KeyMapping.Category.register(id("controls"));
            final KeyMapping keyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping("slimefun_essentials.key_bind.open_config", GLFW.GLFW_KEY_F6, category));
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (keyBinding.isDown()) {
                    client.setScreen(ConfigScreen.buildScreen(client.screen));
                }
            });
        }
    }

    private static <P extends SplitPayload> void receive(P payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> Payloads.accept(payload));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path.toLowerCase(Locale.ROOT));
    }
}
