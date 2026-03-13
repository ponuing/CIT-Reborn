package com.ponuing.pcit.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import com.ponuing.pcit.client.item.ItemCitResolver;
public class PCITClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PCITModelLoadingPlugin.register();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of("pcit", "cit_rules");
            }

            @Override
            public void reload(ResourceManager manager) {
                ItemCitResolver.reloadFromManager(manager);
            }
        });
    }
}
