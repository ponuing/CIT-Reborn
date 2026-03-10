package com.ponuing.pcustomtextures.mixin.client;

import net.minecraft.client.texture.atlas.AtlasLoader;
import net.minecraft.client.texture.atlas.AtlasSource;
import net.minecraft.client.texture.atlas.AtlasSourceType;
import net.minecraft.client.texture.atlas.AtlasSource.SpriteRegions;
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
        if (id == null || !"minecraft".equals(id.getNamespace())) {
            return;
        }
        String path = id.getPath();
        if (!"blocks".equals(path) && !"items".equals(path)) {
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
            }

            @Override
            public AtlasSourceType getType() {
                return null;
            }
        });
    }
}
