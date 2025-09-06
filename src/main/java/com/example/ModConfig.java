package com.example;

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
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(ExampleMod.MOD_ID);
    private static final Path CONFIG_FILE = CONFIG_PATH.resolve("config.json");

    private static ConfigData config;

    // Defines the structure of your config file
    public static class ConfigData {
        public int maxHomes;

        // Set default values here
        private ConfigData() {
            this.maxHomes = 5;
        }
    }

    public static void initialize() {
        try {
            // Create the mod's config directory if it doesn't exist
            Files.createDirectories(CONFIG_PATH);

            if (Files.exists(CONFIG_FILE)) {
                try (FileReader reader = new FileReader(CONFIG_FILE.toFile())) {
                    config = GSON.fromJson(reader, ConfigData.class);
                    // In case a new config option was added and the file exists
                    if (config == null) {
                        config = new ConfigData();
                    }
                }
            } else {
                // Create a new config file with default values
                config = new ConfigData();
                save();
            }
        } catch (IOException e) {
            ExampleMod.LOGGER.error("Failed to initialize mod config", e);
            // Fallback to default config if loading fails
            config = new ConfigData();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            ExampleMod.LOGGER.error("Failed to save mod config", e);
        }
    }

    public static ConfigData get() {
        if (config == null) {
            // This should not happen if initialize() is called correctly, but it's a safe fallback
            initialize();
        }
        return config;
    }
}