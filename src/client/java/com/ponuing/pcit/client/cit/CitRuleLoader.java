package com.ponuing.pcit.client.cit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ponuing.pcit.client.NbtRenderOverrideResolver;
import com.ponuing.pcit.client.enchantment.EnchantmentBlend;
import com.ponuing.pcit.client.enchantment.EnchantmentLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

public final class CitRuleLoader {
    private static final List<String> CIT_ROOTS = List.of("optifine", "mcpatcher", "citresewn");
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

    private CitRuleLoader() {
    }

    public static List<CitRule> scanCitRules(ResourceManager manager) {
        List<CitRule> rules = new ArrayList<>();
        if (manager == null) {
            return rules;
        }

        Map<String, Integer> packOrder = buildPackPriorityMap();
        TextureReader reader = (id) -> readFromManager(manager, id);
        for (String root : CIT_ROOTS) {
            ResourceFinder finder = new ResourceFinder(root + "/cit", ".properties");
            for (Identifier id : finder.findResources(manager).keySet()) {
                List<Resource> resources = readAllResources(manager, id);
                if (resources.isEmpty()) {
                    continue;
                }
                int count = resources.size();
                for (int index = 0; index < count; index++) {
                    Resource resource = resources.get(index);
                    byte[] bytes = readResourceBytes(resource);
                    if (bytes == null) {
                        continue;
                    }
                    int packPriority = packOrder.getOrDefault(safePackId(resource), 0);
                    parseProperties(id.getNamespace(), id.getPath(), bytes, rules, reader, packPriority);
                }
            }
        }
        scanPotionTextures(manager, rules, reader, packOrder);

        rules.sort(Comparator.comparingInt(CitRule::packPriority).reversed()
                .thenComparing(Comparator.comparingInt(CitRule::weight).reversed()));
        return rules;
    }

    public static Map<Identifier, CitGeneratedModelDef> buildGeneratedItemModelMap(List<CitRule> rules, Map<Identifier, Identifier> baseModels, ResourceManager manager) {
        Map<Identifier, CitGeneratedModelDef> map = new HashMap<>();
        if (rules == null || rules.isEmpty()) {
            return Map.of();
        }
        Map<Identifier, Boolean> generatedCache = new HashMap<>();
        for (CitRule rule : rules) {
            if (rule.type() != CitRuleType.ITEM) {
                continue;
            }
            if (rule.sourceTexture() == null) {
                continue;
            }
            for (Identifier itemId : rule.items()) {
                Identifier parent;
                if (rule.itemModelId() != null) {
                    parent = rule.itemModelId();
                } else {
                    parent = baseModels.getOrDefault(itemId, Identifier.ofVanilla("item/generated"));
                }
                if (!isGeneratedItemModel(parent, manager, generatedCache, new HashSet<>())) {
                    continue;
                }
                Identifier modelId = buildGeneratedModelId(rule.ruleKey(), itemId);
                Identifier textureId = rule.sourceTexture().id();
                map.putIfAbsent(modelId, new CitGeneratedModelDef(parent, textureId, rule.sourceTexture()));
            }
        }
        return map.isEmpty() ? Map.of() : Map.copyOf(map);
    }

