package com.chevvy.state;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaState {
    /**
     * Represents a teleport request.
     * @param originalRequester The player who initiated the /tpa or /tpahere command.
     * @param sourcePlayer The player who will be teleported.
     * @param destinationPlayer The player who is the destination of the teleport.
     */
    public record TpaRequest(UUID originalRequester, UUID sourcePlayer, UUID destinationPlayer) {}

    private static final Map<UUID, TpaRequest> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Creates a standard TPA request where the 'from' player wishes to teleport to the 'to' player.
     */
    public static void createTpaRequest(ServerPlayerEntity from, ServerPlayerEntity to) {
        TpaRequest request = new TpaRequest(from.getUuid(), from.getUuid(), to.getUuid());
        pendingRequests.put(to.getUuid(), request);
    }

    /**
     * Creates a "TPA Here" request where the 'requester' asks the 'target' to teleport to them.
     */
    public static void createTpaHereRequest(ServerPlayerEntity requester, ServerPlayerEntity target) {
        TpaRequest request = new TpaRequest(requester.getUuid(), target.getUuid(), requester.getUuid());
        pendingRequests.put(target.getUuid(), request);
    }

    public static TpaRequest getRequest(UUID targetUuid) {
        return pendingRequests.get(targetUuid);
    }

    public static void clearRequest(UUID targetUuid) {
        pendingRequests.remove(targetUuid);
    }
}