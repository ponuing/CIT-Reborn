package com.ponuing.pcit.client.enchantment;

import com.ponuing.pcit.client.NbtRenderOverrideResolver;

public final class EnchantmentGlintResolver {
    private static final ThreadLocal<EnchantmentContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<NbtRenderOverrideResolver.EnchantmentOverride> ACTIVE = new ThreadLocal<>();

    private EnchantmentGlintResolver() {
    }

    public static boolean active() {
        NbtRenderOverrideResolver.ensureLoaded();
        return NbtRenderOverrideResolver.hasEnchantmentRules();
    }

    public static void setContext(EnchantmentContext context) {
        if (context == null) {
            CONTEXT.remove();
            ACTIVE.remove();
            return;
        }
        CONTEXT.set(context);
    }

    public static void apply() {
        EnchantmentContext context = CONTEXT.get();
        if (context == null) {
            ACTIVE.remove();
            return;
        }
        ACTIVE.set(NbtRenderOverrideResolver.resolveEnchantmentOverride(context.stack(), context.handMatch()));
    }

    public static boolean shouldApply() {
        return ACTIVE.get() != null;
    }

    public static NbtRenderOverrideResolver.EnchantmentOverride getActiveOverride() {
        return ACTIVE.get();
    }
}
