package com.ponuing.pcit.mixin.client.item;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface ItemRenderLayerStateAccessor {
    @Accessor("model")
    BakedModel pcit$getModel();

    @Accessor("renderLayer")
    RenderLayer pcit$getRenderLayer();

    @Accessor("specialModelType")
    SpecialModelRenderer<?> pcit$getSpecialModelType();

    @Accessor("specialModelType")
    void pcit$setSpecialModelType(SpecialModelRenderer<?> renderer);

    @Accessor("data")
    Object pcit$getSpecialModelData();

    @Accessor("data")
    void pcit$setSpecialModelData(Object data);
}
