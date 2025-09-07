package com.chevvy.util;

import com.chevvy.config.ModConfig;
import com.chevvy.state.TpaState;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TpaManager {

    private static final AtomicInteger tickCounter = new AtomicInteger(0);

    public static void initialize() {
        // Register a server tick event listener
        ServerTickEvents.END_SERVER_TICK.register(TpaManager::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        // Only run the check once per second (20 ticks) to avoid performance issues
        if (tickCounter.incrementAndGet() >= 20) {
            tickCounter.set(0);
            checkExpiredRequests(server);
        }
    }

    private static void checkExpiredRequests(MinecraftServer server) {
        int timeoutSeconds = ModConfig.get().tpaTimeoutSeconds;
        long timeoutMillis = timeoutSeconds * 1000L;
        long currentTime = System.currentTimeMillis();

        // Use a copy of the keys to prevent ConcurrentModificationException
        for (UUID targetUuid : new ArrayList<>(TpaState.getPendingRequests().keySet())) {
            Map<UUID, TpaState.TpaRequest> playerRequests = TpaState.getRequestsForPlayer(targetUuid);
            if (playerRequests.isEmpty()) continue;

            for (UUID requesterUuid : new ArrayList<>(playerRequests.keySet())) {
                TpaState.TpaRequest request = playerRequests.get(requesterUuid);
                if (request == null) continue;

                if (currentTime - request.creationTime() > timeoutMillis) {
                    // The request has expired, clear it and notify players
                    TpaState.clearRequest(targetUuid, requesterUuid);

                    String requesterName = Objects.requireNonNull(server.getUserCache()).getByUuid(request.originalRequester())
                            .map(GameProfile::getName).orElse("Someone");
                    String targetName = server.getUserCache().getByUuid(targetUuid)
                            .map(GameProfile::getName).orElse("Someone");

                    ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);
                    ServerPlayerEntity originalRequester = server.getPlayerManager().getPlayer(request.originalRequester());

                    if (target != null) {
                        CommandUtils.sendBilingual(target,
                                requesterName + " からのTPAリクエストは" + timeoutSeconds + "秒後に期限切れになりました。",
                                "The TPA request from " + requesterName + " expired after " + timeoutSeconds + " seconds.");
                    }
                    if (originalRequester != null) {
                        CommandUtils.sendBilingual(originalRequester,
                                targetName + " へのTPAリクエストは" + timeoutSeconds + "秒後にタイムアウトしました。",
                                "Your TPA request to " + targetName + " timed out after " + timeoutSeconds + " seconds.");
                    }
                }
            }
        }
    }
}