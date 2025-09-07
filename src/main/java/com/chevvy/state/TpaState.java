package com.chevvy.state;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaState {
    private static final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();

    public static void createRequest(ServerPlayerEntity from, ServerPlayerEntity to) {
        pendingRequests.put(to.getUuid(), from.getUuid());
    }

    public static UUID getRequester(UUID targetUuid) {
        return pendingRequests.get(targetUuid);
    }

    public static void clearRequest(UUID targetUuid) {
        pendingRequests.remove(targetUuid);
    }
}
