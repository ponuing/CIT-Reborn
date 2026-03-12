package com.ponuing.pcustomtextures.mixin.client.item;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
public interface ItemRenderStateAccessor {
    @Accessor("layers")
    ItemRenderState.LayerRenderState[] pcustomtextures$getLayers();

    @Accessor("layerCount")
    int pcustomtextures$getLayerCount();

    @Accessor("modelTransformationMode")
    void pcustomtextures$setModelTransformationMode(net.minecraft.item.ModelTransformationMode mode);

    @Accessor("leftHand")
    void pcustomtextures$setLeftHand(boolean leftHand);
}
