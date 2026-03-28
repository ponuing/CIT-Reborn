package com.ponuing.pcit.mixin.client;

import net.minecraft.client.render.model.SpriteAtlasManager;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SpriteAtlasManager.class)
public class SpriteAtlasManagerMixin {
    @Inject(method = "<init>(Ljava/util/Map;Lnet/minecraft/client/texture/TextureManager;)V", at = @At("RETURN"))
    private void pcit$logAtlasMap(Map<Identifier, Identifier> atlases, TextureManager textureManager, CallbackInfo ci) {
    }
}
