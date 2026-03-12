package com.ponuing.pcustomtextures.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import com.ponuing.pcustomtextures.client.item.ItemCitResolver;
public class PcustomtexturesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PcustomtexturesModelLoadingPlugin.register();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of("pcustomtextures", "cit_rules");
            }

            @Override
            public void reload(ResourceManager manager) {
                ItemCitResolver.reloadFromManager(manager);
            }
        });
    }
}
