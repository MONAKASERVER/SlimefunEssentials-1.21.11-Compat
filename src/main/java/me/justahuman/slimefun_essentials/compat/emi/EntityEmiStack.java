package me.justahuman.slimefun_essentials.compat.emi;

import com.mojang.blaze3d.platform.Lighting;
import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.tooltip.RemainderTooltipComponent;
import me.justahuman.slimefun_essentials.mixins.minecraft.OrbAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
public class EntityEmiStack extends EmiStack {
    private final EntityType<?> type;
    private final @Nullable Entity entity;
    private final boolean baby;
    private final float scale;

    public EntityEmiStack(EntityType<?> type, boolean baby) {
        this.type = type;
        this.entity = type.create(Minecraft.getInstance().level, EntitySpawnReason.COMMAND);
        this.baby = baby && this.entity instanceof Mob;

        if (this.baby) {
            this.scale = 12.0F;
            ((Mob) this.entity).setBaby(true);
        } else if (this.entity instanceof ExperienceOrb orb) {
            this.scale = 20.0F;
            ((OrbAccessor) orb).setAmount(2477);
        } else {
            this.scale = 8.0F;
        }
    }

    @Override
    public EmiStack copy() {
        EntityEmiStack stack = new EntityEmiStack(this.type, this.baby);
        stack.setRemainder(getRemainder().copy());
        stack.comparison = this.comparison;
        return stack;
    }

    public boolean isLarge() {
        return !this.baby && !(this.entity instanceof ExperienceOrb);
    }

    @Override
    public boolean isEmpty() {
        return entity == null;
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        if (entity != null) {
            MouseHandler mouse = Minecraft.getInstance().mouseHandler;
            if (entity instanceof LivingEntity living) {
                drawLivingEntity(draw, x, this.baby ? y - 3 : y, scale, (float) mouse.xpos(), (float) mouse.ypos(), living);
            } else {
                drawEntity(draw, x, y, scale, (float) mouse.xpos(), (float) mouse.ypos(), entity);
            }

            if (this.amount > 1) {
                EmiRenderHelper.renderAmount(EmiDrawContext.wrap(draw), x, y, Component.literal(String.valueOf(this.amount)));
            }
        }
    }

    @Override
    public DataComponentPatch getComponentChanges() {
        return DataComponentPatch.EMPTY;
    }

    @Override
    public Object getKey() {
        return entity;
    }

    @Override
    public Identifier getId() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(this.type);
    }

    @Override
    public List<Component> getTooltipText() {
        return List.of(getName());
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        final List<ClientTooltipComponent> list = new ArrayList<>();
        if (this.entity != null) {
            list.addAll(getTooltipText().stream().map(EmiPort::ordered).map(ClientTooltipComponent::of).toList());
            final String mod;
            if (this.entity instanceof Villager villager) {
                mod = EmiUtil.getModName(BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).getNamespace());
            } else {
                mod = EmiUtil.getModName(BuiltInRegistries.ENTITY_TYPE.getKey(this.entity.getType()).getNamespace());
            }
            list.add(ClientTooltipComponent.create(EmiPort.ordered(EmiPort.literal(mod, ChatFormatting.BLUE, ChatFormatting.ITALIC))));
            if (!getRemainder().isEmpty()) {
                list.add(new RemainderTooltipComponent(this));
            }
        }
        return list;
    }

    @Override
    public Component getName() {
        return entity != null ? entity.getName() : EmiPort.literal("yet another missingno");
    }

    public static void drawLivingEntity(GuiGraphics ctx, int x, int y, float size, float mouseX, float mouseY, LivingEntity entity) {
        float mouseX0 = (ctx.guiWidth() + 51) - mouseX;
        float mouseY0 = (ctx.guiHeight() + 75 - 50) - mouseY;
        float f = (float) Math.atan(mouseX0 / 40.0F);
        float g = (float) Math.atan(mouseY0 / 40.0F);
        Quaternionf quaternionf = (new Quaternionf()).rotateZ(3.1415927F);
        Quaternionf quaternionf2 = (new Quaternionf()).rotateX(g * 20.0F * 0.017453292F);
        quaternionf.mul(quaternionf2);
        float h = entity.yBodyRot;
        float i = entity.getYRot();
        float j = entity.getXRot();
        float k = entity.prevHeadYaw;
        float l = entity.yHeadRot;
        entity.yBodyRot = 180.0F + f * 20.0F;
        entity.setYRot(180.0F + f * 40.0F);
        entity.setXRot(-g * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.prevHeadYaw = entity.getYRot();
        draw(ctx, x, y, size, quaternionf, quaternionf2, entity);
        entity.yBodyRot = h;
        entity.setYRot(i);
        entity.setXRot(j);
        entity.prevHeadYaw = k;
        entity.yHeadRot = l;
    }

    public static void drawEntity(GuiGraphics ctx, int x, int y, float size, float mouseX, float mouseY, Entity entity) {
        float mouseX0 = (ctx.guiWidth() + 51) - mouseX;
        float mouseY0 = (ctx.guiHeight() + 75 - 50) - mouseY;
        float f = (float) Math.atan(mouseX0 / 40.0F);
        float g = (float) Math.atan(mouseY0 / 40.0F);
        Quaternionf quaternionf = (new Quaternionf()).rotateZ(3.1415927F);
        Quaternionf quaternionf2 = (new Quaternionf()).rotateX(g * 20.0F * 0.017453292F);
        quaternionf.mul(quaternionf2);
        float i = entity.getYRot();
        float j = entity.getXRot();
        entity.setYRot(180.0F + f * 40.0F);
        entity.setXRot(-g * 20.0F);
        draw(ctx, x, y, size, quaternionf, quaternionf2, entity);
        entity.setYRot(i);
        entity.setXRot(j);
    }

    @SuppressWarnings("deprecation")
    private static void draw(GuiGraphics ctx, int x, int y, float size, Quaternionf quaternion, @Nullable Quaternionf quaternion2, Entity entity) {
        ctx.pose().push();
        ctx.pose().translate(x + 8, y + 16, 50.0);
        ctx.pose().multiplyPositionMatrix((new Matrix4f()).scaling(size, size, -size));
        ctx.pose().multiply(quaternion);
        Lighting.method_34742();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (quaternion2 != null) {
            quaternion2.conjugate();
            dispatcher.setRotation(quaternion2);
        }

        dispatcher.setRenderShadows(false);
        ctx.draw(vertexConsumers -> dispatcher.render(entity, 0.0, 0.0, 0.0, 1.0F, ctx.getMatrices(), vertexConsumers, 15728880));
        dispatcher.setRenderShadows(true);
        ctx.pose().pop();
        Lighting.enableGuiDepthLighting();
    }
}