package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModState {

    private static final Map<UUID, Map<String, BlockPos>> homes = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type HOMES_TYPE = new TypeToken<HashMap<UUID, HashMap<String, BlockPos>>>() {}.getType();
    private static Path stateFile;

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(ModState::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(ModState::onServerStopping);
    }

    private static void onServerStarted(MinecraftServer server) {
        stateFile = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve(ExampleMod.MOD_ID + ".json");
        try {
            if (Files.exists(stateFile)) {
                FileReader reader = new FileReader(stateFile.toFile());
                Map<UUID, Map<String, BlockPos>> loadedHomes = GSON.fromJson(reader, HOMES_TYPE);
                if (loadedHomes != null) {
                    homes.clear();
                    // Ensure the inner map is also concurrent for thread safety
                    loadedHomes.forEach((uuid, playerHomes) -> homes.put(uuid, new ConcurrentHashMap<>(playerHomes)));
                }
                reader.close();
                ExampleMod.LOGGER.info("Homes loaded successfully.");
            }
        } catch (IOException e) {
            ExampleMod.LOGGER.error("Failed to load homes data", e);
        }
    }

    private static void onServerStopping(MinecraftServer server) {
        save();
    }

    public static BlockPos getHome(UUID playerUuid, String name) {
        Map<String, BlockPos> playerHomes = homes.get(playerUuid);
        if (playerHomes != null) {
            return playerHomes.get(name);
        }
        return null;
    }

    public static Map<String, BlockPos> getHomes(UUID playerUuid) {
        return homes.getOrDefault(playerUuid, Collections.emptyMap());
    }

    public static void setHome(UUID playerUuid, String name, BlockPos pos) {
        homes.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>()).put(name, pos);
        save();
    }

    public static boolean removeHome(UUID playerUuid, String name) {
        Map<String, BlockPos> playerHomes = homes.get(playerUuid);
        if (playerHomes != null && playerHomes.remove(name) != null) {
            save();
            return true;
        }
        return false;
    }

    public static void save() {
        if (stateFile == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(stateFile.toFile())) {
            GSON.toJson(homes, writer);
        } catch (IOException e) {
            ExampleMod.LOGGER.error("Failed to save homes data", e);
        }
    }
}