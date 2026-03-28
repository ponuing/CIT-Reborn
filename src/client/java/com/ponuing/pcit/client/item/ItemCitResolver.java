package com.ponuing.pcit.client.item;

import com.ponuing.pcit.client.NbtRenderOverrideResolver;
import com.ponuing.pcit.client.cit.CitGeneratedModelDef;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ItemCitResolver {
    private ItemCitResolver() {
    }

    public enum HandMatch {
        ANY,
        MAIN,
        OFF;

        private NbtRenderOverrideResolver.HandMatch toInternal() {
            return switch (this) {
                case MAIN -> NbtRenderOverrideResolver.HandMatch.MAIN;
                case OFF -> NbtRenderOverrideResolver.HandMatch.OFF;
                case ANY -> NbtRenderOverrideResolver.HandMatch.ANY;
            };
        }
    }

    public record ItemOverride(
            Identifier modelId,
            Identifier textureId,
            Map<String, Identifier> namedTextures,
            Map<String, Identifier> namedModels
    ) {
    }

    public record GeneratedModelDef(Identifier parentModelId, Identifier textureId) {
    }

    public static ItemOverride resolveItemOverride(ItemStack stack, HandMatch hand) {
        NbtRenderOverrideResolver.ItemOverride internal = NbtRenderOverrideResolver.resolveItemOverride(stack, hand == null ? NbtRenderOverrideResolver.HandMatch.ANY : hand.toInternal());
        if (internal == null) {
            return null;
        }
        return new ItemOverride(
                internal.modelId(),
                internal.textureId(),
                internal.namedTextures(),
                internal.namedModels()
        );
    }

    public static ItemStack resolveItemStackForRender(ItemStack original) {
        return NbtRenderOverrideResolver.resolveItemStackForRender(original);
    }

    public static Sprite resolveSprite(Identifier textureId) {
        return NbtRenderOverrideResolver.resolveSprite(textureId);
    }

    public static Identifier toSpriteId(Identifier textureId) {
        return NbtRenderOverrideResolver.toSpriteId(textureId);
    }

    public static Sprite resolveModelTextureSprite(Identifier modelId) {
        return NbtRenderOverrideResolver.resolveModelTextureSprite(modelId);
    }

    public static BakedModel resolveModelBaked(Identifier modelId) {
        return NbtRenderOverrideResolver.resolveModelBaked(modelId);
    }

    public static Identifier resolveModelTextureId(Identifier modelId) {
        return NbtRenderOverrideResolver.resolveModelTextureId(modelId);
    }

    public static boolean matchesTextureName(Identifier spriteId, String nameKey) {
        return NbtRenderOverrideResolver.matchesTextureName(spriteId, nameKey);
    }

    public static void ensureLoaded() {
        NbtRenderOverrideResolver.ensureLoaded();
    }

    public static void reloadFromManager(ResourceManager manager) {
        NbtRenderOverrideResolver.reloadFromManager(manager);
    }

    public static Map<Identifier, GeneratedModelDef> getGeneratedItemModels() {
        Map<Identifier, CitGeneratedModelDef> internal = NbtRenderOverrideResolver.getGeneratedItemModels();
        if (internal == null || internal.isEmpty()) {
            return Map.of();
        }
        Map<Identifier, GeneratedModelDef> mapped = new HashMap<>();
        for (Map.Entry<Identifier, CitGeneratedModelDef> entry : internal.entrySet()) {
            CitGeneratedModelDef def = entry.getValue();
            mapped.put(entry.getKey(), new GeneratedModelDef(def.parentModelId(), def.textureId()));
        }
        return Map.copyOf(mapped);
    }

    public static Set<Identifier> getExtraItemModels() {
        return NbtRenderOverrideResolver.getExtraItemModels();
    }

    public static String normalizeOptifineModelJson(String relPath, String jsonText) {
        return NbtRenderOverrideResolver.normalizeOptifineModelJson(relPath, jsonText);
    }

    public static String normalizeOptifineModelJson(String relPath, String jsonText, String defaultNamespace) {
        return NbtRenderOverrideResolver.normalizeOptifineModelJson(relPath, jsonText, defaultNamespace);
    }
}
