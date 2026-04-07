package com.ponuing.defaults.mixin.common;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.ModelTextures;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ponuing.cit.CITType;
import com.ponuing.defaults.common.RebornItemModelIdentifier;
import com.ponuing.defaults.mixin.types.item.JsonUnbakedModelAccessor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Restores legacy relative path handling for packed CIT models in 1.21.4+.
 */
@Mixin(targets = "net.minecraft.client.render.model.ModelBaker$BakerImpl")
public class ModelLoaderMixin {
    @Inject(method = "getModel", cancellable = true, at = @At("HEAD"))
    private void CITReborn$loadPackedModel(Identifier originalId, CallbackInfoReturnable<UnbakedModel> cir) {
        if (!RebornItemModelIdentifier.marked(originalId))
            return;

        Identifier id = RebornItemModelIdentifier.unpack(originalId);
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();

        try (InputStream is = resourceManager.getResource(id).orElseThrow().getInputStream();
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonUnbakedModel json = JsonUnbakedModel.deserialize(reader);

            JsonUnbakedModel resolved = resolveModelTextures(json, id, resourceManager);
            resolved = resolveRelativeParent(resolved, id, resourceManager);

            cir.setReturnValue(resolved);
        } catch (Exception ignored) {
        }
    }

    private static JsonUnbakedModel resolveRelativeParent(JsonUnbakedModel model, Identifier id,
                                                           ResourceManager resourceManager) {
        JsonUnbakedModelAccessor accessor = (JsonUnbakedModelAccessor) model;
        Identifier parentId = accessor.CITReborn$getParentId();
        if (parentId == null)
            return model;

        String parentPath = parentId.getPath();
        if (!isRelativeCitPath(parentPath))
            return model;

        Identifier resolvedParent;
        try {
            resolvedParent = CITType.resolveAsset(id, parentPath, "models", ".json", resourceManager);
        } catch (Exception ignored) {
            resolvedParent = null;
        }
        if (resolvedParent == null)
            return model;

        accessor.CITReborn$setParentId(RebornItemModelIdentifier.pack(resolvedParent));
        return model;
    }

    private static boolean isRelativeCitPath(String path) {
        if (path == null)
            return false;
        if (path.startsWith("./"))
            return true;
        String[] split = path.split("/");
        return split.length > 2 && "cit".equals(split[1]);
    }

    private static JsonUnbakedModel resolveModelTextures(JsonUnbakedModel model, Identifier modelIdentifier,
                                                         ResourceManager resourceManager) {
        ModelTextures.Textures original = model.getTextures();
        if (original == null || original.values().isEmpty())
            return model;

        ModelTextures.Textures.Builder builder = new ModelTextures.Textures.Builder();
        boolean changed = false;

        for (Map.Entry<String, ModelTextures.Entry> entry : original.values().entrySet()) {
            String key = entry.getKey();
            ModelTextures.Entry value = entry.getValue();
            if (value == null)
                continue;

            SpriteIdentifier sprite = tryGetSprite(value);
            if (sprite != null) {
                String texturePath = sprite.getTextureId().getPath();
                if (isRelativeCitPath(texturePath)) {
                    Identifier resolved;
                    try {
                        resolved = CITType.resolveAsset(modelIdentifier, texturePath, "textures", ".png", resourceManager);
                    } catch (Exception ignored) {
                        resolved = null;
                    }
                    if (resolved != null) {
                        SpriteIdentifier updated = new SpriteIdentifier(sprite.getAtlasId(), resolved);
                        builder.addSprite(key, updated);
                        if (!updated.equals(sprite))
                            changed = true;
                        continue;
                    }
                }
                builder.addSprite(key, sprite);
                continue;
            }

            String target = tryGetTarget(value);
            if (target != null)
                builder.addTextureReference(key, target);
        }

        if (!changed)
            return model;

        ModelTextures.Textures resolved = builder.build();
        return copyModelWithTextures(model, resolved);
    }

    private static JsonUnbakedModel copyModelWithTextures(JsonUnbakedModel base, ModelTextures.Textures textures) {
        JsonUnbakedModelAccessor accessor = (JsonUnbakedModelAccessor) base;
        List<ModelElement> elements = new ArrayList<>(accessor.CITReborn$getElements());
        JsonUnbakedModel copy = new JsonUnbakedModel(
                accessor.CITReborn$getParentId(),
                elements,
                textures,
                accessor.CITReborn$getAmbientOcclusion(),
                accessor.CITReborn$getGuiLight(),
                accessor.CITReborn$getTransformations()
        );
        ((JsonUnbakedModelAccessor) copy).CITReborn$setParent(base.getParent());
        return copy;
    }

    private static Method SPRITE_ENTRY_MATERIAL;
    private static Method TEXTURE_ENTRY_TARGET;

    private static SpriteIdentifier tryGetSprite(ModelTextures.Entry entry) {
        if (entry == null)
            return null;
        try {
            if (SPRITE_ENTRY_MATERIAL == null)
                SPRITE_ENTRY_MATERIAL = entry.getClass().getMethod("material");
            Object sprite = SPRITE_ENTRY_MATERIAL.invoke(entry);
            return sprite instanceof SpriteIdentifier si ? si : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String tryGetTarget(ModelTextures.Entry entry) {
        if (entry == null)
            return null;
        try {
            if (TEXTURE_ENTRY_TARGET == null)
                TEXTURE_ENTRY_TARGET = entry.getClass().getMethod("target");
            Object target = TEXTURE_ENTRY_TARGET.invoke(entry);
            return target instanceof String s ? s : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
