package com.ponuing.pcit.mixin.client;

import net.minecraft.client.texture.atlas.AtlasLoader;
import net.minecraft.client.texture.atlas.AtlasSource;
import net.minecraft.client.texture.atlas.AtlasSourceType;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(AtlasLoader.class)
public class AtlasLoaderMixin {
    @Shadow @Final private List<AtlasSource> sources;

    @Inject(method = "of", at = @At("RETURN"), cancellable = true)
    private static void pcustomtextures$addOptifineCitSource(ResourceManager resourceManager, Identifier id, CallbackInfoReturnable<AtlasLoader> cir) {
        if (id == null) {
            return;
        }
        AtlasLoader loader = cir.getReturnValue();
        if (loader == null) {
            return;
        }
        ((AtlasLoaderMixin) (Object) loader).sources.add(new AtlasSource() {
            @Override
            public void load(ResourceManager resourceManager, SpriteRegions regions) {
                ResourceFinder finder = new ResourceFinder("optifine/cit", ".png");
                for (Map.Entry<Identifier, Resource> entry : finder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = finder.toResourceId(entry.getKey()).withPrefixedPath("optifine/cit/");
                    regions.add(spriteId, entry.getValue());
                }
                ResourceFinder mcpatcherFinder = new ResourceFinder("mcpatcher/cit", ".png");
                for (Map.Entry<Identifier, Resource> entry : mcpatcherFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = mcpatcherFinder.toResourceId(entry.getKey()).withPrefixedPath("mcpatcher/cit/");
                    regions.add(spriteId, entry.getValue());
                }
                ResourceFinder citresewnFinder = new ResourceFinder("citresewn/cit", ".png");
                for (Map.Entry<Identifier, Resource> entry : citresewnFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = citresewnFinder.toResourceId(entry.getKey()).withPrefixedPath("citresewn/cit/");
                    regions.add(spriteId, entry.getValue());
                }
                ResourceFinder citFinder = new ResourceFinder("cit", ".png");
                for (Map.Entry<Identifier, Resource> entry : citFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = citFinder.toResourceId(entry.getKey()).withPrefixedPath("cit/");
                    regions.add(spriteId, entry.getValue());
                }
                ResourceFinder texturesCitFinder = new ResourceFinder("textures/cit", ".png");
                for (Map.Entry<Identifier, Resource> entry : texturesCitFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = texturesCitFinder.toResourceId(entry.getKey()).withPrefixedPath("cit/");
                    regions.add(spriteId, entry.getValue());
                }
                ResourceFinder texturesOptifineCitFinder = new ResourceFinder("textures/optifine/cit", ".png");
                for (Map.Entry<Identifier, Resource> entry : texturesOptifineCitFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = texturesOptifineCitFinder.toResourceId(entry.getKey()).withPrefixedPath("optifine/cit/");
                    regions.add(spriteId, entry.getValue());
                }
                ResourceFinder texturesMcpatcherCitFinder = new ResourceFinder("textures/mcpatcher/cit", ".png");
                for (Map.Entry<Identifier, Resource> entry : texturesMcpatcherCitFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = texturesMcpatcherCitFinder.toResourceId(entry.getKey()).withPrefixedPath("mcpatcher/cit/");
                    regions.add(spriteId, entry.getValue());
                }
                ResourceFinder texturesCitresewnCitFinder = new ResourceFinder("textures/citresewn/cit", ".png");
                for (Map.Entry<Identifier, Resource> entry : texturesCitresewnCitFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = texturesCitresewnCitFinder.toResourceId(entry.getKey()).withPrefixedPath("citresewn/cit/");
                    regions.add(spriteId, entry.getValue());
                }
            }

            @Override
            public AtlasSourceType getType() {
                return null;
            }
        });
    }
}
