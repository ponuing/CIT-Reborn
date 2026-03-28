package com.ponuing.pcit.mixin.client;

import net.minecraft.client.render.model.SpriteAtlasManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SpriteAtlasManager.AtlasPreparation.class)
public abstract class SpriteAtlasManagerAtlasPreparationMixin {
    @ModifyVariable(method = "getSprite", argsOnly = true, at = @At("HEAD"))
    private Identifier pcit$unwrapTexturePaths(Identifier id) {
        String path = id.getPath();
        if (path.endsWith(".png")) {
            id = id.withPath(p -> p.substring(0, p.length() - 4));
            if (id.getPath().startsWith("textures/")) {
                id = id.withPath(p -> p.substring(9));
            }
        }
        return id;
    }
}
