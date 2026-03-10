package com.ponuing.pcustomtextures.mixin.client;

import com.ponuing.pcustomtextures.client.CitVirtualResourcePackProvider;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ResourcePackManager.class)
public class ResourcePackManagerMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, index = 1)
    private static ResourcePackProvider[] pcustomtextures$addProvider(ResourcePackProvider[] providers) {
        ResourcePackProvider[] extended = new ResourcePackProvider[providers.length + 1];
        System.arraycopy(providers, 0, extended, 0, providers.length);
        extended[providers.length] = new CitVirtualResourcePackProvider();
        return extended;
    }
}
