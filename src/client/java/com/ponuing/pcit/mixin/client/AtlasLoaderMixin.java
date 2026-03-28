package com.ponuing.pcit.mixin.client;

import net.minecraft.client.texture.atlas.AtlasLoader;
import net.minecraft.client.texture.atlas.AtlasSource;
import net.minecraft.client.texture.atlas.AtlasSourceType;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import com.ponuing.pcit.PCIT;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(AtlasLoader.class)
public class AtlasLoaderMixin {
    @Shadow @Final private List<AtlasSource> sources;
    private static final Set<Identifier> LOGGED_ATLAS_COUNTS = ConcurrentHashMap.newKeySet();

    @Inject(method = "of", at = @At("RETURN"), cancellable = true)
    private static void pcit$addOptifineCitSource(ResourceManager resourceManager, Identifier id, CallbackInfoReturnable<AtlasLoader> cir) {
        if (id == null) {
            return;
        }
        if (!"minecraft".equals(id.getNamespace()) || !isItemOrBlockAtlas(id)) {
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
                int optifineCount = 0;
                for (Map.Entry<Identifier, Resource> entry : finder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = finder.toResourceId(entry.getKey()).withPrefixedPath("optifine/cit/");
                    regions.add(spriteId, entry.getValue());
                    optifineCount++;
                }
                ResourceFinder mcpatcherFinder = new ResourceFinder("mcpatcher/cit", ".png");
                int mcpatcherCount = 0;
                for (Map.Entry<Identifier, Resource> entry : mcpatcherFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = mcpatcherFinder.toResourceId(entry.getKey()).withPrefixedPath("mcpatcher/cit/");
                    regions.add(spriteId, entry.getValue());
                    mcpatcherCount++;
                }
                ResourceFinder citresewnFinder = new ResourceFinder("citresewn/cit", ".png");
                int citresewnCount = 0;
                for (Map.Entry<Identifier, Resource> entry : citresewnFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = citresewnFinder.toResourceId(entry.getKey()).withPrefixedPath("citresewn/cit/");
                    regions.add(spriteId, entry.getValue());
                    citresewnCount++;
                }
                ResourceFinder citFinder = new ResourceFinder("cit", ".png");
                int citCount = 0;
                for (Map.Entry<Identifier, Resource> entry : citFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = citFinder.toResourceId(entry.getKey()).withPrefixedPath("cit/");
                    regions.add(spriteId, entry.getValue());
                    citCount++;
                }
                ResourceFinder texturesCitFinder = new ResourceFinder("textures/cit", ".png");
                int texturesCitCount = 0;
                for (Map.Entry<Identifier, Resource> entry : texturesCitFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = texturesCitFinder.toResourceId(entry.getKey()).withPrefixedPath("cit/");
                    regions.add(spriteId, entry.getValue());
                    texturesCitCount++;
                }
                ResourceFinder texturesOptifineCitFinder = new ResourceFinder("textures/optifine/cit", ".png");
                int texturesOptifineCount = 0;
                for (Map.Entry<Identifier, Resource> entry : texturesOptifineCitFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = texturesOptifineCitFinder.toResourceId(entry.getKey()).withPrefixedPath("optifine/cit/");
                    regions.add(spriteId, entry.getValue());
                    texturesOptifineCount++;
                }
                ResourceFinder texturesMcpatcherCitFinder = new ResourceFinder("textures/mcpatcher/cit", ".png");
                int texturesMcpatcherCount = 0;
                for (Map.Entry<Identifier, Resource> entry : texturesMcpatcherCitFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = texturesMcpatcherCitFinder.toResourceId(entry.getKey()).withPrefixedPath("mcpatcher/cit/");
                    regions.add(spriteId, entry.getValue());
                    texturesMcpatcherCount++;
                }
                ResourceFinder texturesCitresewnCitFinder = new ResourceFinder("textures/citresewn/cit", ".png");
                int texturesCitresewnCount = 0;
                for (Map.Entry<Identifier, Resource> entry : texturesCitresewnCitFinder.findResources(resourceManager).entrySet()) {
                    Identifier spriteId = texturesCitresewnCitFinder.toResourceId(entry.getKey()).withPrefixedPath("citresewn/cit/");
                    regions.add(spriteId, entry.getValue());
                    texturesCitresewnCount++;
                }
                if (LOGGED_ATLAS_COUNTS.add(id)) {
                    PCIT.LOGGER.info(
                            "[model] atlas {} sprites optifine/cit={}, mcpatcher/cit={}, citresewn/cit={}, cit={}, textures/optifine/cit={}, textures/mcpatcher/cit={}, textures/citresewn/cit={}, textures/cit={}",
                            id,
                            optifineCount,
                            mcpatcherCount,
                            citresewnCount,
                            citCount,
                            texturesOptifineCount,
                            texturesMcpatcherCount,
                            texturesCitresewnCount,
                            texturesCitCount
                    );
                }
            }

            @Override
            public AtlasSourceType getType() {
                return null;
            }
        });
    }

    private static boolean isItemOrBlockAtlas(Identifier id) {
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        if ("textures/atlas/blocks.png".equals(path)) {
            return true;
        }
        if ("blocks".equals(path)) {
            return true;
        }
        return path.endsWith("/blocks.png") || path.endsWith("/blocks") || path.endsWith("blocks.png");
    }
}
