package com.ponuing.pcit.mixin.client.item;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
public interface ItemRenderStateAccessor {
    @Accessor("layers")
    ItemRenderState.LayerRenderState[] pcit$getLayers();

    @Accessor("layerCount")
    int pcit$getLayerCount();

    @Accessor("modelTransformationMode")
    void pcit$setModelTransformationMode(net.minecraft.item.ModelTransformationMode mode);

    @Accessor("leftHand")
    void pcit$setLeftHand(boolean leftHand);
}
