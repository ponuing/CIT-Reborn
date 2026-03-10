package com.ponuing.pcustomtextures.mixin.client;

import net.minecraft.client.render.model.SpriteAtlasManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SpriteAtlasManager.AtlasPreparation.class)
public abstract class SpriteAtlasManagerAtlasPreparationMixin {

    @ModifyVariable(method = "getSprite", argsOnly = true, at = @At("HEAD"))
    private Identifier pcustomtextures$unwrapTexturePaths(Identifier id) {
        if (id.getPath().endsWith(".png")) {
            id = id.withPath(path -> path.substring(0, path.length() - 4));

            if (id.getPath().startsWith("textures/")) {
                id = id.withPath(path -> path.substring(9));
            }
        }
        return id;
    }
}
