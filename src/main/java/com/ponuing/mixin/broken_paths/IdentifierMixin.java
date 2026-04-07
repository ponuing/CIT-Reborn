package com.ponuing.mixin.broken_paths;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ponuing.CITReborn;
import com.ponuing.config.BrokenPaths;

import static com.ponuing.config.BrokenPaths.processingBrokenPaths;

/**
 * Applies broken paths logic when active.
 * @see BrokenPaths
 * @see ReloadableResourceManagerImplMixin
 */
@Mixin(Identifier.class)
public class IdentifierMixin {
    @Inject(method = "isPathValid", cancellable = true, at = @At("RETURN"))
    private static void CITReborn$brokenpaths$processBrokenPaths(String path, CallbackInfoReturnable<Boolean> cir) {
        if (!processingBrokenPaths)
            return;

        if (!cir.getReturnValue()) {
            if (FabricLoader.getInstance().isDevelopmentEnvironment())
                CITReborn.logWarnLoading("Warning: Encountered broken path: \"" + path + "\"");

            cir.setReturnValue(true);
        }
    }
}
