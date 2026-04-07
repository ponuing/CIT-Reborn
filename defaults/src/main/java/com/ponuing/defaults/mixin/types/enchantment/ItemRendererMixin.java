package com.ponuing.defaults.mixin.types.enchantment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ponuing.cit.CITContext;
import com.ponuing.defaults.cit.types.TypeEnchantment;

import static com.ponuing.defaults.cit.types.TypeEnchantment.CONTAINER;

@Mixin(value = net.minecraft.client.render.item.ItemRenderer.class, priority = 200)
public class ItemRendererMixin {
    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V", at = @At("HEAD"))
    private void CITReborn$enchantment$startApplyingItem(ItemStack stack, ModelTransformationMode renderMode, int light, int overlay, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int seed, CallbackInfo ci) {
        if (!CONTAINER.active())
            return;
        CONTAINER.setContext(new CITContext(stack, world, null));
        CONTAINER.apply();
    }

    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V", at = @At("RETURN"))
    private void CITReborn$enchantment$stopApplyingItem(ItemStack stack, ModelTransformationMode renderMode, int light, int overlay, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int seed, CallbackInfo ci) {
        if (CONTAINER.active())
            CONTAINER.setContext(null);
    }

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V", at = @At("HEAD"))
    private void CITReborn$enchantment$startApplyingItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int light, int overlay, int seed, CallbackInfo ci) {
        if (!CONTAINER.active())
            return;
        CONTAINER.setContext(new CITContext(stack, world, entity));
        CONTAINER.apply();
    }

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V", at = @At("RETURN"))
    private void CITReborn$enchantment$stopApplyingItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int light, int overlay, int seed, CallbackInfo ci) {
        if (CONTAINER.active())
            CONTAINER.setContext(null);
    }

    @Inject(method = "getArmorGlintConsumer", cancellable = true, at = @At("RETURN"))
    private static void CITReborn$enchantment$getArmorGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, boolean glint, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!CONTAINER.shouldApply())
            return;

        VertexConsumer vertexConsumer = TypeEnchantment.GlintRenderLayer.ARMOR_ENTITY_GLINT.tryApply(cir.getReturnValue(), layer, provider);
        if (vertexConsumer != null)
            cir.setReturnValue(vertexConsumer);
    }

    @Inject(method = "getItemGlintConsumer", cancellable = true, at = @At("RETURN"))
    private static void CITReborn$enchantment$getItemGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, boolean solid, boolean glint, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!CONTAINER.shouldApply())
            return;
        VertexConsumer vertexConsumer = MinecraftClient.isFabulousGraphicsOrBetter() && layer == TexturedRenderLayers.getItemEntityTranslucentCull() ? TypeEnchantment.GlintRenderLayer.GLINT_TRANSLUCENT.tryApply(cir.getReturnValue(), layer, provider) : (solid ? TypeEnchantment.GlintRenderLayer.GLINT.tryApply(cir.getReturnValue(), layer, provider) : TypeEnchantment.GlintRenderLayer.ENTITY_GLINT.tryApply(cir.getReturnValue(), layer, provider));
        if (vertexConsumer != null)
            cir.setReturnValue(vertexConsumer);
    }

}
