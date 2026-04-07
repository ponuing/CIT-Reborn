package com.ponuing.defaults.mixin.types.item;

import net.minecraft.client.render.model.GroupableModel;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.ModelIdentifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelBaker.class)
public interface ModelBakerAccessor {
    @Accessor("blockModels")
    Map<ModelIdentifier, GroupableModel> CITReborn$getBlockModels();

    @Mutable
    @Accessor("blockModels")
    void CITReborn$setBlockModels(Map<ModelIdentifier, GroupableModel> blockModels);
}
