package com.example;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModState {

    private static final Map<UUID, Map<String, BlockPos>> homes = new ConcurrentHashMap<>();

    // Custom Gson with BlockPos serializer/deserializer
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockPos.class, (JsonSerializer<BlockPos>) (pos, type, ctx) -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("x", pos.getX());
                obj.addProperty("y", pos.getY());
                obj.addProperty("z", pos.getZ());
                return obj;
            })
            .registerTypeAdapter(BlockPos.class, (JsonDeserializer<BlockPos>) (json, type, ctx) -> {
                JsonObject obj = json.getAsJsonObject();
                int x = obj.get("x").getAsInt();
                int y = obj.get("y").getAsInt();
                int z = obj.get("z").getAsInt();
                return new BlockPos(x, y, z);
            })
            .setPrettyPrinting()
            .create();

    private static final Type HOMES_TYPE =
            new TypeToken<HashMap<UUID, HashMap<String, BlockPos>>>() {}.getType();

    private static Path stateFile;

    public static void initialize() {
        // Get the path to config/modid/homes.json
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(ExampleMod.MOD_ID);
        stateFile = configDir.resolve("homes.json");

        try {
            // Ensure the directory exists
            Files.createDirectories(configDir);
            if (Files.exists(stateFile)) {
                try (FileReader reader = new FileReader(stateFile.toFile())) {
                    Map<UUID, Map<String, BlockPos>> loadedHomes = GSON.fromJson(reader, HOMES_TYPE);
                    if (loadedHomes != null) {
                        homes.clear();
                        loadedHomes.forEach((uuid, playerHomes) ->
                                homes.put(uuid, new ConcurrentHashMap<>(playerHomes)));
                    }
                }
                ExampleMod.LOGGER.info("Homes loaded successfully.");
            }
        } catch (IOException e) {
            ExampleMod.LOGGER.error("Failed to load homes data", e);
        }

        // Register event to save homes when the server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(ModState::onServerStopping);
    }

    private static void onServerStopping(MinecraftServer server) {
        save();
    }

    public static BlockPos getHome(UUID playerUuid, String name) {
        return getHomes(playerUuid).get(name);
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
            // If the player has no homes left, remove them from the map
            if (playerHomes.isEmpty()) {
                homes.remove(playerUuid);
            }
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
