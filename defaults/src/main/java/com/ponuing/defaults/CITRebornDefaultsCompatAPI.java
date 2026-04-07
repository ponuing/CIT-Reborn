package com.ponuing.defaults;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import com.ponuing.defaults.cit.types.TypeArmor;
import com.ponuing.defaults.cit.types.TypeElytra;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Holder for utility compatibility methods for use in other mods.
 */
public abstract class CITRebornDefaultsCompatAPI implements ClientModInitializer {
    /**
     * Entrypoint for client initialization that's only called with CIT Resewn: Defaults present.
     */
    public static final String ENTRYPOINT = "citresewn:defaults_compat";

    public static void initAll() {
        for (EntrypointContainer<CITRebornDefaultsCompatAPI> compat : FabricLoader.getInstance().getEntrypointContainers(CITRebornDefaultsCompatAPI.ENTRYPOINT, CITRebornDefaultsCompatAPI.class))
            compat.getEntrypoint().onInitializeClient();
    }

    /**
     * Registers a slot redirect for type=armor
     * @param redirect returns the currently visible armor item in the given equipment slot or null to not redirect.
     */
    protected final void typeArmorRedirectSlotGetter(BiFunction<LivingEntity, EquipmentSlot, ItemStack> redirect) {
        TypeArmor.CONTAINER.getItemInSlotCompatRedirects.add(redirect);
    }

    /**
     * Registers a slot redirect for type=elytra
     * @param redirect returns the currently visible elytra item or null to not redirect.
     */
    protected final void typeElytraRedirectSlotGetter(Function<LivingEntity, ItemStack> redirect) {
        TypeElytra.CONTAINER.getItemInSlotCompatRedirects.add(redirect);
    }
}
