package com.chevvy.state;

import com.chevvy.BackLocation;
import com.chevvy.ChevvyEssentials;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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

public class BackState {

    private static final Map<UUID, BackLocation> backLocations = new ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BackLocation.class, (JsonSerializer<BackLocation>) (backLocation, type, ctx) -> {
                JsonObject obj = new JsonObject();
                JsonObject posObj = new JsonObject();
                posObj.addProperty("x", backLocation.pos().getX());
                posObj.addProperty("y", backLocation.pos().getY());
                posObj.addProperty("z", backLocation.pos().getZ());
                obj.add("pos", posObj);
                obj.addProperty("dimension", backLocation.dimension().getValue().toString());
                return obj;
            })
            .registerTypeAdapter(BackLocation.class, (JsonDeserializer<BackLocation>) (json, type, ctx) -> {
                JsonObject obj = json.getAsJsonObject();
                JsonObject posObj = obj.getAsJsonObject("pos");
                Vec3d pos = new Vec3d(
                        posObj.get("x").getAsDouble(),
                        posObj.get("y").getAsDouble(),
                        posObj.get("z").getAsDouble()
                );
                RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(obj.get("dimension").getAsString()));
                return new BackLocation(pos, dimension);
            })
            .setPrettyPrinting()
            .create();

    private static final Type BACK_LOCATIONS_TYPE =
            new TypeToken<HashMap<UUID, BackLocation>>() {}.getType();

    private static Path stateFile;

    public static void initialize() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(ChevvyEssentials.MOD_ID);
        stateFile = configDir.resolve("back.json");

        try {
            Files.createDirectories(configDir);
            if (Files.exists(stateFile)) {
                try (FileReader reader = new FileReader(stateFile.toFile())) {
                    Map<UUID, BackLocation> loadedLocations = GSON.fromJson(reader, BACK_LOCATIONS_TYPE);
                    if (loadedLocations != null) {
                        backLocations.clear();
                        backLocations.putAll(new ConcurrentHashMap<>(loadedLocations));
                    }
                }
                ChevvyEssentials.LOGGER.info("Back locations loaded successfully.");
            }
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("Failed to load back locations data", e);
        }

        ServerLifecycleEvents.SERVER_STOPPING.register(BackState::onServerStopping);
    }

    private static void onServerStopping(MinecraftServer server) {
        save();
    }

    public static BackLocation getBackLocation(UUID playerUuid) {
        return backLocations.get(playerUuid);
    }

    public static void setBackLocation(UUID playerUuid, BackLocation location) {
        backLocations.put(playerUuid, location);
        save();
    }

    public static void save() {
        if (stateFile == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(stateFile.toFile())) {
            GSON.toJson(backLocations, writer);
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("Failed to save back locations data", e);
        }
    }
}