    public static Set<Identifier> collectExplicitItemModels(List<CitRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Set.of();
        }
        Set<Identifier> result = new HashSet<>();
        for (CitRule rule : rules) {
            if (rule.type() != CitRuleType.ITEM) {
                continue;
            }
            Identifier modelId = rule.itemModelId();
            if (modelId != null) {
                result.add(modelId);
            }
        }
        return result.isEmpty() ? Set.of() : Set.copyOf(result);
    }

    public static Identifier buildGeneratedModelId(String ruleKey, Identifier itemId) {
        String itemPath = itemId.getNamespace() + "/" + itemId.getPath();
        return Identifier.of("pcit", "item/cit/" + ruleKey + "/" + itemPath);
    }

    public static Map<Identifier, Identifier> loadItemAssetModelsFromManager(ResourceManager manager) {
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
                com.google.gson.JsonElement json = com.google.gson.JsonParser.parseReader(reader);
                if (!json.isJsonObject()) {
                    continue;
                }
                com.google.gson.JsonObject obj = json.getAsJsonObject();
                com.google.gson.JsonElement modelEl = obj.get("model");
                if (modelEl == null) {
                    continue;
                }
                Identifier modelId = parseItemAssetModel(modelEl);
                if (modelId != null) {
                    result.put(itemId, modelId);
                }
            } catch (Exception ignored) {
            }
        }
        return Map.copyOf(result);
    }

    public static Identifier parseItemAssetModel(com.google.gson.JsonElement modelEl) {
        if (modelEl.isJsonPrimitive()) {
            return Identifier.tryParse(modelEl.getAsString());
        }
        if (!modelEl.isJsonObject()) {
            return null;
        }
        com.google.gson.JsonObject modelObj = modelEl.getAsJsonObject();
        String type = modelObj.has("type") ? modelObj.get("type").getAsString() : "";
        if (!"minecraft:model".equals(type)) {
            return null;
        }
        if (!modelObj.has("model")) {
            return null;
        }
        return Identifier.tryParse(modelObj.get("model").getAsString());
    }

    private static void scanPotionTextures(ResourceManager manager, List<CitRule> rules, TextureReader reader, Map<String, Integer> packOrder) {
        if (manager == null || rules == null) {
            return;
        }

        addPotionRulesForFolder(manager, rules, reader, packOrder, "normal", Items.POTION);
        addPotionRulesForFolder(manager, rules, reader, packOrder, "splash", Items.SPLASH_POTION);
        addPotionRulesForFolder(manager, rules, reader, packOrder, "linger", Items.LINGERING_POTION);
    }

    private static void addPotionRulesForFolder(ResourceManager manager, List<CitRule> rules, TextureReader reader, Map<String, Integer> packOrder, String folder, net.minecraft.item.Item item) {
        for (String root : CIT_ROOTS) {
            ResourceFinder finder = new ResourceFinder(root + "/cit/potion/" + folder, ".png");
            for (Identifier id : finder.findResources(manager).keySet()) {
                List<Resource> resources = readAllResources(manager, id);
                if (resources.isEmpty()) {
                    continue;
                }
                int count = resources.size();
                for (int index = 0; index < count; index++) {
                    Resource resource = resources.get(index);
                    int packPriority = packOrder.getOrDefault(safePackId(resource), 0);
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
                    byte[] bytes = readResourceBytes(resource);
                    CitSourceTexture sourceTexture = bytes == null
                            ? null
                            : new CitSourceTexture(Identifier.of(id.getNamespace(), path), bytes);
                    int weight = -1;
                    String ruleKey = "potion:" + id.getNamespace() + ":" + path;

                    if ("empty".equals(name) && "normal".equals(folder)) {
                        Set<Identifier> items = Set.of(Registries.ITEM.getId(Items.GLASS_BOTTLE));
                        rules.add(new CitRule(
                                CitRuleType.ITEM,
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
                                packPriority,
                                NbtRenderOverrideResolver.HandMatch.ANY,
                                null,
                                sourceTexture,
                                ruleKey,
                                textureCandidates,
                                Map.of(),
                                Map.of(),
                                Map.of(),
                                List.of(),
                                null
                        ));
                        continue;
                    }

                    if ("other".equals(name)) {
                        for (String potionName : NO_EFFECT_POTION_NAMES) {
                            Identifier potionId = Identifier.of("minecraft", potionName);
                            Set<Identifier> items = Set.of(Registries.ITEM.getId(item));
                            rules.add(new CitRule(
                                    CitRuleType.ITEM,
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
                                    packPriority,
                                    NbtRenderOverrideResolver.HandMatch.ANY,
                                    null,
                                    sourceTexture,
                                    ruleKey + ":" + potionName,
                                    textureCandidates,
                                    Map.of(),
                                    Map.of(),
                                    Map.of(),
                                    List.of(),
                                    null
                            ));
                        }
                        continue;
                    }

                    Identifier potionId = Identifier.of("minecraft", name);
                    Set<Identifier> items = Set.of(Registries.ITEM.getId(item));
                    rules.add(new CitRule(
                            CitRuleType.ITEM,
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
                            packPriority,
                            NbtRenderOverrideResolver.HandMatch.ANY,
                            null,
                            sourceTexture,
                            ruleKey,
                            textureCandidates,
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            List.of(),
                            null
                    ));
                }
            }
        }
    }

    private static void parseProperties(String defaultNamespace, String propertiesPath, byte[] propertiesBytes, List<CitRule> out, TextureReader reader, int packPriority) {
        Properties properties = new Properties();
        try (InputStream in = new ByteArrayInputStream(propertiesBytes);
             InputStreamReader propReader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            properties.load(propReader);
        } catch (IOException e) {
            return;
        }

        String typeValue = CitPropertyUtils.getProperty(properties, "type");
        String type = (typeValue == null || typeValue.isBlank() ? "item" : typeValue).trim().toLowerCase(Locale.ROOT);
        CitRuleType ruleType;
        if (type.equals("item")) {
            ruleType = CitRuleType.ITEM;
        } else if (type.equals("armor")) {
            ruleType = CitRuleType.ARMOR;
        } else if (type.equals("elytra")) {
            ruleType = CitRuleType.ELYTRA;
        } else if (type.equals("enchantment")) {
            ruleType = CitRuleType.ENCHANTMENT;
        } else {
            return;
        }

        String itemsRaw = CitPropertyUtils.getProperty(properties, "items");
        String matchItemsRaw = CitPropertyUtils.getProperty(properties, "matchItems");
        Set<Identifier> items = parseItems(itemsRaw != null ? itemsRaw : (matchItemsRaw == null ? "" : matchItemsRaw));
        if (items.isEmpty()) {
            return;
        }

        Identifier itemModelId = null;
        List<Identifier> itemTextureCandidates = new ArrayList<>();
        Map<String, List<Identifier>> itemNamedTextures = new HashMap<>();
        Map<String, Identifier> itemNamedModels = new HashMap<>();
        Map<String, List<Identifier>> armorTextures = new HashMap<>();
        List<Identifier> elytraTextureCandidates = new ArrayList<>();
        List<Identifier> enchantmentTextureCandidates = new ArrayList<>();
        CitSourceTexture sourceTexture = null;
        String normalizedPath = propertiesPath == null ? "unknown" : propertiesPath.replace('\\', '/');
        String ruleKey = sha1Hex((defaultNamespace == null ? "" : defaultNamespace + ":") + normalizedPath);
        EnchantmentBlend enchantmentBlend = EnchantmentBlend.GLINT;
        Float enchantmentSpeed = null;
        Float enchantmentRotation = null;
        Integer enchantmentDuration = null;
        Set<EnchantmentLayer> enchantmentLayers = null;
        CitEnchantment enchantment = null;

        if (ruleType == CitRuleType.ITEM) {
            String modelRaw = CitPropertyUtils.getProperty(properties, "model");
            if (modelRaw != null && !modelRaw.isBlank()) {
                itemModelId = parseItemModelIdentifier(defaultNamespace, propertiesPath, modelRaw);
            }

            String tileRaw = CitPropertyUtils.getProperty(properties, "tile");
            String tilesRaw = CitPropertyUtils.getProperty(properties, "tiles");
            String textureRaw = CitPropertyUtils.getProperty(properties, "texture");
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

            if (!itemTextureCandidates.isEmpty()) {
                sourceTexture = findFirstTextureBytes(itemTextureCandidates, reader);
            }

            for (String key : properties.stringPropertyNames()) {
                String normalized = CitPropertyUtils.stripNamespacePrefix(key);
                if (normalized == null || normalized.isBlank()) {
                    continue;
                }
                if (normalized.startsWith("texture.") && normalized.length() > "texture.".length()) {
                    String textureKey = normalized.substring("texture.".length());
                    if ("elytra".equalsIgnoreCase(textureKey)) {
                        continue;
                    }
                    String rawPath = properties.getProperty(key);
                    List<Identifier> textureIds = parseTextureIdentifiers(defaultNamespace, propertiesPath, rawPath);
                    if (!textureIds.isEmpty()) {
                        itemNamedTextures.put(CitTextureResolver.normalizeNameKey(textureKey), textureIds);
                    }
                } else if (normalized.startsWith("model.") && normalized.length() > "model.".length()) {
                    String modelKey = normalized.substring("model.".length());
                    String rawPath = properties.getProperty(key);
                    if (rawPath != null && !rawPath.isBlank()) {
                        Identifier parsed = parseItemModelIdentifier(defaultNamespace, propertiesPath, rawPath);
                        if (parsed != null) {
                            itemNamedModels.put(CitTextureResolver.normalizeNameKey(modelKey), parsed);
                        }
                    }
                }
            }
        }

        if (ruleType == CitRuleType.ARMOR) {
            for (String key : properties.stringPropertyNames()) {
                String normalized = CitPropertyUtils.stripNamespacePrefix(key);
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

        if (ruleType == CitRuleType.ELYTRA) {
            String tilesRaw = CitPropertyUtils.getProperty(properties, "tiles");
            String textureRaw = CitPropertyUtils.getProperty(properties, "texture");
            String tileRaw = CitPropertyUtils.getProperty(properties, "tile");
            String textureElytraRaw = CitPropertyUtils.getProperty(properties, "texture.elytra");
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

        if (ruleType == CitRuleType.ENCHANTMENT) {
            String tilesRaw = CitPropertyUtils.getProperty(properties, "tiles");
            String textureRaw = CitPropertyUtils.getProperty(properties, "texture");
            String tileRaw = CitPropertyUtils.getProperty(properties, "tile");
            if (tilesRaw != null && !tilesRaw.isBlank()) {
                enchantmentTextureCandidates.addAll(parseTilesList(defaultNamespace, propertiesPath, tilesRaw));
            } else if (textureRaw != null && !textureRaw.isBlank()) {
                enchantmentTextureCandidates.addAll(parseTextureIdentifiers(defaultNamespace, propertiesPath, textureRaw));
            } else if (tileRaw != null && !tileRaw.isBlank()) {
                enchantmentTextureCandidates.addAll(parseTileIdentifiers(defaultNamespace, propertiesPath, tileRaw));
            }

            if (enchantmentTextureCandidates.isEmpty()) {
                enchantmentTextureCandidates.addAll(parseImpliedTexture(defaultNamespace, propertiesPath));
            }

            enchantmentBlend = EnchantmentBlend.parse(CitPropertyUtils.getProperty(properties, "blend"));
            enchantmentSpeed = parseFloat(CitPropertyUtils.getProperty(properties, "speed")).orElse(null);
            enchantmentRotation = parseFloat(CitPropertyUtils.getProperty(properties, "rotation")).orElse(null);
            enchantmentDuration = parseInt(CitPropertyUtils.getProperty(properties, "duration")).orElse(null);
            enchantmentLayers = parseEnchantmentLayers(CitPropertyUtils.getProperty(properties, "layer"));
            enchantment = new CitEnchantment(
                    enchantmentTextureCandidates,
                    enchantmentBlend,
                    enchantmentSpeed,
                    enchantmentRotation,
                    enchantmentDuration,
                    enchantmentLayers
            );
        }

        if (itemModelId == null
                && itemTextureCandidates.isEmpty()
                && itemNamedTextures.isEmpty()
                && itemNamedModels.isEmpty()
                && armorTextures.isEmpty()
                && elytraTextureCandidates.isEmpty()
                && (enchantment == null || enchantment.textureCandidates().isEmpty())) {
            return;
        }

        List<CitMatchers.PathMatcherRule> matchers = CitMatchers.parseMatchers(properties);
        CitMatchers.RangeMatcher damageMatcher = CitMatchers.parseRangeMatcher(CitPropertyUtils.getProperty(properties, "damage"));
        Integer damageMask = NbtRenderOverrideResolver.parseIntFlexible(CitPropertyUtils.getProperty(properties, "damageMask")).orElse(null);
        CitMatchers.RangeMatcher stackSizeMatcher = CitMatchers.parseRangeMatcher(CitPropertyUtils.getProperty(properties, "stackSize"));
        CitMatchers.RangeMatcher enchantLevelMatcher = CitMatchers.parseRangeMatcher(CitPropertyUtils.getProperty(properties, "enchantmentLevels"));
        String enchantmentsRaw = CitPropertyUtils.getProperty(properties, "enchantments");
        if (enchantmentsRaw == null || enchantmentsRaw.isBlank()) {
            enchantmentsRaw = CitPropertyUtils.getProperty(properties, "enchantmentIDs");
        }
        Set<Identifier> enchantmentIds = parseIdentifierSet(enchantmentsRaw);
        Boolean damaged = parseBoolean01(CitPropertyUtils.getProperty(properties, "damaged"));
        Boolean unbreakable = parseBoolean01(CitPropertyUtils.getProperty(properties, "unbreakable"));
        Identifier potion = parsePotion(CitPropertyUtils.getProperty(properties, "potion"));
        int weight = parseInt(CitPropertyUtils.getProperty(properties, "weight")).orElse(0);
        NbtRenderOverrideResolver.HandMatch handMatch = NbtRenderOverrideResolver.HandMatch.parse(CitPropertyUtils.getProperty(properties, "hand"));

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
                packPriority,
                handMatch,
                itemModelId,
                sourceTexture,
                ruleKey,
                itemTextureCandidates,
                itemNamedTextures,
                itemNamedModels,
                armorTextures,
                elytraTextureCandidates,
                enchantment
        ));
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
                    Identifier legacy = NbtRenderOverrideResolver.LEGACY_ENCHANTMENT_IDS.get(legacyId);
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

    private static Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value.trim()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<Float> parseFloat(String value) {
        try {
            return Optional.of(Float.parseFloat(value.trim()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Set<EnchantmentLayer> parseEnchantmentLayers(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Set<EnchantmentLayer> layers = java.util.EnumSet.noneOf(EnchantmentLayer.class);
        String normalized = raw.replace(",", " ");
        for (String tokenRaw : normalized.split("\\s+")) {
            if (tokenRaw.isBlank()) {
                continue;
            }
            String token = tokenRaw.trim().toLowerCase(Locale.ROOT);
            if ("all".equals(token) || "any".equals(token)) {
                return null;
            }
            EnchantmentLayer layer = switch (token) {
                case "0", "glint", "item" -> EnchantmentLayer.GLINT;
                case "1", "entity" -> EnchantmentLayer.ENTITY;
                case "2", "armor" -> EnchantmentLayer.ARMOR;
                case "3", "translucent", "glint_translucent" -> EnchantmentLayer.GLINT_TRANSLUCENT;
                default -> null;
            };
            if (layer != null) {
                layers.add(layer);
            }
        }
        return layers.isEmpty() ? null : Set.copyOf(layers);
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

        if (propertiesPath != null) {
            int slash = propertiesPath.lastIndexOf('/');
            String dir = slash >= 0 ? propertiesPath.substring(0, slash) : "";
            if (dir.startsWith("optifine/") || dir.startsWith("mcpatcher/") || dir.startsWith("citresewn/")) {
                return Identifier.of(namespace, CitTextureResolver.normalizePath(dir + "/" + model));
            }
        }

        return Identifier.of(namespace, "item/" + model);
    }

    private static List<Identifier> parseTilesList(String namespace, String propertiesPath, String raw) {
        List<Identifier> ids = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return ids;
        }
        for (String token : raw.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            ids.addAll(parseTileIdentifiers(namespace, propertiesPath, token));
        }
        return ids;
    }

    private static List<Identifier> parseTileIdentifiers(String namespace, String propertiesPath, String raw) {
        return parseTextureIdentifiers(namespace, propertiesPath, raw);
    }

    private static List<Identifier> parseTextureIdentifiers(String namespace, String propertiesPath, String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String path = raw.trim().replace("\\", "/");
        List<Identifier> ids = new ArrayList<>();
        if (path.contains(":")) {
            String[] parts = path.split(":", 2);
            String ns = parts[0];
            String p = parts[1];
            if (p.startsWith("textures/")) {
                p = p.substring("textures/".length());
            }
            if (p.endsWith(".png")) {
                p = p.substring(0, p.length() - 4);
            }
            Identifier id = Identifier.tryParse(ns + ":" + p + ".png");
            return id == null ? List.of() : List.of(id);
        }

        if (path.startsWith("./")) {
            String dir = getResourceDir(namespace, propertiesPath);
            String rel = path.substring(2);
            if (rel.endsWith(".png")) {
                rel = rel.substring(0, rel.length() - 4);
            }
            addId(ids, namespace, CitTextureResolver.normalizePath(dir + "/" + rel + ".png"));
            return ids;
        }
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        String basePng = path + ".png";

        if (path.startsWith("optifine/") || path.startsWith("mcpatcher/") || path.startsWith("citresewn/")) {
            addId(ids, namespace, basePng);
            addId(ids, namespace, "textures/" + basePng);
            return ids;
        }

        boolean hasExplicitDir = path.contains("/");
        if (!hasExplicitDir && propertiesPath != null && !propertiesPath.isBlank()) {
            String dir = getResourceDir(namespace, propertiesPath);
            if (!dir.isBlank()) {
                addId(ids, namespace, CitTextureResolver.normalizePath(dir + "/" + basePng));
            }
        }

        for (String root : CIT_ROOTS) {
            addId(ids, namespace, root + "/cit/" + basePng);
        }
        addId(ids, namespace, "textures/models/armor/" + basePng);
        addId(ids, namespace, "textures/entity/" + basePng);
        addId(ids, namespace, "textures/" + basePng);
        return ids;
    }

    private static void addElytraEntityCandidate(List<Identifier> ids, String namespace, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String path = raw.trim().replace("\\", "/");
        String ns = namespace;
        if (path.contains(":")) {
            String[] parts = path.split(":", 2);
            ns = parts[0];
            path = parts[1];
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (!path.endsWith("_elytra")) {
            path = path + "_elytra";
        }
        addId(ids, ns, "textures/entity/" + path + ".png");
    }

    private static List<Identifier> parseImpliedTexture(String namespace, String propertiesPath) {
        if (propertiesPath == null) {
            return List.of();
        }
        String path = propertiesPath.replace('\\', '/');
        if (path.endsWith(".properties")) {
            path = path.substring(0, path.length() - ".properties".length());
        }
        String dir = getResourceDir(namespace, path);
        String name = path;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (name.contains(".")) {
            name = name.substring(0, name.indexOf('.'));
        }
        return parseTextureIdentifiers(namespace, path, dir + "/" + name);
    }

    private static void addId(List<Identifier> ids, String namespace, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        String normalized = CitTextureResolver.normalizePath(path);
        ids.add(Identifier.of(namespace, normalized));
    }

    private static String getResourceDir(String namespace, String propertiesPath) {
        if (propertiesPath == null) {
            return "";
        }
        String path = propertiesPath.replace('\\', '/');
        if (path.startsWith("optifine/") || path.startsWith("mcpatcher/") || path.startsWith("citresewn/")) {
            int slash = path.lastIndexOf('/');
            if (slash >= 0) {
                return path.substring(0, slash);
            }
            return path;
        }
        return namespace + ":" + path;
    }

    private static String sha1Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
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

    private static String safePackId(Resource resource) {
        if (resource == null) {
            return "unknown";
        }
        try {
            return resource.getPackId();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static Map<String, Integer> buildPackPriorityMap() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return Map.of();
        }
        ResourcePackManager manager = client.getResourcePackManager();
        if (manager == null) {
            return Map.of();
        }
        List<ResourcePackProfile> enabled = new ArrayList<>(manager.getEnabledProfiles());
        if (enabled.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < enabled.size(); i++) {
            ResourcePackProfile profile = enabled.get(i);
            if (profile != null) {
                order.put(profile.getId(), i);
            }
        }
        return order;
    }

    private static List<Resource> readAllResources(ResourceManager manager, Identifier id) {
        if (manager == null || id == null) {
            return List.of();
        }
        try {
            return manager.getAllResources(id);
        } catch (Exception e) {
            return List.of();
        }
    }

    private interface TextureReader {
        Optional<byte[]> read(Identifier id);
    }

    private static CitSourceTexture findFirstTextureBytes(List<Identifier> candidates, TextureReader reader) {
        if (reader == null) {
            return null;
        }
        for (Identifier id : candidates) {
            Optional<byte[]> bytes = reader.read(id);
            if (bytes.isPresent()) {
                return new CitSourceTexture(id, bytes.get());
            }
        }
        return null;
    }

    private static boolean isGeneratedItemModel(Identifier modelId, ResourceManager manager, Map<Identifier, Boolean> cache, Set<Identifier> visiting) {
        if (modelId == null) {
            return false;
        }
        Boolean cached = cache.get(modelId);
        if (cached != null) {
            return cached;
        }
        if (visiting.contains(modelId)) {
            return false;
        }
        visiting.add(modelId);
        boolean result = isGeneratedItemModelInternal(modelId, manager, cache, visiting);
        visiting.remove(modelId);
        cache.put(modelId, result);
        return result;
    }

    private static boolean isGeneratedItemModelInternal(Identifier modelId, ResourceManager manager, Map<Identifier, Boolean> cache, Set<Identifier> visiting) {
        String modelPath = modelId.getPath();
        String rawPath = modelPath.startsWith("models/") ? modelPath.substring("models/".length()) : modelPath;
        boolean optifineModel = CitTextureResolver.isCitRootPath(rawPath);
        Identifier modelResource = optifineModel
                ? Identifier.of(modelId.getNamespace(), rawPath + ".json")
                : Identifier.of(modelId.getNamespace(), "models/" + rawPath + ".json");
        JsonObject obj = null;
        if (manager != null) {
            try {
                Resource resource = manager.getResource(modelResource).orElse(null);
                if (resource != null) {
                    try (InputStream in = resource.getInputStream()) {
                        String jsonText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                        if (optifineModel) {
                            String normalized = CitTextureResolver.normalizeOptifineModelJson(rawPath, jsonText, modelId.getNamespace());
                            if (normalized != null && !normalized.isBlank()) {
                                jsonText = normalized;
                            }
                        }
                        JsonElement element = JsonParser.parseString(jsonText);
                        if (element.isJsonObject()) {
                            obj = element.getAsJsonObject();
                        }
                    }
                }
            } catch (Exception ignored) {
                obj = null;
            }
        }

        if (obj != null) {
            JsonElement elementsEl = obj.get("elements");
            if (elementsEl != null && elementsEl.isJsonArray() && !elementsEl.getAsJsonArray().isEmpty()) {
                return false;
            }
            String parentRaw = null;
            JsonElement parentEl = obj.get("parent");
            if (parentEl != null && parentEl.isJsonPrimitive() && parentEl.getAsJsonPrimitive().isString()) {
                parentRaw = parentEl.getAsString();
            }
            if (parentRaw == null || parentRaw.isBlank()) {
                return isGeneratedParentPath(modelId.getPath());
            }
            String resolvedParent = parentRaw.trim().replace("\\", "/");
            if (optifineModel) {
                int slash = rawPath.lastIndexOf('/');
                String dir = slash >= 0 ? rawPath.substring(0, slash) : rawPath;
                String resolved = CitTextureResolver.resolveOptifineModelPath(dir, resolvedParent);
                if (resolved != null && !resolved.isBlank()) {
                    resolvedParent = resolved;
                }
            }
            resolvedParent = stripModelSuffix(resolvedParent);
            if (resolvedParent.startsWith("builtin/") || resolvedParent.startsWith("minecraft:builtin/")) {
                return false;
            }
            String parentPath = resolvedParent;
            String parentNamespace = modelId.getNamespace();
            if (resolvedParent.contains(":")) {
                Identifier parsed = Identifier.tryParse(resolvedParent);
                if (parsed != null) {
                    parentNamespace = parsed.getNamespace();
                    parentPath = parsed.getPath();
                }
            }
            if (isGeneratedParentPath(parentPath)) {
                return true;
            }
            Identifier parentId = Identifier.tryParse(parentNamespace + ":" + parentPath);
            if (parentId == null) {
                return false;
            }
            return isGeneratedItemModel(parentId, manager, cache, visiting);
        }

        return isGeneratedParentPath(modelId.getPath());
    }

    private static String stripModelSuffix(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().replace("\\", "/");
        if (value.endsWith(".json")) {
            value = value.substring(0, value.length() - 5);
        }
        if (value.startsWith("models/")) {
            value = value.substring("models/".length());
        }
        return value;
    }

    private static boolean isGeneratedParentPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.replace("\\", "/");
        if (normalized.contains(":")) {
            Identifier parsed = Identifier.tryParse(normalized);
            if (parsed != null) {
                normalized = parsed.getPath();
            }
        }
        if (normalized.startsWith("item/")) {
            return true;
        }
        return normalized.equals("builtin/generated") || normalized.equals("minecraft:builtin/generated");
    }
}
