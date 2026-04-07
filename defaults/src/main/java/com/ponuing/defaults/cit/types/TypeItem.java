package com.ponuing.defaults.cit.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.util.Either;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelTextures;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.GeneratedItemModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import com.ponuing.CITReborn;
import com.ponuing.api.CITTypeContainer;
import com.ponuing.cit.*;
import com.ponuing.config.BrokenPaths;
import com.ponuing.config.CITRebornConfig;
import com.ponuing.defaults.cit.conditions.ConditionItems;
import com.ponuing.defaults.mixin.types.item.JsonUnbakedModelAccessor;
import com.ponuing.pack.format.PropertyGroup;
import com.ponuing.pack.format.PropertyKey;
import com.ponuing.pack.format.PropertyValue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * ///// PORTED FROM BETA \\\\\
 * This shit was ported from the
 * beta and will be rewritten at
 * some point!
 * \\\\\                  /////
 */
public class TypeItem extends CITType {
    public static final Container CONTAINER = new Container();

    private static final String GENERATED_SUB_CITS_PREFIX = "sub_cititem_generated_";
    public static final Set<Identifier> GENERATED_SUB_CITS_SEEN = new HashSet<>();
    private static final Identifier ITEM_GENERATED_MODEL = Identifier.of("minecraft", "item/generated");
    private static final Identifier ITEM_HANDHELD_MODEL = Identifier.of("minecraft", "item/handheld");

    private static final Field JSON_PARENT_ID_FIELD;

    static {
        Field parentIdField = null;
        try {
            parentIdField = JsonUnbakedModel.class.getDeclaredField("parentId");
            parentIdField.setAccessible(true);
        } catch (Exception ignored) {
        }
        JSON_PARENT_ID_FIELD = parentIdField;
    }

    private final List<Item> items = new ArrayList<>();

    private Identifier propertiesIdentifier;
    public Map<Identifier, Identifier> assetIdentifiers = new LinkedHashMap<>();
    public Map<Identifier, UnbakedModel> unbakedAssets = new LinkedHashMap<>();
    private Map<String, Either<SpriteIdentifier, String>> textureOverrideMap = new HashMap<>();
    private boolean isTexture = false;

    public BakedModel bakedModel = null;

    @Override
    public Set<PropertyKey> typeProperties() {
        return Set.of(PropertyKey.of("model"), PropertyKey.of("texture"), PropertyKey.of("tile"));
    }

