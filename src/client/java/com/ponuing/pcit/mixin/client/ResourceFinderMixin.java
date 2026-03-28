package com.ponuing.pcit.mixin.client;

import net.minecraft.resource.ResourceFinder;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResourceFinder.class)
public class ResourceFinderMixin {
    @Shadow @Final private String fileExtension;

    @Inject(method = "toResourcePath", cancellable = true, at = @At("HEAD"))
    private void pcit$forceAbsoluteTextureIdentifier(Identifier id, CallbackInfoReturnable<Identifier> cir) {
        if (id != null && id.getPath().endsWith(".png") && ".png".equals(this.fileExtension)) {
            cir.setReturnValue(id);
        }
    }
}
