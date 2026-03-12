package com.ponuing.pcustomtextures.client.item;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.WrapperBakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;

public final class TransformationOverrideBakedModel extends WrapperBakedModel {
    private final ModelTransformation transformation;

    public TransformationOverrideBakedModel(BakedModel wrapped, ModelTransformation transformation) {
        super(wrapped);
        this.transformation = transformation;
    }

    @Override
    public ModelTransformation getTransformation() {
        return transformation != null ? transformation : super.getTransformation();
    }
}
