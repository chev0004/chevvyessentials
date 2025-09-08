package com.chevvy.util;

import com.chevvy.config.ModConfig;
import com.chevvy.state.VoteState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.atomic.AtomicInteger;

public class VoteManager {

    private static final AtomicInteger tickCounter = new AtomicInteger(0);

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(VoteManager::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        // Run once per second (20 ticks)
        if (tickCounter.incrementAndGet() >= 20) {
            tickCounter.set(0);
            checkExpiredVote(server);
        }
    }

    private static void checkExpiredVote(MinecraftServer server) {
        if (!VoteState.isVoteActive()) {
            return;
        }

        VoteState.ActiveVote vote = VoteState.getCurrentVote();
        long timeoutMillis = ModConfig.get().voteTimeoutSeconds * 1000L;

        if (System.currentTimeMillis() - vote.startTime() > timeoutMillis) {
            Text jpMessage = Text.literal("投票は時間切れで失敗しました。").formatted(Formatting.GRAY);
            Text enMessage = Text.literal("The vote failed to pass in time.").formatted(Formatting.GRAY);

            server.getPlayerManager().broadcast(jpMessage, false);
            server.getPlayerManager().broadcast(enMessage, false);

            VoteState.endVote();
        }
    }
}