package com.ponuing.defaults.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;
import com.ponuing.CITReborn;

import java.io.*;

public class CITRebornDefaultsConfig {
    public float type_enchantment_scroll_multiplier = 1f;

    private static final File FILE = new File("config/citreborn-defaults.json");

    public static final CITRebornDefaultsConfig INSTANCE = read();

    public static CITRebornDefaultsConfig read() {
        if (!FILE.exists())
            return new CITRebornDefaultsConfig().write();

        Reader reader = null;
        try {
            return new Gson().fromJson(reader = new FileReader(FILE), CITRebornDefaultsConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public CITRebornDefaultsConfig write() {
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            FILE.getParentFile().mkdirs();
            writer = gson.newJsonWriter(new FileWriter(FILE));
            writer.setIndent("    ");

            gson.toJson(gson.toJsonTree(this, CITRebornDefaultsConfig.class), writer);
        } catch (Exception e) {
            CITReborn.LOG.error("Couldn't save defaults config");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return this;
    }
}
