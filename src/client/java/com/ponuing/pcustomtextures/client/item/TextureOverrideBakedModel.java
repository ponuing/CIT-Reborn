package com.ponuing.pcustomtextures.client.item;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.WrapperBakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

public final class TextureOverrideBakedModel extends WrapperBakedModel {
    private final Sprite sprite;

    public TextureOverrideBakedModel(BakedModel wrapped, Sprite sprite) {
        super(wrapped);
        this.sprite = sprite;
    }

    @Override
    public List<BakedQuad> getQuads(net.minecraft.block.BlockState state, Direction face, Random random) {
        if (sprite == null) {
            return super.getQuads(state, face, random);
        }

        List<BakedQuad> original = super.getQuads(state, face, random);
        if (original.isEmpty()) {
            return original;
        }

        List<BakedQuad> replaced = new ArrayList<>(original.size());
        for (BakedQuad quad : original) {
            Sprite oldSprite = quad.getSprite();
            if (oldSprite == null || oldSprite == sprite) {
                replaced.add(quad);
                continue;
            }

            int[] remapped = remapUvs(quad.getVertexData(), oldSprite, sprite);
            replaced.add(new BakedQuad(
                    remapped,
                    quad.getTintIndex(),
                    quad.getFace(),
                    sprite,
                    quad.hasShade(),
                    quad.getLightEmission()
            ));
        }
        return replaced;
    }

    @Override
    public Sprite getParticleSprite() {
        return sprite != null ? sprite : super.getParticleSprite();
    }

    private static int[] remapUvs(int[] vertexData, Sprite from, Sprite to) {
        if (vertexData == null || vertexData.length == 0) {
            return vertexData;
        }
        int stride = vertexData.length / 4;
        if (stride < 6 || vertexData.length % 4 != 0) {
            return vertexData;
        }

        float fromMinU = from.getMinU();
        float fromMaxU = from.getMaxU();
        float fromMinV = from.getMinV();
        float fromMaxV = from.getMaxV();
        float toMinU = to.getMinU();
        float toMaxU = to.getMaxU();
        float toMinV = to.getMinV();
        float toMaxV = to.getMaxV();
        float fromDU = fromMaxU - fromMinU;
        float fromDV = fromMaxV - fromMinV;
        float toDU = toMaxU - toMinU;
        float toDV = toMaxV - toMinV;

        int[] remapped = vertexData.clone();
        for (int v = 0; v < 4; v++) {
            int base = v * stride;
            int uIndex = base + 4;
            int vIndex = base + 5;
            float u = Float.intBitsToFloat(remapped[uIndex]);
            float vCoord = Float.intBitsToFloat(remapped[vIndex]);
            float uNorm = fromDU == 0.0f ? 0.0f : (u - fromMinU) / fromDU;
            float vNorm = fromDV == 0.0f ? 0.0f : (vCoord - fromMinV) / fromDV;
            float newU = toMinU + uNorm * toDU;
            float newV = toMinV + vNorm * toDV;
            remapped[uIndex] = Float.floatToRawIntBits(newU);
            remapped[vIndex] = Float.floatToRawIntBits(newV);
        }
        return remapped;
    }
}
