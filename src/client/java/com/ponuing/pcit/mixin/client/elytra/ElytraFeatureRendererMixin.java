package com.ponuing.pcit.mixin.client.elytra;

import com.ponuing.pcit.client.NbtRenderOverrideResolver;
import com.ponuing.pcit.client.elytra.ElytraCitResolver;
import com.ponuing.pcit.client.enchantment.EnchantmentContext;
import com.ponuing.pcit.client.enchantment.EnchantmentGlintResolver;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("HEAD")
    )
    private void pcit$enchantment$startApplying(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float tickDelta, float animationProgress, CallbackInfo ci) {
        if (!EnchantmentGlintResolver.active()) {
            return;
        }
        if (state == null) {
            EnchantmentGlintResolver.setContext(null);
            return;
        }
        ItemStack stack = state.equippedChestStack;
        if (stack == null || stack.isEmpty()) {
            EnchantmentGlintResolver.setContext(null);
            return;
        }
        EnchantmentGlintResolver.setContext(new EnchantmentContext(stack, null, null, NbtRenderOverrideResolver.HandMatch.ANY));
        EnchantmentGlintResolver.apply();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("RETURN")
    )
    private void pcit$enchantment$stopApplying(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float tickDelta, float animationProgress, CallbackInfo ci) {
        EnchantmentGlintResolver.setContext(null);
    }

    @Inject(method = "getTexture(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    private static void pcit$overrideElytraTexture(BipedEntityRenderState state, CallbackInfoReturnable<Identifier> cir) {
        if (state == null) {
            return;
        }
        ItemStack stack = state.equippedChestStack;
        if (stack == null || !stack.isOf(Items.ELYTRA)) {
            return;
        }
        Identifier override = ElytraCitResolver.resolveElytraTextureOverride(stack);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
