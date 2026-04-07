package com.ponuing.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;
import com.ponuing.CITReborn;

import java.io.*;

/**
 * Contains runtime representation of CIT Reborn's config, encoded using GSON.
 */
public class CITRebornConfig {
    /**
     * Whether CIT Reborn should work or not.<br>
     * Requires a restart.
     */
    public boolean enabled = true;
    /**
     * Mutes pack loading errors from logs.
     */
    public boolean mute_errors = false;
    /**
     * Mutes pack loading warnings from logs.
     */
    public boolean mute_warns = false;
    /**
     * Invalidating interval for CITs' caches in milliseconds. Set to 0 to disable caching.
     */
    public int cache_ms = 50;
    /**
     * Should broken paths be allowed in resourcepacks. Requires a restart.
     * @see BrokenPaths
     */
    public boolean broken_paths = false;

    /**
     * CIT Reborn's config storage file.
     */
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "citreborn.json");

    /**
     * Active instance of the current config.
     */
    public static final CITRebornConfig INSTANCE = read();

    /**
     * Reads the stored config.
     * @see #FILE
     * @return the read config
     */
    public static CITRebornConfig read() {
        if (!FILE.exists())
            return new CITRebornConfig().write();

        Reader reader = null;
        try {
            return new Gson().fromJson(reader = new FileReader(FILE), CITRebornConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     * Saves this config to file.
     * @see #FILE
     * @return this
     */
    public CITRebornConfig write() {
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            writer = gson.newJsonWriter(new FileWriter(FILE));
            writer.setIndent("    ");

            gson.toJson(gson.toJsonTree(this, CITRebornConfig.class), writer);
        } catch (Exception e) {
            CITReborn.LOG.error("Couldn't save config");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return this;
    }
}
