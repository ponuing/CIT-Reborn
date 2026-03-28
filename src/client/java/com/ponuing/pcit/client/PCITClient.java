package com.ponuing.pcit.client;

import net.fabricmc.api.ClientModInitializer;
import com.ponuing.pcit.client.item.ItemCitResolver;

public class PCITClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PCITModelLoadingPlugin.register();
        // Keep a safe early read; model loading will reload from manager.
        ItemCitResolver.ensureLoaded();
    }
}
