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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModState {

    private static final Map<UUID, BlockPos> homes = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type HOMES_TYPE = new TypeToken<HashMap<UUID, BlockPos>>() {}.getType();
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
                Map<UUID, BlockPos> loadedHomes = GSON.fromJson(reader, HOMES_TYPE);
                if (loadedHomes != null) {
                    homes.clear();
                    homes.putAll(loadedHomes);
                }
                reader.close();
                ExampleMod.LOGGER.info("Homes loaded successfully for " + homes.size() + " players.");
            }
        } catch (IOException e) {
            ExampleMod.LOGGER.error("Failed to load homes data", e);
        }
    }

    private static void onServerStopping(MinecraftServer server) {
        save();
    }

    public static BlockPos getHome(UUID playerUuid) {
        return homes.get(playerUuid);
    }

    public static void setHome(UUID playerUuid, BlockPos pos) {
        homes.put(playerUuid, pos);
        save();
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