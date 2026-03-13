package com.ponuing.pcit.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import com.ponuing.pcit.PCIT;
import com.ponuing.pcit.client.item.ItemCitResolver;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PCITModelLoadingPlugin {
    private PCITModelLoadingPlugin() {
    }

    public static void register() {
        ModelLoadingPlugin.register(PCITModelLoadingPlugin::initialize);
    }

    private static void initialize(ModelLoadingPlugin.Context context) {
        final Map<Identifier, ItemCitResolver.GeneratedModelDef>[] modelsRef = new Map[]{ItemCitResolver.getGeneratedItemModels()};
        final Set<Identifier>[] extraModelsRef = new Set[]{ItemCitResolver.getExtraItemModels()};
        if (modelsRef[0].isEmpty() && extraModelsRef[0].isEmpty()) {
            ItemCitResolver.ensureLoaded();
            modelsRef[0] = ItemCitResolver.getGeneratedItemModels();
            extraModelsRef[0] = ItemCitResolver.getExtraItemModels();
        }
        if (modelsRef[0].isEmpty() && extraModelsRef[0].isEmpty()) {
            return;
        }

        ResourceManager manager = MinecraftClient.getInstance() != null ? MinecraftClient.getInstance().getResourceManager() : null;
        Set<Identifier> extraModels = manager != null ? filterExistingModels(extraModelsRef[0], manager) : extraModelsRef[0];
        Set<Identifier> allModels = new HashSet<>(modelsRef[0].keySet());
        allModels.addAll(extraModels);
        context.addModels(allModels);
        context.modifyModelOnLoad().register(ModelModifier.OVERRIDE_PHASE, (model, ctx) -> {
            Identifier id = ctx.id();
            ItemCitResolver.GeneratedModelDef def = modelsRef[0].get(id);
            if (def == null) {
                if (!extraModels.contains(id)) {
                    return model;
                }
                UnbakedModel optifineModel = loadOptifineModel(id);
                return optifineModel != null ? optifineModel : model;
            }

            String parentString = def.parentModelId().toString();
            String textureString = toModelTextureString(def.textureId());
        String json = "{\"parent\":\"" + parentString + "\",\"textures\":{\"layer0\":\"" + textureString + "\"}}";
        UnbakedModel parsed = UnbakedModelDeserializer.deserialize(new StringReader(json));
        return parsed != null ? parsed : model;
        });
    }

    private static Set<Identifier> filterExistingModels(Set<Identifier> models, ResourceManager manager) {
        if (models == null || models.isEmpty() || manager == null) {
            return models == null ? Set.of() : models;
        }
        Set<Identifier> result = new HashSet<>();
        for (Identifier id : models) {
            if (modelResourceExists(id, manager)) {
                result.add(id);
            }
        }
        return result;
    }

    private static boolean modelResourceExists(Identifier id, ResourceManager manager) {
        if (id == null || manager == null) {
            return false;
        }
        String path = id.getPath();
        Identifier resourceId;
        if (isCitRootPath(path)) {
            resourceId = Identifier.of(id.getNamespace(), path + ".json");
        } else {
            resourceId = Identifier.of(id.getNamespace(), "models/" + path + ".json");
        }
        return manager.getResource(resourceId).isPresent();
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
        if (!isCitRootPath(path)) {
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
            jsonText = ItemCitResolver.normalizeOptifineModelJson(path, jsonText);
            return JsonUnbakedModel.deserialize(new StringReader(jsonText));
        } catch (Exception e) {
            PCIT.LOGGER.warn("[model] failed to load optifine model {}", id, e);
            return null;
        }
    }

    private static boolean isCitRootPath(String path) {
        if (path == null) {
            return false;
        }
        if (path.startsWith("cit/")) {
            return true;
        }
        return path.startsWith("optifine/cit/")
                || path.startsWith("mcpatcher/cit/")
                || path.startsWith("citresewn/cit/");
    }
}
