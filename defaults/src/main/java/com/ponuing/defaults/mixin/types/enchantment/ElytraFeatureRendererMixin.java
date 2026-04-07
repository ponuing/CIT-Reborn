package com.ponuing.defaults.mixin.types.enchantment;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ponuing.cit.CITContext;

import static com.ponuing.defaults.cit.types.TypeEnchantment.CONTAINER;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V", at = @At("HEAD"))
    private void CITReborn$enchantment$setAppliedContextAndStartApplyingElytra(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, BipedEntityRenderState state, float f, float g, CallbackInfo ci) {
        if (CONTAINER.active()) {
            ItemStack stack = state.equippedChestStack;
            if (stack != null)
                CONTAINER.setContext(new CITContext(stack, null, null)).apply();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V", at = @At("RETURN"))
    private void CITReborn$enchantment$stopApplyingElytra(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, BipedEntityRenderState state, float f, float g, CallbackInfo ci) {
        if (CONTAINER.active())
            CONTAINER.setContext(null);
    }
}
