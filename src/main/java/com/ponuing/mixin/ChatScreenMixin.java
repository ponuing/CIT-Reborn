package com.ponuing.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.ponuing.CITRebornCommand;
import com.ponuing.config.CITRebornConfigScreenFactory;

import static com.ponuing.CITRebornCommand.openConfig;

/**
 * Opens the config screen when running the "/citreborn config" command.
 * @see CITRebornCommand#openConfig
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    /**
     * If {@link CITRebornCommand#openConfig} is true, changes the screen that's opened when the chat is closed to the config screen.
     * @see CITRebornCommand#openConfig
     */
    @ModifyArg(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
    public Screen CITReborn$redirectConfigScreen(Screen original) {
        if (openConfig) {
            openConfig = false;
            return FabricLoader.getInstance().isModLoaded("cloth-config2") ?
                    CITRebornConfigScreenFactory.create(null) :
                    new NoticeScreen(() -> MinecraftClient.getInstance().setScreen(null), Text.of("CIT Reborn"), Text.of("CIT Reborn requires Cloth Config to be able to show the config."));
        }

        return original;
    }
}
