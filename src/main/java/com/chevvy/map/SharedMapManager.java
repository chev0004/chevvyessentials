package com.chevvy.map;

import com.chevvy.ChevvyEssentials;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Simple manager storing explored chunk positions per-dimension.
 * Persists to JSON and can export a compact PNG where each chunk = square.
 */
public class SharedMapManager {
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(ChevvyEssentials.MOD_ID);
    private static final Path DATA_FILE = CONFIG_DIR.resolve("shared-map-chunks.json");
    private static final Path EXPORT_FILE = CONFIG_DIR.resolve("shared-map.png");

    // Map dimension -> set of encoded chunk longs (encoded as (x<<32) | (z & 0xffffffffL))
    private final Map<String, Set<Long>> explored = new HashMap<>();
    private boolean dirty = false;

    private static SharedMapManager INSTANCE;

    public static synchronized void initialize() {
        if (INSTANCE == null) {
            INSTANCE = new SharedMapManager();
            INSTANCE.load();
        }
    }

    public static SharedMapManager instance() {
        if (INSTANCE == null) initialize();
        return INSTANCE;
    }

    private SharedMapManager() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException ignored) {}
    }

    public synchronized void markChunk(RegistryKey<World> dimension, int chunkX, int chunkZ) {
        String dimId = dimension.getValue().toString();
        explored.computeIfAbsent(dimId, k -> new HashSet<>());
        long key = encode(chunkX, chunkZ);
        if (explored.get(dimId).add(key)) {
            dirty = true;
        }
    }

    public synchronized boolean hasChunk(RegistryKey<World> dimension, int chunkX, int chunkZ) {
        String dimId = dimension.getValue().toString();
        Set<Long> set = explored.get(dimId);
        if (set == null) return false;
        return set.contains(encode(chunkX, chunkZ));
    }

    private static long encode(int x, int z) {
        return (((long) x) << 32) | (z & 0xffffffffL);
    }

    private static int decodeX(long key) {
        return (int) (key >> 32);
    }

    private static int decodeZ(long key) {
        return (int) key;
    }

    // --- persistence ---
    public synchronized void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer w = new FileWriter(DATA_FILE.toFile())) {
                Type t = new TypeToken<Map<String, Set<Long>>>(){}.getType();
                GSON.toJson(explored, t, w);
            }
            dirty = false;
            ChevvyEssentials.LOGGER.info("SharedMapManager: saved " + DATA_FILE.toString());
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("SharedMapManager: failed to save", e);
        }
    }

    public synchronized void load() {
        if (!Files.exists(DATA_FILE)) return;
        try (Reader r = new FileReader(DATA_FILE.toFile())) {
            Type t = new TypeToken<Map<String, Set<Long>>>(){}.getType();
            Map<String, Set<Long>> loaded = GSON.fromJson(r, t);
            if (loaded != null) {
                explored.clear();
                explored.putAll(loaded);
            }
            ChevvyEssentials.LOGGER.info("SharedMapManager: loaded " + explored.values().stream().mapToInt(Set::size).sum() + " chunks.");
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("SharedMapManager: failed to load", e);
        }
    }

    public synchronized void maybeSavePeriodic(int tickCounter, int saveIntervalTicks) {
        if (!dirty) return;
        if (tickCounter % saveIntervalTicks == 0) {
            save();
        }
    }

    /**
     * Exports a simple PNG where each explored chunk is rendered as a filled square.
     * scale = pixels per chunk (e.g. 4 = each chunk is 4x4 pixels)
     */
    public synchronized void exportAsImage(int scale) {
        // For simplicity we only export OVERWORLD dimension if present, otherwise the first dimension present.
        if (explored.isEmpty()) {
            ChevvyEssentials.LOGGER.info("SharedMapManager: no chunks to export.");
            return;
        }
        String dim = explored.keySet().iterator().next();
        Set<Long> set = explored.get(dim);
        if (set == null || set.isEmpty()) {
            ChevvyEssentials.LOGGER.info("SharedMapManager: nothing to export for dim " + dim);
            return;
        }

        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (long k : set) {
            int cx = decodeX(k);
            int cz = decodeZ(k);
            if (cx < minX) minX = cx;
            if (cz < minZ) minZ = cz;
            if (cx > maxX) maxX = cx;
            if (cz > maxZ) maxZ = cz;
        }

        int widthChunks = maxX - minX + 1;
        int heightChunks = maxZ - minZ + 1;
        int imgW = widthChunks * scale;
        int imgH = heightChunks * scale;

        BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);

        // background
        int bg = 0xff2b2b2b; // dark grey
        for (int x = 0; x < imgW; x++) for (int y = 0; y < imgH; y++) img.setRGB(x, y, bg);

        int exploredColor = 0xff56a64f; // green-ish
        for (long k : set) {
            int cx = decodeX(k);
            int cz = decodeZ(k);
            int px = (cx - minX) * scale;
            int py = (cz - minZ) * scale;
            for (int x = px; x < px + scale; x++) {
                for (int y = py; y < py + scale; y++) {
                    if (x >= 0 && x < imgW && y >= 0 && y < imgH) img.setRGB(x, y, exploredColor);
                }
            }
        }

        try {
            Files.createDirectories(CONFIG_DIR);
            try (OutputStream os = new FileOutputStream(EXPORT_FILE.toFile())) {
                ImageIO.write(img, "png", os);
            }
            ChevvyEssentials.LOGGER.info("SharedMapManager: exported map to " + EXPORT_FILE.toString());
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("SharedMapManager: failed export", e);
        }
    }
}
