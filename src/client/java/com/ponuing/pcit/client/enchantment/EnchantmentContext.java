package com.ponuing.pcit.client.enchantment;

import com.ponuing.pcit.client.NbtRenderOverrideResolver;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public record EnchantmentContext(
        ItemStack stack,
        World world,
        LivingEntity entity,
        NbtRenderOverrideResolver.HandMatch handMatch
) {
}
