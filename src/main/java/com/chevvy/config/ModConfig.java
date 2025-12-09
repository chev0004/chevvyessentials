package com.chevvy.config;

import com.chevvy.ChevvyEssentials;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(ChevvyEssentials.MOD_ID);
    private static final Path CONFIG_FILE = CONFIG_PATH.resolve("config.json");

    private static ConfigData config;

    public static class ConfigData {
        public int maxHomes;
        public int tpaTimeoutSeconds;
        public int voteThresholdPercent;
        public int voteTimeoutSeconds;

        private ConfigData() {
            this.maxHomes = 5;
            this.tpaTimeoutSeconds = 60;
            this.voteThresholdPercent = 70;
            this.voteTimeoutSeconds = 60;
        }
    }

    public static void initialize() {
        try {
            Files.createDirectories(CONFIG_PATH);

            if (Files.exists(CONFIG_FILE)) {
                try (FileReader reader = new FileReader(CONFIG_FILE.toFile())) {
                    config = GSON.fromJson(reader, ConfigData.class);
                    if (config == null) {
                        config = new ConfigData();
                    }
                }
            } else {
                config = new ConfigData();
                save();
            }
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("Failed to initialize mod config", e);
            config = new ConfigData();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("Failed to save mod config", e);
        }
    }

    public static ConfigData get() {
        if (config == null) {
            initialize();
        }
        return config;
    }
}