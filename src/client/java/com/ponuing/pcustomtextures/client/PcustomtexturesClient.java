package com.ponuing.pcustomtextures.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class PcustomtexturesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PcustomtexturesModelLoadingPlugin.register();
        NbtRenderOverrideResolver.reload();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("pcustomtextures")
                        .then(literal("reload").executes(context -> {
                            NbtRenderOverrideResolver.reload();
                            context.getSource().sendFeedback(Text.literal("pcustomtextures: rules reloaded"));
                            return 1;
                        }))));
    }
}
