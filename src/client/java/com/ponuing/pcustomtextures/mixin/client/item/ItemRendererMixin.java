package com.ponuing.pcustomtextures.mixin.client.item;

import com.ponuing.pcustomtextures.client.item.ItemCitResolver;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private ItemStack pcustomtextures$overrideItemStack(ItemStack original) {
        return ItemCitResolver.resolveItemStackForRender(original);
    }

    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private ItemStack pcustomtextures$overrideItemStackFromLiving(ItemStack original) {
        return ItemCitResolver.resolveItemStackForRender(original);
    }
}
