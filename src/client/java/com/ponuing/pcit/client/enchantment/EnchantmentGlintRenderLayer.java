package com.ponuing.pcit.client.enchantment;

import com.ponuing.pcit.client.NbtRenderOverrideResolver;
import com.ponuing.pcit.mixin.client.enchantment.RenderPhaseAccessor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class EnchantmentGlintRenderLayer {
    private static final Map<CacheKey, RenderLayer> CACHE = new ConcurrentHashMap<>();

    private EnchantmentGlintRenderLayer() {
    }

    public static VertexConsumer apply(VertexConsumerProvider provider, RenderLayer baseLayer, NbtRenderOverrideResolver.EnchantmentOverride override, EnchantmentLayer layer) {
        if (provider == null || override == null || override.textureId() == null) {
            return null;
        }
        if (!matchesLayer(override.layers(), layer)) {
            return null;
        }
        RenderLayer glintLayer = getLayer(override.textureId(), layer, override.blend());
        if (glintLayer == null) {
            return null;
        }
        VertexConsumer glintConsumer = provider.getBuffer(glintLayer);
        if (baseLayer == null) {
            return glintConsumer;
        }
        return VertexConsumers.union(glintConsumer, provider.getBuffer(baseLayer));
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static boolean matchesLayer(Set<EnchantmentLayer> layers, EnchantmentLayer layer) {
        if (layer == null) {
            return true;
        }
        return layers == null || layers.isEmpty() || layers.contains(layer);
    }

    private static RenderLayer getLayer(Identifier textureId, EnchantmentLayer layer, EnchantmentBlend blend) {
        CacheKey key = new CacheKey(textureId, layer, blend == null ? EnchantmentBlend.GLINT : blend);
        return CACHE.computeIfAbsent(key, EnchantmentGlintRenderLayer::buildLayer);
    }

    private static RenderLayer buildLayer(CacheKey key) {
        RenderPhase.ShaderProgram program = switch (key.layer) {
            case ARMOR -> RenderPhaseAccessor.pcit$armorEntityGlintProgram();
            case ENTITY -> RenderPhaseAccessor.pcit$entityGlintProgram();
            case GLINT_TRANSLUCENT -> RenderPhaseAccessor.pcit$translucentGlintProgram();
            case GLINT -> RenderPhaseAccessor.pcit$glintProgram();
        };
        RenderPhase.Texturing texturing = switch (key.layer) {
            case ENTITY, ARMOR -> RenderPhaseAccessor.pcit$entityGlintTexturing();
            case GLINT_TRANSLUCENT, GLINT -> RenderPhaseAccessor.pcit$glintTexturing();
        };
        RenderPhase.Transparency transparency = resolveTransparency(key.blend);

        RenderLayer.MultiPhaseParameters params = RenderLayer.MultiPhaseParameters.builder()
                .program(program)
                .texture(new RenderPhase.Texture(key.textureId, TriState.TRUE, false))
                .transparency(transparency)
                .depthTest(RenderPhaseAccessor.pcit$equalDepthTest())
                .cull(RenderPhaseAccessor.pcit$disableCulling())
                .writeMaskState(RenderPhaseAccessor.pcit$colorMask())
                .texturing(texturing)
                .layering(RenderPhaseAccessor.pcit$viewOffsetZLayering())
                .target(RenderPhaseAccessor.pcit$itemEntityTarget())
                .build(false);

        String name = "pcit_glint_" + key.layer.name().toLowerCase(Locale.ROOT) + "_" + sanitizeTextureId(key.textureId);
        return RenderLayer.of(name, VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, true, params);
    }

    private static RenderPhase.Transparency resolveTransparency(EnchantmentBlend blend) {
        if (blend == null) {
            return RenderPhaseAccessor.pcit$glintTransparency();
        }
        return switch (blend) {
            case ADDITIVE -> RenderPhaseAccessor.pcit$additiveTransparency();
            case TRANSLUCENT -> RenderPhaseAccessor.pcit$translucentTransparency();
            case SOLID -> RenderPhaseAccessor.pcit$noTransparency();
            case GLINT -> RenderPhaseAccessor.pcit$glintTransparency();
        };
    }

    private static String sanitizeTextureId(Identifier id) {
        String path = id.getNamespace() + "_" + id.getPath();
        return path.replace('/', '_').replace(':', '_');
    }

    private record CacheKey(Identifier textureId, EnchantmentLayer layer, EnchantmentBlend blend) {
    }
}
