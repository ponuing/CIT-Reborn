package com.ponuing.pcustomtextures.mixin.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface ItemRenderLayerStateAccessor {
    @Accessor("model")
    BakedModel pcustomtextures$getModel();

    @Accessor("renderLayer")
    RenderLayer pcustomtextures$getRenderLayer();

    @Accessor("specialModelType")
    SpecialModelRenderer<?> pcustomtextures$getSpecialModelType();

    @Accessor("specialModelType")
    void pcustomtextures$setSpecialModelType(SpecialModelRenderer<?> renderer);

    @Accessor("data")
    Object pcustomtextures$getSpecialModelData();

    @Accessor("data")
    void pcustomtextures$setSpecialModelData(Object data);
}
