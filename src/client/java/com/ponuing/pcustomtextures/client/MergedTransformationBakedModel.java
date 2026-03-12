package com.ponuing.pcustomtextures.client;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.WrapperBakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;

public final class MergedTransformationBakedModel extends WrapperBakedModel {
    private final ModelTransformation baseTransformation;
    private final ModelTransformation overrideTransformation;

    public MergedTransformationBakedModel(BakedModel wrapped, ModelTransformation baseTransformation, ModelTransformation overrideTransformation) {
        super(wrapped);
        this.baseTransformation = baseTransformation;
        this.overrideTransformation = overrideTransformation;
    }

    @Override
    public ModelTransformation getTransformation() {
        if (baseTransformation == null) {
            return overrideTransformation != null ? overrideTransformation : super.getTransformation();
        }
        if (overrideTransformation == null) {
            return baseTransformation;
        }
        return new ModelTransformation(
                pick(baseTransformation.thirdPersonLeftHand(), overrideTransformation.thirdPersonLeftHand()),
                pick(baseTransformation.thirdPersonRightHand(), overrideTransformation.thirdPersonRightHand()),
                pick(baseTransformation.firstPersonLeftHand(), overrideTransformation.firstPersonLeftHand()),
                pick(baseTransformation.firstPersonRightHand(), overrideTransformation.firstPersonRightHand()),
                pick(baseTransformation.head(), overrideTransformation.head()),
                pick(baseTransformation.gui(), overrideTransformation.gui()),
                pick(baseTransformation.ground(), overrideTransformation.ground()),
                pick(baseTransformation.fixed(), overrideTransformation.fixed())
        );
    }

    private static Transformation pick(Transformation base, Transformation override) {
        if (override == null) {
            return base;
        }
        return Transformation.IDENTITY.equals(override) ? base : override;
    }
}
