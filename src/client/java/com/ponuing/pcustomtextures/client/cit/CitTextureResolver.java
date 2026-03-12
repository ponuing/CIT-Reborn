package com.ponuing.pcustomtextures.client.cit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelNameSupplier;
import net.minecraft.client.model.SpriteGetter;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelRotation;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CitTextureResolver {
    private static final Identifier ITEM_ATLAS_ID = Identifier.ofVanilla("textures/atlas/items.png");
    private static final List<String> CIT_ROOTS = List.of("optifine", "mcpatcher", "citresewn");
    private static final int MODEL_SPRITE_CACHE_LIMIT = 1024;
    private static final Map<Identifier, Sprite> MODEL_SPRITE_CACHE = java.util.Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Identifier, Sprite> eldest) {
                    return size() > MODEL_SPRITE_CACHE_LIMIT;
                }
            }
    );
    private static final int MODEL_BAKED_CACHE_LIMIT = 256;
    private static final Map<Identifier, BakedModel> MODEL_BAKED_CACHE = java.util.Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Identifier, BakedModel> eldest) {
                    return size() > MODEL_BAKED_CACHE_LIMIT;
                }
            }
    );

    private CitTextureResolver() {
    }

    public static void clearCaches() {
        MODEL_SPRITE_CACHE.clear();
        MODEL_BAKED_CACHE.clear();
    }

    public static Identifier toSpriteId(Identifier textureId) {
        if (textureId == null) {
            return null;
        }
        String path = textureId.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return Identifier.of(textureId.getNamespace(), path);
    }

    public static Sprite resolveSprite(Identifier textureId) {
        if (textureId == null) {
            return null;
        }
        Identifier spriteId = toSpriteId(textureId);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        boolean preferItems = textureId.getPath().startsWith("textures/item/");
        Sprite primary = resolveSpriteFromAtlas(client, preferItems ? ITEM_ATLAS_ID : SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, spriteId);
        if (!isMissingSprite(primary)) {
            return primary;
        }
        Sprite secondary = resolveSpriteFromAtlas(client, preferItems ? SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE : ITEM_ATLAS_ID, spriteId);
        if (!isMissingSprite(secondary)) {
            return secondary;
        }
        return primary != null ? primary : secondary;
    }

    private static Sprite resolveSpriteFromAtlas(MinecraftClient client, Identifier atlasId, Identifier spriteId) {
        if (client == null || atlasId == null || spriteId == null) {
            return null;
        }
        try {
            return client.getSpriteAtlas(atlasId).apply(spriteId);
        } catch (Exception e) {
            return null;
        }
    }

    public static Sprite resolveModelTextureSprite(Identifier modelId) {
        if (modelId == null) {
            return null;
        }
        Sprite cached = MODEL_SPRITE_CACHE.get(modelId);
        if (cached != null) {
            return cached;
        }
        Sprite resolved = resolveModelTextureSpriteInternal(modelId);
        if (resolved != null) {
            MODEL_SPRITE_CACHE.put(modelId, resolved);
        }
        return resolved;
    }

    public static BakedModel resolveModelBaked(Identifier modelId) {
        if (modelId == null) {
            return null;
        }
        BakedModel cached = MODEL_BAKED_CACHE.get(modelId);
        if (cached != null) {
            return cached;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getResourceManager() == null) {
            return null;
        }
        ResourceManager manager = client.getResourceManager();
        SimpleModelBaker baker = new SimpleModelBaker(client, manager);
        UnbakedModel unbaked = loadUnbakedModel(manager, modelId);
        if (unbaked == null) {
            return null;
        }
        BakedModel baked = UnbakedModel.bake(unbaked, baker, ModelRotation.X0_Y0);
        if (baked != null) {
            MODEL_BAKED_CACHE.put(modelId, baked);
        }
        return baked;
    }

    public static Identifier resolveModelTextureId(Identifier modelId) {
        if (modelId == null) {
            return null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getResourceManager() == null) {
            return null;
        }
        String modelPath = modelId.getPath();
        Identifier modelResource = Identifier.of(modelId.getNamespace(), "models/" + modelPath + ".json");
        Resource resource = client.getResourceManager().getResource(modelResource).orElse(null);
        boolean optifineModel = false;
        if (resource == null && isCitRootPath(modelPath)) {
            optifineModel = true;
            modelResource = Identifier.of(modelId.getNamespace(), modelPath + ".json");
            resource = client.getResourceManager().getResource(modelResource).orElse(null);
        }
        if (resource == null) {
            return null;
        }
        try (InputStream in = resource.getInputStream();
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            if (!json.isJsonObject()) {
                return null;
            }
            JsonObject obj = json.getAsJsonObject();
            if (optifineModel) {
                String jsonText = obj.toString();
                jsonText = normalizeOptifineModelJson(modelPath, jsonText);
                if (jsonText != null && !jsonText.isBlank()) {
                    json = JsonParser.parseString(jsonText);
                    if (!json.isJsonObject()) {
                        return null;
                    }
                    obj = json.getAsJsonObject();
                }
            }
            if (!obj.has("textures")) {
                return null;
            }
            JsonObject textures = obj.getAsJsonObject("textures");
            String textureValue = resolveTextureReference(textures);
            if (textureValue == null) {
                return null;
            }
            Identifier textureId = resolveTextureIdentifier(modelId, textureValue);
            if (textureId == null) {
                return null;
            }
            if (client.getResourceManager().getResource(textureId).isPresent()) {
                return textureId;
            }
            Identifier alias = resolveOptifineAlias(textureId);
            if (alias != null && client.getResourceManager().getResource(alias).isPresent()) {
                return alias;
            }
            return textureId;
        } catch (Exception e) {
            return null;
        }
    }

    private static UnbakedModel loadUnbakedModel(ResourceManager manager, Identifier modelId) {
        if (manager == null || modelId == null) {
            return null;
        }
        String modelPath = modelId.getPath();
        String rawPath = modelPath.startsWith("models/") ? modelPath.substring("models/".length()) : modelPath;
        boolean optifineModel = isCitRootPath(rawPath);
        Identifier modelResource = optifineModel
                ? Identifier.of(modelId.getNamespace(), rawPath + ".json")
                : Identifier.of(modelId.getNamespace(), "models/" + rawPath + ".json");
        Resource resource = manager.getResource(modelResource).orElse(null);
        if (resource == null) {
            return null;
        }
        try (InputStream in = resource.getInputStream();
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            String jsonText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (optifineModel) {
                jsonText = normalizeOptifineModelJson(rawPath, jsonText);
                return JsonUnbakedModel.deserialize(new java.io.StringReader(jsonText));
            }
            return UnbakedModelDeserializer.deserialize(new java.io.StringReader(jsonText));
        } catch (Exception e) {
            return null;
        }
    }

    private static Sprite resolveModelTextureSpriteInternal(Identifier modelId) {
        if (modelId == null) {
            return null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        String modelPath = modelId.getPath();
        Identifier modelResource = Identifier.of(modelId.getNamespace(), "models/" + modelPath + ".json");
        Resource resource = client.getResourceManager().getResource(modelResource).orElse(null);
        boolean optifineModel = false;
        if (resource == null && isCitRootPath(modelPath)) {
            optifineModel = true;
            modelResource = Identifier.of(modelId.getNamespace(), modelPath + ".json");
            resource = client.getResourceManager().getResource(modelResource).orElse(null);
        }
        if (resource == null) {
            return null;
        }
        try (InputStream in = resource.getInputStream();
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            if (!json.isJsonObject()) {
                return null;
            }
            JsonObject obj = json.getAsJsonObject();
            if (optifineModel) {
                String jsonText = obj.toString();
                jsonText = normalizeOptifineModelJson(modelPath, jsonText);
                if (jsonText != null && !jsonText.isBlank()) {
                    json = JsonParser.parseString(jsonText);
                    if (!json.isJsonObject()) {
                        return null;
                    }
                    obj = json.getAsJsonObject();
                }
            }
            if (!obj.has("textures")) {
                return null;
            }
            JsonObject textures = obj.getAsJsonObject("textures");
            String textureValue = resolveTextureReference(textures);
            if (textureValue == null) {
                return null;
            }
            Identifier textureId = resolveTextureIdentifier(modelId, textureValue);
            if (textureId == null) {
                return null;
            }
            return resolveSprite(textureId);
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveTextureReference(JsonObject textures) {
        if (textures == null) {
            return null;
        }
        if (textures.has("layer0")) {
            return resolveTextureReference(textures, "layer0", 0);
        }
        if (textures.has("particle")) {
            return resolveTextureReference(textures, "particle", 0);
        }
        for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
            String value = entry.getValue().getAsString();
            if (value != null && !value.startsWith("#")) {
                return value;
            }
        }
        return null;
    }

    private static String resolveTextureReference(JsonObject textures, String key, int depth) {
        if (textures == null || key == null || depth > 5) {
            return null;
        }
        JsonElement element = textures.get(key);
        if (element == null) {
            return null;
        }
        String value = element.getAsString();
        if (value == null) {
            return null;
        }
        if (value.startsWith("#")) {
            return resolveTextureReference(textures, value.substring(1), depth + 1);
        }
        return value;
    }

    private static Identifier resolveTextureIdentifier(Identifier modelId, String textureValue) {
        String texture = textureValue;
        String namespace = modelId.getNamespace();
        String path = texture;
        if (texture.contains(":")) {
            Identifier parsed = Identifier.tryParse(texture);
            if (parsed == null) {
                return null;
            }
            namespace = parsed.getNamespace();
            path = parsed.getPath();
        }
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        String normalized = normalizePath(path);
        Identifier candidate = Identifier.of(namespace, normalized);
        if (normalized.contains("optifine/") || normalized.contains("mcpatcher/") || normalized.contains("citresewn/")) {
            Identifier alias = resolveOptifineAlias(candidate);
            if (alias != null) {
                return alias;
            }
        }
        return candidate;
    }

    public static boolean isCitRootPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        for (String root : CIT_ROOTS) {
            if (path.startsWith(root + "/")) {
                return true;
            }
        }
        return false;
    }

    private static Identifier resolveOptifineAlias(Identifier textureId) {
        if (textureId == null) {
            return null;
        }
        String path = textureId.getPath();
        if (path.contains("mcpatcher/")) {
            path = path.replace("mcpatcher/", "optifine/");
        }
        if (path.contains("citresewn/")) {
            path = path.replace("citresewn/", "optifine/");
        }
        return Identifier.of(textureId.getNamespace(), path);
    }

    private static boolean isMissingSprite(Sprite sprite) {
        if (sprite == null) {
            return true;
        }
        return sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId());
    }

    private static final class SimpleModelBaker implements Baker {
        private final MinecraftClient client;
        private final ResourceManager manager;
        private final ModelNameSupplier nameSupplier;
        private final Map<Identifier, UnbakedModel> unbakedCache = new HashMap<>();
        private final Map<Identifier, BakedModel> bakedCache = new HashMap<>();
        private final Set<Identifier> inProgress = new HashSet<>();
        private final BakedModel missingModel;

        private SimpleModelBaker(MinecraftClient client, ResourceManager manager) {
            this.client = client;
            this.manager = manager;
            this.nameSupplier = () -> "pcustomtextures";
            this.missingModel = client.getBakedModelManager().getMissingBlockModel();
        }

        @Override
        public BakedModel bake(Identifier id, ModelBakeSettings settings) {
            if (id == null) {
                return missingModel;
            }
            if (bakedCache.containsKey(id)) {
                return bakedCache.get(id);
            }
            if (inProgress.contains(id)) {
                return missingModel;
            }
            inProgress.add(id);
            try {
                UnbakedModel unbaked = unbakedCache.get(id);
                if (unbaked == null) {
                    unbaked = loadUnbakedModel(manager, id);
                    if (unbaked == null) {
                        return missingModel;
                    }
                    unbakedCache.put(id, unbaked);
                }
                BakedModel bakedModel = UnbakedModel.bake(unbaked, this, settings != null ? settings : ModelRotation.X0_Y0);
                if (bakedModel != null) {
                    bakedCache.put(id, bakedModel);
                    return bakedModel;
                }
                return missingModel;
            } finally {
                inProgress.remove(id);
            }
        }

        @Override
        public SpriteGetter getSpriteGetter() {
            return new SpriteGetter() {
                @Override
                public Sprite get(SpriteIdentifier id) {
                    if (id == null) {
                        return client.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(MissingSprite.getMissingSpriteId());
                    }
                    Identifier spriteId = id.getTextureId();
                    if (spriteId == null) {
                        return client.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(MissingSprite.getMissingSpriteId());
                    }
                    return client.getSpriteAtlas(id.getAtlasId()).apply(spriteId);
                }

                @Override
                public Sprite getMissing(String name) {
                    return client.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(MissingSprite.getMissingSpriteId());
                }
            };
        }

        @Override
        public ModelNameSupplier getModelNameSupplier() {
            return nameSupplier;
        }
    }

    public static String normalizeOptifineModelJson(String relPath, String jsonText) {
        if (relPath == null || jsonText == null || jsonText.isBlank()) {
            return jsonText;
        }
        if (!isCitRootPath(relPath)) {
            return jsonText;
        }
        try {
            JsonElement element = JsonParser.parseString(jsonText);
            if (!element.isJsonObject()) {
                return jsonText;
            }
            JsonObject obj = element.getAsJsonObject();
            JsonElement texturesElement = obj.get("textures");
            if (texturesElement == null || !texturesElement.isJsonObject()) {
                return jsonText;
            }
            JsonObject textures = texturesElement.getAsJsonObject();
            int slash = relPath.lastIndexOf('/');
            String dir = slash >= 0 ? relPath.substring(0, slash) : relPath;
            boolean changed = false;
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                JsonElement value = entry.getValue();
                if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    continue;
                }
                String tex = value.getAsString();
                String resolved = resolveOptifineTexturePath(dir, tex);
                if (resolved != null) {
                    textures.addProperty(entry.getKey(), resolved);
                    changed = true;
                }
            }
            JsonElement parentEl = obj.get("parent");
            if (parentEl != null && parentEl.isJsonPrimitive() && parentEl.getAsJsonPrimitive().isString()) {
                String parentRaw = parentEl.getAsString();
                String resolvedParent = resolveOptifineModelPath(dir, parentRaw);
                if (resolvedParent != null && !resolvedParent.equals(parentRaw)) {
                    obj.addProperty("parent", resolvedParent);
                    changed = true;
                }
            }
            JsonElement overridesEl = obj.get("overrides");
            if (overridesEl != null && overridesEl.isJsonArray()) {
                boolean overridesChanged = false;
                for (JsonElement overrideEl : overridesEl.getAsJsonArray()) {
                    if (!overrideEl.isJsonObject()) {
                        continue;
                    }
                    JsonObject overrideObj = overrideEl.getAsJsonObject();
                    JsonElement modelEl = overrideObj.get("model");
                    if (modelEl == null || !modelEl.isJsonPrimitive() || !modelEl.getAsJsonPrimitive().isString()) {
                        continue;
                    }
                    String modelRaw = modelEl.getAsString();
                    String resolvedModel = resolveOptifineModelPath(dir, modelRaw);
                    if (resolvedModel != null && !resolvedModel.equals(modelRaw)) {
                        overrideObj.addProperty("model", resolvedModel);
                        overridesChanged = true;
                    }
                }
                if (overridesChanged) {
                    changed = true;
                }
            }
            return changed ? obj.toString() : jsonText;
        } catch (Exception e) {
            return jsonText;
        }
    }

    private static boolean applyTextureSizeScaling(JsonObject obj) {
        JsonElement sizeEl = obj.get("texture_size");
        if (sizeEl == null || !sizeEl.isJsonArray() || sizeEl.getAsJsonArray().size() < 2) {
            return false;
        }
        float width = sizeEl.getAsJsonArray().get(0).getAsFloat();
        float height = sizeEl.getAsJsonArray().get(1).getAsFloat();
        if (width <= 0 || height <= 0) {
            return false;
        }
        float scaleU = 16.0f / width;
        float scaleV = 16.0f / height;
        if (Math.abs(scaleU - 1.0f) < 0.0001f && Math.abs(scaleV - 1.0f) < 0.0001f) {
            return false;
        }
        JsonElement elementsEl = obj.get("elements");
        if (elementsEl == null || !elementsEl.isJsonArray()) {
            return false;
        }
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
            for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                JsonElement faceEl = faceEntry.getValue();
                if (faceEl == null || !faceEl.isJsonObject()) {
                    continue;
                }
                JsonObject faceObj = faceEl.getAsJsonObject();
                JsonElement uvEl = faceObj.get("uv");
                if (uvEl == null || !uvEl.isJsonArray() || uvEl.getAsJsonArray().size() < 4) {
                    continue;
                }
                float u0 = uvEl.getAsJsonArray().get(0).getAsFloat() * scaleU;
                float v0 = uvEl.getAsJsonArray().get(1).getAsFloat() * scaleV;
                float u1 = uvEl.getAsJsonArray().get(2).getAsFloat() * scaleU;
                float v1 = uvEl.getAsJsonArray().get(3).getAsFloat() * scaleV;
                faceObj.add("uv", new com.google.gson.JsonArray());
                faceObj.getAsJsonArray("uv").add(u0);
                faceObj.getAsJsonArray("uv").add(v0);
                faceObj.getAsJsonArray("uv").add(u1);
                faceObj.getAsJsonArray("uv").add(v1);
                changed = true;
            }
        }
        return changed;
    }

    public static String resolveOptifineTexturePath(String dir, String tex) {
        if (tex == null || tex.isBlank()) {
            return null;
        }
        if (tex.startsWith("textures/")) {
            String trimmed = tex.substring("textures/".length());
            if (isCitRootPath(trimmed)) {
                return trimmed;
            }
            return null;
        }
        if (tex.startsWith("./")) {
            return dir + "/" + tex.substring(2);
        }
        if (isCitRootPath(tex)) {
            return tex;
        }
        if (tex.startsWith("item/") || tex.startsWith("block/") || tex.contains(":")) {
            return null;
        }
        return dir + "/" + tex;
    }

    public static String resolveOptifineModelPath(String dir, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().replace("\\", "/");
        String namespace = null;
        String path = value;
        int colon = value.indexOf(':');
        if (colon >= 0) {
            namespace = value.substring(0, colon);
            path = value.substring(colon + 1);
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        }
        if (path.startsWith("models/")) {
            path = path.substring("models/".length());
        }

        String resolved = null;
        if (path.startsWith("./")) {
            resolved = dir + "/" + path.substring(2);
        } else if (isCitRootPath(path)) {
            resolved = path;
        } else if (!path.contains("/")) {
            resolved = dir + "/" + path;
        } else {
            return null;
        }

        resolved = normalizePath(resolved);
        return namespace != null ? namespace + ":" + resolved : resolved;
    }

    public static String normalizePath(String raw) {
        String input = raw.replace("\\", "/");
        String[] parts = input.split("/");
        List<String> result = new java.util.ArrayList<>();
        for (String part : parts) {
            if (part.isBlank() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!result.isEmpty()) {
                    result.remove(result.size() - 1);
                }
                continue;
            }
            result.add(part);
        }
        return String.join("/", result);
    }

    public static String normalizeNameKey(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(colon + 1);
        }
        if (value.startsWith("textures/")) {
            value = value.substring("textures/".length());
        }
        if (value.endsWith(".png")) {
            value = value.substring(0, value.length() - 4);
        }
        return value.toLowerCase(java.util.Locale.ROOT);
    }

    public static boolean matchesTextureName(Identifier spriteId, String nameKey) {
        if (spriteId == null || nameKey == null || nameKey.isBlank()) {
            return false;
        }
        String path = spriteId.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        String normalizedPath = path.toLowerCase(java.util.Locale.ROOT);
        String normalizedName = normalizeNameKey(nameKey);
        if (normalizedName.isBlank()) {
            return false;
        }
        if (normalizedPath.equals(normalizedName)) {
            return true;
        }
        return normalizedPath.endsWith("/" + normalizedName);
    }

    public static Identifier findExistingTexture(List<Identifier> candidates) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        return findExistingTexture(client.getResourceManager(), candidates);
    }

    public static Identifier findExistingTexture(ResourceManager manager, List<Identifier> candidates) {
        if (manager == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (Identifier id : candidates) {
            if (id == null) {
                continue;
            }
            if (manager.getResource(id).isPresent()) {
                return id;
            }
        }
        return null;
    }
}
