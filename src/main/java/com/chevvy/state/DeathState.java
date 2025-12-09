package com.chevvy.state;

import com.chevvy.ChevvyEssentials;
import com.chevvy.DeathLocation;
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

public class DeathState {

    private static final Map<UUID, DeathLocation> deathLocations = new ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(DeathLocation.class, (JsonSerializer<DeathLocation>) (deathLocation, type, ctx) -> {
                JsonObject obj = new JsonObject();
                JsonObject posObj = new JsonObject();
                posObj.addProperty("x", deathLocation.pos().getX());
                posObj.addProperty("y", deathLocation.pos().getY());
                posObj.addProperty("z", deathLocation.pos().getZ());
                obj.add("pos", posObj);
                obj.addProperty("dimension", deathLocation.dimension().getValue().toString());
                return obj;
            })
            .registerTypeAdapter(DeathLocation.class, (JsonDeserializer<DeathLocation>) (json, type, ctx) -> {
                JsonObject obj = json.getAsJsonObject();
                JsonObject posObj = obj.getAsJsonObject("pos");
                Vec3d pos = new Vec3d(
                        posObj.get("x").getAsDouble(),
                        posObj.get("y").getAsDouble(),
                        posObj.get("z").getAsDouble()
                );
                RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(obj.get("dimension").getAsString()));
                return new DeathLocation(pos, dimension);
            })
            .setPrettyPrinting()
            .create();

    private static final Type DEATH_LOCATIONS_TYPE =
            new TypeToken<HashMap<UUID, DeathLocation>>() {}.getType();

    private static Path stateFile;

    public static void initialize() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(ChevvyEssentials.MOD_ID);
        stateFile = configDir.resolve("deaths.json");

        try {
            Files.createDirectories(configDir);
            if (Files.exists(stateFile)) {
                try (FileReader reader = new FileReader(stateFile.toFile())) {
                    Map<UUID, DeathLocation> loadedLocations = GSON.fromJson(reader, DEATH_LOCATIONS_TYPE);
                    if (loadedLocations != null) {
                        deathLocations.clear();
                        deathLocations.putAll(new ConcurrentHashMap<>(loadedLocations));
                    }
                }
                ChevvyEssentials.LOGGER.info("Death locations loaded successfully.");
            }
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("Failed to load death locations data", e);
        }

        ServerLifecycleEvents.SERVER_STOPPING.register(DeathState::onServerStopping);
    }

    private static void onServerStopping(MinecraftServer server) {
        save();
    }

    public static DeathLocation getDeathLocation(UUID playerUuid) {
        return deathLocations.get(playerUuid);
    }

    public static void setDeathLocation(UUID playerUuid, DeathLocation location) {
        deathLocations.put(playerUuid, location);
        save();
    }

    public static void save() {
        if (stateFile == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(stateFile.toFile())) {
            GSON.toJson(deathLocations, writer);
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("Failed to save death locations data", e);
        }
    }
}