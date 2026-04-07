package com.ponuing.defaults.compat;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LegacyNbtUtils {
    private LegacyNbtUtils() {
    }

    public static final Map<Integer, Identifier> LEGACY_ENCHANTMENT_IDS = Map.ofEntries(
            Map.entry(0, Identifier.of("minecraft", "protection")),
            Map.entry(1, Identifier.of("minecraft", "fire_protection")),
            Map.entry(2, Identifier.of("minecraft", "feather_falling")),
            Map.entry(3, Identifier.of("minecraft", "blast_protection")),
            Map.entry(4, Identifier.of("minecraft", "projectile_protection")),
            Map.entry(5, Identifier.of("minecraft", "respiration")),
            Map.entry(6, Identifier.of("minecraft", "aqua_affinity")),
            Map.entry(7, Identifier.of("minecraft", "thorns")),
            Map.entry(8, Identifier.of("minecraft", "depth_strider")),
            Map.entry(9, Identifier.of("minecraft", "frost_walker")),
            Map.entry(10, Identifier.of("minecraft", "binding_curse")),
            Map.entry(16, Identifier.of("minecraft", "sharpness")),
            Map.entry(17, Identifier.of("minecraft", "smite")),
            Map.entry(18, Identifier.of("minecraft", "bane_of_arthropods")),
            Map.entry(19, Identifier.of("minecraft", "knockback")),
            Map.entry(20, Identifier.of("minecraft", "fire_aspect")),
            Map.entry(21, Identifier.of("minecraft", "looting")),
            Map.entry(22, Identifier.of("minecraft", "sweeping_edge")),
            Map.entry(32, Identifier.of("minecraft", "efficiency")),
            Map.entry(33, Identifier.of("minecraft", "silk_touch")),
            Map.entry(34, Identifier.of("minecraft", "unbreaking")),
            Map.entry(35, Identifier.of("minecraft", "fortune")),
            Map.entry(48, Identifier.of("minecraft", "power")),
            Map.entry(49, Identifier.of("minecraft", "punch")),
            Map.entry(50, Identifier.of("minecraft", "flame")),
            Map.entry(51, Identifier.of("minecraft", "infinity")),
            Map.entry(61, Identifier.of("minecraft", "luck_of_the_sea")),
            Map.entry(62, Identifier.of("minecraft", "lure")),
            Map.entry(65, Identifier.of("minecraft", "riptide")),
            Map.entry(66, Identifier.of("minecraft", "loyalty")),
            Map.entry(67, Identifier.of("minecraft", "channeling")),
            Map.entry(68, Identifier.of("minecraft", "impaling")),
            Map.entry(70, Identifier.of("minecraft", "mending")),
            Map.entry(71, Identifier.of("minecraft", "vanishing_curse"))
    );

    public static final Map<Identifier, Integer> LEGACY_STATUS_EFFECT_IDS = Map.ofEntries(
            Map.entry(Identifier.of("minecraft", "speed"), 1),
            Map.entry(Identifier.of("minecraft", "slowness"), 2),
            Map.entry(Identifier.of("minecraft", "haste"), 3),
            Map.entry(Identifier.of("minecraft", "mining_fatigue"), 4),
            Map.entry(Identifier.of("minecraft", "strength"), 5),
            Map.entry(Identifier.of("minecraft", "instant_health"), 6),
            Map.entry(Identifier.of("minecraft", "instant_damage"), 7),
            Map.entry(Identifier.of("minecraft", "jump_boost"), 8),
            Map.entry(Identifier.of("minecraft", "nausea"), 9),
            Map.entry(Identifier.of("minecraft", "regeneration"), 10),
            Map.entry(Identifier.of("minecraft", "resistance"), 11),
            Map.entry(Identifier.of("minecraft", "fire_resistance"), 12),
            Map.entry(Identifier.of("minecraft", "water_breathing"), 13),
            Map.entry(Identifier.of("minecraft", "invisibility"), 14),
            Map.entry(Identifier.of("minecraft", "blindness"), 15),
            Map.entry(Identifier.of("minecraft", "night_vision"), 16),
            Map.entry(Identifier.of("minecraft", "hunger"), 17),
            Map.entry(Identifier.of("minecraft", "weakness"), 18),
            Map.entry(Identifier.of("minecraft", "poison"), 19),
            Map.entry(Identifier.of("minecraft", "wither"), 20),
            Map.entry(Identifier.of("minecraft", "health_boost"), 21),
            Map.entry(Identifier.of("minecraft", "absorption"), 22),
            Map.entry(Identifier.of("minecraft", "saturation"), 23),
            Map.entry(Identifier.of("minecraft", "glowing"), 24),
            Map.entry(Identifier.of("minecraft", "levitation"), 25),
            Map.entry(Identifier.of("minecraft", "luck"), 26),
            Map.entry(Identifier.of("minecraft", "unluck"), 27),
            Map.entry(Identifier.of("minecraft", "slow_falling"), 28),
            Map.entry(Identifier.of("minecraft", "conduit_power"), 29),
            Map.entry(Identifier.of("minecraft", "dolphins_grace"), 30),
            Map.entry(Identifier.of("minecraft", "bad_omen"), 31),
            Map.entry(Identifier.of("minecraft", "hero_of_the_village"), 32),
            Map.entry(Identifier.of("minecraft", "darkness"), 33)
    );

    public static NbtCompound readFullStackNbt(ItemStack stack, World world) {
        if (stack == null || world == null) {
            return null;
        }

        NbtElement nbt = stack.toNbt(world.getRegistryManager());
        if (nbt instanceof NbtCompound compound) {
            injectBlockEntityTagFromComponents(compound);
            injectDisplayNameFromComponents(compound, stack);
            injectLoreFromComponents(compound, stack);
            injectPotionTagsFromComponents(compound);
            return compound;
        }
        return null;
    }

    public static NbtCompound extractCustomData(NbtCompound root) {
        if (root == null || !root.contains("components", NbtElement.COMPOUND_TYPE)) {
            return null;
        }
        NbtCompound components = root.getCompound("components");
        return components.contains("minecraft:custom_data", NbtElement.COMPOUND_TYPE)
                ? components.getCompound("minecraft:custom_data")
                : null;
    }

    public static NbtCompound extractTag(NbtCompound root) {
        if (root == null || !root.contains("tag", NbtElement.COMPOUND_TYPE)) {
            return null;
        }
        return root.getCompound("tag");
    }

    public static void injectDisplayNameFromComponents(NbtCompound root, ItemStack stack) {
        if (root == null || stack == null) {
            return;
        }
        if (root.contains("display", NbtElement.COMPOUND_TYPE)) {
            NbtCompound display = root.getCompound("display");
            if (display.contains("Name", NbtElement.STRING_TYPE)) {
                return;
            }
        }
        Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (name == null) {
            return;
        }
        NbtCompound display = root.contains("display", NbtElement.COMPOUND_TYPE)
                ? root.getCompound("display")
                : new NbtCompound();
        display.putString("Name", name.getString());
        root.put("display", display);
    }

    public static void injectLoreFromComponents(NbtCompound root, ItemStack stack) {
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
        NbtCompound display = root.contains("display", NbtElement.COMPOUND_TYPE)
                ? root.getCompound("display")
                : new NbtCompound();
        NbtList list = new NbtList();
        for (Object line : lines) {
            if (line instanceof Text text) {
                list.add(NbtString.of(text.getString()));
            } else {
                list.add(NbtString.of(String.valueOf(line)));
            }
        }
        display.put("Lore", list);
        root.put("display", display);
    }

    public static void injectPotionTagsFromComponents(NbtCompound root) {
        if (root == null || !root.contains("components", NbtElement.COMPOUND_TYPE)) {
            return;
        }
        NbtCompound components = root.getCompound("components");
        if (!components.contains("minecraft:potion_contents", NbtElement.COMPOUND_TYPE)) {
            return;
        }

        NbtCompound potionContents = components.getCompound("minecraft:potion_contents");
        NbtCompound customData = components.contains("minecraft:custom_data", NbtElement.COMPOUND_TYPE)
                ? components.getCompound("minecraft:custom_data")
                : null;

        String potionId = readString(potionContents, "potion", "Potion");
        if (isBlank(potionId) && customData != null) {
            potionId = readString(customData, "Potion", "potion");
        }

        Integer color = readInt(potionContents, "custom_color", "CustomPotionColor", "customColor");
        if (color == null && customData != null) {
            color = readInt(customData, "CustomPotionColor", "custom_color", "customColor");
        }

        NbtList effects = null;
        if (customData != null) {
            effects = customData.getList("CustomPotionEffects", NbtElement.COMPOUND_TYPE);
        }
        if ((effects == null || effects.isEmpty()) && potionContents.contains("custom_effects", NbtElement.LIST_TYPE)) {
            effects = potionContents.getList("custom_effects", NbtElement.COMPOUND_TYPE);
        }

        if (!isBlank(potionId) || color != null || (effects != null && !effects.isEmpty())) {
            if (isBlank(potionId) && root.contains("Potion", NbtElement.STRING_TYPE)) {
                potionId = root.getString("Potion");
            }
            if (color == null && root.contains("CustomPotionColor", NbtElement.INT_TYPE)) {
                color = root.getInt("CustomPotionColor");
            }
        }

        if (!isBlank(potionId)) {
            root.putString("Potion", potionId);
        }
        if (color != null) {
            root.putInt("CustomPotionColor", color);
        }
        if (effects != null && !effects.isEmpty()) {
            NbtList legacy = buildLegacyPotionEffects(effects);
            root.put("CustomPotionEffects", legacy);
        }
    }

    public static NbtList buildLegacyPotionEffects(NbtList source) {
        NbtList legacy = new NbtList();
        if (source == null || source.isEmpty()) {
            return legacy;
        }
        for (int i = 0; i < source.size(); i++) {
            NbtCompound effect = source.getCompound(i);
            if (effect == null || effect.isEmpty()) {
                continue;
            }
            String id = readString(effect, "id", "Id");
            if (isBlank(id)) {
                Integer numericId = readInt(effect, "id", "Id");
                if (numericId != null) {
                    id = numericId.toString();
                }
            }

            Integer amplifier = readInt(effect, "amplifier", "Amplifier");
            Integer duration = readInt(effect, "duration", "Duration");
            Boolean ambient = readBooleanLike(effect, "ambient", "Ambient");
            Boolean showParticles = readBooleanLike(effect, "show_particles", "showParticles", "ShowParticles");
            Boolean showIcon = readBooleanLike(effect, "show_icon", "showIcon", "ShowIcon");

            NbtCompound legacyEffect = new NbtCompound();
            if (!isBlank(id)) {
                Identifier effectId = Identifier.tryParse(id);
                if (effectId != null) {
                    Integer legacyId = LEGACY_STATUS_EFFECT_IDS.get(effectId);
                    if (legacyId != null) {
                        legacyEffect.putInt("Id", legacyId);
                    } else {
                        legacyEffect.putString("Id", id);
                    }
                } else {
                    legacyEffect.putString("Id", id);
                }
            }
            if (amplifier != null) {
                legacyEffect.putInt("Amplifier", amplifier);
            }
            if (duration != null) {
                legacyEffect.putInt("Duration", duration);
            }
            if (ambient != null) {
                legacyEffect.putBoolean("Ambient", ambient);
            }
            if (showParticles != null) {
                legacyEffect.putBoolean("ShowParticles", showParticles);
            }
            if (showIcon != null) {
                legacyEffect.putBoolean("ShowIcon", showIcon);
            }
            legacy.add(legacyEffect);
        }
        return legacy;
    }

    public static String readString(NbtCompound compound, String... keys) {
        if (compound == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (compound.contains(key, NbtElement.STRING_TYPE)) {
                return compound.getString(key);
            }
        }
        return null;
    }

    public static Integer readInt(NbtCompound compound, String... keys) {
        if (compound == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (compound.contains(key, NbtElement.NUMBER_TYPE)) {
                return compound.getInt(key);
            }
            if (compound.contains(key, NbtElement.STRING_TYPE)) {
                String raw = compound.getString(key);
                Integer parsed = parseIntFlexible(raw).orElse(null);
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return null;
    }

    public static Boolean readBooleanLike(NbtCompound compound, String... keys) {
        if (compound == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (compound.contains(key, NbtElement.BYTE_TYPE)) {
                return compound.getByte(key) != 0;
            }
            if (compound.contains(key, NbtElement.INT_TYPE)) {
                int val = compound.getInt(key);
                if (val == 0 || val == 1) {
                    return val == 1;
                }
            }
            if (compound.contains(key, NbtElement.STRING_TYPE)) {
                String raw = compound.getString(key);
                if ("true".equalsIgnoreCase(raw)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(raw)) {
                    return false;
                }
            }
        }
        return null;
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static List<?> extractLoreLines(Object lore) {
        if (lore == null) {
            return null;
        }
        if (lore instanceof List<?> list) {
            return list;
        }
        if (lore instanceof LoreComponent loreComponent) {
            return loreComponent.lines();
        }
        return null;
    }

    public static void injectBlockEntityTagFromComponents(NbtCompound root) {
        if (root == null) {
            return;
        }
        if (root.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE)) {
            return;
        }
        if (!root.contains("components", NbtElement.COMPOUND_TYPE)) {
            return;
        }

        NbtCompound components = root.getCompound("components");
        if (components.contains("minecraft:block_entity_data", NbtElement.COMPOUND_TYPE)) {
            NbtCompound blockEntityData = components.getCompound("minecraft:block_entity_data");
            NbtCompound legacy = new NbtCompound();
            for (String key : blockEntityData.getKeys()) {
                legacy.put(key, blockEntityData.get(key));
            }

            if (legacy.contains("Items", NbtElement.LIST_TYPE)) {
                NbtList source = legacy.getList("Items", NbtElement.COMPOUND_TYPE);
                NbtList converted = convertContainerList(source);
                legacy.put("Items", converted);
            } else if (legacy.contains("items", NbtElement.LIST_TYPE)) {
                NbtList source = legacy.getList("items", NbtElement.COMPOUND_TYPE);
                NbtList converted = convertContainerList(source);
                legacy.put("Items", converted);
            }

            if (!legacy.isEmpty()) {
                root.put("BlockEntityTag", legacy);
            }
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
            if (!converted.isEmpty()) {
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
            return new NbtList();
        }
        java.util.Map<Integer, NbtCompound> bySlot = new java.util.HashMap<>();
        int maxSlot = -1;
        for (int i = 0; i < containerList.size(); i++) {
            NbtCompound entry = containerList.getCompound(i);
            Integer slot = readSlot(entry);
            NbtCompound item = readItem(entry);
            if (slot == null || item == null) {
                continue;
            }
            NbtCompound converted = convertContainerItem(slot, item);
            if (converted != null && !converted.isEmpty()) {
                bySlot.put(slot, converted);
                if (slot > maxSlot) {
                    maxSlot = slot;
                }
            }
        }
        NbtList result = new NbtList();
        for (int i = 0; i <= maxSlot; i++) {
            result.add(bySlot.getOrDefault(i, new NbtCompound()));
        }
        return result;
    }

    private static Integer readSlot(NbtCompound entry) {
        if (entry == null) {
            return null;
        }
        if (entry.contains("Slot", NbtElement.NUMBER_TYPE)) {
            return entry.getInt("Slot");
        }
        if (entry.contains("slot", NbtElement.NUMBER_TYPE)) {
            return entry.getInt("slot");
        }
        return null;
    }

    private static NbtCompound readItem(NbtCompound entry) {
        if (entry == null) {
            return null;
        }
        if (entry.contains("item", NbtElement.COMPOUND_TYPE)) {
            return entry.getCompound("item");
        }
        if (entry.contains("Item", NbtElement.COMPOUND_TYPE)) {
            return entry.getCompound("Item");
        }
        return null;
    }

    private static NbtCompound convertContainerItem(int slot, NbtCompound item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        String id = readString(item, "id", "Id");
        if (isBlank(id)) {
            return null;
        }
        int count = 1;
        if (item.contains("count", NbtElement.NUMBER_TYPE)) {
            count = item.getInt("count");
        } else if (item.contains("Count", NbtElement.NUMBER_TYPE)) {
            count = item.getInt("Count");
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

    public static String normalizeComponentPath(String raw) {
        if (raw == null) {
            return null;
        }
        String path = raw.replace("\\:", ":").trim();
        if (path.isEmpty()) {
            return path;
        }
        if (!path.contains(":") && path.startsWith("minecraft.")) {
            path = "minecraft:" + path.substring("minecraft.".length());
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
        if (!first && segment.contains(":")) {
            return segment;
        }
        return segment;
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
}
