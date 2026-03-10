package com.ponuing.pcustomtextures.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import com.ponuing.pcustomtextures.Pcustomtextures;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PcustomtexturesModelLoadingPlugin {
    private PcustomtexturesModelLoadingPlugin() {
    }

    public static void register() {
        ModelLoadingPlugin.register(PcustomtexturesModelLoadingPlugin::initialize);
    }

    private static void initialize(ModelLoadingPlugin.Context context) {
        final Map<Identifier, NbtRenderOverrideResolver.GeneratedModelDef>[] modelsRef = new Map[]{NbtRenderOverrideResolver.getGeneratedItemModels()};
        final Set<Identifier>[] extraModelsRef = new Set[]{NbtRenderOverrideResolver.getExtraItemModels()};
        if (modelsRef[0].isEmpty() && extraModelsRef[0].isEmpty()) {
            NbtRenderOverrideResolver.ensureLoaded();
            modelsRef[0] = NbtRenderOverrideResolver.getGeneratedItemModels();
            extraModelsRef[0] = NbtRenderOverrideResolver.getExtraItemModels();
        }
        if (modelsRef[0].isEmpty() && extraModelsRef[0].isEmpty()) {
            return;
        }

        Set<Identifier> allModels = new HashSet<>(modelsRef[0].keySet());
        allModels.addAll(extraModelsRef[0]);
        context.addModels(allModels);
        context.modifyModelOnLoad().register(ModelModifier.OVERRIDE_PHASE, (model, ctx) -> {
            Identifier id = ctx.id();
            NbtRenderOverrideResolver.GeneratedModelDef def = modelsRef[0].get(id);
            if (def == null) {
                if (!extraModelsRef[0].contains(id)) {
                    return model;
                }
                UnbakedModel optifineModel = loadOptifineModel(id);
                return optifineModel != null ? optifineModel : model;
            }

            String parentString = def.parentModelId().toString();
            String textureString = toModelTextureString(def.textureId());
            String json = "{\"parent\":\"" + parentString + "\",\"textures\":{\"layer0\":\"" + textureString + "\"}}";
            //Pcustomtextures.LOGGER.info("[pcustomtextures][model] build model {} parent={} texture={}", id, parentString, textureString);
            UnbakedModel parsed = UnbakedModelDeserializer.deserialize(new StringReader(json));
            return parsed != null ? parsed : model;
        });
    }

    private static String toModelTextureString(Identifier textureId) {
        String path = textureId.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return textureId.getNamespace() + ":" + path;
    }

    private static UnbakedModel loadOptifineModel(Identifier id) {
        if (id == null) {
            return null;
        }
        String path = id.getPath();
        if (!(path.startsWith("optifine/") || path.startsWith("cit/"))) {
            return null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        ResourceManager manager = client.getResourceManager();
        if (manager == null) {
            return null;
        }
        Identifier resourceId = Identifier.of(id.getNamespace(), path + ".json");
        Resource resource = manager.getResource(resourceId).orElse(null);
        if (resource == null) {
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            String jsonText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            jsonText = NbtRenderOverrideResolver.normalizeOptifineModelJson(path, jsonText);
            return UnbakedModelDeserializer.deserialize(new StringReader(jsonText));
        } catch (Exception e) {
            Pcustomtextures.LOGGER.warn("[pcustomtextures][model] failed to load optifine model {}", id, e);
            return null;
        }
    }
}