    @Override
    public void load(List<CITCondition> conditions, PropertyGroup properties, ResourceManager resourceManager) throws CITParsingException {
        this.propertiesIdentifier = properties.identifier;
        for (CITCondition condition : conditions)
            if (condition instanceof ConditionItems conditionItems)
                items.addAll(Arrays.asList(conditionItems.items));

        if (this.items.size() == 0)
            try {
                Identifier propertiesName = Identifier.tryParse(properties.stripName());
                if (!Registries.ITEM.containsId(propertiesName))
                    throw new Exception();
                Item item = Registries.ITEM.get(propertiesName);
                conditions.add(new ConditionItems(item));
                this.items.add(item);
            } catch (Exception ignored) {
                throw new CITParsingException("Not targeting any item type", properties, -1);
            }

        Identifier assetIdentifier;
        PropertyValue modelProp = properties.getLastWithoutMetadata("citresewn", "model");
        boolean containsTexture = modelProp == null && !properties.get("citresewn", "texture", "tile").isEmpty();

        if (!containsTexture) {
            assetIdentifier = resolveAsset(properties.identifier, modelProp, "models", ".json", resourceManager);
            if (assetIdentifier != null)
                assetIdentifiers.put(null, assetIdentifier);
            else if (modelProp != null) {
                assetIdentifier = resolveAsset(properties.identifier, modelProp, "models", ".json", resourceManager);
                if (assetIdentifier != null)
                    assetIdentifiers.put(null, assetIdentifier);
            }
        }

        for (PropertyValue property : properties.get("citresewn", "model")) {
            Identifier subIdentifier = resolveAsset(properties.identifier, property, "models", ".json", resourceManager);
            if (subIdentifier == null)
                throw new CITParsingException("Cannot resolve path", properties, property.position());

            String subItem = property.keyMetadata();
            Identifier subItemIdentifier = fixDeprecatedSubItem(subItem, properties, property.position());
            assetIdentifiers.put(subItemIdentifier == null ? Identifier.of("minecraft", "item/" + subItem) : subItemIdentifier, subIdentifier);
        }

        if (assetIdentifiers.size() == 0) { // attempt to load texture
            isTexture = true;
            PropertyValue textureProp = properties.getLastWithoutMetadata("citresewn", "texture", "tile");
            assetIdentifier = resolveAsset(properties.identifier, textureProp, "textures", ".png", resourceManager);
            if (assetIdentifier != null)
                assetIdentifiers.put(null, assetIdentifier);

            for (PropertyValue property : properties.get("citresewn", "texture", "tile")) {
                if (property.keyMetadata() == null)
                    continue;
                Identifier subIdentifier = resolveAsset(properties.identifier, property, "textures", ".png", resourceManager);
                if (subIdentifier == null)
                    throw new CITParsingException("Cannot resolve path", properties, property.position());

                String subItem = property.keyMetadata();
                Identifier subItemIdentifier = fixDeprecatedSubItem(subItem, properties, property.position());
                assetIdentifiers.put(subItemIdentifier == null ? Identifier.of("minecraft", "item/" + subItem) : subItemIdentifier, subIdentifier);
            }
        } else { // attempt to load textureOverrideMap from textures
            PropertyValue textureProp = properties.getLastWithoutMetadata("citresewn", "texture", "tile");
            if (textureProp != null) {
                assetIdentifier = resolveAsset(properties.identifier, textureProp, "textures", ".png", resourceManager);
                if (assetIdentifier != null)
                    textureOverrideMap.put(null, Either.left(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, assetIdentifier)));
                else
                    throw new CITParsingException("Cannot resolve path", properties, textureProp.position());
            }

            for (PropertyValue property : properties.get("citresewn", "texture", "tile")) {
                textureProp = property;
                Identifier subIdentifier = resolveAsset(properties.identifier, textureProp, "textures", ".png", resourceManager);
                if (subIdentifier == null)
                    throw new CITParsingException("Cannot resolve path", properties, property.position());

                textureOverrideMap.put(property.keyMetadata(), Either.left(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, subIdentifier)));
            }
        }

        if (assetIdentifiers.size() == 0)
            throw new CITParsingException("Could not resolve a replacement model/texture", properties, -1);
    }

    public void loadUnbakedAssets(ResourceManager resourceManager) throws Exception {
        boolean hasOverrides = textureOverrideMap != null && !textureOverrideMap.isEmpty();
        boolean needsUniqueId = !isTexture && hasOverrides && propertiesIdentifier != null;
        try {
            if (isTexture) {
                JsonUnbakedModel itemJson = getModelForFirstItemType(resourceManager);
                if (itemJson != null)
                    resolveParentChain(itemJson, resourceManager, new HashMap<>());

                if (itemJson != null && itemJson.getTextures().values().size() > 1) {
                    Identifier defaultAsset = assetIdentifiers.get(null);
                    Map<String, Either<SpriteIdentifier, String>> layeredOverrides =
                            buildLayeredOverrides(itemJson.getTextures(), assetIdentifiers, defaultAsset);
                    if (!layeredOverrides.isEmpty()) {
                        ModelTextures.Textures merged = applyTextureOverrides(itemJson.getTextures(), layeredOverrides);
                        merged = ensureParticle(merged);
                        if (merged != itemJson.getTextures())
                            itemJson = copyModelWithTextures(itemJson, merged);

                        if (assetIdentifiers.isEmpty() || (assetIdentifiers.size() == 1 && assetIdentifiers.containsKey(null))) {
                            Identifier modelId = defaultAsset != null ? toModelId(defaultAsset) : generateSubModelId(propertiesIdentifier);
                            unbakedAssets.put(modelId, itemJson);
                            return;
                        }
                    }
                }

                Identifier baseIdentifier = assetIdentifiers.get(null);
                if (baseIdentifier == null && !assetIdentifiers.isEmpty())
                    baseIdentifier = assetIdentifiers.values().iterator().next();
                if (baseIdentifier == null)
                    throw new Exception("No base asset resolved");

                UnbakedModel model = loadUnbakedAsset(resourceManager, baseIdentifier);
                ModelTextures.Textures textures = model.getTextures();
                ModelTextures.Textures merged = ensureParticle(textures);
                if (merged != textures)
                    model = new OverrideUnbakedModel(model, merged);

                Identifier modelId = toModelId(baseIdentifier);
                unbakedAssets.put(modelId, model);
                return;
            }

            Identifier baseIdentifier = assetIdentifiers.get(null);
            if (baseIdentifier == null && !assetIdentifiers.isEmpty())
                baseIdentifier = assetIdentifiers.values().iterator().next();
            if (baseIdentifier == null)
                throw new Exception("No base asset resolved");

            UnbakedModel model = loadUnbakedAsset(resourceManager, baseIdentifier);
            ModelTextures.Textures textures = model.getTextures();
            ModelTextures.Textures merged = textures;
            if (hasOverrides)
                merged = applyTextureOverrides(merged, textureOverrideMap);
            merged = ensureParticle(merged);
            if (merged != textures)
                model = new OverrideUnbakedModel(model, merged);
            Identifier modelId = toModelId(baseIdentifier);
            if (needsUniqueId)
                modelId = generateSubModelId(propertiesIdentifier);
            unbakedAssets.put(modelId, model);
        } finally {
            assetIdentifiers = null;
            textureOverrideMap = null;
        }
    }

    private static Identifier toModelId(Identifier assetIdentifier) {
        String path = assetIdentifier.getPath();
        if (path.startsWith("textures/"))
            path = path.substring("textures/".length());
        if (path.startsWith("models/"))
            path = path.substring("models/".length());
        if (path.endsWith(".png"))
            path = path.substring(0, path.length() - ".png".length());
        else if (path.endsWith(".json"))
            path = path.substring(0, path.length() - ".json".length());
        return Identifier.of(assetIdentifier.getNamespace(), path);
    }

    private static Identifier toSpriteId(Identifier textureIdentifier) {
        String path = textureIdentifier.getPath();
        if (path.startsWith("textures/"))
            path = path.substring("textures/".length());
        if (path.endsWith(".png"))
            path = path.substring(0, path.length() - ".png".length());
        return Identifier.of(textureIdentifier.getNamespace(), path);
    }

    private static Identifier generateSubModelId(Identifier propertiesId) {
        String basePath = propertiesId.getPath();
        if (basePath.endsWith(".properties"))
            basePath = basePath.substring(0, basePath.length() - ".properties".length());

        String path = GENERATED_SUB_CITS_PREFIX + basePath;
        Identifier candidate = Identifier.of(propertiesId.getNamespace(), path);
        if (GENERATED_SUB_CITS_SEEN.add(candidate))
            return candidate;

        int i = 1;
        while (true) {
            candidate = Identifier.of(propertiesId.getNamespace(), path + "_" + i);
            if (GENERATED_SUB_CITS_SEEN.add(candidate))
                return candidate;
            i++;
        }
    }

    private UnbakedModel loadUnbakedAsset(ResourceManager resourceManager, Identifier assetIdentifier) throws Exception {
        if (assetIdentifier.getPath().endsWith(".json")) {
            try (InputStream is = resourceManager.getResource(assetIdentifier).orElseThrow().getInputStream()) {
                String jsonText = readResourceToString(is);
                JsonObject jsonObject;
                try (JsonReader jsonReader = new JsonReader(new StringReader(jsonText))) {
                    jsonReader.setLenient(true);
                    jsonObject = JsonParser.parseReader(jsonReader).getAsJsonObject();
                }
                boolean sanitized = sanitizeJsonTextures(jsonObject, assetIdentifier, resourceManager);
                Reader reader = new StringReader(sanitized ? jsonObject.toString() : jsonText);
                JsonUnbakedModel json = JsonUnbakedModel.deserialize(reader);
                resolveParentChain(json, resourceManager, new HashMap<>());
                JsonUnbakedModel resolved = resolveModelTextures(json, assetIdentifier, resourceManager);
                return resolved == null ? json : resolved;
            }
        } else if (assetIdentifier.getPath().endsWith(".png")) {
            JsonUnbakedModel baseModel = getModelForFirstItemType(resourceManager);
            if (baseModel != null) {
                resolveParentChain(baseModel, resourceManager, new HashMap<>());
            }
            ModelTextures.Textures baseTextures = baseModel == null ? ModelTextures.Textures.EMPTY : baseModel.getTextures();
            ModelTextures.Textures override = new ModelTextures.Textures.Builder()
                    .addSprite("layer0", new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, assetIdentifier))
                    .build();
            ModelTextures.Textures merged = mergeTextures(baseTextures, override);
            merged = ensureParticle(merged);
            if (baseModel == null) {
                return new JsonUnbakedModel(ITEM_GENERATED_MODEL, new ArrayList<>(), merged, null, null, null);
            }
            if (merged != baseTextures)
                return copyModelWithTextures(baseModel, merged);
            return baseModel;
        }

        throw new Exception("Unknown asset type");
    }

    private static String readResourceToString(InputStream is) throws Exception {
        byte[] data = is.readAllBytes();
        return new String(data, StandardCharsets.UTF_8);
    }

    private static boolean sanitizeJsonTextures(JsonObject json, Identifier modelIdentifier,
                                                ResourceManager resourceManager) {
        JsonElement texturesElement = json.get("textures");
        if (texturesElement == null || !texturesElement.isJsonObject())
            return false;

        boolean changed = false;
        JsonObject textures = texturesElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
            JsonElement valueElement = entry.getValue();
            if (valueElement == null || !valueElement.isJsonPrimitive())
                continue;
            JsonPrimitive primitive = valueElement.getAsJsonPrimitive();
            if (!primitive.isString())
                continue;
            String value = primitive.getAsString();
            if (value == null || value.isBlank() || value.startsWith("#"))
                continue;

            String resolved = resolveTextureString(value, modelIdentifier, resourceManager);
            if (resolved != null && !resolved.equals(value)) {
                entry.setValue(new JsonPrimitive(resolved));
                changed = true;
            }
        }

        return changed;
    }

    private static String resolveTextureString(String raw, Identifier modelIdentifier,
                                               ResourceManager resourceManager) {
        boolean allowBroken = BrokenPaths.processingBrokenPaths || CITRebornConfig.INSTANCE.broken_paths;
        try {
            Identifier resolved = resolveAsset(modelIdentifier, raw, "textures", ".png", resourceManager);
            if (resolved != null)
                return resolved.toString();
        } catch (Exception ignored) {
        }

        String value = raw.replace('\\', '/');
        if (value.startsWith("./"))
            value = value.substring(2);
        if (value.startsWith("textures/"))
            value = value.substring("textures/".length());
        if (value.endsWith(".png"))
            value = value.substring(0, value.length() - ".png".length());

        String candidate = value;
        if (!allowBroken && Identifier.tryParse(candidate) == null)
            candidate = candidate.toLowerCase(Locale.ROOT);

        Identifier resolved = null;
        try {
            resolved = resolveAsset(modelIdentifier, candidate, "textures", ".png", resourceManager);
        } catch (Exception ignored) {
            resolved = null;
        }

        if (resolved != null)
            return resolved.toString();

        return candidate;
    }

    private static Map<String, Either<SpriteIdentifier, String>> buildLayeredOverrides(ModelTextures.Textures baseTextures,
                                                                                        Map<Identifier, Identifier> assets,
                                                                                        Identifier defaultAsset) {
        Map<String, Either<SpriteIdentifier, String>> overrides = new HashMap<>();
        for (Map.Entry<String, ModelTextures.Entry> entry : baseTextures.values().entrySet()) {
            String layerName = entry.getKey();
            ModelTextures.Entry value = entry.getValue();
            Identifier originalId = extractIdentifier(value);
            Identifier replacement = originalId == null ? null : assets.remove(originalId);
            if (replacement != null) {
                overrides.put(layerName, Either.left(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, replacement)));
                continue;
            }
            if (defaultAsset != null)
                overrides.put(layerName, Either.left(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, defaultAsset)));
        }
        return overrides;
    }

    private static Identifier extractIdentifier(ModelTextures.Entry entry) {
        if (entry == null)
            return null;
        SpriteIdentifier sprite = tryGetSprite(entry);
        if (sprite != null)
            return sprite.getTextureId();
        String target = tryGetTarget(entry);
        if (target != null && !target.startsWith("#"))
            return Identifier.tryParse(target);
        return null;
    }

    private static ModelTextures.Textures applyTextureOverrides(ModelTextures.Textures base,
                                                                Map<String, Either<SpriteIdentifier, String>> overrides) {
        if (overrides == null || overrides.isEmpty())
            return base;

        Map<String, ModelTextures.Entry> baseValues = base.values();
        Either<SpriteIdentifier, String> defaultOverride = overrides.get(null);

        if (baseValues.isEmpty()) {
            if (defaultOverride == null)
                return base;
            ModelTextures.Textures.Builder builder = new ModelTextures.Textures.Builder();
            addOverrideEntry(builder, "layer0", defaultOverride);
            return builder.build();
        }

        ModelTextures.Textures.Builder builder = new ModelTextures.Textures.Builder();
        boolean changed = false;

        for (Map.Entry<String, ModelTextures.Entry> entry : baseValues.entrySet()) {
            String key = entry.getKey();
            ModelTextures.Entry value = entry.getValue();
            Either<SpriteIdentifier, String> override = overrides.get(key);
            if (override == null && defaultOverride != null)
                override = defaultOverride;
            if (override == null) {
                String textureName = extractTextureName(value);
                if (textureName != null)
                    override = overrides.get(textureName);
            }

            if (override != null) {
                addOverrideEntry(builder, key, override);
                changed = true;
                continue;
            }

            SpriteIdentifier sprite = tryGetSprite(value);
            if (sprite != null) {
                builder.addSprite(key, sprite);
                continue;
            }
            String target = tryGetTarget(value);
            if (target != null) {
                builder.addTextureReference(key, target);
            }
        }

        return changed ? builder.build() : base;
    }

    private static void addOverrideEntry(ModelTextures.Textures.Builder builder, String key,
                                         Either<SpriteIdentifier, String> override) {
        if (override.left().isPresent()) {
            builder.addSprite(key, override.left().get());
            return;
        }
        if (override.right().isPresent()) {
            String target = override.right().get();
            if (target != null && target.startsWith("#"))
                target = target.substring(1);
            if (target != null && !target.isBlank())
                builder.addTextureReference(key, target);
        }
    }

    private static String extractTextureName(ModelTextures.Entry entry) {
        if (entry == null)
            return null;
        SpriteIdentifier sprite = tryGetSprite(entry);
        if (sprite != null) {
            String path = sprite.getTextureId().getPath();
            int slash = path.lastIndexOf('/');
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            if (name.endsWith(".png"))
                name = name.substring(0, name.length() - ".png".length());
            return name;
        }
        return null;
    }

    private static Identifier getJsonParentId(JsonUnbakedModel model) {
        if (JSON_PARENT_ID_FIELD == null)
            return null;
        try {
            return (Identifier) JSON_PARENT_ID_FIELD.get(model);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonUnbakedModel getModelForFirstItemType(ResourceManager resourceManager) {
        Identifier firstItemIdentifier = Registries.ITEM.getId(this.items.iterator().next());
        Identifier modelIdentifier = Identifier.of(firstItemIdentifier.getNamespace(), "models/item/" + firstItemIdentifier.getPath() + ".json");
        try (InputStream is = resourceManager.getResource(modelIdentifier).orElseThrow().getInputStream();
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return JsonUnbakedModel.deserialize(reader);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonUnbakedModel copyModelWithTextures(JsonUnbakedModel base, ModelTextures.Textures textures) {
        JsonUnbakedModelAccessor accessor = (JsonUnbakedModelAccessor) base;
        JsonUnbakedModel copy = new JsonUnbakedModel(
                accessor.CITReborn$getParentId(),
                new ArrayList<>(accessor.CITReborn$getElements()),
                textures,
                accessor.CITReborn$getAmbientOcclusion(),
                accessor.CITReborn$getGuiLight(),
                accessor.CITReborn$getTransformations()
        );
        ((JsonUnbakedModelAccessor) copy).CITReborn$setParent(base.getParent());
        return copy;
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
                Identifier resolved = resolveAsset(modelIdentifier, texturePath, "textures", ".png", resourceManager);
                if (resolved == null && texturePath.startsWith("items/")) {
                    String fixedPath = "item/" + texturePath.substring("items/".length());
                    resolved = resolveAsset(modelIdentifier, fixedPath, "textures", ".png", resourceManager);
                }
                if (resolved != null) {
                    SpriteIdentifier updated = new SpriteIdentifier(sprite.getAtlasId(), resolved);
                    builder.addSprite(key, updated);
                    if (!updated.equals(sprite))
                        changed = true;
                } else {
                    builder.addSprite(key, sprite);
                }
                continue;
            }

            String target = tryGetTarget(value);
            if (target != null) {
                builder.addTextureReference(key, target);
                continue;
            }
        }

        if (!changed)
            return model;

        ModelTextures.Textures resolved = builder.build();
        return copyModelWithTextures(model, resolved);
    }

    private static Method SPRITE_ENTRY_MATERIAL;
    private static Method TEXTURE_ENTRY_TARGET;

    private static SpriteIdentifier tryGetSprite(ModelTextures.Entry entry) {
        if (entry == null)
            return null;
        try {
            if (SPRITE_ENTRY_MATERIAL == null) {
                Method m = entry.getClass().getMethod("material");
                SPRITE_ENTRY_MATERIAL = m;
            }
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
            if (TEXTURE_ENTRY_TARGET == null) {
                Method m = entry.getClass().getMethod("target");
                TEXTURE_ENTRY_TARGET = m;
            }
            Object target = TEXTURE_ENTRY_TARGET.invoke(entry);
            return target instanceof String s ? s : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void resolveParentChain(JsonUnbakedModel model, ResourceManager resourceManager,
                                           Map<Identifier, UnbakedModel> cache) {
        JsonUnbakedModel current = model;
        while (current != null) {
            JsonUnbakedModelAccessor accessor = (JsonUnbakedModelAccessor) current;
            if (current.getParent() != null)
                return;
            Identifier parentId = accessor.CITReborn$getParentId();
            if (parentId == null)
                return;

            UnbakedModel parent = cache.get(parentId);
            if (parent == null) {
                parent = loadParentModel(parentId, resourceManager, cache);
            }
            if (parent == null)
                return;
            accessor.CITReborn$setParent(parent);
            if (parent instanceof JsonUnbakedModel parentJson) {
                current = parentJson;
                continue;
            }
            return;
        }
    }

    private static UnbakedModel loadParentModel(Identifier parentId, ResourceManager resourceManager,
                                                Map<Identifier, UnbakedModel> cache) {
        if (GeneratedItemModel.GENERATED.equals(parentId)) {
            GeneratedItemModel generated = new GeneratedItemModel();
            cache.put(parentId, generated);
            return generated;
        }

        Identifier modelIdentifier = toModelJson(parentId);
        try (InputStream is = resourceManager.getResource(modelIdentifier).orElseThrow().getInputStream();
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonUnbakedModel parent = JsonUnbakedModel.deserialize(reader);
            cache.put(parentId, parent);
            resolveParentChain(parent, resourceManager, cache);
            return parent;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Identifier toModelJson(Identifier modelId) {
        String path = modelId.getPath();
        if (!path.startsWith("models/"))
            path = "models/" + path;
        if (!path.endsWith(".json"))
            path = path + ".json";
        return Identifier.of(modelId.getNamespace(), path);
    }

    public Identifier fixDeprecatedSubItem(String subItem, PropertyGroup properties, int position) {
        if (subItem == null)
            return null;
        String replacement = switch (subItem) {
            case "bow_standby" -> "bow";
            case "crossbow_standby" -> "crossbow";
            case "potion_bottle_drinkable" -> "potion";
            case "potion_bottle_splash" -> "splash_potion";
            case "potion_bottle_lingering" -> "lingering_potion";


            default -> null;
        };

        if (replacement != null) {
            CITReborn.logWarnLoading(properties.messageWithDescriptorOf("Warning: Using deprecated sub item id \"" + subItem + "\" instead of \"" + replacement + "\"", position));

            return Identifier.of("minecraft", "item/" + replacement);
        }

        return null;
    }

    public BakedModel getItemModel(CITContext context, int seed) {
        return bakedModel;
    }

    public boolean isTexture() {
        return isTexture;
    }

    public Item getReferenceItem() {
        if (items.isEmpty())
            return null;
        return items.get(0);
    }

    public static BakedModel overrideTransformation(BakedModel model, ModelTransformation transformation) {
        if (model == null || transformation == null)
            return model;
        if (transformation.equals(model.getTransformation()))
            return model;
        return new TransformOverrideBakedModel(model, transformation);
    }

    private static ModelTextures.Textures buildOverrideTextures(Map<String, Either<SpriteIdentifier, String>> overrides) {
        ModelTextures.Textures.Builder builder = new ModelTextures.Textures.Builder();
        for (Map.Entry<String, Either<SpriteIdentifier, String>> entry : overrides.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank())
                key = "layer0";
            Either<SpriteIdentifier, String> value = entry.getValue();
            if (value.left().isPresent()) {
                builder.addSprite(key, value.left().get());
            } else if (value.right().isPresent()) {
                String target = value.right().get();
                if (target != null && target.startsWith("#"))
                    target = target.substring(1);
                if (target != null && !target.isBlank())
                    builder.addTextureReference(key, target);
            }
        }
        return builder.build();
    }

    private static ModelTextures.Textures mergeTextures(ModelTextures.Textures base, ModelTextures.Textures extra) {
        if (extra == ModelTextures.Textures.EMPTY || extra.values().isEmpty())
            return base;
        Map<String, ModelTextures.Entry> merged = new LinkedHashMap<>(base.values());
        merged.putAll(extra.values());
        return new ModelTextures.Textures(merged);
    }

    private static ModelTextures.Textures ensureParticle(ModelTextures.Textures textures) {
        if (textures.values().containsKey("particle"))
            return textures;
        String sourceKey = textures.values().containsKey("layer0")
                ? "layer0"
                : (textures.values().isEmpty() ? null : textures.values().keySet().iterator().next());
        if (sourceKey == null)
            return textures;
        ModelTextures.Textures.Builder builder = new ModelTextures.Textures.Builder()
                .addTextureReference("particle", sourceKey);
        return mergeTextures(textures, builder.build());
    }

    private static final class OverrideUnbakedModel implements UnbakedModel {
        private final UnbakedModel delegate;
        private final ModelTextures.Textures textures;

        private OverrideUnbakedModel(UnbakedModel delegate, ModelTextures.Textures textures) {
            this.delegate = delegate;
            this.textures = textures;
        }

        @Override
        public BakedModel bake(ModelTextures textures, Baker baker, net.minecraft.client.render.model.ModelBakeSettings settings,
                               boolean hasDepth, boolean usesBlockLight, ModelTransformation transformation) {
            return delegate.bake(textures, baker, settings, hasDepth, usesBlockLight, transformation);
        }

        @Override
        public Boolean getAmbientOcclusion() {
            return delegate.getAmbientOcclusion();
        }

        @Override
        public UnbakedModel.GuiLight getGuiLight() {
            return delegate.getGuiLight();
        }

        @Override
        public ModelTransformation getTransformation() {
            return delegate.getTransformation();
        }

        @Override
        public ModelTextures.Textures getTextures() {
            return textures;
        }

        @Override
        public UnbakedModel getParent() {
            return delegate.getParent();
        }

        @Override
        public void resolve(ResolvableModel.Resolver resolver) {
            delegate.resolve(resolver);
        }
    }

    private static final class TransformOverrideBakedModel implements BakedModel {
        private final BakedModel delegate;
        private final ModelTransformation transformation;

        private TransformOverrideBakedModel(BakedModel delegate, ModelTransformation transformation) {
            this.delegate = delegate;
            this.transformation = transformation;
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
            return delegate.getQuads(state, face, random);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public boolean hasDepth() {
            return delegate.hasDepth();
        }

        @Override
        public boolean isSideLit() {
            return delegate.isSideLit();
        }

        @Override
        public Sprite getParticleSprite() {
            return delegate.getParticleSprite();
        }

        @Override
        public ModelTransformation getTransformation() {
            return transformation;
        }
    }

    public static class Container extends CITTypeContainer<TypeItem> {
        public Container() {
            super(TypeItem.class, TypeItem::new, "item");
        }

        public Set<CIT<TypeItem>> loaded = new HashSet<>();
        public Map<Item, Set<CIT<TypeItem>>> loadedTyped = new IdentityHashMap<>();

        @Override
        public void load(List<CIT<TypeItem>> parsedCITs) {
            loaded.addAll(parsedCITs);
            for (CIT<TypeItem> cit : parsedCITs)
                for (CITCondition condition : cit.conditions)
                    if (condition instanceof ConditionItems items)
                        for (Item item : items.items)
                            if (item != null)
                                loadedTyped.computeIfAbsent(item, i -> new LinkedHashSet<>()).add(cit);
        }

        @Override
        public void dispose() {
            loaded.clear();
            loadedTyped.clear();
        }

        public CIT<TypeItem> getCIT(CITContext context, int seed) {
            return ((CITCacheItem) (Object) context.stack).CITReborn$getCacheTypeItem().get(context).get();
        }

        public CIT<TypeItem> getRealTimeCIT(CITContext context) {
            Set<CIT<TypeItem>> loadedForItemType = loadedTyped.get(context.stack.getItem());
            if (loadedForItemType != null)
                for (CIT<TypeItem> cit : loadedForItemType)
                    if (cit.test(context))
                        return cit;

            return null;
        }
    }

    public interface CITCacheItem {
        CITCache.Single<TypeItem> CITReborn$getCacheTypeItem();
    }

    public interface BakedModelManagerMixinAccess {
        void CITReborn$forceMojankModel(BakedModel model);
    }
}
