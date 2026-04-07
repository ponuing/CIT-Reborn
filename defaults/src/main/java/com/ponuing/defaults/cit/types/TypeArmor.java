package com.ponuing.defaults.cit.types;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import com.ponuing.api.CITTypeContainer;
import com.ponuing.cit.*;
import com.ponuing.defaults.cit.conditions.ConditionItems;
import com.ponuing.pack.format.PropertyGroup;
import com.ponuing.pack.format.PropertyKey;
import com.ponuing.pack.format.PropertyValue;

import java.util.*;
import java.util.function.BiFunction;

public class TypeArmor extends CITType {
    public static final Container CONTAINER = new Container();

    private static final ThreadLocal<Map<String, Identifier>> ACTIVE_TEXTURES = new ThreadLocal<>();

    public final Map<String, Identifier> textures = new HashMap<>();

    @Override
    public Set<PropertyKey> typeProperties() {
        return Set.of(PropertyKey.of("texture"));
    }

    @Override
    public void load(List<CITCondition> conditions, PropertyGroup properties, ResourceManager resourceManager) throws CITParsingException {
        boolean itemsConditionPresent = false;
        for (CITCondition condition : conditions)
            if (condition instanceof ConditionItems conditionItems)
                for (Item item : conditionItems.items)
                    if (item instanceof ArmorItem)
                        itemsConditionPresent = true;
                    else
                        throw new CITParsingException("This type only accepts armor items for the items condition", properties, -1);

        if (!itemsConditionPresent)
            try {
                Identifier propertiesName = Identifier.tryParse(properties.stripName());
                if (!Registries.ITEM.containsId(propertiesName))
                    throw new Exception();
                Item item = Registries.ITEM.get(propertiesName);
                if (!(item instanceof ArmorItem))
                    throw new Exception();
                conditions.add(new ConditionItems(item));
            } catch (Exception ignored) {
                throw new CITParsingException("Not targeting any item type", properties, -1);
            }

        for (PropertyValue propertyValue : properties.get("citresewn", "texture")) {
            Identifier identifier = resolveAsset(properties.identifier, propertyValue, "textures", ".png", resourceManager);
            if (identifier == null)
                throw new CITParsingException("Could not resolve texture", properties, propertyValue.position());

            textures.put(propertyValue.keyMetadata(), identifier);
        }
        if (textures.size() == 0)
            throw new CITParsingException("Texture not specified", properties, -1);
    }

    public static class Container extends CITTypeContainer<TypeArmor> {
        public Container() {
            super(TypeArmor.class, TypeArmor::new, "armor");
        }

        public final List<BiFunction<LivingEntity, EquipmentSlot, ItemStack>> getItemInSlotCompatRedirects = new ArrayList<>();

        public Set<CIT<TypeArmor>> loaded = new HashSet<>();
        public Map<ArmorItem, Set<CIT<TypeArmor>>> loadedTyped = new IdentityHashMap<>();

        @Override
        public void load(List<CIT<TypeArmor>> parsedCITs) {
            loaded.addAll(parsedCITs);
            for (CIT<TypeArmor> cit : parsedCITs)
                for (CITCondition condition : cit.conditions)
                    if (condition instanceof ConditionItems items)
                        for (Item item : items.items)
                            if (item instanceof ArmorItem armorItem)
                                loadedTyped.computeIfAbsent(armorItem, i -> new LinkedHashSet<>()).add(cit);
        }

        @Override
        public void dispose() {
            loaded.clear();
            loadedTyped.clear();
        }

        public CIT<TypeArmor> getCIT(CITContext context) {
            return ((CITCacheArmor) (Object) context.stack).CITReborn$getCacheTypeArmor().get(context).get();
        }

        public CIT<TypeArmor> getRealTimeCIT(CITContext context) {
            if (!(context.stack.getItem() instanceof ArmorItem))
                return null;

            Set<CIT<TypeArmor>> loadedForItemType = loadedTyped.get(context.stack.getItem());
            if (loadedForItemType != null)
                for (CIT<TypeArmor> cit : loadedForItemType)
                    if (cit.test(context))
                        return cit;

            return null;
        }

        public ItemStack getVisualItemInSlot(LivingEntity entity, EquipmentSlot slot) {
            for (BiFunction<LivingEntity, EquipmentSlot, ItemStack> redirect : getItemInSlotCompatRedirects) {
                ItemStack stack = redirect.apply(entity, slot);
                if (stack != null)
                    return stack;
            }

            return entity.getEquippedStack(slot);
        }
    }

    public interface CITCacheArmor {
        CITCache.Single<TypeArmor> CITReborn$getCacheTypeArmor();
    }

    public static void pushActiveTextures(Map<String, Identifier> textures) {
        ACTIVE_TEXTURES.set(textures);
    }

    public static void clearActiveTextures() {
        ACTIVE_TEXTURES.remove();
    }

    public static Identifier resolveActiveTexture(EquipmentModel.LayerType layerType, Identifier layerTextureId) {
        Map<String, Identifier> textures = ACTIVE_TEXTURES.get();
        if (textures == null || textures.isEmpty())
            return null;

        boolean legs = layerType == EquipmentModel.LayerType.HUMANOID_LEGGINGS;
        boolean overlay = layerTextureId != null && layerTextureId.getPath().endsWith("_overlay");
        String textureName = layerTextureId == null ? null : layerTextureId.getPath();
        if (textureName != null && textureName.contains("/"))
            textureName = textureName.substring(textureName.lastIndexOf('/') + 1);
        if (textureName != null && overlay && textureName.endsWith("_overlay"))
            textureName = textureName.substring(0, textureName.length() - "_overlay".length());

        String layerSuffix = legs ? "layer_2" : "layer_1";
        String overlaySuffix = overlay ? "_overlay" : "";

        List<String> candidates = new ArrayList<>();
        if (textureName != null && !textureName.isBlank())
            candidates.add(textureName + "_" + layerSuffix + overlaySuffix);
        candidates.add(layerSuffix + overlaySuffix);
        if (textureName != null && !textureName.isBlank())
            candidates.add(textureName + "_" + layerSuffix);
        candidates.add(legs ? "2" : "1");
        candidates.add("layer" + (legs ? "2" : "1") + overlaySuffix);

        for (String key : candidates) {
            Identifier identifier = textures.get(key);
            if (identifier != null)
                return identifier;
        }

        return textures.get(null);
    }
}
