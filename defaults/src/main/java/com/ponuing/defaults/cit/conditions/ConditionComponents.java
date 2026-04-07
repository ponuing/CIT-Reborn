package com.ponuing.defaults.cit.conditions;

import net.minecraft.component.ComponentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.ponuing.api.CITConditionContainer;
import com.ponuing.cit.CITCondition;
import com.ponuing.cit.CITContext;
import com.ponuing.cit.CITParsingException;
import com.ponuing.defaults.compat.LegacyNbtUtils;
import com.ponuing.pack.format.PropertyGroup;
import com.ponuing.pack.format.PropertyKey;
import com.ponuing.pack.format.PropertyValue;

public class ConditionComponents extends CITCondition {
    public static final CITConditionContainer<ConditionComponents> CONTAINER = new CITConditionContainer<>(ConditionComponents.class, ConditionComponents::new,
            "components", "component", "nbt");

    private ComponentType<?> componentType;
    private String componentMetadata;
    private String matchValue;

    private ConditionNBT fallbackNBTCheck;
    private ConditionNBT legacyNbtCheck;

    @Override
    public void load(PropertyKey key, PropertyValue value, PropertyGroup properties) throws CITParsingException {
        String metadata = value.keyMetadata();
        if (key.path().equals("nbt")) {
            if (metadata == null || metadata.isEmpty()) {
                throw new CITParsingException("Missing nbt path", properties, value.position());
            }
            String[] nbtPath = metadata.split("\\.");
            for (String s : nbtPath) {
                if (s.isEmpty()) {
                    throw new CITParsingException("Path segment cannot be empty", properties, value.position());
                }
            }
            this.legacyNbtCheck = new ConditionNBT();
            this.legacyNbtCheck.loadNbtCondition(value, properties, nbtPath, value.value());
            return;
        }

        metadata = LegacyNbtUtils.normalizeComponentPath(metadata);
        if (metadata == null || metadata.isBlank()) {
            throw new CITParsingException("Missing component path", properties, value.position());
        }

        String componentId = metadata.split("\\.")[0];

        if ((this.componentType = Registries.DATA_COMPONENT_TYPE.get(Identifier.tryParse(componentId))) == null)
            throw new CITParsingException("Unknown component type \"" + componentId + "\"", properties, value.position());

        metadata = metadata.substring(componentId.length());
        if (metadata.startsWith("."))
            metadata = metadata.substring(1);
        this.componentMetadata = metadata;

        this.matchValue = value.value();

        this.fallbackNBTCheck = new ConditionNBT();
        String[] metadataNbtPath = metadata.split("\\.");
        if (metadataNbtPath.length == 1 && metadataNbtPath[0].isEmpty())
            metadataNbtPath = new String[0];
        this.fallbackNBTCheck.loadNbtCondition(value, properties, metadataNbtPath, this.matchValue);
    }

    @Override
    public boolean test(CITContext context) {
        if (legacyNbtCheck != null) {
            NbtCompound root = LegacyNbtUtils.readFullStackNbt(context.stack, context.world);
            if (root == null) {
                return false;
            }
            if (legacyNbtCheck.testPath(root, 0, context)) {
                return true;
            }
            NbtCompound customData = LegacyNbtUtils.extractCustomData(root);
            if (customData != null && legacyNbtCheck.testPath(customData, 0, context)) {
                return true;
            }
            NbtCompound tag = LegacyNbtUtils.extractTag(root);
            return tag != null && legacyNbtCheck.testPath(tag, 0, context);
        }

        Object stackComponent = context.stack.getComponents().get(this.componentType);
        if (stackComponent != null) {
            if (stackComponent instanceof Text text) {
                if (this.fallbackNBTCheck.testString(null, text, context))
                    return true;
            } /*else if (stackComponent instanceof LoreComponent lore) {
                //todo avoid nbt based check if possible
            }*/

            NbtElement fallbackComponentNBT = ((ComponentType<Object>) this.componentType).getCodec().encodeStart(context.world.getRegistryManager().getOps(NbtOps.INSTANCE), stackComponent).getOrThrow();
            return this.fallbackNBTCheck.testPath(fallbackComponentNBT, 0, context);
        }
        return false;
    }
}
