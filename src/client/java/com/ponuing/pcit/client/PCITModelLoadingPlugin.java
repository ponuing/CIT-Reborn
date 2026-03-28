package com.ponuing.pcit.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.concurrent.ConcurrentHashMap;

public final class PCITModelLoadingPlugin {
    private static final Set<String> LOGGED_MODEL_LOAD_FAILURES = ConcurrentHashMap.newKeySet();

    private PCITModelLoadingPlugin() {
    }

    public static void register() {
        ModelLoadingPlugin.register(PCITModelLoadingPlugin::initialize);
    }

    private static void initialize(ModelLoadingPlugin.Context context) {
        ResourceManager manager = MinecraftClient.getInstance() != null ? MinecraftClient.getInstance().getResourceManager() : null;
        if (manager != null) {
            ItemCitResolver.reloadFromManager(manager);
        } else {
            ItemCitResolver.ensureLoaded();
        }

        Map<Identifier, ItemCitResolver.GeneratedModelDef> generatedModels = ItemCitResolver.getGeneratedItemModels();
        Set<Identifier> extraModelsRaw = ItemCitResolver.getExtraItemModels();
        if (generatedModels == null) {
            generatedModels = Map.of();
        }
        if ((generatedModels == null || generatedModels.isEmpty()) && (extraModelsRaw == null || extraModelsRaw.isEmpty())) {
            return;
        }

        Set<Identifier> extraModels = manager != null ? filterExistingModels(extraModelsRaw, manager) : extraModelsRaw;
        if (extraModels == null) {
            extraModels = Set.of();
        }
        final Map<Identifier, ItemCitResolver.GeneratedModelDef> generatedModelsFinal = generatedModels;
        final Set<Identifier> extraModelsFinal = extraModels;
        Set<Identifier> allModels = new HashSet<>(generatedModels.keySet());
        allModels.addAll(extraModels);
        context.addModels(allModels);
        context.modifyModelOnLoad().register(ModelModifier.OVERRIDE_PHASE, (model, ctx) -> {
            Identifier id = ctx.id();
            ItemCitResolver.GeneratedModelDef def = generatedModelsFinal.get(id);
            if (def == null) {
                if (extraModelsFinal.contains(id) || isCitRootPath(id.getPath())) {
                    UnbakedModel optifineModel = loadOptifineModel(id);
                    return optifineModel != null ? optifineModel : model;
                }
                return model;
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
        if (path.startsWith("models/")) {
            path = path.substring("models/".length());
        }
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
        if (path.startsWith("models/")) {
            path = path.substring("models/".length());
        }
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
            jsonText = ItemCitResolver.normalizeOptifineModelJson(path, jsonText, id.getNamespace());
            jsonText = sanitizeSelfParent(id, path, jsonText);
            return JsonUnbakedModel.deserialize(new StringReader(jsonText));
        } catch (Exception e) {
            if (id != null && LOGGED_MODEL_LOAD_FAILURES.add(id.toString())) {
                PCIT.LOGGER.warn("[model] failed to load optifine model {}", id, e);
            }
            return null;
        }
    }

    private static boolean isCitRootPath(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("cit/")
                || path.startsWith("optifine/cit/")
                || path.startsWith("mcpatcher/cit/")
                || path.startsWith("citresewn/cit/");
    }

    private static String sanitizeSelfParent(Identifier id, String path, String jsonText) {
        if (jsonText == null || jsonText.isBlank() || id == null || path == null) {
            return jsonText;
        }
        try {
            JsonElement element = JsonParser.parseString(jsonText);
            if (!element.isJsonObject()) {
                return jsonText;
            }
            JsonObject obj = element.getAsJsonObject();
            JsonElement parentEl = obj.get("parent");
            if (parentEl == null || !parentEl.isJsonPrimitive() || !parentEl.getAsJsonPrimitive().isString()) {
                return jsonText;
            }
            String parentRaw = parentEl.getAsString().trim().replace("\\", "/");
            String selfPath = path;
            if (selfPath.endsWith(".json")) {
                selfPath = selfPath.substring(0, selfPath.length() - 5);
            }
            if (selfPath.startsWith("models/")) {
                selfPath = selfPath.substring("models/".length());
            }
            String parentPath = parentRaw;
            if (parentPath.endsWith(".json")) {
                parentPath = parentPath.substring(0, parentPath.length() - 5);
            }
            if (parentPath.startsWith("models/")) {
                parentPath = parentPath.substring("models/".length());
            }
            String selfWithNs = id.getNamespace() + ":" + selfPath;
            boolean matchesSelf = parentPath.equals(selfPath)
                    || parentPath.equals(selfWithNs)
                    || parentRaw.equals(selfPath)
                    || parentRaw.equals(selfWithNs);
            if (matchesSelf) {
                boolean hasElements = obj.has("elements")
                        && obj.get("elements").isJsonArray()
                        && obj.getAsJsonArray("elements").size() > 0;
                if (hasElements) {
                    obj.remove("parent");
                } else {
                    obj.addProperty("parent", "minecraft:item/generated");
                }
                return obj.toString();
            }
        } catch (Exception ignored) {
            return jsonText;
        }
        return jsonText;
    }

    private static String inlineTextureReferences(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return jsonText;
        }
        try {
            JsonElement element = JsonParser.parseString(jsonText);
            if (!element.isJsonObject()) {
                return jsonText;
            }
            JsonObject obj = element.getAsJsonObject();
            JsonElement texturesEl = obj.get("textures");
            JsonElement elementsEl = obj.get("elements");
            if (texturesEl == null || !texturesEl.isJsonObject() || elementsEl == null || !elementsEl.isJsonArray()) {
                return jsonText;
            }
            JsonObject textures = texturesEl.getAsJsonObject();
            boolean changed = false;
            for (JsonElement elementEl : elementsEl.getAsJsonArray()) {
                if (!elementEl.isJsonObject()) {
                    continue;
                }
                JsonObject elementObj = elementEl.getAsJsonObject();
                JsonElement facesEl = elementObj.get("faces");
                if (facesEl == null || !facesEl.isJsonObject()) {
                    continue;
                }
                JsonObject faces = facesEl.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
                    JsonElement faceEl = entry.getValue();
                    if (faceEl == null || !faceEl.isJsonObject()) {
                        continue;
                    }
                    JsonObject faceObj = faceEl.getAsJsonObject();
                    JsonElement textureEl = faceObj.get("texture");
                    if (textureEl == null || !textureEl.isJsonPrimitive() || !textureEl.getAsJsonPrimitive().isString()) {
                        continue;
                    }
                    String textureRef = textureEl.getAsString();
                    if (textureRef == null || !textureRef.startsWith("#")) {
                        continue;
                    }
                    String key = textureRef.substring(1);
                    String resolved = resolveTextureReference(textures, key, 0);
                    if (resolved != null && !resolved.isBlank() && !resolved.equals(textureRef)) {
                        faceObj.addProperty("texture", resolved);
                        changed = true;
                    }
                }
            }
            return changed ? obj.toString() : jsonText;
        } catch (Exception ignored) {
            return jsonText;
        }
    }

    private static String resolveTextureReference(JsonObject textures, String key, int depth) {
        if (textures == null || key == null || key.isBlank() || depth > 8) {
            return null;
        }
        JsonElement valueEl = textures.get(key);
        if (valueEl == null || !valueEl.isJsonPrimitive() || !valueEl.getAsJsonPrimitive().isString()) {
            return null;
        }
        String value = valueEl.getAsString();
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith("#")) {
            return resolveTextureReference(textures, value.substring(1), depth + 1);
        }
        return value;
    }

}
