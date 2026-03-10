package com.ponuing.pcustomtextures.client;

import com.ponuing.pcustomtextures.Pcustomtextures;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class NbtRenderOverrideResolver {
    private static final Identifier ITEM_ATLAS_ID = Identifier.ofVanilla("textures/atlas/items.png");
    private static final CopyOnWriteArrayList<CitRule> RULES = new CopyOnWriteArrayList<>();
    private static volatile Map<Identifier, GeneratedModelDef> GENERATED_ITEM_MODELS = Map.of();
    private static volatile Map<Identifier, Identifier> ITEM_BASE_MODELS = Map.of();
    private static volatile Map<Identifier, byte[]> VIRTUAL_TEXTURES = Map.of();
    private static volatile Map<Identifier, List<CitRule>> ITEM_RULES = Map.of();
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

    private NbtRenderOverrideResolver() {
    }

    public static ItemStack resolveItemStackForRender(ItemStack original) {
        return original;
    }

    public static Identifier resolveItemTextureOverride(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        //Pcustomtextures.LOGGER.info("[pcustomtextures][item] resolve texture for {} x{}", itemId, stack.getCount());
        CitRule rule = findMatchingRule(stack, false);
        if (rule == null) {
            //Pcustomtextures.LOGGER.info("[pcustomtextures][item] no matching rule or no texture candidates for {}", itemId);
            return null;
        }

        if (rule.sourceTexture() != null) {
            Identifier virtualTexture = buildVirtualTextureId(rule.ruleKey(), itemId);
            //Pcustomtextures.LOGGER.info("[pcustomtextures][item] matched rule, using virtual texture {}", virtualTexture);
            return virtualTexture;
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
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        CitRule rule = findMatchingRule(stack, false);
        if (rule == null) {
            return null;
        }

        if (rule.itemModelId() != null) {
            return rule.itemModelId();
        }

        return null;
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

    private static Sprite resolveModelTextureSpriteInternal(Identifier modelId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getResourceManager() == null) {
            return null;
        }
        Identifier modelResource = Identifier.of(modelId.getNamespace(), "models/" + modelId.getPath() + ".json");
        Resource resource = client.getResourceManager().getResource(modelResource).orElse(null);
        if (resource == null) {
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            String jsonText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
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
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        return Identifier.of(namespace, path);
    }

    private static Identifier resolveOptifineAlias(Identifier textureId) {
        if (textureId == null) {
            return null;
        }
        String path = textureId.getPath();
        if (!path.startsWith("textures/optifine/cit/")) {
            return null;
        }
        String rest = path.substring("textures/optifine/cit/".length());
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

    public static Map<Identifier, GeneratedModelDef> getGeneratedItemModels() {
        return GENERATED_ITEM_MODELS;
    }

    public static Map<Identifier, byte[]> getVirtualTextures() {
        return VIRTUAL_TEXTURES;
    }

    public static Identifier resolveArmorTextureOverride(ItemStack stack, Identifier originalTextureId) {
        CitRule rule = findMatchingRule(stack, true);
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

    public static void reload() {
        try {
            //Pcustomtextures.LOGGER.info("[pcustomtextures][model] reload start");
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path resourcepacksDir = gameDir.resolve("resourcepacks");
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                ITEM_BASE_MODELS = loadItemAssetModels(client.getResourceManager());
            } else {
                ITEM_BASE_MODELS = Map.of();
            }
            List<CitRule> parsedRules = scanOptifineCitRules(resourcepacksDir);

            RULES.clear();
            RULES.addAll(parsedRules);
            RULE_CACHE.clear();
            ITEM_RULES = indexRulesByItem(parsedRules);
            GENERATED_ITEM_MODELS = buildGeneratedItemModelMap(parsedRules, ITEM_BASE_MODELS);
            Map<Identifier, byte[]> virtualResources = new HashMap<>(buildVirtualTextures(GENERATED_ITEM_MODELS));
            virtualResources.putAll(buildOptifineCitVirtualResources(resourcepacksDir));
            VIRTUAL_TEXTURES = Map.copyOf(virtualResources);
            CitVirtualResourcePack.updateMappings(VIRTUAL_TEXTURES);
            MODEL_SPRITE_CACHE.clear();

            Pcustomtextures.LOGGER.info("[pcustomtextures][model] loaded rules={}, genModels={}, virtualTextures={}", parsedRules.size(), GENERATED_ITEM_MODELS.size(), VIRTUAL_TEXTURES.size());
            if (client != null) {
                client.execute(client::reloadResources);
            }
        } catch (Exception e) {
            RULES.clear();
            Pcustomtextures.LOGGER.error("Failed to reload OptiFine CIT rules", e);
        }
    }

    private static List<CitRule> scanOptifineCitRules(Path resourcepacksDir) throws IOException {
        List<CitRule> rules = new ArrayList<>();
        if (Files.notExists(resourcepacksDir)) {
            return rules;
        }

        try (var packs = Files.list(resourcepacksDir)) {
            for (Path pack : packs.toList()) {
                String fileName = pack.getFileName().toString().toLowerCase(Locale.ROOT);
                if (Files.isDirectory(pack)) {
                    scanDirectoryPack(pack, rules);
                } else if (fileName.endsWith(".zip")) {
                    scanZipPack(pack, rules);
                }
            }
        }

        rules.sort(Comparator.comparingInt(CitRule::weight).reversed());
        return rules;
    }

    private static void scanDirectoryPack(Path packDir, List<CitRule> out) {
        Path assetsDir = packDir.resolve("assets");
        if (Files.notExists(assetsDir)) {
            return;
        }

        TextureReader reader = (id) -> readFromDirectory(packDir, id);

        try (var namespaces = Files.list(assetsDir)) {
            for (Path namespaceDir : namespaces.toList()) {
                if (!Files.isDirectory(namespaceDir)) {
                    continue;
                }

                String namespace = namespaceDir.getFileName().toString();
                Path citDir = namespaceDir.resolve(Paths.get("optifine", "cit"));
                if (Files.notExists(citDir)) {
                    continue;
                }

                try (var walk = Files.walk(citDir)) {
                    for (Path file : walk.toList()) {
                        if (!Files.isRegularFile(file) || !file.toString().toLowerCase(Locale.ROOT).endsWith(".properties")) {
                            continue;
                        }

                        byte[] propsBytes = Files.readAllBytes(file);
                        String propsPath = toUnixPath(packDir.relativize(file));
                        parseProperties(namespace, propsPath, propsBytes, out, reader);
                    }
                }
            }
        } catch (Exception e) {
            //Pcustomtextures.LOGGER.warn("Failed to scan directory pack {}", packDir, e);
        }
    }

    private static void scanZipPack(Path zipPath, List<CitRule> out) {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            TextureReader reader = (id) -> readFromZip(zip, id);
            List<? extends ZipEntry> entries = zip.stream().toList();
            for (ZipEntry entry : entries) {
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                String lower = name.toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".properties") || !name.contains("/optifine/cit/")) {
                    continue;
                }

                String[] parts = name.split("/");
                if (parts.length < 4 || !parts[0].equals("assets")) {
                    continue;
                }

                String namespace = parts[1];
                byte[] propsBytes = readZipEntry(zip, name).orElse(null);
                if (propsBytes == null) {
                    continue;
                }

                parseProperties(namespace, name, propsBytes, out, reader);
            }
        } catch (Exception e) {
            //Pcustomtextures.LOGGER.warn("Failed to scan zip pack {}", zipPath, e);
        }
    }

    private static void parseProperties(String defaultNamespace, String propertiesPath, byte[] propertiesBytes, List<CitRule> out, TextureReader reader) {
        Properties properties = new Properties();
        try (InputStream in = new ByteArrayInputStream(propertiesBytes)) {
            properties.load(in);
        } catch (IOException e) {
            return;
        }

        String type = properties.getProperty("type", "item").trim().toLowerCase(Locale.ROOT);
        if (!type.equals("item") && !type.equals("armor")) {
            return;
        }

        Set<Identifier> items = parseItems(properties.getProperty("items", properties.getProperty("matchItems", "")));
        if (items.isEmpty()) {
            return;
        }

        Identifier itemModelId = null;
        List<Identifier> itemTextureCandidates = new ArrayList<>();
        Map<String, List<Identifier>> armorTextures = new HashMap<>();
        SourceTexture sourceTexture = null;
        String ruleKey = sha1Hex(propertiesPath == null ? "unknown" : propertiesPath.replace('\\', '/'));

        if (type.equals("item")) {
            String modelRaw = properties.getProperty("model");
            if (modelRaw != null && !modelRaw.isBlank()) {
                itemModelId = parseItemModelIdentifier(defaultNamespace, propertiesPath, modelRaw);
            }

            String tileRaw = properties.getProperty("tile");
            String tilesRaw = properties.getProperty("tiles");
            String textureRaw = properties.getProperty("texture");
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

        if (type.equals("armor")) {
            for (String key : properties.stringPropertyNames()) {
                if (!key.startsWith("texture.")) {
                    continue;
                }

                String textureKey = key.substring("texture.".length()).toLowerCase(Locale.ROOT);
                String rawPath = properties.getProperty(key);
                List<Identifier> textureIds = parseTextureIdentifiers(defaultNamespace, propertiesPath, rawPath);
                if (!textureIds.isEmpty()) {
                    armorTextures.put(textureKey, textureIds);
                }
            }
        }

        if (itemModelId == null && itemTextureCandidates.isEmpty() && armorTextures.isEmpty()) {
            return;
        }

        List<PathMatcherRule> matchers = parseMatchers(properties);
        RangeMatcher damageMatcher = parseRangeMatcher(properties.getProperty("damage"));
        RangeMatcher stackSizeMatcher = parseRangeMatcher(properties.getProperty("stackSize"));
        RangeMatcher enchantLevelMatcher = parseRangeMatcher(properties.getProperty("enchantmentLevels"));
        Set<Identifier> enchantmentIds = parseIdentifierSet(properties.getProperty("enchantments", properties.getProperty("enchantmentIDs", "")));
        Boolean damaged = parseBoolean01(properties.getProperty("damaged"));
        Boolean unbreakable = parseBoolean01(properties.getProperty("unbreakable"));
        Identifier potion = parsePotion(properties.getProperty("potion"));
        int weight = parseInt(properties.getProperty("weight")).orElse(0);

        out.add(new CitRule(
                items,
                matchers,
                damageMatcher,
                stackSizeMatcher,
                enchantLevelMatcher,
                enchantmentIds,
                damaged,
                unbreakable,
                potion,
                weight,
                itemModelId,
                sourceTexture,
                ruleKey,
                itemTextureCandidates,
                armorTextures
        ));
    }

    private static Map<Identifier, GeneratedModelDef> buildGeneratedItemModelMap(List<CitRule> rules, Map<Identifier, Identifier> baseModels) {
        Map<Identifier, GeneratedModelDef> map = new HashMap<>();
        for (CitRule rule : rules) {
            if (rule.sourceTexture() == null) {
                continue;
            }
            for (Identifier itemId : rule.items()) {
                Identifier modelId = buildGeneratedModelId(rule.ruleKey(), itemId);
                Identifier parent = baseModels.getOrDefault(itemId, Identifier.ofVanilla("item/generated"));
                Identifier virtualTexture = buildVirtualTextureId(rule.ruleKey(), itemId);
                //Pcustomtextures.LOGGER.info("[pcustomtextures][model] gen model for {} parent={} virtualTex={} sourceTex={} bytes={}", itemId, parent, virtualTexture, rule.sourceTexture().id(), rule.sourceTexture().bytes().length);
                map.put(modelId, new GeneratedModelDef(parent, virtualTexture, rule.sourceTexture()));
            }
        }
        return Map.copyOf(map);
    }

    private static Identifier buildGeneratedModelId(String ruleKey, Identifier itemId) {
        String itemPath = itemId.getNamespace() + "/" + itemId.getPath();
        return Identifier.of("pcustomtextures", "item/cit/" + ruleKey + "/" + itemPath);
    }

    private static Identifier buildVirtualTextureId(String ruleKey, Identifier itemId) {
        String itemPath = itemId.getNamespace() + "/" + itemId.getPath();
        return Identifier.of("pcustomtextures", "textures/item/cit/" + ruleKey + "/" + itemPath + ".png");
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

    private static Map<Identifier, byte[]> buildVirtualTextures(Map<Identifier, GeneratedModelDef> models) {
        Map<Identifier, byte[]> map = new HashMap<>();
        for (GeneratedModelDef def : models.values()) {
            map.put(def.virtualTextureId(), def.sourceTexture().bytes());
        }
        return Map.copyOf(map);
    }

    private static Map<Identifier, byte[]> buildOptifineCitVirtualResources(Path resourcepacksDir) {
        Map<Identifier, byte[]> map = new HashMap<>();
        if (Files.notExists(resourcepacksDir)) {
            return map;
        }

        try (var packs = Files.list(resourcepacksDir)) {
            for (Path pack : packs.toList()) {
                String fileName = pack.getFileName().toString().toLowerCase(Locale.ROOT);
                if (Files.isDirectory(pack)) {
                    addOptifineCitResourcesFromDirectory(pack, map);
                } else if (fileName.endsWith(".zip")) {
                    addOptifineCitResourcesFromZip(pack, map);
                }
            }
        } catch (Exception e) {
            //Pcustomtextures.LOGGER.warn("Failed to build OptiFine CIT virtual resources", e);
        }

        return map;
    }

    private static void addOptifineCitResourcesFromDirectory(Path packDir, Map<Identifier, byte[]> out) {
        Path assetsDir = packDir.resolve("assets");
        if (Files.notExists(assetsDir)) {
            return;
        }

        try (var namespaces = Files.list(assetsDir)) {
            for (Path namespaceDir : namespaces.toList()) {
                if (!Files.isDirectory(namespaceDir)) {
                    continue;
                }

                String namespace = namespaceDir.getFileName().toString();
                Path citDir = namespaceDir.resolve(Paths.get("optifine", "cit"));
                if (Files.notExists(citDir)) {
                    continue;
                }

                try (var walk = Files.walk(citDir)) {
                    for (Path file : walk.toList()) {
                        if (!Files.isRegularFile(file)) {
                            continue;
                        }
                        String rel = toUnixPath(namespaceDir.relativize(file));
                        byte[] bytes = Files.readAllBytes(file);
                        mapOptifineCitResource(out, namespace, rel, bytes);
                    }
                }
            }
        } catch (Exception e) {
            //Pcustomtextures.LOGGER.warn("Failed to scan OptiFine CIT files in {}", packDir, e);
        }
    }

    private static void addOptifineCitResourcesFromZip(Path zipPath, Map<Identifier, byte[]> out) {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            for (ZipEntry entry : zip.stream().toList()) {
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName().replace('\\', '/');
                String lower = name.toLowerCase(Locale.ROOT);
                if (!lower.contains("/optifine/cit/") || !lower.startsWith("assets/")) {
                    continue;
                }

                String[] parts = name.split("/");
                if (parts.length < 4) {
                    continue;
                }
                String namespace = parts[1];
                String rel = String.join("/", java.util.Arrays.copyOfRange(parts, 2, parts.length));
                byte[] bytes = readZipEntry(zip, name).orElse(null);
                if (bytes == null) {
                    continue;
                }
                mapOptifineCitResource(out, namespace, rel, bytes);
            }
        } catch (Exception e) {
            //Pcustomtextures.LOGGER.warn("Failed to scan OptiFine CIT zip {}", zipPath, e);
        }
    }

    private static void mapOptifineCitResource(Map<Identifier, byte[]> out, String namespace, String relPath, byte[] bytes) {
        if (bytes == null || relPath == null) {
            return;
        }
        String lower = relPath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".properties")) {
            return;
        }

        if (lower.endsWith(".png") || lower.endsWith(".png.mcmeta")) {
            Identifier id = Identifier.of(namespace, "textures/" + relPath);
            out.putIfAbsent(id, bytes);
            addOptifineItemAlias(out, namespace, relPath, bytes);
            return;
        }

        if (lower.endsWith(".json")) {
            byte[] updated = updateOptifineModelJson(namespace, relPath, bytes);
            Identifier id = Identifier.of(namespace, "models/" + relPath);
            out.putIfAbsent(id, updated != null ? updated : bytes);
        }
    }

    private static byte[] updateOptifineModelJson(String namespace, String relPath, byte[] bytes) {
        if (relPath == null || !relPath.startsWith("optifine/cit/")) {
            return null;
        }
        try {
            String jsonText = new String(bytes, StandardCharsets.UTF_8);
            JsonElement element = JsonParser.parseString(jsonText);
            if (!element.isJsonObject()) {
                return null;
            }
            JsonObject obj = element.getAsJsonObject();
            JsonElement texturesElement = obj.get("textures");
            if (texturesElement == null || !texturesElement.isJsonObject()) {
                return null;
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
                    String itemPath = toOptifineItemAliasPath(resolved);
                    textures.addProperty(entry.getKey(), itemPath != null ? itemPath : resolved);
                    changed = true;
                }
            }
            if (!changed) {
                return null;
            }
            return obj.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveOptifineTexturePath(String dir, String tex) {
        if (tex == null || tex.isBlank()) {
            return null;
        }
        if (tex.startsWith("./")) {
            return dir + "/" + tex.substring(2);
        }
        if (tex.startsWith("optifine/cit/")) {
            return tex;
        }
        if (tex.startsWith("item/") || tex.startsWith("block/") || tex.contains(":")) {
            return null;
        }
        return dir + "/" + tex;
    }

    private static void addOptifineItemAlias(Map<Identifier, byte[]> out, String namespace, String relPath, byte[] bytes) {
        if (relPath == null || !relPath.startsWith("optifine/cit/")) {
            return;
        }
        String rest = relPath.substring("optifine/cit/".length());
        Identifier alias = Identifier.of(namespace, "textures/item/optifine_cit/" + rest);
        out.putIfAbsent(alias, bytes);
    }

    private static String toOptifineItemAliasPath(String resolvedPath) {
        if (resolvedPath == null || !resolvedPath.startsWith("optifine/cit/")) {
            return null;
        }
        String rest = resolvedPath.substring("optifine/cit/".length());
        return "item/optifine_cit/" + rest;
    }

    private static Map<Identifier, Identifier> loadItemAssetModels(ResourceManager manager) {
        if (manager == null) {
            return Map.of();
        }

        Map<Identifier, Identifier> result = new HashMap<>();
        ResourceFinder finder = ResourceFinder.json("items");
        Map<Identifier, Resource> resources = finder.findResources(manager);
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier itemId = entry.getKey();
            try (InputStream in = entry.getValue().getInputStream();
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

        if (model.startsWith("item/") || model.startsWith("optifine/") || model.startsWith("cit/")) {
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
            addId(ids, namespace, "optifine/cit/" + path);
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

    private static CitRule findMatchingRule(ItemStack stack, boolean armorMode) {
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
                armorMode,
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
            if (armorMode && rule.armorTextures().isEmpty()) {
                continue;
            }
            if (!armorMode && rule.itemModelId() == null && rule.itemTextureCandidates().isEmpty()) {
                continue;
            }
            if (!rule.items().contains(itemId)) {
                continue;
            }
            if (!matchesRule(rule, stack, fullStackNbt, componentsNbt)) {
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

    private static boolean matchesRule(CitRule rule, ItemStack stack, NbtCompound fullStackNbt, NbtCompound componentsNbt) {
        if (rule.damageMatcher() != null) {
            if (!stack.isDamageable()) {
                return false;
            }
            if (!rule.damageMatcher().matches(stack.getDamage(), stack.getMaxDamage(), false)) {
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
            PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
            Identifier potionId = potionContents == null
                    ? null
                    : potionContents.potion()
                    .flatMap(RegistryEntry::getKey)
                    .map(RegistryKey::getValue)
                    .orElse(null);

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
                for (var entry : enchants.getEnchantmentEntries()) {
                    if (rule.enchantLevelMatcher().matches(entry.getIntValue(), 255, false)) {
                        levelMatch = true;
                        break;
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
                return false;
            }
            NbtElement listElement = PathMatcherRule.resolvePathWithFallback(root, key.listPath());
            if (!(listElement instanceof net.minecraft.nbt.NbtList list)) {
                return false;
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
            NbtElement resolved = PathMatcherRule.resolvePathOnElement(element, matcher.subPath());
            if (resolved == null || !matcher.matcher().matches(resolved)) {
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
            return compound;
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
            if (key.startsWith("nbt.")) {
                String path = key.substring("nbt.".length());
                ValueMatcher matcher = ValueMatcher.parse(properties.getProperty(key, ""));
                if (matcher != null) {
                    matchers.add(new PathMatcherRule(SourceNbt.FULL_STACK, path, matcher));
                }
            } else if (key.startsWith("components.")) {
                String path = key.substring("components.".length());
                ValueMatcher matcher = ValueMatcher.parse(properties.getProperty(key, ""));
                if (matcher != null) {
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

        for (String token : cleaned.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }

            String[] parts = token.split("-");
            if (parts.length == 1) {
                Integer value = parseInt(parts[0]).orElse(null);
                if (value != null) {
                    ranges.add(new Range(value, value));
                }
            } else {
                Integer min = parts[0].isBlank() ? Integer.MIN_VALUE : parseInt(parts[0]).orElse(null);
                Integer max = parts[1].isBlank() ? Integer.MAX_VALUE : parseInt(parts[1]).orElse(null);
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

    private static Optional<byte[]> readZipEntry(ZipFile zip, String rel) {
        ZipEntry entry = zip.getEntry(rel);
        if (entry == null || entry.isDirectory()) {
            return Optional.empty();
        }
        try (InputStream in = zip.getInputStream(entry)) {
            return Optional.of(in.readAllBytes());
        } catch (IOException e) {
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

    private static String toUnixPath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private enum SourceNbt {
        FULL_STACK,
        COMPONENTS_ONLY
    }

    private record CitRule(
            Set<Identifier> items,
            List<PathMatcherRule> matchers,
            RangeMatcher damageMatcher,
            RangeMatcher stackSizeMatcher,
            RangeMatcher enchantLevelMatcher,
            Set<Identifier> enchantmentIds,
            Boolean damaged,
            Boolean unbreakable,
            Identifier potion,
            int weight,
            Identifier itemModelId,
            SourceTexture sourceTexture,
            String ruleKey,
            List<Identifier> itemTextureCandidates,
            Map<String, List<Identifier>> armorTextures
    ) {
        static final CitRule NO_MATCH = new CitRule(Set.of(), List.of(), null, null, null, Set.of(), null, null, null, 0, null, null, "no_match", List.of(), Map.of());
    }

    private record RuleCacheKey(
            Identifier itemId,
            boolean armorMode,
            int count,
            int damage,
            int fullNbtHash,
            int componentsHash
    ) {
    }

    public record GeneratedModelDef(Identifier parentModelId, Identifier virtualTextureId, SourceTexture sourceTexture) {
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

    private static Optional<byte[]> readFromDirectory(Path packDir, Identifier id) {
        Path file = packDir.resolve("assets").resolve(id.getNamespace()).resolve(id.getPath());
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<byte[]> readFromZip(ZipFile zip, Identifier id) {
        String rel = "assets/" + id.getNamespace() + "/" + id.getPath();
        return readZipEntry(zip, rel);
    }

    private record PathMatcherRule(SourceNbt source, String path, ValueMatcher matcher) {
        boolean matches(NbtCompound root) {
            if (root == null) {
                return false;
            }
            NbtElement element = resolvePath(root, path);
            return element != null && matcher.matches(element);
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
            for (int i = 0; i < parts.length - 1; i++) {
                Integer index = tryParse(parts[i]);
                if (index == null) {
                    continue;
                }
                if (i == 0 || i == parts.length - 1) {
                    return null;
                }
                String listPath = String.join(".", java.util.Arrays.copyOfRange(parts, 0, i));
                String subPath = String.join(".", java.util.Arrays.copyOfRange(parts, i + 1, parts.length));
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

    private record ValueMatcher(Mode mode, String value, Pattern regex, boolean ignoreCase) {
        static ValueMatcher parse(String raw) {
            if (raw == null) {
                return null;
            }
            String value = raw.trim();
            if (value.isBlank()) {
                return null;
            }

            if (value.startsWith("regex:")) {
                String pattern = value.substring("regex:".length());
                return new ValueMatcher(Mode.REGEX, pattern, Pattern.compile(pattern), false);
            }
            if (value.startsWith("iregex:")) {
                String pattern = value.substring("iregex:".length());
                return new ValueMatcher(Mode.REGEX, pattern, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), true);
            }
            if (value.startsWith("pattern:")) {
                return new ValueMatcher(Mode.PATTERN, value.substring("pattern:".length()), null, false);
            }
            if (value.startsWith("ipattern:")) {
                return new ValueMatcher(Mode.PATTERN, value.substring("ipattern:".length()), null, true);
            }

            return new ValueMatcher(Mode.EXACT, value, null, false);
        }

        boolean matches(NbtElement element) {
            String actual = nbtToComparableString(element);
            if (actual == null) {
                return false;
            }

            return switch (mode) {
                case EXACT -> compareExact(actual, value, ignoreCase);
                case PATTERN -> wildcardMatch(actual, value, ignoreCase);
                case REGEX -> regex.matcher(actual).matches();
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
        REGEX
    }
}
