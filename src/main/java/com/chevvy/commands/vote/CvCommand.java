package com.chevvy.commands.vote;

import com.chevvy.config.ModConfig;
import com.chevvy.state.VoteState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.Objects;

public class CvCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("cv")
                .executes(context -> {
                    sendCvHelp(context.getSource().getPlayerOrThrow());
                    return 1;
                })
                .then(CommandManager.literal("d")
                        .executes(context -> handleVoteExecution(context.getSource(), VoteState.VoteType.DAY, "時間を昼にする", "change the time to day", "/cv d")))
                .then(CommandManager.literal("n")
                        .executes(context -> handleVoteExecution(context.getSource(), VoteState.VoteType.NIGHT, "時間を夜にする", "change the time to night", "/cv n")))
                .then(CommandManager.literal("c")
                        .executes(context -> handleVoteExecution(context.getSource(), VoteState.VoteType.CLEAR, "天候を晴れにする", "make the weather clear", "/cv c")))
                .then(CommandManager.literal("r")
                        .executes(context -> handleVoteExecution(context.getSource(), VoteState.VoteType.RAIN, "天候を雨にする", "change the weather to rain", "/cv r")))
        );
    }

    private static int handleVoteExecution(ServerCommandSource source, VoteState.VoteType type, String descriptionJp, String descriptionEn, String command) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        MinecraftServer server = source.getServer();

        if (VoteState.isVoteActive()) {
            VoteState.ActiveVote currentVote = VoteState.getCurrentVote();
            // If the command matches the current vote type, treat it as a 'yes' vote.
            if (currentVote.type() == type) {
                if (currentVote.votes().contains(player.getUuid())) {
                    CommandUtils.sendBilingual(player,
                            Text.literal("既にこの投票に投票済みです。").formatted(Formatting.GRAY),
                            Text.literal("You have already voted in this poll.").formatted(Formatting.GRAY));
                    return 0;
                }

                VoteState.addVote(player.getUuid());

                int onlinePlayers = server.getPlayerManager().getCurrentPlayerCount();
                int requiredPercent = ModConfig.get().voteThresholdPercent;
                int currentVotes = currentVote.votes().size();
                int currentPercent = (int) Math.floor((double) currentVotes / onlinePlayers * 100);

                Text jpAnnounce = Text.empty()
                        .append(Text.literal(player.getName().getString()).formatted(Formatting.AQUA))
                        .append(Text.literal("さんが").formatted(Formatting.GRAY))
                        .append(Text.literal(currentVote.descriptionJp()).formatted(Formatting.GREEN))
                        .append(Text.literal("に賛成票を投じました。現在の投票率: ").formatted(Formatting.GRAY))
                        .append(Text.literal(currentPercent + "%").formatted(Formatting.YELLOW))
                        .append(Text.literal(" (" + requiredPercent + "% 必要)").formatted(Formatting.GRAY));

                Text enAnnounce = Text.empty()
                        .append(Text.literal(player.getName().getString()).formatted(Formatting.AQUA))
                        .append(Text.literal(" voted to ").formatted(Formatting.GRAY))
                        .append(Text.literal(currentVote.descriptionEn()).formatted(Formatting.GREEN))
                        .append(Text.literal(". Vote is now at ").formatted(Formatting.GRAY))
                        .append(Text.literal(currentPercent + "%").formatted(Formatting.YELLOW))
                        .append(Text.literal(" (" + requiredPercent + "% required).").formatted(Formatting.GRAY));

                server.getPlayerManager().broadcast(jpAnnounce, false);
                server.getPlayerManager().broadcast(enAnnounce, false);

                checkVoteCompletion(server);

            } else { // A different vote is active
                CommandUtils.sendBilingual(player,
                        Text.literal("別の投票が進行中です: ").formatted(Formatting.GRAY)
                                .append(Text.literal(currentVote.descriptionJp()).formatted(Formatting.GREEN)),
                        Text.literal("Another vote is already in progress to ").formatted(Formatting.GRAY)
                                .append(Text.literal(currentVote.descriptionEn()).formatted(Formatting.GREEN))
                );
            }
        } else { // No vote is active, so start a new one.
            VoteState.startVote(type, player.getUuid(), descriptionJp, descriptionEn);

            Text jpMessage = Text.empty()
                    .append(Text.literal(player.getName().getString()).formatted(Formatting.AQUA))
                    .append(Text.literal("さんが").formatted(Formatting.GRAY))
                    .append(Text.literal(descriptionJp).formatted(Formatting.GREEN))
                    .append(Text.literal("ための投票を開始しました！ ").formatted(Formatting.GRAY))
                    .append(Text.literal(command).formatted(Formatting.YELLOW))
                    .append(Text.literal(" で投票してください。").formatted(Formatting.GRAY));

            Text enMessage = Text.empty()
                    .append(Text.literal(player.getName().getString()).formatted(Formatting.AQUA))
                    .append(Text.literal(" has started a vote to ").formatted(Formatting.GRAY))
                    .append(Text.literal(descriptionEn).formatted(Formatting.GREEN))
                    .append(Text.literal("! Vote with ").formatted(Formatting.GRAY))
                    .append(Text.literal(command).formatted(Formatting.YELLOW))
                    .append(Text.literal(".").formatted(Formatting.GRAY));

            server.getPlayerManager().broadcast(jpMessage, false);
            server.getPlayerManager().broadcast(enMessage, false);

            checkVoteCompletion(server);
        }
        return 1;
    }

    private static void checkVoteCompletion(MinecraftServer server) {
        if (!VoteState.isVoteActive()) return;

        int onlinePlayers = server.getPlayerManager().getCurrentPlayerCount();
        double threshold = ModConfig.get().voteThresholdPercent / 100.0;
        int requiredVotes = (int) Math.max(1, Math.ceil(onlinePlayers * threshold));

        VoteState.ActiveVote vote = VoteState.getCurrentVote();
        if (vote.votes().size() >= requiredVotes) {
            executeVoteAction(server, vote);
            VoteState.endVote();
        }
    }

    private static void executeVoteAction(MinecraftServer server, VoteState.ActiveVote vote) {
        Text jpMessage;
        Text enMessage;

        switch (vote.type()) {
            case DAY:
                Objects.requireNonNull(server.getWorld(World.OVERWORLD)).setTimeOfDay(1000); // Morning
                jpMessage = Text.literal("投票が可決されました！ 時間が昼になります。").formatted(Formatting.GRAY);
                enMessage = Text.literal("Vote passed! The time will now be day.").formatted(Formatting.GRAY);
                break;
            case NIGHT:
                Objects.requireNonNull(server.getWorld(World.OVERWORLD)).setTimeOfDay(13000); // Night
                jpMessage = Text.literal("投票が可決されました！ 時間が夜になります。").formatted(Formatting.GRAY);
                enMessage = Text.literal("Vote passed! The time will now be night.").formatted(Formatting.GRAY);
                break;
            case CLEAR:
                server.getOverworld().setWeather(120000, 0, false, false); // Clear for 100 minutes
                jpMessage = Text.literal("投票が可決されました！ 天候が晴れになります。").formatted(Formatting.GRAY);
                enMessage = Text.literal("Vote passed! The weather will now be clear.").formatted(Formatting.GRAY);
                break;
            case RAIN:
                server.getOverworld().setWeather(0, 6000, true, false); // Rain for 5 minutes
                jpMessage = Text.literal("投票が可決されました！ 天候が雨になります。").formatted(Formatting.GRAY);
                enMessage = Text.literal("Vote passed! The weather will now be rain.").formatted(Formatting.GRAY);
                break;
            default:
                return;
        }

        server.getPlayerManager().broadcast(jpMessage, false);
        server.getPlayerManager().broadcast(enMessage, false);
    }

    public static void sendCvHelp(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("--- ChevvyEssentials Vote Help ---").formatted(Formatting.GOLD));
        player.sendMessage(Text.literal("/cv d").formatted(Formatting.YELLOW));
        player.sendMessage(Text.literal("  時間を昼にする投票を開始/参加する").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("  Starts/joins a vote to change the time to day.").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("/cv n").formatted(Formatting.YELLOW));
        player.sendMessage(Text.literal("  時間を夜にする投票を開始/参加する").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("  Starts/joins a vote to change the time to night.").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("/cv c").formatted(Formatting.YELLOW));
        player.sendMessage(Text.literal("  天候を晴れにする投票を開始/参加する").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("  Starts/joins a vote to change the weather to clear.").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("/cv r").formatted(Formatting.YELLOW));
        player.sendMessage(Text.literal("  天候を雨にする投票を開始/参加する").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("  Starts/joins a vote to change the weather to rain.").formatted(Formatting.GRAY));
    }
}