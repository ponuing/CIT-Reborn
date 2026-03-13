package com.ponuing.pcit.mixin.client.item;

import com.ponuing.pcit.client.NbtRenderOverrideResolver;
import com.ponuing.pcit.client.enchantment.EnchantmentContext;
import com.ponuing.pcit.client.enchantment.EnchantmentGlintRenderLayer;
import com.ponuing.pcit.client.enchantment.EnchantmentGlintResolver;
import com.ponuing.pcit.client.enchantment.EnchantmentLayer;
import com.ponuing.pcit.client.item.ItemCitResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private ItemStack pcit$overrideItemStack(ItemStack original) {
        return ItemCitResolver.resolveItemStackForRender(original);
    }

    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private ItemStack pcit$overrideItemStackFromLiving(ItemStack original) {
        return ItemCitResolver.resolveItemStackForRender(original);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("HEAD")
    )
    private void pcit$enchantment$startApplying(LivingEntity entity, ItemStack stack, ModelTransformationMode mode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int light, int overlay, int seed, CallbackInfo ci) {
        if (!EnchantmentGlintResolver.active()) {
            return;
        }
        if (stack == null || stack.isEmpty()) {
            EnchantmentGlintResolver.setContext(null);
            return;
        }
        NbtRenderOverrideResolver.HandMatch handMatch = NbtRenderOverrideResolver.HandMatch.ANY;
        if (mode == ModelTransformationMode.GUI) {
            handMatch = NbtRenderOverrideResolver.HandMatch.MAIN;
        } else {
            handMatch = leftHanded ? NbtRenderOverrideResolver.HandMatch.OFF : NbtRenderOverrideResolver.HandMatch.MAIN;
        }
        EnchantmentGlintResolver.setContext(new EnchantmentContext(stack, world, entity, handMatch));
        EnchantmentGlintResolver.apply();
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("RETURN")
    )
    private void pcit$enchantment$stopApplying(LivingEntity entity, ItemStack stack, ModelTransformationMode mode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int light, int overlay, int seed, CallbackInfo ci) {
        EnchantmentGlintResolver.setContext(null);
    }

    @Inject(method = "getArmorGlintConsumer", cancellable = true, at = @At("RETURN"))
    private static void pcit$enchantment$getArmorGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, boolean glint, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!glint || !EnchantmentGlintResolver.shouldApply()) {
            return;
        }
        VertexConsumer consumer = EnchantmentGlintRenderLayer.apply(provider, layer, EnchantmentGlintResolver.getActiveOverride(), EnchantmentLayer.ARMOR);
        if (consumer != null) {
            cir.setReturnValue(consumer);
        }
    }

    @Inject(method = "getDynamicDisplayGlintConsumer", cancellable = true, at = @At("RETURN"))
    private static void pcit$enchantment$getDynamicDisplayGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, MatrixStack.Entry entry, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!EnchantmentGlintResolver.shouldApply()) {
            return;
        }
        VertexConsumer glintConsumer = EnchantmentGlintRenderLayer.apply(provider, null, EnchantmentGlintResolver.getActiveOverride(), EnchantmentLayer.GLINT);
        if (glintConsumer != null) {
            cir.setReturnValue(VertexConsumers.union(new OverlayVertexConsumer(glintConsumer, entry, 1f), cir.getReturnValue()));
        }
    }

    @Inject(method = "getItemGlintConsumer", cancellable = true, at = @At("RETURN"))
    private static void pcit$enchantment$getItemGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, boolean solid, boolean glint, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!glint || !EnchantmentGlintResolver.shouldApply()) {
            return;
        }
        EnchantmentLayer target = MinecraftClient.isFabulousGraphicsOrBetter() && layer == TexturedRenderLayers.getItemEntityTranslucentCull()
                ? EnchantmentLayer.GLINT_TRANSLUCENT
                : (solid ? EnchantmentLayer.GLINT : EnchantmentLayer.ENTITY);
        VertexConsumer consumer = EnchantmentGlintRenderLayer.apply(provider, layer, EnchantmentGlintResolver.getActiveOverride(), target);
        if (consumer != null) {
            cir.setReturnValue(consumer);
        }
    }
}
