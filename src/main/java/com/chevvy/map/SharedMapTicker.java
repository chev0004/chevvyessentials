package com.chevvy.map;

import com.chevvy.ChevvyEssentials;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class SharedMapTicker {
    private static int tickCounter = 0;
    // Save every 1200 ticks (~60s). Mark player chunk every 20 ticks (1s).
    private static final int SAVE_INTERVAL = 1200;
    private static final int MARK_INTERVAL = 20;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            tickCounter++;

            if (tickCounter % MARK_INTERVAL == 0) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (player == null) continue;
                    var world = player.getWorld();
                    var dimKey = world.getRegistryKey();
                    int chunkX = player.getChunkPos().x;
                    int chunkZ = player.getChunkPos().z;
                    SharedMapManager.instance().markChunk(dimKey, chunkX, chunkZ);
                }
            }

            // periodic save
            SharedMapManager.instance().maybeSavePeriodic(tickCounter, SAVE_INTERVAL);
        });
    }
}
