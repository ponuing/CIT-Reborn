package com.ponuing.pcustomtextures.client.elytra;

import com.ponuing.pcustomtextures.client.NbtRenderOverrideResolver;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ElytraCitResolver {
    private ElytraCitResolver() {
    }

    public static Identifier resolveElytraTextureOverride(ItemStack stack) {
        return NbtRenderOverrideResolver.resolveElytraTextureOverride(stack);
    }

    public static void ensureLoaded() {
        NbtRenderOverrideResolver.ensureLoaded();
    }

    public static void reloadFromManager(ResourceManager manager) {
        NbtRenderOverrideResolver.reloadFromManager(manager);
    }
}
