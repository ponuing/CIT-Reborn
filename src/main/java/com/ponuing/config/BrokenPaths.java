package com.ponuing.config;

import net.minecraft.util.Identifier;
import com.ponuing.mixin.broken_paths.*;

/**
 * Broken paths are resourcepack file paths that do not follow {@link Identifier}'s specifications.<br>
 * When enabled in config, CIT Reborn will forcibly allow broken paths to load.<br>
 * If not enabled, broken paths has no effect on the game.
 * @see CITRebornConfig#broken_paths
 * @see CITRebornMixinConfiguration#broken_paths
 * @see ReloadableResourceManagerImplMixin
 * @see IdentifierMixin
 * @see AbstractFileResourcePackMixin
 */
public class BrokenPaths {
    /**
     * When enabled, {@link Identifier}s will not check for their path's validity.
     * @see ReloadableResourceManagerImplMixin
     * @see IdentifierMixin
     */
    public static boolean processingBrokenPaths = false;
}
