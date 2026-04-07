package com.ponuing.mixin.broken_paths;

import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceReload;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ponuing.CITReborn;
import com.ponuing.config.BrokenPaths;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.ponuing.config.BrokenPaths.processingBrokenPaths;

/**
 * Starts/Stops broken paths logic.
 * @see BrokenPaths
 * @see IdentifierMixin
 */
@Mixin(ReloadableResourceManagerImpl.class)
public class ReloadableResourceManagerImplMixin {
    @Shadow @Final private ResourceType type;

    @Inject(method = "reload", at = @At("RETURN"))
    public void CITReborn$brokenpaths$onReload(Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, List<ResourcePack> packs, CallbackInfoReturnable<ResourceReload> cir) {
        if (processingBrokenPaths = this.type == ResourceType.CLIENT_RESOURCES) {
            CITReborn.LOG.error("[CITReborn] Caution! Broken paths is enabled!");
            cir.getReturnValue().whenComplete().thenRun(() -> processingBrokenPaths = false);
        }
    }
}
