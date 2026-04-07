package com.ponuing.defaults.mixin.types.armor;

import net.minecraft.client.model.Model;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.ponuing.cit.CIT;
import com.ponuing.cit.CITContext;
import com.ponuing.defaults.cit.types.TypeArmor;

@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureRendererMixin {
    @Redirect(
        method = "renderArmor(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;ILnet/minecraft/client/render/entity/model/BipedEntityModel;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/equipment/EquipmentRenderer;render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
        )
    )
    private void CITReborn$renderArmor(EquipmentRenderer renderer,
                                       EquipmentModel.LayerType layerType,
                                       RegistryKey<EquipmentAsset> assetId,
                                       Model model,
                                       ItemStack stack,
                                       MatrixStack matrices,
                                       VertexConsumerProvider vertices,
                                       int light,
                                       MatrixStack ignoredMatrices,
                                       VertexConsumerProvider ignoredVertices,
                                       ItemStack ignoredStack,
                                       EquipmentSlot slot,
                                       int ignoredLight,
                                       net.minecraft.client.render.entity.model.BipedEntityModel<?> ignoredModel) {
        if (TypeArmor.CONTAINER.active()) {
            CIT<TypeArmor> cit = TypeArmor.CONTAINER.getCIT(new CITContext(stack, null, null));
            if (cit != null) {
                TypeArmor.pushActiveTextures(cit.type.textures);
                try {
                    renderer.render(layerType, assetId, model, stack, matrices, vertices, light);
                } finally {
                    TypeArmor.clearActiveTextures();
                }
                return;
            }
        }

        renderer.render(layerType, assetId, model, stack, matrices, vertices, light);
    }
}
