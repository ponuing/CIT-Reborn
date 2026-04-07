package com.ponuing.defaults.mixin.types.armor;

import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ponuing.defaults.cit.types.TypeArmor;

@Mixin(EquipmentModel.Layer.class)
public class EquipmentModelLayerMixin {
    @Inject(method = "getFullTextureId", at = @At("HEAD"), cancellable = true)
    private void CITReborn$overrideArmorTexture(EquipmentModel.LayerType layerType, CallbackInfoReturnable<Identifier> cir) {
        EquipmentModel.Layer self = (EquipmentModel.Layer) (Object) this;
        Identifier override = TypeArmor.resolveActiveTexture(layerType, self.textureId());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
