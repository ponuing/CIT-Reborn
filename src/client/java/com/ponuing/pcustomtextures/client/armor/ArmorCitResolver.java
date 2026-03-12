package com.ponuing.pcustomtextures.client.armor;

import com.ponuing.pcustomtextures.client.NbtRenderOverrideResolver;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ArmorCitResolver {
    private ArmorCitResolver() {
    }

    public static Identifier resolveArmorTextureOverride(ItemStack stack, Identifier originalTextureId) {
        return NbtRenderOverrideResolver.resolveArmorTextureOverride(stack, originalTextureId);
    }

    public static void ensureLoaded() {
        NbtRenderOverrideResolver.ensureLoaded();
    }

    public static void reloadFromManager(ResourceManager manager) {
        NbtRenderOverrideResolver.reloadFromManager(manager);
    }
}
