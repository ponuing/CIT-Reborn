package com.ponuing.pcit.client;

import com.ponuing.pcit.PCIT;
import com.ponuing.pcit.client.cit.CitGeneratedModelDef;
import com.ponuing.pcit.client.cit.CitEnchantment;
import com.ponuing.pcit.client.cit.CitMatchers;
import com.ponuing.pcit.client.cit.CitRule;
import com.ponuing.pcit.client.cit.CitRuleLoader;
import com.ponuing.pcit.client.cit.CitRuleType;
import com.ponuing.pcit.client.cit.CitTextureResolver;
import com.ponuing.pcit.client.cit.NbtComponentUtils;
import com.ponuing.pcit.client.enchantment.EnchantmentBlend;
import com.ponuing.pcit.client.enchantment.EnchantmentGlintRenderLayer;
import com.ponuing.pcit.client.enchantment.EnchantmentLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.resource.ResourceManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NbtRenderOverrideResolver {
    private static final CopyOnWriteArrayList<CitRule> RULES = new CopyOnWriteArrayList<>();
    private static volatile Map<Identifier, CitGeneratedModelDef> GENERATED_ITEM_MODELS = Map.of();
    private static volatile Map<Identifier, Identifier> ITEM_BASE_MODELS = Map.of();
    private static volatile Set<Identifier> EXTRA_ITEM_MODELS = Set.of();
    private static volatile Map<Identifier, List<CitRule>> ITEM_RULES = Map.of();
    private static volatile boolean HAS_ENCHANTMENT_RULES = false;
    // ID for compatibility old nbt properties
    public static final Map<Integer, Identifier> LEGACY_ENCHANTMENT_IDS = Map.ofEntries(
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
    public static final Map<Identifier, Integer> LEGACY_STATUS_EFFECT_IDS = Map.ofEntries(
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
    private static final java.util.concurrent.atomic.AtomicBoolean INITIAL_LOAD_DONE = new java.util.concurrent.atomic.AtomicBoolean(false);

    public enum HandMatch {
        ANY,
        MAIN,
        OFF;

        public static HandMatch parse(String raw) {
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
        CitRule rule = findMatchingRule(stack, CitRuleType.ITEM, hand);
        if (rule == null) {
            return null;
        }

        if (rule.sourceTexture() != null) {
            Identifier textureId = rule.sourceTexture().id();
            return textureId;
        }

        if (rule.itemTextureCandidates().isEmpty()) {
            return null;
        }

        Identifier found = CitTextureResolver.findExistingTexture(rule.itemTextureCandidates());
        if (found == null) {
            return null;
        }

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
        CitRule rule = findMatchingRule(stack, CitRuleType.ITEM, hand);
        if (rule == null) {
            return null;
        }

        if (rule.itemModelId() != null) {
            return rule.itemModelId();
        }

        return null;
    }

    public record ItemOverride(
            Identifier modelId,
            Identifier textureId,
            Map<String, Identifier> namedTextures,
            Map<String, Identifier> namedModels
    ) {
    }

    public record EnchantmentOverride(
            Identifier textureId,
            EnchantmentBlend blend,
            Float speed,
            Float rotation,
            Integer duration,
            Set<EnchantmentLayer> layers
    ) {
    }

    public static ItemOverride resolveItemOverride(ItemStack stack, HandMatch hand) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ensureLoaded();
        CitRule rule = findMatchingRule(stack, CitRuleType.ITEM, hand);
        if (rule == null) {
            return null;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        Identifier textureId = null;
        if (rule.sourceTexture() != null) {
            textureId = rule.sourceTexture().id();
        } else if (!rule.itemTextureCandidates().isEmpty()) {
            textureId = CitTextureResolver.findExistingTexture(rule.itemTextureCandidates());
        }

        Map<String, Identifier> namedTextures = resolveNamedTextureOverrides(rule.itemNamedTextures());
        Map<String, Identifier> namedModels = rule.itemNamedModels().isEmpty()
                ? Map.of()
                : Map.copyOf(rule.itemNamedModels());

        Identifier modelId = rule.itemModelId();
        if (textureId != null) {
            Identifier generatedId = CitRuleLoader.buildGeneratedModelId(rule.ruleKey(), itemId);
            if (GENERATED_ITEM_MODELS.containsKey(generatedId)) {
                modelId = generatedId;
                textureId = null;
            }
        }

        return new ItemOverride(modelId, textureId, namedTextures, namedModels);
    }

    public static Identifier resolveElytraTextureOverride(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ensureLoaded();
        CitRule rule = findMatchingRule(stack, CitRuleType.ELYTRA, HandMatch.ANY);
        if (rule == null || rule.elytraTextureCandidates().isEmpty()) {
            return null;
        }
        return CitTextureResolver.findExistingTexture(rule.elytraTextureCandidates());
    }

    public static EnchantmentOverride resolveEnchantmentOverride(ItemStack stack, HandMatch hand) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ensureLoaded();
        CitRule rule = findMatchingRule(stack, CitRuleType.ENCHANTMENT, hand);
        if (rule == null) {
            return null;
        }
        CitEnchantment enchantment = rule.enchantment();
        if (enchantment == null || enchantment.textureCandidates() == null || enchantment.textureCandidates().isEmpty()) {
            return null;
        }
        Identifier textureId = rule.sourceTexture() != null
                ? rule.sourceTexture().id()
                : CitTextureResolver.findExistingTexture(enchantment.textureCandidates());
        if (textureId == null) {
            return null;
        }
        return new EnchantmentOverride(
                textureId,
                enchantment.blend(),
                enchantment.speed(),
                enchantment.rotation(),
                enchantment.duration(),
                enchantment.layers()
        );
    }

    public static Identifier toSpriteId(Identifier textureId) {
        return CitTextureResolver.toSpriteId(textureId);
    }

    public static Sprite resolveSprite(Identifier textureId) {
        return CitTextureResolver.resolveSprite(textureId);
    }

    public static Sprite resolveModelTextureSprite(Identifier modelId) {
        return CitTextureResolver.resolveModelTextureSprite(modelId);
    }

    public static BakedModel resolveModelBaked(Identifier modelId) {
        return CitTextureResolver.resolveModelBaked(modelId);
    }

    public static Identifier resolveModelTextureId(Identifier modelId) {
        return CitTextureResolver.resolveModelTextureId(modelId);
    }

    public static Map<Identifier, CitGeneratedModelDef> getGeneratedItemModels() {
        return GENERATED_ITEM_MODELS;
    }

    public static Set<Identifier> getExtraItemModels() {
        return EXTRA_ITEM_MODELS;
    }

    public static Identifier resolveArmorTextureOverride(ItemStack stack, Identifier originalTextureId) {
        ensureLoaded();
        CitRule rule = findMatchingRule(stack, CitRuleType.ARMOR, HandMatch.ANY);
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
                Identifier existing = CitTextureResolver.findExistingTexture(ids);
                return existing;
            }
        }

        return null;
    }

    public static boolean hasEnchantmentRules() {
        return HAS_ENCHANTMENT_RULES;
    }

    private static Map<String, Identifier> resolveNamedTextureOverrides(Map<String, List<Identifier>> namedTextures) {
        if (namedTextures == null || namedTextures.isEmpty()) {
            return Map.of();
        }
        Map<String, Identifier> resolved = new HashMap<>();
        for (Map.Entry<String, List<Identifier>> entry : namedTextures.entrySet()) {
            List<Identifier> ids = entry.getValue();
            if (ids == null || ids.isEmpty()) {
                continue;
            }
            Identifier existing = CitTextureResolver.findExistingTexture(ids);
            if (existing != null) {
                resolved.put(entry.getKey(), existing);
            }
        }
        return resolved.isEmpty() ? Map.of() : Map.copyOf(resolved);
    }

    public static void ensureLoaded() {
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

    public static void reloadFromManager(ResourceManager manager) {
        if (manager == null) {
            return;
        }
        INITIAL_LOAD_DONE.set(true);
        reloadInternalFromManager(manager);
    }

    private static void reloadInternalFromManager(ResourceManager manager) {
        try {
            ITEM_BASE_MODELS = CitRuleLoader.loadItemAssetModelsFromManager(manager);
            List<CitRule> parsedRules = CitRuleLoader.scanCitRules(manager);

            RULES.clear();
            RULES.addAll(parsedRules);
            RULE_CACHE.clear();
            ITEM_RULES = indexRulesByItem(parsedRules);
            GENERATED_ITEM_MODELS = CitRuleLoader.buildGeneratedItemModelMap(parsedRules, ITEM_BASE_MODELS, manager);
            EXTRA_ITEM_MODELS = CitRuleLoader.collectExplicitItemModels(parsedRules);
            HAS_ENCHANTMENT_RULES = parsedRules.stream().anyMatch(rule -> rule.type() == CitRuleType.ENCHANTMENT);
            CitTextureResolver.clearCaches();
            EnchantmentGlintRenderLayer.clearCache();

            PCIT.LOGGER.info("[model] loaded rules={}, genModels={}, explicitModels={}", parsedRules.size(), GENERATED_ITEM_MODELS.size(), EXTRA_ITEM_MODELS.size());
        } catch (Exception e) {
            INITIAL_LOAD_DONE.set(false);
            RULES.clear();
            HAS_ENCHANTMENT_RULES = false;
            PCIT.LOGGER.error("Failed to reload OptiFine CIT rules", e);
        }
    }

    public static String normalizeOptifineModelJson(String relPath, String jsonText) {
        return CitTextureResolver.normalizeOptifineModelJson(relPath, jsonText);
    }

    public static String normalizeOptifineModelJson(String relPath, String jsonText, String defaultNamespace) {
        return CitTextureResolver.normalizeOptifineModelJson(relPath, jsonText, defaultNamespace);
    }

    public static boolean matchesTextureName(Identifier spriteId, String nameKey) {
        return CitTextureResolver.matchesTextureName(spriteId, nameKey);
    }

    public static String normalizeComponentPath(String raw) {
        return NbtComponentUtils.normalizeComponentPath(raw);
    }

    private static CitRule findMatchingRule(ItemStack stack, CitRuleType mode, HandMatch hand) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        NbtCompound fullStackNbt = NbtComponentUtils.readFullStackNbt(stack);
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
            if (mode == CitRuleType.ARMOR && rule.armorTextures().isEmpty()) {
                continue;
            }
            if (mode == CitRuleType.ITEM
                    && rule.itemModelId() == null
                    && rule.itemTextureCandidates().isEmpty()
                    && rule.itemNamedTextures().isEmpty()
                    && rule.itemNamedModels().isEmpty()) {
                continue;
            }
            if (mode == CitRuleType.ELYTRA && rule.elytraTextureCandidates().isEmpty()) {
                continue;
            }
            if (mode == CitRuleType.ENCHANTMENT) {
                CitEnchantment enchantment = rule.enchantment();
                if (enchantment == null || enchantment.textureCandidates() == null || enchantment.textureCandidates().isEmpty()) {
                    continue;
                }
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

        List<CitMatchers.PathMatcherRule> directMatchers = new ArrayList<>();
        Map<CitMatchers.ListMatcherKey, List<CitMatchers.ListMatcher>> listMatchers = new HashMap<>();

        for (CitMatchers.PathMatcherRule matcher : rule.matchers()) {
            CitMatchers.ListIndexPath listPath = CitMatchers.ListIndexPath.parse(matcher.path());
            if (listPath == null) {
                directMatchers.add(matcher);
                continue;
            }
            CitMatchers.ListMatcherKey key = new CitMatchers.ListMatcherKey(matcher.source(), listPath.listPath(), listPath.index());
            listMatchers.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new CitMatchers.ListMatcher(listPath.subPath(), matcher.matcher()));
        }

        for (var entry : listMatchers.entrySet()) {
            CitMatchers.ListMatcherKey key = entry.getKey();
            NbtCompound root = key.source() == CitMatchers.SourceNbt.COMPONENTS_ONLY ? componentsNbt : fullStackNbt;
            if (root == null) {
                if (!CitMatchers.allMatchersMatchNull(entry.getValue())) {
                    return false;
                }
                continue;
            }
            NbtElement listElement = CitMatchers.PathMatcherRule.resolvePathWithFallback(root, key.listPath());
            if (!(listElement instanceof net.minecraft.nbt.NbtList list) || list.isEmpty()) {
                if (!CitMatchers.allMatchersMatchNull(entry.getValue())) {
                    return false;
                }
                continue;
            }
            if (!CitMatchers.matchesListGroup(list, key.index(), entry.getValue())) {
                return false;
            }
        }

        for (CitMatchers.PathMatcherRule matcher : directMatchers) {
            NbtCompound root = matcher.source() == CitMatchers.SourceNbt.COMPONENTS_ONLY ? componentsNbt : fullStackNbt;
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
            raw = NbtComponentUtils.readString(fullStackNbt, "Potion");
            if (NbtComponentUtils.isBlank(raw) && fullStackNbt.contains("tag", NbtElement.COMPOUND_TYPE)) {
                raw = NbtComponentUtils.readString(fullStackNbt.getCompound("tag"), "Potion");
            }
        }
        if (NbtComponentUtils.isBlank(raw) && componentsNbt != null) {
            if (componentsNbt.contains("minecraft:custom_data", NbtElement.COMPOUND_TYPE)) {
                raw = NbtComponentUtils.readString(componentsNbt.getCompound("minecraft:custom_data"), "Potion", "potion");
            }
            if (NbtComponentUtils.isBlank(raw) && componentsNbt.contains("minecraft:potion_contents", NbtElement.COMPOUND_TYPE)) {
                raw = NbtComponentUtils.readString(componentsNbt.getCompound("minecraft:potion_contents"), "potion");
            }
        }

        if (NbtComponentUtils.isBlank(raw)) {
            return null;
        }
        return Identifier.tryParse(raw);
    }

    public static Optional<Integer> parseIntFlexible(String value) {
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

    public static int findRangeSeparator(String token) {
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

    private record RuleCacheKey(
            Identifier itemId,
            CitRuleType mode,
            HandMatch hand,
            int count,
            int damage,
            int fullNbtHash,
            int componentsHash
    ) {
    }
}
