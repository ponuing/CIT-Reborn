package com.ponuing.pcustomtextures.client;

import com.ponuing.pcustomtextures.Pcustomtextures;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class NbtRenderOverrideResolver {
    private static final CopyOnWriteArrayList<CitRule> RULES = new CopyOnWriteArrayList<>();

    private NbtRenderOverrideResolver() {
    }

    public static ItemStack resolveItemStackForRender(ItemStack original) {
        CitRule rule = findMatchingRule(original, false);
        if (rule == null || rule.itemModelId() == null) {
            return original;
        }

        ItemStack copy = original.copy();
        copy.set(DataComponentTypes.ITEM_MODEL, rule.itemModelId());
        return copy;
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
        if (client == null) {
            return null;
        }

        var manager = client.getResourceManager();
        for (Identifier candidate : candidates) {
            if (manager.getResource(candidate).isPresent()) {
                return candidate;
            }
        }
        return null;
    }

    public static void reload() {
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path resourcepacksDir = gameDir.resolve("resourcepacks");
            List<CitRule> parsedRules = scanOptifineCitRules(resourcepacksDir);

            RULES.clear();
            RULES.addAll(parsedRules);

            Pcustomtextures.LOGGER.info("Loaded {} OptiFine CIT rules", parsedRules.size());
            MinecraftClient client = MinecraftClient.getInstance();
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
                        parseProperties(namespace, propsPath, propsBytes, out);
                    }
                }
            }
        } catch (Exception e) {
            Pcustomtextures.LOGGER.warn("Failed to scan directory pack {}", packDir, e);
        }
    }

    private static void scanZipPack(Path zipPath, List<CitRule> out) {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
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

                parseProperties(namespace, name, propsBytes, out);
            }
        } catch (Exception e) {
            Pcustomtextures.LOGGER.warn("Failed to scan zip pack {}", zipPath, e);
        }
    }

    private static void parseProperties(String defaultNamespace, String propertiesPath, byte[] propertiesBytes, List<CitRule> out) {
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
        Map<String, List<Identifier>> armorTextures = new HashMap<>();

        if (type.equals("item")) {
            String modelRaw = properties.getProperty("model");
            if (modelRaw != null && !modelRaw.isBlank()) {
                itemModelId = parseItemModelIdentifier(defaultNamespace, modelRaw);
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

        if (itemModelId == null && armorTextures.isEmpty()) {
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
                armorTextures
        ));
    }

    private static Identifier parseItemModelIdentifier(String namespace, String raw) {
        String model = raw.trim().replace("\\", "/");
        if (model.endsWith(".json")) {
            model = model.substring(0, model.length() - 5);
        }

        if (model.contains(":")) {
            return Identifier.tryParse(model);
        }

        if (model.startsWith("item/")) {
            return Identifier.of(namespace, model);
        }

        return Identifier.of(namespace, "item/" + model);
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

        for (CitRule rule : RULES) {
            if (armorMode && rule.armorTextures().isEmpty()) {
                continue;
            }
            if (!armorMode && rule.itemModelId() == null) {
                continue;
            }
            if (!rule.items().contains(itemId)) {
                continue;
            }
            if (!matchesRule(rule, stack, fullStackNbt, componentsNbt)) {
                continue;
            }
            return rule;
        }

        return null;
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

        for (PathMatcherRule matcher : rule.matchers()) {
            NbtCompound root = matcher.source() == SourceNbt.COMPONENTS_ONLY ? componentsNbt : fullStackNbt;
            if (!matcher.matches(root)) {
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
        return nbt instanceof NbtCompound compound ? compound : null;
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
            Map<String, List<Identifier>> armorTextures
    ) {
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
            NbtElement current = root;
            for (String part : path.split("\\.")) {
                if (current == null) {
                    return null;
                }
                if (current instanceof NbtCompound compound) {
                    current = compound.contains(part) ? compound.get(part) : null;
                } else {
                    return null;
                }
            }
            return current;
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
