package com.ponuing.defaults.mixin.types.item;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.ModelIdentifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ponuing.defaults.cit.types.TypeItem;

@Mixin(BakedModelManager.class)
public class BakedModelManagerMixin implements TypeItem.BakedModelManagerMixinAccess {
    private BakedModel CITReborn$forcedMojankModel = null;

    @Inject(method = "getModel", cancellable = true, at =
    @At("HEAD"))
    private void CITReborn$getCITMojankModel(ModelIdentifier id, CallbackInfoReturnable<BakedModel> cir) {
        if (CITReborn$forcedMojankModel != null) {
            cir.setReturnValue(CITReborn$forcedMojankModel);
            CITReborn$forcedMojankModel = null;
        }
    }

    @Override
    public void CITReborn$forceMojankModel(BakedModel model) {
        this.CITReborn$forcedMojankModel = model;
    }
}
