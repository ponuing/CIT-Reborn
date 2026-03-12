package com.ponuing.pcustomtextures.mixin.client.elytra;

import com.ponuing.pcustomtextures.client.elytra.ElytraCitResolver;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin {
    @Inject(method = "getTexture(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    private static void pcustomtextures$overrideElytraTexture(BipedEntityRenderState state, CallbackInfoReturnable<Identifier> cir) {
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
