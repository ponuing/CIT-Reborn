package com.ponuing.pcustomtextures.client;

import com.ponuing.pcustomtextures.Pcustomtextures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelRotation;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.model.ModelNameSupplier;
import net.minecraft.client.model.SpriteGetter;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.lang.reflect.Method;

public final class NbtRenderOverrideResolver {
    private static final Identifier ITEM_ATLAS_ID = Identifier.ofVanilla("textures/atlas/items.png");
    private static final List<String> CIT_ROOTS = List.of("optifine", "mcpatcher", "citresewn");
    private static final CopyOnWriteArrayList<CitRule> RULES = new CopyOnWriteArrayList<>();
    private static volatile Map<Identifier, GeneratedModelDef> GENERATED_ITEM_MODELS = Map.of();
    private static volatile Map<Identifier, Identifier> ITEM_BASE_MODELS = Map.of();
    private static volatile Set<Identifier> EXTRA_ITEM_MODELS = Set.of();
    private static volatile Map<Identifier, List<CitRule>> ITEM_RULES = Map.of();
    private static final Map<Integer, Identifier> LEGACY_ENCHANTMENT_IDS = Map.ofEntries(
            java.util.Map.entry(0, Identifier.of("minecraft", "protection")),
            java.util.Map.entry(1, Identifier.of("minecraft", "fire_protection")),
            java.util.Map.entry(2, Identifier.of("minecraft", "feather_falling")),
            java.util.Map.entry(3, Identifier.of("minecraft", "blast_protection")),
            java.util.Map.entry(4, Identifier.of("minecraft", "projectile_protection")),
            java.util.Map.entry(5, Identifier.of("minecraft", "respiration")),
            java.util.Map.entry(6, Identifier.of("minecraft", "aqua_affinity")),
            java.util.Map.entry(7, Identifier.of("minecraft", "thorns")),
            java.util.Map.entry(8, Identifier.of("minecraft", "depth_strider")),
            java.util.Map.entry(9, Identifier.of("minecraft", "frost_walker")),
            java.util.Map.entry(10, Identifier.of("minecraft", "binding_curse")),
            java.util.Map.entry(16, Identifier.of("minecraft", "sharpness")),
            java.util.Map.entry(17, Identifier.of("minecraft", "smite")),
            java.util.Map.entry(18, Identifier.of("minecraft", "bane_of_arthropods")),
            java.util.Map.entry(19, Identifier.of("minecraft", "knockback")),
            java.util.Map.entry(20, Identifier.of("minecraft", "fire_aspect")),
            java.util.Map.entry(21, Identifier.of("minecraft", "looting")),
            java.util.Map.entry(22, Identifier.of("minecraft", "sweeping_edge")),
            java.util.Map.entry(32, Identifier.of("minecraft", "efficiency")),
            java.util.Map.entry(33, Identifier.of("minecraft", "silk_touch")),
            java.util.Map.entry(34, Identifier.of("minecraft", "unbreaking")),
            java.util.Map.entry(35, Identifier.of("minecraft", "fortune")),
            java.util.Map.entry(48, Identifier.of("minecraft", "power")),
            java.util.Map.entry(49, Identifier.of("minecraft", "punch")),
            java.util.Map.entry(50, Identifier.of("minecraft", "flame")),
            java.util.Map.entry(51, Identifier.of("minecraft", "infinity")),
            java.util.Map.entry(61, Identifier.of("minecraft", "luck_of_the_sea")),
            java.util.Map.entry(62, Identifier.of("minecraft", "lure")),
            java.util.Map.entry(65, Identifier.of("minecraft", "riptide")),
            java.util.Map.entry(66, Identifier.of("minecraft", "loyalty")),
            java.util.Map.entry(67, Identifier.of("minecraft", "channeling")),
            java.util.Map.entry(68, Identifier.of("minecraft", "impaling")),
            java.util.Map.entry(70, Identifier.of("minecraft", "mending")),
            java.util.Map.entry(71, Identifier.of("minecraft", "vanishing_curse"))
    );
    private static final Map<Identifier, Integer> LEGACY_STATUS_EFFECT_IDS = Map.ofEntries(
            java.util.Map.entry(Identifier.of("minecraft", "speed"), 1),
            java.util.Map.entry(Identifier.of("minecraft", "slowness"), 2),
            java.util.Map.entry(Identifier.of("minecraft", "haste"), 3),
            java.util.Map.entry(Identifier.of("minecraft", "mining_fatigue"), 4),
            java.util.Map.entry(Identifier.of("minecraft", "strength"), 5),
            java.util.Map.entry(Identifier.of("minecraft", "instant_health"), 6),
            java.util.Map.entry(Identifier.of("minecraft", "instant_damage"), 7),
            java.util.Map.entry(Identifier.of("minecraft", "jump_boost"), 8),
            java.util.Map.entry(Identifier.of("minecraft", "nausea"), 9),
            java.util.Map.entry(Identifier.of("minecraft", "regeneration"), 10),
            java.util.Map.entry(Identifier.of("minecraft", "resistance"), 11),
            java.util.Map.entry(Identifier.of("minecraft", "fire_resistance"), 12),
            java.util.Map.entry(Identifier.of("minecraft", "water_breathing"), 13),
            java.util.Map.entry(Identifier.of("minecraft", "invisibility"), 14),
            java.util.Map.entry(Identifier.of("minecraft", "blindness"), 15),
            java.util.Map.entry(Identifier.of("minecraft", "night_vision"), 16),
            java.util.Map.entry(Identifier.of("minecraft", "hunger"), 17),
            java.util.Map.entry(Identifier.of("minecraft", "weakness"), 18),
            java.util.Map.entry(Identifier.of("minecraft", "poison"), 19),
            java.util.Map.entry(Identifier.of("minecraft", "wither"), 20),
            java.util.Map.entry(Identifier.of("minecraft", "health_boost"), 21),
            java.util.Map.entry(Identifier.of("minecraft", "absorption"), 22),
            java.util.Map.entry(Identifier.of("minecraft", "saturation"), 23),
            java.util.Map.entry(Identifier.of("minecraft", "glowing"), 24),
            java.util.Map.entry(Identifier.of("minecraft", "levitation"), 25),
            java.util.Map.entry(Identifier.of("minecraft", "luck"), 26),
            java.util.Map.entry(Identifier.of("minecraft", "unluck"), 27),
            java.util.Map.entry(Identifier.of("minecraft", "slow_falling"), 28),
            java.util.Map.entry(Identifier.of("minecraft", "conduit_power"), 29),
            java.util.Map.entry(Identifier.of("minecraft", "dolphins_grace"), 30),
            java.util.Map.entry(Identifier.of("minecraft", "bad_omen"), 31),
            java.util.Map.entry(Identifier.of("minecraft", "hero_of_the_village"), 32),
            java.util.Map.entry(Identifier.of("minecraft", "darkness"), 33)
    );
    private static final int RULE_CACHE_LIMIT = 2048;
    private static final Map<RuleCacheKey, CitRule> RULE_CACHE = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<RuleCacheKey, CitRule> eldest) {
                    return size() > RULE_CACHE_LIMIT;
                }
            }
    );
    private static final int MODEL_SPRITE_CACHE_LIMIT = 1024;
    private static final Map<Identifier, Sprite> MODEL_SPRITE_CACHE = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Identifier, Sprite> eldest) {
                    return size() > MODEL_SPRITE_CACHE_LIMIT;
                }
            }
    );
    private static final int MODEL_BAKED_CACHE_LIMIT = 256;
    private static final Map<Identifier, BakedModel> MODEL_BAKED_CACHE = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Identifier, BakedModel> eldest) {
                    return size() > MODEL_BAKED_CACHE_LIMIT;
                }
            }
    );
    private static final java.util.concurrent.atomic.AtomicBoolean INITIAL_LOAD_DONE = new java.util.concurrent.atomic.AtomicBoolean(false);

    private enum RuleType {
        ITEM,
        ARMOR,
        ELYTRA
    }

    public enum HandMatch {
        ANY,
        MAIN,
        OFF;

        static HandMatch parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return ANY;
            }
            String value = raw.trim().toLowerCase(Locale.ROOT);
            return switch (value) {
                case "main", "mainhand", "main_hand" -> MAIN;
                case "off", "offhand", "off_hand" -> OFF;
                default -> ANY;
            };
        }
    }

    private NbtRenderOverrideResolver() {
    }

    public static ItemStack resolveItemStackForRender(ItemStack original) {
        return original;
    }

    public static Identifier resolveItemTextureOverride(ItemStack stack) {
        return resolveItemTextureOverride(stack, HandMatch.ANY);
    }

    public static Identifier resolveItemTextureOverride(ItemStack stack, HandMatch hand) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ensureLoaded();
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        //Pcustomtextures.LOGGER.info("[pcustomtextures][item] resolve texture for {} x{}", itemId, stack.getCount());
        CitRule rule = findMatchingRule(stack, RuleType.ITEM, hand);
        if (rule == null) {
            //Pcustomtextures.LOGGER.info("[pcustomtextures][item] no matching rule or no texture candidates for {}", itemId);
            return null;
        }

        if (rule.sourceTexture() != null) {
            Identifier textureId = rule.sourceTexture().id();
            //Pcustomtextures.LOGGER.info("[pcustomtextures][item] matched rule, using texture {}", textureId);
            return textureId;
        }

        if (rule.itemTextureCandidates().isEmpty()) {
            //Pcustomtextures.LOGGER.info("[pcustomtextures][item] matched rule but no texture candidates for {}", itemId);
            return null;
        }

        Identifier found = findExistingTexture(rule.itemTextureCandidates());
        if (found == null) {
            //Pcustomtextures.LOGGER.info("[pcustomtextures][item] matched rule but no texture resource found for {}", itemId);
            return null;
        }

        //Pcustomtextures.LOGGER.info("[pcustomtextures][item] matched rule, using texture {}", found);
        return found;
    }

    public static Identifier resolveItemModelOverride(ItemStack stack) {
        return resolveItemModelOverride(stack, HandMatch.ANY);
    }

    public static Identifier resolveItemModelOverride(ItemStack stack, HandMatch hand) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        ensureLoaded();
        CitRule rule = findMatchingRule(stack, RuleType.ITEM, hand);
        if (rule == null) {
            return null;
        }

        if (rule.itemModelId() != null) {
            return rule.itemModelId();
        }

        return null;
    }

    public static Identifier resolveElytraTextureOverride(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ensureLoaded();
        CitRule rule = findMatchingRule(stack, RuleType.ELYTRA, HandMatch.ANY);
        if (rule == null || rule.elytraTextureCandidates().isEmpty()) {
            return null;
        }
        return findExistingTexture(rule.elytraTextureCandidates());
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || textureId == null) {
            return null;
        }
        Identifier spriteId = toSpriteId(textureId);
        if (spriteId == null) {
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
        try (InputStream in = resource.getInputStream()) {
            String jsonText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (optifineModel) {
                jsonText = normalizeOptifineModelJson(modelPath, jsonText);
            }
            JsonElement element = JsonParser.parseString(jsonText);
            if (!element.isJsonObject()) {
                return null;
            }
            JsonObject obj = element.getAsJsonObject();
            JsonElement texturesElement = obj.get("textures");
            if (texturesElement == null || !texturesElement.isJsonObject()) {
                return null;
            }
            Map<String, String> textures = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : texturesElement.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    textures.put(entry.getKey(), value.getAsString());
                }
            }
            if (textures.isEmpty()) {
                return null;
            }
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
        try (InputStream in = resource.getInputStream()) {
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
        try (InputStream in = resource.getInputStream()) {
            String jsonText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (optifineModel) {
                jsonText = normalizeOptifineModelJson(modelPath, jsonText);
            }
            JsonElement element = JsonParser.parseString(jsonText);
            if (!element.isJsonObject()) {
                return null;
            }
            JsonObject obj = element.getAsJsonObject();
            JsonElement texturesElement = obj.get("textures");
            if (texturesElement == null || !texturesElement.isJsonObject()) {
                return null;
            }
            Map<String, String> textures = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : texturesElement.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    textures.put(entry.getKey(), value.getAsString());
                }
            }
            if (textures.isEmpty()) {
                return null;
            }
            String textureValue = resolveTextureReference(textures);
            if (textureValue == null) {
                return null;
            }
            Identifier textureId = resolveTextureIdentifier(modelId, textureValue);
            Sprite sprite = resolveSprite(textureId);
            if (!isMissingSprite(sprite)) {
                return sprite;
            }
            Identifier alias = resolveOptifineAlias(textureId);
            Sprite aliasSprite = resolveSprite(alias);
            if (!isMissingSprite(aliasSprite)) {
                return aliasSprite;
            }
            return sprite;
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveTextureReference(Map<String, String> textures) {
        if (textures.containsKey("layer0")) {
            return resolveTextureReference(textures, "layer0", 0);
        }
        if (textures.containsKey("0")) {
            return resolveTextureReference(textures, "0", 0);
        }
        String firstKey = textures.keySet().iterator().next();
        return resolveTextureReference(textures, firstKey, 0);
    }

    private static String resolveTextureReference(Map<String, String> textures, String key, int depth) {
        if (depth > 5 || key == null) {
            return null;
        }
        String value = textures.get(key);
        if (value == null) {
            return null;
        }
        if (value.startsWith("#")) {
            return resolveTextureReference(textures, value.substring(1), depth + 1);
        }
        return value;
    }

    private static Identifier resolveTextureIdentifier(Identifier modelId, String textureValue) {
        if (textureValue == null) {
            return null;
        }
        String namespace = modelId.getNamespace();
        String path = textureValue;
        int colon = textureValue.indexOf(':');
        if (colon >= 0) {
            namespace = textureValue.substring(0, colon);
            path = textureValue.substring(colon + 1);
        }
        int slash = modelId.getPath().lastIndexOf('/');
        String modelDir = slash >= 0 ? modelId.getPath().substring(0, slash) : modelId.getPath();
        if (path.startsWith("./")) {
            path = modelDir + "/" + path.substring(2);
        } else if (!path.startsWith("item/") && !path.startsWith("block/") && !path.startsWith("textures/") && !path.contains("/")) {
            path = modelDir + "/" + path;
        }
        if (path.startsWith("textures/")) {
            String trimmed = path.substring("textures/".length());
            if (isCitRootPath(trimmed)) {
                path = trimmed;
            }
        }
        if (!path.startsWith("textures/") && !isCitRootPath(path)) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        return Identifier.of(namespace, path);
    }

    private static boolean isCitRootPath(String path) {
        if (path == null) {
            return false;
        }
        if (path.startsWith("cit/")) {
            return true;
        }
        for (String root : CIT_ROOTS) {
            if (path.startsWith(root + "/cit/")) {
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
        String rest;
        if (path.startsWith("textures/optifine/cit/")) {
            rest = path.substring("textures/optifine/cit/".length());
        } else if (path.startsWith("optifine/cit/")) {
            rest = path.substring("optifine/cit/".length());
        } else {
            return null;
        }
        return Identifier.of(textureId.getNamespace(), "textures/item/optifine_cit/" + rest);
    }

    private static boolean isMissingSprite(Sprite sprite) {
        if (sprite == null) {
            return true;
        }
        try {
            return MissingSprite.getMissingSpriteId().equals(sprite.getContents().getId());
        } catch (Exception e) {
            return false;
        }
    }


    private static final class SimpleModelBaker implements Baker {
        private final ResourceManager manager;
        private final SpriteGetter spriteGetter;
        private final ModelNameSupplier nameSupplier;
        private final Map<Identifier, UnbakedModel> unbakedCache = new HashMap<>();
        private final Map<Identifier, BakedModel> bakedCache = new HashMap<>();
        private final Set<Identifier> inProgress = new HashSet<>();
        private final BakedModel missingModel;

        private SimpleModelBaker(MinecraftClient client, ResourceManager manager) {
            this.manager = manager;
            this.missingModel = client.getBakedModelManager().getMissingBlockModel();
            this.spriteGetter = new SpriteGetter() {
                @Override
                public Sprite get(SpriteIdentifier id) {
                    try {
                        return client.getSpriteAtlas(id.getAtlasId()).apply(id.getTextureId());
                    } catch (Exception e) {
                        return getMissing("sprite");
                    }
                }

                @Override
                public Sprite getMissing(String name) {
                    try {
                        return client.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(MissingSprite.getMissingSpriteId());
                    } catch (Exception e) {
                        return null;
                    }
                }
            };
            this.nameSupplier = () -> "pcustomtextures";
        }

        @Override
        public BakedModel bake(Identifier id, ModelBakeSettings settings) {
            if (id == null) {
                return missingModel;
            }
            BakedModel baked = bakedCache.get(id);
            if (baked != null) {
                return baked;
            }
            if (!inProgress.add(id)) {
                return missingModel;
            }
            try {
                UnbakedModel unbaked = unbakedCache.get(id);
                if (unbaked == null) {
                    unbaked = loadUnbakedModel(manager, id);
                    if (unbaked != null) {
                        unbakedCache.put(id, unbaked);
                    }
                }
                if (unbaked == null) {
                    return missingModel;
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
            return spriteGetter;
        }

        @Override
        public ModelNameSupplier getModelNameSupplier() {
            return nameSupplier;
        }
    }

    public static Map<Identifier, GeneratedModelDef> getGeneratedItemModels() {
        return GENERATED_ITEM_MODELS;
    }

    public static Set<Identifier> getExtraItemModels() {
        return EXTRA_ITEM_MODELS;
    }

    public static Identifier resolveArmorTextureOverride(ItemStack stack, Identifier originalTextureId) {
        ensureLoaded();
        CitRule rule = findMatchingRule(stack, RuleType.ARMOR, HandMatch.ANY);
        if (rule == null || rule.armorTextures().isEmpty() || originalTextureId == null) {
            return null;
        }

        String path = originalTextureId.getPath();
        int slash = path.lastIndexOf('/');
        String file = slash >= 0 ? path.substring(slash + 1) : path;
        if (!file.endsWith(".png")) {
            return null;
        }

        String base = file.substring(0, file.length() - 4);
        int layer = path.contains("/humanoid_leggings/") ? 2 : (path.contains("/humanoid/") ? 1 : -1);
        if (layer == -1) {
            return null;
        }

        List<String> keys = new ArrayList<>();
        if (base.endsWith("_overlay")) {
            String plain = base.substring(0, base.length() - "_overlay".length());
            keys.add((plain + "_layer_" + layer + "_overlay").toLowerCase(Locale.ROOT));
            keys.add((base + "_layer_" + layer).toLowerCase(Locale.ROOT));
        } else {
            keys.add((base + "_layer_" + layer).toLowerCase(Locale.ROOT));
            keys.add(base.toLowerCase(Locale.ROOT));
        }

        for (String key : keys) {
            List<Identifier> ids = rule.armorTextures().get(key);
            if (ids != null && !ids.isEmpty()) {
                Identifier existing = findExistingTexture(ids);
                return existing;
            }
        }

        return null;
    }

    private static Identifier findExistingTexture(List<Identifier> candidates) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getResourceManager() == null) {
            return null;
        }

        var manager = client.getResourceManager();
        for (Identifier candidate : candidates) {
            //Pcustomtextures.LOGGER.info("[pcustomtextures][item] checking texture candidate {}", candidate);
            if (manager.getResource(candidate).isPresent()) {
                return candidate;
            }
        }
        return null;
    }

    static void ensureLoaded() {
        if (INITIAL_LOAD_DONE.get()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        ResourceManager manager = client.getResourceManager();
        if (manager == null) {
            return;
        }
        if (!INITIAL_LOAD_DONE.compareAndSet(false, true)) {
            return;
        }
        reloadInternalFromManager(manager);
    }

    static void reloadFromManager(ResourceManager manager) {
        if (manager == null) {
            return;
        }
        INITIAL_LOAD_DONE.set(true);
        reloadInternalFromManager(manager);
    }

    private static void reloadInternalFromManager(ResourceManager manager) {
        try {
            //Pcustomtextures.LOGGER.info("[pcustomtextures][model] reload start");
            ITEM_BASE_MODELS = loadItemAssetModelsFromManager(manager);
            List<CitRule> parsedRules = scanCitRules(manager);

            RULES.clear();
            RULES.addAll(parsedRules);
            RULE_CACHE.clear();
            ITEM_RULES = indexRulesByItem(parsedRules);
            GENERATED_ITEM_MODELS = buildGeneratedItemModelMap(parsedRules, ITEM_BASE_MODELS);
            EXTRA_ITEM_MODELS = collectExplicitItemModels(parsedRules);
            MODEL_SPRITE_CACHE.clear();
            MODEL_BAKED_CACHE.clear();

            Pcustomtextures.LOGGER.info("[pcustomtextures][model] loaded rules={}, genModels={}, explicitModels={}", parsedRules.size(), GENERATED_ITEM_MODELS.size(), EXTRA_ITEM_MODELS.size());
        } catch (Exception e) {
            INITIAL_LOAD_DONE.set(false);
            RULES.clear();
            Pcustomtextures.LOGGER.error("Failed to reload OptiFine CIT rules", e);
        }
    }

    private static List<CitRule> scanCitRules(ResourceManager manager) {
        List<CitRule> rules = new ArrayList<>();
        if (manager == null) {
            return rules;
        }

        TextureReader reader = (id) -> readFromManager(manager, id);
        for (String root : CIT_ROOTS) {
            ResourceFinder finder = new ResourceFinder(root + "/cit", ".properties");
            for (Map.Entry<Identifier, Resource> entry : finder.findResources(manager).entrySet()) {
                Identifier id = entry.getKey();
                byte[] bytes = readResourceBytes(entry.getValue());
                if (bytes == null) {
                    continue;
                }
                parseProperties(id.getNamespace(), id.getPath(), bytes, rules, reader);
            }
        }
        scanPotionTextures(manager, rules, reader);

        rules.sort(Comparator.comparingInt(CitRule::weight).reversed());
        return rules;
    }

    private static void scanPotionTextures(ResourceManager manager, List<CitRule> rules, TextureReader reader) {
        if (manager == null) {
            return;
        }
        if (rules == null) {
            return;
        }

        addPotionRulesForFolder(manager, rules, reader, "normal", Items.POTION);
        addPotionRulesForFolder(manager, rules, reader, "splash", Items.SPLASH_POTION);
        addPotionRulesForFolder(manager, rules, reader, "linger", Items.LINGERING_POTION);
    }

    private static void addPotionRulesForFolder(ResourceManager manager, List<CitRule> rules, TextureReader reader, String folder, net.minecraft.item.Item item) {
        for (String root : CIT_ROOTS) {
            ResourceFinder finder = new ResourceFinder(root + "/cit/potion/" + folder, ".png");
            for (Map.Entry<Identifier, Resource> entry : finder.findResources(manager).entrySet()) {
                Identifier id = entry.getKey();
                String path = id.getPath();
                int slash = path.lastIndexOf('/');
                String name = slash >= 0 ? path.substring(slash + 1) : path;
                if (!name.endsWith(".png")) {
                    continue;
                }
                name = name.substring(0, name.length() - 4);
                if (name.isBlank()) {
                    continue;
                }

                List<Identifier> textureCandidates = List.of(Identifier.of(id.getNamespace(), path));
                SourceTexture sourceTexture = findFirstTextureBytes(textureCandidates, reader);
                int weight = -1;
                String ruleKey = "potion:" + id.getNamespace() + ":" + path;

                if ("empty".equals(name) && "normal".equals(folder)) {
                    Set<Identifier> items = Set.of(Registries.ITEM.getId(Items.GLASS_BOTTLE));
                    rules.add(new CitRule(
                            RuleType.ITEM,
                            items,
                            List.of(),
                            null,
                            null,
                            null,
                            null,
                            Set.of(),
                            null,
                            null,
                            null,
                            weight,
                            HandMatch.ANY,
                            null,
                            sourceTexture,
                            ruleKey,
                            textureCandidates,
                            Map.of(),
                            List.of()
                    ));
                    continue;
                }

                if ("other".equals(name)) {
                    for (String potionName : NO_EFFECT_POTION_NAMES) {
                        Identifier potionId = Identifier.of("minecraft", potionName);
                        Set<Identifier> items = Set.of(Registries.ITEM.getId(item));
                        rules.add(new CitRule(
                                RuleType.ITEM,
                                items,
                                List.of(),
                                null,
                                null,
                                null,
                                null,
                                Set.of(),
                                null,
                                null,
                                potionId,
                                weight,
                                HandMatch.ANY,
                                null,
                                sourceTexture,
                                ruleKey + ":" + potionName,
                                textureCandidates,
                                Map.of(),
                                List.of()
                        ));
                    }
                    continue;
                }

                Identifier potionId = Identifier.of("minecraft", name);
                Set<Identifier> items = Set.of(Registries.ITEM.getId(item));
                rules.add(new CitRule(
                        RuleType.ITEM,
                        items,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        Set.of(),
                        null,
                        null,
                        potionId,
                        weight,
                        HandMatch.ANY,
                        null,
                        sourceTexture,
                        ruleKey,
                        textureCandidates,
                        Map.of(),
                        List.of()
                ));
            }
        }
    }

    private static final List<String> NO_EFFECT_POTION_NAMES = List.of(
            "artless",
            "awkward",
            "bland",
            "bulky",
            "bungling",
            "buttered",
            "charming",
            "clear",
            "cordial",
            "dashing",
            "debonair",
            "elegant",
            "fancy",
            "flat",
            "foul",
            "gross",
            "harsh",
            "milky",
            "mundane",
            "odorless",
            "potent",
            "rank",
            "sparkling",
            "stinky",
            "suave",
            "thick",
            "thin",
            "uninteresting"
    );

    private static void parseProperties(String defaultNamespace, String propertiesPath, byte[] propertiesBytes, List<CitRule> out, TextureReader reader) {
        Properties properties = new Properties();
        try (InputStream in = new ByteArrayInputStream(propertiesBytes);
             InputStreamReader propReader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            properties.load(propReader);
        } catch (IOException e) {
            return;
        }

        String typeValue = getProperty(properties, "type");
        String type = (typeValue == null || typeValue.isBlank() ? "item" : typeValue).trim().toLowerCase(Locale.ROOT);
        RuleType ruleType;
        if (type.equals("item")) {
            ruleType = RuleType.ITEM;
        } else if (type.equals("armor")) {
            ruleType = RuleType.ARMOR;
        } else if (type.equals("elytra")) {
            ruleType = RuleType.ELYTRA;
        } else {
            return;
        }

        String itemsRaw = getProperty(properties, "items");
        String matchItemsRaw = getProperty(properties, "matchItems");
        Set<Identifier> items = parseItems(itemsRaw != null ? itemsRaw : (matchItemsRaw == null ? "" : matchItemsRaw));
        if (items.isEmpty()) {
            return;
        }

        Identifier itemModelId = null;
        List<Identifier> itemTextureCandidates = new ArrayList<>();
        Map<String, List<Identifier>> armorTextures = new HashMap<>();
        List<Identifier> elytraTextureCandidates = new ArrayList<>();
        SourceTexture sourceTexture = null;
        String normalizedPath = propertiesPath == null ? "unknown" : propertiesPath.replace('\\', '/');
        String ruleKey = sha1Hex((defaultNamespace == null ? "" : defaultNamespace + ":") + normalizedPath);

        if (ruleType == RuleType.ITEM) {
            String modelRaw = getProperty(properties, "model");
            if (modelRaw != null && !modelRaw.isBlank()) {
                itemModelId = parseItemModelIdentifier(defaultNamespace, propertiesPath, modelRaw);
            }

            String tileRaw = getProperty(properties, "tile");
            String tilesRaw = getProperty(properties, "tiles");
            String textureRaw = getProperty(properties, "texture");
            if (tileRaw != null && !tileRaw.isBlank()) {
                itemTextureCandidates.addAll(parseTileIdentifiers(defaultNamespace, propertiesPath, tileRaw));
            } else if (tilesRaw != null && !tilesRaw.isBlank()) {
                itemTextureCandidates.addAll(parseTilesList(defaultNamespace, propertiesPath, tilesRaw));
            } else if (textureRaw != null && !textureRaw.isBlank()) {
                itemTextureCandidates.addAll(parseTileIdentifiers(defaultNamespace, propertiesPath, textureRaw));
            }

            if (itemModelId == null && itemTextureCandidates.isEmpty()) {
                itemTextureCandidates.addAll(parseImpliedTexture(defaultNamespace, propertiesPath));
            }

            if (itemModelId == null && !itemTextureCandidates.isEmpty()) {
                sourceTexture = findFirstTextureBytes(itemTextureCandidates, reader);
                if (sourceTexture != null) {
                    //Pcustomtextures.LOGGER.info("[pcustomtextures][model] source texture for rule {} -> {} ({} bytes)", ruleKey, sourceTexture.id(), sourceTexture.bytes().length);
                } else {
                    //Pcustomtextures.LOGGER.info("[pcustomtextures][model] no source texture bytes for rule {}", ruleKey);
                }
            }
        }

        if (ruleType == RuleType.ARMOR) {
            for (String key : properties.stringPropertyNames()) {
                String normalized = stripNamespacePrefix(key);
                if (!normalized.startsWith("texture.")) {
                    continue;
                }

                String textureKey = normalized.substring("texture.".length()).toLowerCase(Locale.ROOT);
                String rawPath = properties.getProperty(key);
                List<Identifier> textureIds = parseTextureIdentifiers(defaultNamespace, propertiesPath, rawPath);
                if (!textureIds.isEmpty()) {
                    armorTextures.put(textureKey, textureIds);
                }
            }
        }

        if (ruleType == RuleType.ELYTRA) {
            String tilesRaw = getProperty(properties, "tiles");
            String textureRaw = getProperty(properties, "texture");
            String tileRaw = getProperty(properties, "tile");
            String textureElytraRaw = getProperty(properties, "texture.elytra");
            if ((textureRaw == null || textureRaw.isBlank()) && textureElytraRaw != null && !textureElytraRaw.isBlank()) {
                textureRaw = textureElytraRaw;
            }

            String singleRaw = null;
            if (tilesRaw != null && !tilesRaw.isBlank()) {
                elytraTextureCandidates.addAll(parseTilesList(defaultNamespace, propertiesPath, tilesRaw));
            } else if (textureRaw != null && !textureRaw.isBlank()) {
                elytraTextureCandidates.addAll(parseTileIdentifiers(defaultNamespace, propertiesPath, textureRaw));
                singleRaw = textureRaw;
            } else if (tileRaw != null && !tileRaw.isBlank()) {
                elytraTextureCandidates.addAll(parseTileIdentifiers(defaultNamespace, propertiesPath, tileRaw));
                singleRaw = tileRaw;
            }

            if (singleRaw != null) {
                addElytraEntityCandidate(elytraTextureCandidates, defaultNamespace, singleRaw);
            }

            if (elytraTextureCandidates.isEmpty()) {
                elytraTextureCandidates.addAll(parseImpliedTexture(defaultNamespace, propertiesPath));
            }
        }

        if (itemModelId == null && itemTextureCandidates.isEmpty() && armorTextures.isEmpty() && elytraTextureCandidates.isEmpty()) {
            return;
        }

        List<PathMatcherRule> matchers = parseMatchers(properties);
        RangeMatcher damageMatcher = parseRangeMatcher(getProperty(properties, "damage"));
        Integer damageMask = parseIntFlexible(getProperty(properties, "damageMask")).orElse(null);
        RangeMatcher stackSizeMatcher = parseRangeMatcher(getProperty(properties, "stackSize"));
        RangeMatcher enchantLevelMatcher = parseRangeMatcher(getProperty(properties, "enchantmentLevels"));
        String enchantmentsRaw = getProperty(properties, "enchantments");
        if (enchantmentsRaw == null || enchantmentsRaw.isBlank()) {
            enchantmentsRaw = getProperty(properties, "enchantmentIDs");
        }
        Set<Identifier> enchantmentIds = parseIdentifierSet(enchantmentsRaw);
        Boolean damaged = parseBoolean01(getProperty(properties, "damaged"));
        Boolean unbreakable = parseBoolean01(getProperty(properties, "unbreakable"));
        Identifier potion = parsePotion(getProperty(properties, "potion"));
        int weight = parseInt(getProperty(properties, "weight")).orElse(0);
        HandMatch handMatch = HandMatch.parse(getProperty(properties, "hand"));

        out.add(new CitRule(
                ruleType,
                items,
                matchers,
                damageMatcher,
                damageMask,
                stackSizeMatcher,
                enchantLevelMatcher,
                enchantmentIds,
                damaged,
                unbreakable,
                potion,
                weight,
                handMatch,
                itemModelId,
                sourceTexture,
                ruleKey,
                itemTextureCandidates,
                armorTextures,
                elytraTextureCandidates
        ));
    }

    private static Map<Identifier, GeneratedModelDef> buildGeneratedItemModelMap(List<CitRule> rules, Map<Identifier, Identifier> baseModels) {
        Map<Identifier, GeneratedModelDef> map = new HashMap<>();
        for (CitRule rule : rules) {
            if (rule.type() != RuleType.ITEM) {
                continue;
            }
            if (rule.sourceTexture() == null) {
                continue;
            }
            for (Identifier itemId : rule.items()) {
                Identifier modelId = buildGeneratedModelId(rule.ruleKey(), itemId);
                Identifier parent = baseModels.getOrDefault(itemId, Identifier.ofVanilla("item/generated"));
                Identifier textureId = rule.sourceTexture().id();
                //Pcustomtextures.LOGGER.info("[pcustomtextures][model] gen model for {} parent={} texture={} sourceTex={} bytes={}", itemId, parent, textureId, rule.sourceTexture().id(), rule.sourceTexture().bytes().length);
                map.put(modelId, new GeneratedModelDef(parent, textureId, rule.sourceTexture()));
            }
        }
        return Map.copyOf(map);
    }

    private static Set<Identifier> collectExplicitItemModels(List<CitRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Set.of();
        }
        Set<Identifier> result = new HashSet<>();
        for (CitRule rule : rules) {
            if (rule.type() != RuleType.ITEM) {
                continue;
            }
            Identifier modelId = rule.itemModelId();
            if (modelId != null) {
                result.add(modelId);
            }
        }
        return result.isEmpty() ? Set.of() : Set.copyOf(result);
    }

    private static Identifier buildGeneratedModelId(String ruleKey, Identifier itemId) {
        String itemPath = itemId.getNamespace() + "/" + itemId.getPath();
        return Identifier.of("pcustomtextures", "item/cit/" + ruleKey + "/" + itemPath);
    }

    private static String sha1Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    static String normalizeOptifineModelJson(String relPath, String jsonText) {
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

    private static String resolveOptifineTexturePath(String dir, String tex) {
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

    private static String resolveOptifineModelPath(String dir, String raw) {
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

    private static Map<Identifier, Identifier> loadItemAssetModelsFromManager(ResourceManager manager) {
        if (manager == null) {
            return Map.of();
        }

        Map<Identifier, Identifier> result = new HashMap<>();
        ResourceFinder finder = ResourceFinder.json("items");
        for (Map.Entry<Identifier, Resource> entry : finder.findResources(manager).entrySet()) {
            Identifier itemId = finder.toResourceId(entry.getKey());
            if (itemId == null) {
                continue;
            }
            byte[] bytes = readResourceBytes(entry.getValue());
            if (bytes == null) {
                continue;
            }
            try (InputStream in = new ByteArrayInputStream(bytes);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement json = JsonParser.parseReader(reader);
                if (!json.isJsonObject()) {
                    continue;
                }
                JsonObject obj = json.getAsJsonObject();
                JsonElement modelEl = obj.get("model");
                if (modelEl == null) {
                    continue;
                }
                Identifier modelId = parseItemAssetModel(modelEl);
                if (modelId != null) {
                    result.put(itemId, modelId);
                }
            } catch (Exception e) {
                //Pcustomtextures.LOGGER.warn("Failed to read item asset {}", itemId, e);
            }
        }
        return Map.copyOf(result);
    }

    private static Identifier parseItemAssetModel(JsonElement modelEl) {
        if (modelEl.isJsonPrimitive()) {
            return Identifier.tryParse(modelEl.getAsString());
        }
        if (!modelEl.isJsonObject()) {
            return null;
        }
        JsonObject modelObj = modelEl.getAsJsonObject();
        String type = modelObj.has("type") ? modelObj.get("type").getAsString() : "";
        if (!"minecraft:model".equals(type)) {
            return null;
        }
        if (!modelObj.has("model")) {
            return null;
        }
        return Identifier.tryParse(modelObj.get("model").getAsString());
    }

    private static Identifier parseItemModelIdentifier(String namespace, String propertiesPath, String raw) {
        String model = raw.trim().replace("\\", "/");
        if (model.endsWith(".json")) {
            model = model.substring(0, model.length() - 5);
        }

        if (model.contains(":")) {
            return Identifier.tryParse(model);
        }

        if (model.startsWith("models/")) {
            model = model.substring("models/".length());
        }

        if (model.startsWith("item/")
                || model.startsWith("optifine/")
                || model.startsWith("mcpatcher/")
                || model.startsWith("citresewn/")
                || model.startsWith("cit/")) {
            return Identifier.of(namespace, model);
        }

        if (propertiesPath != null && !propertiesPath.isBlank()) {
            String dir = getResourceDir(namespace, propertiesPath);
            if (!dir.isBlank()) {
                return Identifier.of(namespace, normalizePath(dir + "/" + model));
            }
        }

        return Identifier.of(namespace, "item/" + model);
    }

    private static List<Identifier> parseTilesList(String namespace, String propertiesPath, String raw) {
        List<Identifier> ids = new ArrayList<>();
        String[] tokens = raw.trim().split("\\s+");
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (token.equals("<skip>") || token.equals("<default>")) {
                continue;
            }
            if (token.matches("\\d+-\\d+")) {
                String[] parts = token.split("-");
                int start = parseInt(parts[0]).orElse(-1);
                int end = parseInt(parts[1]).orElse(-1);
                if (start >= 0 && end >= start) {
                    for (int i = start; i <= end; i++) {
                        ids.addAll(parseTileIdentifiers(namespace, propertiesPath, String.valueOf(i)));
                    }
                }
            } else {
                ids.addAll(parseTileIdentifiers(namespace, propertiesPath, token));
            }
        }
        return ids;
    }

    private static List<Identifier> parseTileIdentifiers(String namespace, String propertiesPath, String raw) {
        return parseTextureIdentifiers(namespace, propertiesPath, raw);
    }

    private static List<Identifier> parseTextureIdentifiers(String namespace, String propertiesPath, String raw) {
        List<Identifier> ids = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return ids;
        }

        String path = raw.trim().replace("\\", "/");
        if (!path.endsWith(".png")) {
            path += ".png";
        }

        if (path.contains(":")) {
            String[] split = path.split(":", 2);
            String ns = split[0];
            String p = normalizePath(split[1]);
            addId(ids, ns, p);
            if (!p.startsWith("textures/")) {
                addId(ids, ns, "textures/" + p);
            }
            return ids;
        }

        if (path.startsWith("./")) {
            String dir = getResourceDir(namespace, propertiesPath);
            String p = normalizePath(dir + "/" + path.substring(2));
            addId(ids, namespace, p);
            if (!p.startsWith("textures/")) {
                addId(ids, namespace, "textures/" + p);
            }
            return ids;
        }

        if (!path.contains("/")) {
            String dir = getResourceDir(namespace, propertiesPath);
            if (!dir.isBlank()) {
                String rel = normalizePath(dir + "/" + path);
                addId(ids, namespace, rel);
                if (!rel.startsWith("textures/")) {
                    addId(ids, namespace, "textures/" + rel);
                }
            }
            for (String root : CIT_ROOTS) {
                addId(ids, namespace, root + "/cit/" + path);
            }
            addId(ids, namespace, "textures/models/armor/" + path);
            addId(ids, namespace, "textures/" + path);
            return ids;
        }

        String normalized = normalizePath(path);
        addId(ids, namespace, normalized);
        if (!normalized.startsWith("textures/")) {
            addId(ids, namespace, "textures/" + normalized);
        }
        return ids;
    }

    private static String getProperty(Properties properties, String key) {
        if (properties == null || key == null) {
            return null;
        }
        return firstNonBlank(
                properties.getProperty(key),
                properties.getProperty("citresewn:" + key),
                properties.getProperty("citresewn." + key),
                properties.getProperty("optifine:" + key),
                properties.getProperty("optifine." + key),
                properties.getProperty("mcpatcher:" + key),
                properties.getProperty("mcpatcher." + key)
        );
    }

    private static String stripNamespacePrefix(String key) {
        if (key == null) {
            return null;
        }
        if (key.startsWith("citresewn:")) {
            return key.substring("citresewn:".length());
        }
        if (key.startsWith("citresewn.")) {
            return key.substring("citresewn.".length());
        }
        if (key.startsWith("optifine:")) {
            return key.substring("optifine:".length());
        }
        if (key.startsWith("optifine.")) {
            return key.substring("optifine.".length());
        }
        if (key.startsWith("mcpatcher:")) {
            return key.substring("mcpatcher:".length());
        }
        if (key.startsWith("mcpatcher.")) {
            return key.substring("mcpatcher.".length());
        }
        return key;
    }

    private static String normalizeComponentPath(String raw) {
        if (raw == null) {
            return null;
        }
        String path = raw.replace("\\:", ":").trim();
        if (path.isEmpty()) {
            return path;
        }
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return path;
        }
        parts[0] = normalizeComponentSegment(parts[0], true);
        for (int i = 1; i < parts.length; i++) {
            parts[i] = normalizeComponentSegment(parts[i], false);
        }
        return String.join(".", parts);
    }

    private static String normalizeComponentSegment(String segment, boolean first) {
        if (segment == null) {
            return null;
        }
        if (segment.startsWith("~")) {
            return "minecraft:" + segment.substring(1);
        }
        if (first && !segment.contains(":")) {
            return "minecraft:" + segment;
        }
        return segment;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static void addElytraEntityCandidate(List<Identifier> ids, String namespace, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String path = raw.trim().replace("\\", "/");
        if (path.startsWith("./")) {
            return;
        }
        String ns = namespace;
        int colon = path.indexOf(':');
        if (colon >= 0) {
            ns = path.substring(0, colon);
            path = path.substring(colon + 1);
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        if (path.contains("/")) {
            return;
        }
        addId(ids, ns, normalizePath("textures/entity/" + path + ".png"));
    }

    private static List<Identifier> parseImpliedTexture(String namespace, String propertiesPath) {
        List<Identifier> ids = new ArrayList<>();
        if (propertiesPath == null || propertiesPath.isBlank()) {
            return ids;
        }

        String unix = propertiesPath.replace('\\', '/');
        if (!unix.endsWith(".properties")) {
            return ids;
        }

        String basePath = unix.substring(0, unix.length() - ".properties".length());
        String prefix = "assets/" + namespace + "/";
        if (basePath.startsWith(prefix)) {
            basePath = basePath.substring(prefix.length());
        }

        String normalized = normalizePath(basePath + ".png");
        addId(ids, namespace, normalized);
        if (!normalized.startsWith("textures/")) {
            addId(ids, namespace, "textures/" + normalized);
        }
        return ids;
    }

    private static void addId(List<Identifier> ids, String namespace, String path) {
        Identifier id = Identifier.tryParse(namespace + ":" + path);
        if (id != null && !ids.contains(id)) {
            ids.add(id);
        }
    }

    private static String getResourceDir(String namespace, String propertiesPath) {
        String unix = propertiesPath.replace('\\', '/');
        String dir = unix.contains("/") ? unix.substring(0, unix.lastIndexOf('/')) : "";
        String prefix = "assets/" + namespace + "/";
        if (dir.startsWith(prefix)) {
            return dir.substring(prefix.length());
        }
        return dir;
    }

    private static CitRule findMatchingRule(ItemStack stack, RuleType mode, HandMatch hand) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        NbtCompound fullStackNbt = readFullStackNbt(stack);
        NbtCompound componentsNbt = fullStackNbt != null && fullStackNbt.contains("components", NbtElement.COMPOUND_TYPE)
                ? fullStackNbt.getCompound("components")
                : null;
        RuleCacheKey cacheKey = new RuleCacheKey(
                itemId,
                mode,
                hand == null ? HandMatch.ANY : hand,
                stack.getCount(),
                stack.isDamageable() ? stack.getDamage() : -1,
                hashNbt(fullStackNbt),
                hashNbt(componentsNbt)
        );
        CitRule cached = RULE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached == CitRule.NO_MATCH ? null : cached;
        }

        List<CitRule> rules = ITEM_RULES.get(itemId);
        if (rules == null || rules.isEmpty()) {
            RULE_CACHE.put(cacheKey, CitRule.NO_MATCH);
            return null;
        }

        for (CitRule rule : rules) {
            if (rule.type() != mode) {
                continue;
            }
            if (mode == RuleType.ARMOR && rule.armorTextures().isEmpty()) {
                continue;
            }
            if (mode == RuleType.ITEM && rule.itemModelId() == null && rule.itemTextureCandidates().isEmpty()) {
                continue;
            }
            if (mode == RuleType.ELYTRA && rule.elytraTextureCandidates().isEmpty()) {
                continue;
            }
            if (!rule.items().contains(itemId)) {
                continue;
            }
            if (!matchesRule(rule, stack, fullStackNbt, componentsNbt, hand)) {
                continue;
            }
            RULE_CACHE.put(cacheKey, rule);
            return rule;
        }

        RULE_CACHE.put(cacheKey, CitRule.NO_MATCH);
        return null;
    }

    private static int hashNbt(NbtCompound nbt) {
        return nbt == null ? 0 : nbt.hashCode();
    }

    private static Map<Identifier, List<CitRule>> indexRulesByItem(List<CitRule> rules) {
        Map<Identifier, List<CitRule>> map = new HashMap<>();
        for (CitRule rule : rules) {
            for (Identifier itemId : rule.items()) {
                map.computeIfAbsent(itemId, ignored -> new ArrayList<>()).add(rule);
            }
        }
        for (var entry : map.entrySet()) {
            entry.setValue(List.copyOf(entry.getValue()));
        }
        return Map.copyOf(map);
    }

    private static boolean matchesRule(CitRule rule, ItemStack stack, NbtCompound fullStackNbt, NbtCompound componentsNbt, HandMatch hand) {
        if (rule.handMatch() != null && rule.handMatch() != HandMatch.ANY) {
            if (hand == null || hand == HandMatch.ANY) {
                return false;
            }
            if (rule.handMatch() != hand) {
                return false;
            }
        }

        if (rule.damageMatcher() != null) {
            if (!stack.isDamageable()) {
                return false;
            }
            int damageValue = stack.getDamage();
            if (rule.damageMask() != null) {
                damageValue = damageValue & rule.damageMask();
            }
            if (!rule.damageMatcher().matches(damageValue, stack.getMaxDamage(), false)) {
                return false;
            }
        }

        if (rule.damaged() != null) {
            boolean isDamaged = stack.isDamageable() && stack.getDamage() > 0;
            if (rule.damaged() != isDamaged) {
                return false;
            }
        }

        if (rule.stackSizeMatcher() != null && !rule.stackSizeMatcher().matches(stack.getCount(), stack.getMaxCount(), false)) {
            return false;
        }

        if (rule.unbreakable() != null) {
            boolean isUnbreakable = stack.get(DataComponentTypes.UNBREAKABLE) != null;
            if (rule.unbreakable() != isUnbreakable) {
                return false;
            }
        }

        if (rule.potion() != null) {
            Identifier potionId = resolvePotionId(stack, fullStackNbt, componentsNbt);
            if (!rule.potion().equals(potionId)) {
                return false;
            }
        }

        if (!rule.enchantmentIds().isEmpty() || rule.enchantLevelMatcher() != null) {
            ItemEnchantmentsComponent enchants = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
            if (enchants == null || enchants.isEmpty()) {
                return false;
            }

            if (!rule.enchantmentIds().isEmpty()) {
                boolean foundAny = false;
                for (var entry : enchants.getEnchantmentEntries()) {
                    Identifier enchantId = entry.getKey().getKey().map(RegistryKey::getValue).orElse(null);
                    if (enchantId != null && rule.enchantmentIds().contains(enchantId)) {
                        foundAny = true;
                        break;
                    }
                }
                if (!foundAny) {
                    return false;
                }
            }

            if (rule.enchantLevelMatcher() != null) {
                boolean levelMatch = false;
                if (rule.enchantmentIds().isEmpty()) {
                    int total = 0;
                    for (var entry : enchants.getEnchantmentEntries()) {
                        total += entry.getIntValue();
                    }
                    levelMatch = rule.enchantLevelMatcher().matches(total, 255, false);
                } else {
                    for (var entry : enchants.getEnchantmentEntries()) {
                        Identifier enchantId = entry.getKey().getKey().map(RegistryKey::getValue).orElse(null);
                        if (enchantId != null && rule.enchantmentIds().contains(enchantId)
                                && rule.enchantLevelMatcher().matches(entry.getIntValue(), 255, false)) {
                            levelMatch = true;
                            break;
                        }
                    }
                }
                if (!levelMatch) {
                    return false;
                }
            }
        }

        List<PathMatcherRule> directMatchers = new ArrayList<>();
        Map<ListMatcherKey, List<ListMatcher>> listMatchers = new HashMap<>();

        for (PathMatcherRule matcher : rule.matchers()) {
            ListIndexPath listPath = ListIndexPath.parse(matcher.path());
            if (listPath == null) {
                directMatchers.add(matcher);
                continue;
            }
            ListMatcherKey key = new ListMatcherKey(matcher.source(), listPath.listPath(), listPath.index());
            listMatchers.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new ListMatcher(listPath.subPath(), matcher.matcher()));
        }

        for (var entry : listMatchers.entrySet()) {
            ListMatcherKey key = entry.getKey();
            NbtCompound root = key.source() == SourceNbt.COMPONENTS_ONLY ? componentsNbt : fullStackNbt;
            if (root == null) {
                if (!allMatchersMatchNull(entry.getValue())) {
                    return false;
                }
                continue;
            }
            NbtElement listElement = PathMatcherRule.resolvePathWithFallback(root, key.listPath());
            if (!(listElement instanceof net.minecraft.nbt.NbtList list) || list.isEmpty()) {
                if (!allMatchersMatchNull(entry.getValue())) {
                    return false;
                }
                continue;
            }
            if (!matchesListGroup(list, key.index(), entry.getValue())) {
                return false;
            }
        }

        for (PathMatcherRule matcher : directMatchers) {
            NbtCompound root = matcher.source() == SourceNbt.COMPONENTS_ONLY ? componentsNbt : fullStackNbt;
            if (!matcher.matches(root)) {
                return false;
            }
        }

        return true;
    }

    private static Identifier resolvePotionId(ItemStack stack, NbtCompound fullStackNbt, NbtCompound componentsNbt) {
        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        Identifier potionId = potionContents == null
                ? null
                : potionContents.potion()
                .flatMap(RegistryEntry::getKey)
                .map(RegistryKey::getValue)
                .orElse(null);
        if (potionId != null) {
            return potionId;
        }

        String raw = null;
        if (fullStackNbt != null) {
            raw = readString(fullStackNbt, "Potion");
            if (isBlank(raw) && fullStackNbt.contains("tag", NbtElement.COMPOUND_TYPE)) {
                raw = readString(fullStackNbt.getCompound("tag"), "Potion");
            }
        }
        if (isBlank(raw) && componentsNbt != null) {
            if (componentsNbt.contains("minecraft:custom_data", NbtElement.COMPOUND_TYPE)) {
                raw = readString(componentsNbt.getCompound("minecraft:custom_data"), "Potion", "potion");
            }
            if (isBlank(raw) && componentsNbt.contains("minecraft:potion_contents", NbtElement.COMPOUND_TYPE)) {
                raw = readString(componentsNbt.getCompound("minecraft:potion_contents"), "potion");
            }
        }

        if (isBlank(raw)) {
            return null;
        }
        return Identifier.tryParse(raw);
    }

    private static boolean matchesListGroup(net.minecraft.nbt.NbtList list, int preferredIndex, List<ListMatcher> matchers) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        if (preferredIndex >= 0 && preferredIndex < list.size()) {
            if (matchesListElement(list.get(preferredIndex), matchers)) {
                return true;
            }
        }
        for (int i = 0; i < list.size(); i++) {
            if (i == preferredIndex) {
                continue;
            }
            if (matchesListElement(list.get(i), matchers)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesListElement(NbtElement element, List<ListMatcher> matchers) {
        if (element == null) {
            return false;
        }
        for (ListMatcher matcher : matchers) {
            NbtElement resolved = matcher.subPath().isEmpty()
                    ? element
                    : PathMatcherRule.resolvePathOnElement(element, matcher.subPath());
            if (!matcher.matcher().matchesNullable(resolved)) {
                return false;
            }
        }
        return true;
    }

    private static boolean allMatchersMatchNull(List<ListMatcher> matchers) {
        if (matchers == null || matchers.isEmpty()) {
            return true;
        }
        for (ListMatcher matcher : matchers) {
            if (!matcher.matcher().matchesNullable(null)) {
                return false;
            }
        }
        return true;
    }

    private static NbtCompound readFullStackNbt(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return null;
        }

        NbtElement nbt = stack.toNbt(client.world.getRegistryManager());
        if (nbt instanceof NbtCompound compound) {
            injectBlockEntityTagFromComponents(compound);
            injectDisplayNameFromComponents(compound, stack);
            injectLoreFromComponents(compound, stack);
            injectPotionTagsFromComponents(compound);
            return compound;
        }
        return null;
    }

    private static void injectDisplayNameFromComponents(NbtCompound root, ItemStack stack) {
        if (root == null || stack == null) {
            return;
        }
        if (root.contains("display", NbtElement.COMPOUND_TYPE)) {
            NbtCompound display = root.getCompound("display");
            if (display.contains("Name", NbtElement.STRING_TYPE)) {
                return;
            }
        }
        var name = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (name == null) {
            return;
        }
        NbtCompound display = root.contains("display", NbtElement.COMPOUND_TYPE)
                ? root.getCompound("display")
                : new NbtCompound();
        display.putString("Name", name.getString());
        root.put("display", display);
    }

    private static void injectLoreFromComponents(NbtCompound root, ItemStack stack) {
        if (root == null || stack == null) {
            return;
        }
        if (root.contains("display", NbtElement.COMPOUND_TYPE)) {
            NbtCompound display = root.getCompound("display");
            if (display.contains("Lore", NbtElement.LIST_TYPE)) {
                return;
            }
        }
        Object lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return;
        }
        List<?> lines = extractLoreLines(lore);
        if (lines == null || lines.isEmpty()) {
            return;
        }
        NbtList list = new NbtList();
        for (Object line : lines) {
            if (line instanceof Text text) {
                list.add(NbtString.of(text.getString()));
            } else if (line != null) {
                list.add(NbtString.of(String.valueOf(line)));
            }
        }
        if (list.isEmpty()) {
            return;
        }
        NbtCompound display = root.contains("display", NbtElement.COMPOUND_TYPE)
                ? root.getCompound("display")
                : new NbtCompound();
        display.put("Lore", list);
        root.put("display", display);
    }

    private static void injectPotionTagsFromComponents(NbtCompound root) {
        if (root == null || !root.contains("components", NbtElement.COMPOUND_TYPE)) {
            return;
        }
        NbtCompound components = root.getCompound("components");
        NbtCompound potionContents = components.contains("minecraft:potion_contents", NbtElement.COMPOUND_TYPE)
                ? components.getCompound("minecraft:potion_contents")
                : null;
        NbtCompound customData = components.contains("minecraft:custom_data", NbtElement.COMPOUND_TYPE)
                ? components.getCompound("minecraft:custom_data")
                : null;

        if (!root.contains("Potion", NbtElement.STRING_TYPE)) {
            String potionId = readString(potionContents, "potion", "Potion");
            if (isBlank(potionId) && customData != null) {
                potionId = readString(customData, "Potion", "potion");
            }
            if (!isBlank(potionId)) {
                root.putString("Potion", potionId);
            }
        }

        if (!root.contains("CustomPotionColor", NbtElement.INT_TYPE)) {
            Integer color = readInt(potionContents, "custom_color", "CustomPotionColor", "customColor");
            if (color == null && customData != null) {
                color = readInt(customData, "CustomPotionColor", "custom_color", "customColor");
            }
            if (color != null) {
                root.putInt("CustomPotionColor", color);
            }
        }

        if (!root.contains("CustomPotionEffects", NbtElement.LIST_TYPE)) {
            NbtList source = null;
            if (potionContents != null && potionContents.contains("custom_effects", NbtElement.LIST_TYPE)) {
                source = potionContents.getList("custom_effects", NbtElement.COMPOUND_TYPE);
            }
            if ((source == null || source.isEmpty()) && customData != null) {
                if (customData.contains("custom_potion_effects", NbtElement.LIST_TYPE)) {
                    source = customData.getList("custom_potion_effects", NbtElement.COMPOUND_TYPE);
                } else if (customData.contains("CustomPotionEffects", NbtElement.LIST_TYPE)) {
                    source = customData.getList("CustomPotionEffects", NbtElement.COMPOUND_TYPE);
                }
            }
            if (source != null && !source.isEmpty()) {
                NbtList legacy = buildLegacyPotionEffects(source);
                if (!legacy.isEmpty()) {
                    root.put("CustomPotionEffects", legacy);
                }
            }
        }
    }

    private static NbtList buildLegacyPotionEffects(NbtList source) {
        NbtList legacy = new NbtList();
        for (int i = 0; i < source.size(); i++) {
            NbtElement element = source.get(i);
            if (!(element instanceof NbtCompound effect)) {
                continue;
            }
            NbtCompound legacyEntry = new NbtCompound();
            String id = readString(effect, "id", "Id");
            if (!isBlank(id)) {
                legacyEntry.putString("Id", id);
            } else {
                Integer numericId = readInt(effect, "id", "Id");
                if (numericId != null) {
                    legacyEntry.putInt("Id", numericId);
                }
            }
            Integer amplifier = readInt(effect, "amplifier", "Amplifier");
            if (amplifier != null) {
                legacyEntry.putInt("Amplifier", amplifier);
            }
            Integer duration = readInt(effect, "duration", "Duration");
            if (duration != null) {
                legacyEntry.putInt("Duration", duration);
            }
            Boolean ambient = readBooleanLike(effect, "ambient", "Ambient");
            if (ambient != null) {
                legacyEntry.putByte("Ambient", (byte) (ambient ? 1 : 0));
            }
            Boolean showParticles = readBooleanLike(effect, "show_particles", "showParticles", "ShowParticles");
            if (showParticles != null) {
                legacyEntry.putByte("ShowParticles", (byte) (showParticles ? 1 : 0));
            }
            Boolean showIcon = readBooleanLike(effect, "show_icon", "showIcon", "ShowIcon");
            if (showIcon != null) {
                legacyEntry.putByte("ShowIcon", (byte) (showIcon ? 1 : 0));
            }
            if (!legacyEntry.isEmpty()) {
                legacy.add(legacyEntry);
            }
        }
        return legacy;
    }

    private static String readString(NbtCompound compound, String... keys) {
        if (compound == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            if (compound.contains(key, NbtElement.STRING_TYPE)) {
                String value = compound.getString(key);
                if (!isBlank(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private static Integer readInt(NbtCompound compound, String... keys) {
        if (compound == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || !compound.contains(key)) {
                continue;
            }
            NbtElement element = compound.get(key);
            if (element instanceof AbstractNbtNumber number) {
                return number.intValue();
            }
            if (element instanceof NbtString str) {
                Integer parsed = parseIntFlexible(str.asString()).orElse(null);
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private static Boolean readBooleanLike(NbtCompound compound, String... keys) {
        if (compound == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || !compound.contains(key)) {
                continue;
            }
            NbtElement element = compound.get(key);
            if (element instanceof NbtByte b) {
                return b.byteValue() != 0;
            }
            if (element instanceof AbstractNbtNumber number) {
                return number.intValue() != 0;
            }
            if (element instanceof NbtString str) {
                String raw = str.asString().trim().toLowerCase(Locale.ROOT);
                if (raw.equals("true") || raw.equals("1")) {
                    return true;
                }
                if (raw.equals("false") || raw.equals("0")) {
                    return false;
                }
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static List<?> extractLoreLines(Object lore) {
        for (String method : new String[]{"lines", "getLines", "value"}) {
            try {
                Method m = lore.getClass().getMethod(method);
                Object value = m.invoke(lore);
                if (value instanceof List<?> list) {
                    return list;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static void injectBlockEntityTagFromComponents(NbtCompound root) {
        if (root.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE)) {
            return;
        }
        if (!root.contains("components", NbtElement.COMPOUND_TYPE)) {
            return;
        }
        NbtCompound components = root.getCompound("components");

        if (components.contains("minecraft:block_entity_data", NbtElement.COMPOUND_TYPE)) {
            NbtCompound blockEntity = components.getCompound("minecraft:block_entity_data");
            root.put("BlockEntityTag", blockEntity.copy());
            return;
        }

        NbtElement container = components.get("minecraft:container");
        if (container instanceof NbtCompound containerCompound) {
            NbtList items = extractItemsList(containerCompound);
            if (items != null) {
                NbtCompound blockEntity = new NbtCompound();
                blockEntity.put("Items", copyList(items));
                root.put("BlockEntityTag", blockEntity);
            }
            return;
        }

        if (container instanceof NbtList containerList) {
            NbtList converted = convertContainerList(containerList);
            if (converted != null) {
                NbtCompound blockEntity = new NbtCompound();
                blockEntity.put("Items", converted);
                root.put("BlockEntityTag", blockEntity);
            }
        }
    }

    private static NbtList extractItemsList(NbtCompound containerCompound) {
        if (containerCompound.contains("Items", NbtElement.LIST_TYPE)) {
            return containerCompound.getList("Items", NbtElement.COMPOUND_TYPE);
        }
        if (containerCompound.contains("items", NbtElement.LIST_TYPE)) {
            return containerCompound.getList("items", NbtElement.COMPOUND_TYPE);
        }
        return null;
    }

    private static NbtList copyList(NbtList original) {
        NbtList copy = new NbtList();
        for (NbtElement element : original) {
            copy.add(element.copy());
        }
        return copy;
    }

    private static NbtList convertContainerList(NbtList containerList) {
        if (containerList == null || containerList.isEmpty()) {
            return null;
        }
        Map<Integer, NbtCompound> bySlot = new HashMap<>();
        int maxSlot = -1;
        for (NbtElement element : containerList) {
            if (!(element instanceof NbtCompound entry)) {
                continue;
            }
            Integer slot = readSlot(entry);
            if (slot == null) {
                continue;
            }
            NbtCompound item = readItem(entry);
            if (item == null) {
                continue;
            }
            NbtCompound legacy = convertContainerItem(slot, item);
            if (legacy == null) {
                continue;
            }
            bySlot.put(slot, legacy);
            maxSlot = Math.max(maxSlot, slot);
        }
        if (maxSlot < 0) {
            return null;
        }
        NbtList items = new NbtList();
        for (int i = 0; i <= maxSlot; i++) {
            items.add(bySlot.getOrDefault(i, new NbtCompound()));
        }
        return items;
    }

    private static Integer readSlot(NbtCompound entry) {
        if (entry.contains("slot", NbtElement.NUMBER_TYPE)) {
            return entry.getInt("slot");
        }
        if (entry.contains("Slot", NbtElement.NUMBER_TYPE)) {
            return entry.getInt("Slot");
        }
        return null;
    }

    private static NbtCompound readItem(NbtCompound entry) {
        if (entry.contains("item", NbtElement.COMPOUND_TYPE)) {
            return entry.getCompound("item");
        }
        if (entry.contains("Item", NbtElement.COMPOUND_TYPE)) {
            return entry.getCompound("Item");
        }
        return null;
    }

    private static NbtCompound convertContainerItem(int slot, NbtCompound item) {
        String id = item.getString("id");
        if (id == null || id.isBlank()) {
            return null;
        }
        int count = item.contains("count", NbtElement.NUMBER_TYPE) ? item.getInt("count") : item.getInt("Count");
        if (count <= 0) {
            count = 1;
        }
        NbtCompound legacy = new NbtCompound();
        legacy.putString("id", id);
        legacy.put("Count", NbtByte.of((byte) count));
        legacy.put("Slot", NbtByte.of((byte) slot));

        NbtCompound tag = buildLegacyItemTag(item);
        if (tag != null && !tag.isEmpty()) {
            legacy.put("tag", tag);
        }
        return legacy;
    }

    private static NbtCompound buildLegacyItemTag(NbtCompound item) {
        if (!item.contains("components", NbtElement.COMPOUND_TYPE)) {
            return null;
        }
        NbtCompound components = item.getCompound("components");
        NbtCompound tag = new NbtCompound();

        if (components.contains("minecraft:enchantments", NbtElement.COMPOUND_TYPE)) {
            NbtCompound ench = components.getCompound("minecraft:enchantments");
            if (ench.contains("levels", NbtElement.COMPOUND_TYPE)) {
                NbtCompound levels = ench.getCompound("levels");
                NbtList list = new NbtList();
                for (String key : levels.getKeys()) {
                    NbtElement value = levels.get(key);
                    if (value instanceof AbstractNbtNumber number) {
                        NbtCompound entry = new NbtCompound();
                        entry.putString("id", key);
                        entry.put("lvl", NbtShort.of((short) number.intValue()));
                        list.add(entry);
                    }
                }
                if (!list.isEmpty()) {
                    tag.put("Enchantments", list);
                }
            }
        }

        return tag.isEmpty() ? null : tag;
    }

    private static List<PathMatcherRule> parseMatchers(Properties properties) {
        List<PathMatcherRule> matchers = new ArrayList<>();

        for (String key : properties.stringPropertyNames()) {
            String normalized = stripNamespacePrefix(key);
            if (normalized.startsWith("nbt.")) {
                String path = normalized.substring("nbt.".length());
                ValueMatcher matcher = ValueMatcher.parse(properties.getProperty(key, ""));
                if (matcher != null) {
                    matchers.add(new PathMatcherRule(SourceNbt.FULL_STACK, path, matcher));
                }
            } else if (normalized.startsWith("components.")) {
                String path = normalizeComponentPath(normalized.substring("components.".length()));
                ValueMatcher matcher = ValueMatcher.parse(properties.getProperty(key, ""));
                if (matcher != null && path != null && !path.isBlank()) {
                    matchers.add(new PathMatcherRule(SourceNbt.COMPONENTS_ONLY, path, matcher));
                }
            }
        }

        return matchers;
    }

    private static RangeMatcher parseRangeMatcher(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        List<Range> ranges = new ArrayList<>();
        boolean percent = raw.contains("%");
        String cleaned = raw.replace("%", "").trim();

        for (String tokenRaw : cleaned.split("\\s+")) {
            String token = tokenRaw.replace("(", "").replace(")", "");
            if (token.isBlank()) {
                continue;
            }

            int sep = findRangeSeparator(token);
            if (sep < 0) {
                Integer value = parseIntFlexible(token).orElse(null);
                if (value != null) {
                    ranges.add(new Range(value, value));
                }
            } else {
                String left = token.substring(0, sep);
                String right = token.substring(sep + 1);
                Integer min = left.isBlank() ? Integer.MIN_VALUE : parseIntFlexible(left).orElse(null);
                Integer max = right.isBlank() ? Integer.MAX_VALUE : parseIntFlexible(right).orElse(null);
                if (min != null && max != null) {
                    ranges.add(new Range(min, max));
                }
            }
        }

        return ranges.isEmpty() ? null : new RangeMatcher(ranges, percent);
    }

    private static Set<Identifier> parseItems(String rawItems) {
        Set<Identifier> items = new HashSet<>();
        for (String token : rawItems.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }

            Identifier id = Identifier.tryParse(token.contains(":") ? token : "minecraft:" + token);
            if (id != null && Registries.ITEM.containsId(id)) {
                items.add(id);
            }
        }
        return items;
    }

    private static Set<Identifier> parseIdentifierSet(String raw) {
        Set<Identifier> set = new HashSet<>();
        if (raw == null || raw.isBlank()) {
            return set;
        }

        for (String token : raw.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (token.chars().allMatch(Character::isDigit)) {
                try {
                    int legacyId = Integer.parseInt(token);
                    Identifier legacy = LEGACY_ENCHANTMENT_IDS.get(legacyId);
                    if (legacy != null) {
                        set.add(legacy);
                    }
                } catch (Exception ignored) {
                }
                continue;
            }
            Identifier id = Identifier.tryParse(token.contains(":") ? token : "minecraft:" + token);
            if (id != null) {
                set.add(id);
            }
        }
        return set;
    }

    private static Boolean parseBoolean01(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.equals("1") || value.equals("true")) {
            return true;
        }
        if (value.equals("0") || value.equals("false")) {
            return false;
        }
        return null;
    }

    private static Identifier parsePotion(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Identifier.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
    }

    private static byte[] readResourceBytes(Resource resource) {
        if (resource == null) {
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private static Optional<byte[]> readFromManager(ResourceManager manager, Identifier id) {
        if (manager == null || id == null) {
            return Optional.empty();
        }
        try {
            Resource resource = manager.getResource(id).orElse(null);
            byte[] bytes = readResourceBytes(resource);
            return bytes != null ? Optional.of(bytes) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value.trim()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseIntFlexible(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        try {
            if (trimmed.startsWith("#")) {
                return Optional.of(Integer.parseUnsignedInt(trimmed.substring(1), 16));
            }
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
                return Optional.of(Integer.parseUnsignedInt(trimmed.substring(2), 16));
            }
            return Optional.of(Integer.parseInt(trimmed));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static int findRangeSeparator(String token) {
        if (token == null) {
            return -1;
        }
        for (int i = 1; i < token.length(); i++) {
            if (token.charAt(i) == '-') {
                return i;
            }
        }
        return -1;
    }

    private static String normalizePath(String raw) {
        String input = raw.replace("\\", "/");
        String[] parts = input.split("/");
        List<String> result = new ArrayList<>();
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

    private enum SourceNbt {
        FULL_STACK,
        COMPONENTS_ONLY
    }

    private record CitRule(
            RuleType type,
            Set<Identifier> items,
            List<PathMatcherRule> matchers,
            RangeMatcher damageMatcher,
            Integer damageMask,
            RangeMatcher stackSizeMatcher,
            RangeMatcher enchantLevelMatcher,
            Set<Identifier> enchantmentIds,
            Boolean damaged,
            Boolean unbreakable,
            Identifier potion,
            int weight,
            HandMatch handMatch,
            Identifier itemModelId,
            SourceTexture sourceTexture,
            String ruleKey,
            List<Identifier> itemTextureCandidates,
            Map<String, List<Identifier>> armorTextures,
            List<Identifier> elytraTextureCandidates
    ) {
        static final CitRule NO_MATCH = new CitRule(RuleType.ITEM, Set.of(), List.of(), null, null, null, null, Set.of(), null, null, null, 0, HandMatch.ANY, null, null, "no_match", List.of(), Map.of(), List.of());
    }

    private record RuleCacheKey(
            Identifier itemId,
            RuleType mode,
            HandMatch hand,
            int count,
            int damage,
            int fullNbtHash,
            int componentsHash
    ) {
    }

    public record GeneratedModelDef(Identifier parentModelId, Identifier textureId, SourceTexture sourceTexture) {
    }

    private record SourceTexture(Identifier id, byte[] bytes) {
    }

    private interface TextureReader {
        Optional<byte[]> read(Identifier id);
    }

    private static SourceTexture findFirstTextureBytes(List<Identifier> candidates, TextureReader reader) {
        if (reader == null) {
            return null;
        }
        for (Identifier id : candidates) {
            Optional<byte[]> bytes = reader.read(id);
            if (bytes.isPresent()) {
                return new SourceTexture(id, bytes.get());
            }
        }
        return null;
    }

    private record PathMatcherRule(SourceNbt source, String path, ValueMatcher matcher) {
        boolean matches(NbtCompound root) {
            if (root == null) {
                return matcher.matchesNullable(null);
            }
            NbtElement element = resolvePath(root, path);
            return matcher.matchesNullable(element);
        }

        private static NbtElement resolvePath(NbtCompound root, String path) {
            NbtElement direct = resolvePathInternal(root, path);
            if (direct != null) {
                return direct;
            }

            // OptiFine-style nbt.* often refers to custom_data; fall back to it if present.
            if (root.contains("components", NbtElement.COMPOUND_TYPE)) {
                NbtCompound components = root.getCompound("components");
                if (components.contains("minecraft:custom_data", NbtElement.COMPOUND_TYPE)) {
                    return resolvePathInternal(components.getCompound("minecraft:custom_data"), path);
                }
            }

            if (root.contains("tag", NbtElement.COMPOUND_TYPE)) {
                return resolvePathInternal(root.getCompound("tag"), path);
            }

            return null;
        }

        private static NbtElement resolvePathWithFallback(NbtCompound root, String path) {
            return resolvePath(root, path);
        }

        private static NbtElement resolvePathDirect(NbtCompound root, String path) {
            return resolvePathInternal(root, path);
        }

        private static NbtElement resolvePathOnElement(NbtElement element, String path) {
            if (element instanceof NbtCompound compound) {
                return resolvePathInternal(compound, path);
            }
            return null;
        }

        private static NbtElement resolvePathInternal(NbtCompound root, String path) {
            NbtElement current = root;
            for (String part : path.split("\\.")) {
                if (current == null) {
                    return null;
                }
                if (current instanceof NbtCompound compound) {
                    current = compound.contains(part) ? compound.get(part) : null;
                    continue;
                }
                if (current instanceof net.minecraft.nbt.NbtList list) {
                    if ("count".equals(part)) {
                        return NbtInt.of(list.size());
                    }
                    Integer index = parseIntIndex(part);
                    if (index == null || index < 0 || index >= list.size()) {
                        return null;
                    }
                    current = list.get(index);
                    continue;
                }
                return null;
            }
            return current;
        }

        private static Integer parseIntIndex(String raw) {
            try {
                return Integer.parseInt(raw);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private record ListMatcherKey(SourceNbt source, String listPath, int index) {
    }

    private record ListMatcher(String subPath, ValueMatcher matcher) {
    }

    private record ListIndexPath(String listPath, int index, String subPath) {
        static ListIndexPath parse(String path) {
            if (path == null || path.isBlank()) {
                return null;
            }
            String[] parts = path.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                boolean wildcard = "*".equals(parts[i]);
                Integer index = wildcard ? Integer.valueOf(-1) : tryParse(parts[i]);
                if (index == null) {
                    continue;
                }
                if (i == 0) {
                    return null;
                }
                if (!wildcard && i == parts.length - 1) {
                    return null;
                }
                String listPath = String.join(".", java.util.Arrays.copyOfRange(parts, 0, i));
                String subPath = i + 1 < parts.length
                        ? String.join(".", java.util.Arrays.copyOfRange(parts, i + 1, parts.length))
                        : "";
                return new ListIndexPath(listPath, index, subPath);
            }
            return null;
        }

        private static Integer tryParse(String raw) {
            try {
                return Integer.parseInt(raw);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private record RangeMatcher(List<Range> ranges, boolean percent) {
        boolean matches(int value, int max, boolean ignorePercent) {
            int checked = value;
            if (percent && !ignorePercent) {
                checked = (int) Math.floor((value * 100.0) / Math.max(1, max));
            }

            for (Range range : ranges) {
                if (checked >= range.min() && checked <= range.max()) {
                    return true;
                }
            }
            return false;
        }
    }

    private record Range(int min, int max) {
    }

    private record ValueMatcher(
            Mode mode,
            String value,
            Pattern regex,
            boolean ignoreCase,
            RangeMatcher rangeMatcher,
            Boolean existsExpected,
            boolean raw,
            boolean negate
    ) {
        static ValueMatcher parse(String raw) {
            if (raw == null) {
                return null;
            }
            String value = raw.trim();
            if (value.isBlank()) {
                return null;
            }

            boolean negate = false;
            if (value.startsWith("!")) {
                negate = true;
                value = value.substring(1).trim();
            }

            boolean rawMode = false;
            if (value.startsWith("raw:")) {
                rawMode = true;
                value = value.substring("raw:".length());
            }

            if (value.startsWith("exists:")) {
                String flag = value.substring("exists:".length()).trim().toLowerCase(Locale.ROOT);
                Boolean expected = flag.equals("true") ? Boolean.TRUE : flag.equals("false") ? Boolean.FALSE : null;
                return new ValueMatcher(Mode.EXISTS, value, null, false, null, expected, rawMode, negate);
            }

            if (value.startsWith("range:")) {
                String rangeRaw = value.substring("range:".length());
                RangeMatcher matcher = parseRangeMatcher(rangeRaw);
                return matcher == null ? null : new ValueMatcher(Mode.RANGE, value, null, false, matcher, null, rawMode, negate);
            }

            if (value.startsWith("regex:")) {
                String pattern = value.substring("regex:".length());
                return new ValueMatcher(Mode.REGEX, pattern, Pattern.compile(pattern), false, null, null, rawMode, negate);
            }
            if (value.startsWith("iregex:")) {
                String pattern = value.substring("iregex:".length());
                return new ValueMatcher(Mode.REGEX, pattern, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), true, null, null, rawMode, negate);
            }
            if (value.startsWith("pattern:")) {
                return new ValueMatcher(Mode.PATTERN, value.substring("pattern:".length()), null, false, null, null, rawMode, negate);
            }
            if (value.startsWith("ipattern:")) {
                return new ValueMatcher(Mode.PATTERN, value.substring("ipattern:".length()), null, true, null, null, rawMode, negate);
            }

            return new ValueMatcher(Mode.EXACT, value, null, false, null, null, rawMode, negate);
        }

        boolean matchesNullable(NbtElement element) {
            boolean result;
            if (mode == Mode.EXISTS) {
                boolean exists = element != null;
                result = existsExpected == null ? exists : existsExpected == exists;
            } else {
                if (element == null) {
                    result = false;
                } else {
                    result = matchesElement(element);
                }
            }
            return negate ? !result : result;
        }

        private boolean matchesElement(NbtElement element) {
            if (mode == Mode.RANGE) {
                Integer number = nbtToInt(element);
                return number != null && rangeMatcher != null && rangeMatcher.matches(number, number, true);
            }

            String actual = raw ? element.toString() : nbtToComparableString(element);
            if (actual == null) {
                return false;
            }

            if (mode == Mode.EXACT) {
                if (compareExact(actual, value, ignoreCase)) {
                    return true;
                }
                Integer expectedNum = parseIntFlexible(value).orElse(null);
                if (expectedNum != null) {
                    Integer actualNum = nbtToInt(element);
                    if (actualNum != null && actualNum.intValue() == expectedNum.intValue()) {
                        return true;
                    }
                }
                return false;
            }

            return switch (mode) {
                case PATTERN -> wildcardMatch(actual, value, ignoreCase);
                case REGEX -> regex.matcher(actual).matches();
                case RANGE, EXISTS, EXACT -> false;
            };
        }

        private static String nbtToComparableString(NbtElement element) {
            if (element instanceof NbtString nbtString) {
                return nbtString.asString();
            }
            if (element instanceof NbtByte b && (b.byteValue() == 0 || b.byteValue() == 1)) {
                return b.byteValue() == 1 ? "true" : "false";
            }
            if (element instanceof AbstractNbtNumber number) {
                return number.numberValue().toString();
            }
            return element.toString();
        }

        private static Integer nbtToInt(NbtElement element) {
            if (element instanceof AbstractNbtNumber number) {
                return number.intValue();
            }
            if (element instanceof NbtString nbtString) {
                String raw = nbtString.asString();
                Integer parsed = parseIntFlexible(raw).orElse(null);
                if (parsed != null) {
                    return parsed;
                }
                Identifier id = Identifier.tryParse(raw);
                if (id != null) {
                    Integer legacy = LEGACY_STATUS_EFFECT_IDS.get(id);
                    if (legacy != null) {
                        return legacy;
                    }
                }
                return null;
            }
            String raw = element.toString();
            return parseIntFlexible(raw).orElse(null);
        }

        private static boolean compareExact(String actual, String expected, boolean ignoreCase) {
            return ignoreCase ? actual.equalsIgnoreCase(expected) : actual.equals(expected);
        }

        private static boolean wildcardMatch(String actual, String pattern, boolean ignoreCase) {
            String regex = Pattern.quote(pattern).replace("\\*", ".*").replace("\\?", ".");
            Pattern compiled = ignoreCase ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE) : Pattern.compile(regex);
            return compiled.matcher(actual).matches();
        }
    }

    private enum Mode {
        EXACT,
        PATTERN,
        REGEX,
        RANGE,
        EXISTS
    }
}
