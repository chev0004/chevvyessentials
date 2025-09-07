package com.chevvy.state;

import com.chevvy.ChevvyEssentials;
import com.chevvy.Home;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HomeState {

    private static final Map<UUID, Map<String, Home>> homes = new ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Home.class, (JsonSerializer<Home>) (home, type, ctx) -> {
                JsonObject obj = new JsonObject();
                JsonObject posObj = new JsonObject();
                posObj.addProperty("x", home.pos().getX());
                posObj.addProperty("y", home.pos().getY());
                posObj.addProperty("z", home.pos().getZ());
                obj.add("pos", posObj);
                obj.addProperty("dimension", home.dimension().getValue().toString());
                return obj;
            })
            .registerTypeAdapter(Home.class, (JsonDeserializer<Home>) (json, type, ctx) -> {
                JsonObject obj = json.getAsJsonObject();
                JsonObject posObj = obj.getAsJsonObject("pos");
                Vec3d pos = new Vec3d(
                        posObj.get("x").getAsDouble(),
                        posObj.get("y").getAsDouble(),
                        posObj.get("z").getAsDouble()
                );
                RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(obj.get("dimension").getAsString()));
                return new Home(pos, dimension);
            })
            .setPrettyPrinting()
            .create();

    private static final Type HOMES_TYPE =
            new TypeToken<HashMap<UUID, HashMap<String, Home>>>() {}.getType();

    private static Path stateFile;

    public static void initialize() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(ChevvyEssentials.MOD_ID);
        stateFile = configDir.resolve("homes.json");

        try {
            Files.createDirectories(configDir);
            if (Files.exists(stateFile)) {
                try (FileReader reader = new FileReader(stateFile.toFile())) {
                    Map<UUID, Map<String, Home>> loadedHomes = GSON.fromJson(reader, HOMES_TYPE);
                    if (loadedHomes != null) {
                        homes.clear();
                        loadedHomes.forEach((uuid, playerHomes) ->
                                homes.put(uuid, new ConcurrentHashMap<>(playerHomes)));
                    }
                }
                ChevvyEssentials.LOGGER.info("Homes loaded successfully.");
            }
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("Failed to load homes data", e);
        }

        ServerLifecycleEvents.SERVER_STOPPING.register(HomeState::onServerStopping);
    }

    private static void onServerStopping(MinecraftServer server) {
        save();
    }

    public static Home getHome(UUID playerUuid, String name) {
        return getHomes(playerUuid).get(name);
    }

    public static Map<String, Home> getHomes(UUID playerUuid) {
        return homes.getOrDefault(playerUuid, Collections.emptyMap());
    }

    public static void setHome(UUID playerUuid, String name, Home home) {
        homes.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>()).put(name, home);
        save();
    }

    public static boolean removeHome(UUID playerUuid, String name) {
        Map<String, Home> playerHomes = homes.get(playerUuid);
        if (playerHomes != null && playerHomes.remove(name) != null) {
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
            ChevvyEssentials.LOGGER.error("Failed to save homes data", e);
        }
    }
}