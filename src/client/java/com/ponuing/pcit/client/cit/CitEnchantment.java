package com.ponuing.pcit.client.cit;

import com.ponuing.pcit.client.enchantment.EnchantmentBlend;
import com.ponuing.pcit.client.enchantment.EnchantmentLayer;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Set;

public record CitEnchantment(
        List<Identifier> textureCandidates,
        EnchantmentBlend blend,
        Float speed,
        Float rotation,
        Integer duration,
        Set<EnchantmentLayer> layers
) {
}
