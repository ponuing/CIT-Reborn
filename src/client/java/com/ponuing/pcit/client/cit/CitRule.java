package com.ponuing.pcit.client.cit;

import com.ponuing.pcit.client.NbtRenderOverrideResolver;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record CitRule(
        CitRuleType type,
        Set<Identifier> items,
        List<CitMatchers.PathMatcherRule> matchers,
        CitMatchers.RangeMatcher damageMatcher,
        Integer damageMask,
        CitMatchers.RangeMatcher stackSizeMatcher,
        CitMatchers.RangeMatcher enchantLevelMatcher,
        Set<Identifier> enchantmentIds,
        Boolean damaged,
        Boolean unbreakable,
        Identifier potion,
        int weight,
        NbtRenderOverrideResolver.HandMatch handMatch,
        Identifier itemModelId,
        CitSourceTexture sourceTexture,
        String ruleKey,
        List<Identifier> itemTextureCandidates,
        Map<String, List<Identifier>> itemNamedTextures,
        Map<String, Identifier> itemNamedModels,
        Map<String, List<Identifier>> armorTextures,
        List<Identifier> elytraTextureCandidates
) {
    public static final CitRule NO_MATCH = new CitRule(
            CitRuleType.ITEM,
            Set.of(),
            List.of(),
            null,
            null,
            null,
            null,
            Set.of(),
            null,
            null,
            null,
            0,
            NbtRenderOverrideResolver.HandMatch.ANY,
            null,
            null,
            "no_match",
            List.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
    );
}
