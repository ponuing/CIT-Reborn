package com.ponuing.defaults.mixin.types.elytra;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.ponuing.cit.CIT;
import com.ponuing.cit.CITContext;
import com.ponuing.defaults.cit.types.TypeElytra;

import static com.ponuing.defaults.cit.types.TypeElytra.CONTAINER;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin {
    @ModifyVariable(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
        at = @At(value = "STORE"),
        ordinal = 0
    )
    public Identifier CITReborn$overrideCapeElytra(Identifier used, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, BipedEntityRenderState state, float f, float g) {
        if (!CONTAINER.active())
            return used;

        ItemStack equippedStack = state.equippedChestStack;
        if (equippedStack == null || !equippedStack.isOf(Items.ELYTRA))
            return used;

        CIT<TypeElytra> cit = CONTAINER.getCIT(new CITContext(equippedStack, null, null));
        return cit == null ? used : cit.type.texture;
    }
}
