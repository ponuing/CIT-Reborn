package com.ponuing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.ponuing.config.CITRebornConfig;
import com.ponuing.cit.CITRegistry;

/**
 * Main initializer for CIT Reborn. Contains various internal utilities(just logging for now).
 */
public class CITReborn implements ClientModInitializer {
    public static final Logger LOG = LogManager.getLogger("CITReborn");
    public static final CITReborn INSTANCE = new CITReborn();

    @Override
    public void onInitializeClient() {
        CITRegistry.registerAll();

        if (FabricLoader.getInstance().isModLoaded("fabric-command-api-v2"))
            CITRebornCommand.register();
    }

    /**
     * Logs an info line in CIT Reborn's name.
     * @param message log message
     */
    public static void info(String message) {
        LOG.info("[CITReborn] " + message);
    }

    /**
     * Logs a warning line in CIT Reborn's name if enabled in config.
     * @see CITRebornConfig#mute_warns
     * @param message warn message
     */
    public static void logWarnLoading(String message) {
        if (CITRebornConfig.INSTANCE.mute_warns)
            return;
        LOG.error("[CITReborn] " + message);
    }

    /**
     * Logs an error line in CIT Reborn's name if enabled in config.
     * @see CITRebornConfig#mute_errors
     * @param message error message
     */
    public static void logErrorLoading(String message) {
        if (CITRebornConfig.INSTANCE.mute_errors)
            return;
        LOG.error("{CITReborn} " + message);
    }
}
