package com.chevvy;

import com.chevvy.config.ModConfig;
import com.chevvy.state.TpaState;
import com.chevvy.util.CommandUtils;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

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
        for (UUID targetUuid : TpaState.getPendingRequests().keySet()) {
            TpaState.TpaRequest request = TpaState.getRequest(targetUuid);

            if (request == null) {
                continue;
            }

            if (currentTime - request.creationTime() > timeoutMillis) {
                // Remove the request immediately to prevent race conditions
                TpaState.clearRequest(targetUuid);

                // Get player names from their profiles for the message
                String requesterName = Objects.requireNonNull(server.getUserCache()).getByUuid(request.originalRequester())
                        .map(GameProfile::getName).orElse("Someone");
                String targetName = server.getUserCache().getByUuid(targetUuid)
                        .map(GameProfile::getName).orElse("Someone");

                // Get the online player entities to send them a message
                ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);
                ServerPlayerEntity originalRequester = server.getPlayerManager().getPlayer(request.originalRequester());

                // Notify players if they are still online
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