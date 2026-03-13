package com.ponuing.pcit.client.item;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.WrapperBakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;

import java.lang.reflect.Method;

public final class MergedTransformationBakedModel extends WrapperBakedModel {
    private final ModelTransformation merged;

    public MergedTransformationBakedModel(BakedModel wrapped, ModelTransformation baseTransformation, ModelTransformation overrideTransformation) {
        super(wrapped);
        this.merged = mergeTransformations(baseTransformation, overrideTransformation);
    }

    @Override
    public ModelTransformation getTransformation() {
        return merged != null ? merged : super.getTransformation();
    }

    private static ModelTransformation mergeTransformations(ModelTransformation base, ModelTransformation override) {
        if (base == null) {
            return override;
        }
        if (override == null) {
            return base;
        }
        try {
            Method method = ModelTransformation.class.getMethod("with", ModelTransformation.class);
            Object merged = method.invoke(base, override);
            if (merged instanceof ModelTransformation transformation) {
                return transformation;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return override;
    }
}
