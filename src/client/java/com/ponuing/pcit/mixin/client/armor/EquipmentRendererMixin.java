package com.ponuing.pcit.mixin.client.armor;

import com.ponuing.pcit.client.NbtRenderOverrideResolver;
import com.ponuing.pcit.client.armor.ArmorCitResolver;
import com.ponuing.pcit.client.enchantment.EnchantmentContext;
import com.ponuing.pcit.client.enchantment.EnchantmentGlintResolver;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(net.minecraft.client.render.entity.equipment.EquipmentRenderer.class)
public class EquipmentRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V",
            at = @At("HEAD")
    )
    private void pcit$enchantment$startApplying(
            EquipmentModel.LayerType layerType,
            RegistryKey<EquipmentAsset> assetKey,
            Model model,
            ItemStack stack,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            Identifier playerTexture,
            CallbackInfo ci
    ) {
        if (!EnchantmentGlintResolver.active()) {
            return;
        }
        if (stack == null || stack.isEmpty()) {
            EnchantmentGlintResolver.setContext(null);
            return;
        }
        EnchantmentGlintResolver.setContext(new EnchantmentContext(stack, null, null, NbtRenderOverrideResolver.HandMatch.ANY));
        EnchantmentGlintResolver.apply();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V",
            at = @At("RETURN")
    )
    private void pcit$enchantment$stopApplying(
            EquipmentModel.LayerType layerType,
            RegistryKey<EquipmentAsset> assetKey,
            Model model,
            ItemStack stack,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            Identifier playerTexture,
            CallbackInfo ci
    ) {
        EnchantmentGlintResolver.setContext(null);
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0)
    )
    private Object pcit$overrideArmorTexture(
            Function<Object, Object> function,
            Object key,
            EquipmentModel.LayerType layerType,
            RegistryKey<EquipmentAsset> assetKey,
            Model model,
            ItemStack stack,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            Identifier playerTexture
    ) {
        Identifier original = (Identifier) function.apply(key);
        Identifier override = ArmorCitResolver.resolveArmorTextureOverride(stack, original);
        return override != null ? override : original;
    }
}
