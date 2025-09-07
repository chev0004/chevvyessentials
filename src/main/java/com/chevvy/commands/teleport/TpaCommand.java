package com.chevvy.commands.teleport;

import com.chevvy.state.TpaState;
import com.chevvy.util.CommandUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.stream.Collectors;

public class TpaCommand {
    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        SuggestionProvider<ServerCommandSource> playerSuggestions = (context, builder) ->
                CommandSource.suggestMatching(context.getSource().getServer().getPlayerNames(), builder);

        SuggestionProvider<ServerCommandSource> pendingRequestSuggestionProvider = (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            Map<UUID, TpaState.TpaRequest> requests = TpaState.getRequestsForPlayer(player.getUuid());
            List<String> requesters = new ArrayList<>();
            for (UUID requesterUuid : requests.keySet()) {
                Objects.requireNonNull(context.getSource().getServer().getUserCache()).getByUuid(requesterUuid)
                        .ifPresent(profile -> requesters.add(profile.getName()));
            }
            return CommandSource.suggestMatching(requesters, builder);
        };

        dispatcher.register(CommandManager.literal("tpa")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    sendTpaHelp(player);
                    return 1;
                })
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(playerSuggestions)
                        .executes(context -> {
                            ServerPlayerEntity requester = context.getSource().getPlayer();
                            if (requester == null) return 0;

                            String targetName = StringArgumentType.getString(context, "player");
                            ServerPlayerEntity target = Objects.requireNonNull(requester.getServer()).getPlayerManager().getPlayer(targetName);
                            if (target == null) {
                                CommandUtils.sendBilingual(requester, "プレイヤーが見つかりません: " + targetName, "Player not found: " + targetName);
                                return 0;
                            }

                            if (target.getUuid().equals(requester.getUuid())) {
                                CommandUtils.sendBilingual(requester, "自分自身にはテレポートできません。", "You can’t send a TPA request to yourself.");
                                return 0;
                            }

                            TpaState.createTpaRequest(requester, target);
                            CommandUtils.sendBilingual(requester, targetName + " にTPAリクエストを送りました。", "Sent TPA request to " + targetName);

                            target.sendMessage(Text.literal(requester.getName().getString() + " があなたにテレポートしようとしています。/tpa accept または /tpa deny を入力してください。"), false);
                            target.sendMessage(Text.literal(requester.getName().getString() + " wants to teleport to you. Type /tpa accept or /tpa deny."), false);

                            return 1;
                        }))
                .then(CommandManager.literal("accept")
                        .executes(context -> executeResponse(context.getSource(), null, true))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .suggests(pendingRequestSuggestionProvider)
                                .executes(context -> executeResponse(context.getSource(), StringArgumentType.getString(context, "player"), true))))
                .then(CommandManager.literal("deny")
                        .executes(context -> executeResponse(context.getSource(), null, false))
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .suggests(pendingRequestSuggestionProvider)
                                .executes(context -> executeResponse(context.getSource(), StringArgumentType.getString(context, "player"), false))))
        );
    }

    public static void sendTpaHelp(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("--- ChevvyEssentials TPA Help ---").formatted(Formatting.GOLD));
        player.sendMessage(Text.literal("/tpa <player>").formatted(Formatting.AQUA));
        player.sendMessage(Text.literal("  プレイヤーにテレポートリクエストを送信します。").formatted(Formatting.WHITE));
        player.sendMessage(Text.literal("  Sends a teleport request to a player.").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("/tpahere <player>").formatted(Formatting.AQUA));
        player.sendMessage(Text.literal("  プレイヤーにあなたの場所へのテレポートをリクエストします。").formatted(Formatting.WHITE));
        player.sendMessage(Text.literal("  Requests a player to teleport to your location.").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("/tpa accept [player]").formatted(Formatting.AQUA));
        player.sendMessage(Text.literal("  保留中のテレポートリクエストを承認します。").formatted(Formatting.WHITE));
        player.sendMessage(Text.literal("  Accepts a pending teleport request.").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("/tpa deny [player]").formatted(Formatting.AQUA));
        player.sendMessage(Text.literal("  保留中のテレポートリクエストを拒否します。").formatted(Formatting.WHITE));
        player.sendMessage(Text.literal("  Denies a pending teleport request.").formatted(Formatting.GRAY));
    }

    private static int executeResponse(ServerCommandSource source, String requesterName, boolean accept) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        Map<UUID, TpaState.TpaRequest> requests = TpaState.getRequestsForPlayer(player.getUuid());

        if (requests.isEmpty()) {
            CommandUtils.sendBilingual(player, "保留中のTPAリクエストはありません。", "You have no pending TPA requests.");
            return 0;
        }

        TpaState.TpaRequest requestToProcess = null;

        if (requesterName != null) {
            UUID requesterUuid = Objects.requireNonNull(source.getServer().getUserCache()).findByName(requesterName).map(GameProfile::getId).orElse(null);
            if (requesterUuid != null) {
                requestToProcess = requests.get(requesterUuid);
            }
            if (requestToProcess == null) {
                CommandUtils.sendBilingual(player, requesterName + " からの保留中のリクエストはありません。", "You have no pending request from " + requesterName + ".");
                return 0;
            }
        } else {
            if (requests.size() > 1) {
                String pendingRequesters = requests.keySet().stream()
                        .map(uuid -> Objects.requireNonNull(source.getServer().getUserCache()).getByUuid(uuid).map(GameProfile::getName).orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", "));
                String command = accept ? "/tpa accept <player>" : "/tpa deny <player>";
                CommandUtils.sendBilingual(player,
                        "複数のTPAリクエストがあります: " + pendingRequesters + "。 " + command + " を使用して選択してください。",
                        "You have multiple TPA requests from: " + pendingRequesters + ". Please use " + command + " to choose.");
                return 0;
            }
            requestToProcess = requests.values().iterator().next();
        }

        if (accept) {
            return acceptRequest(player, requestToProcess);
        } else {
            return denyRequest(player, requestToProcess);
        }
    }

    private static int acceptRequest(ServerPlayerEntity acceptor, TpaState.TpaRequest request) {
        ServerPlayerEntity source = Objects.requireNonNull(acceptor.getServer()).getPlayerManager().getPlayer(request.sourcePlayer());
        ServerPlayerEntity destination = Objects.requireNonNull(acceptor.getServer()).getPlayerManager().getPlayer(request.destinationPlayer());

        if (source == null || destination == null) {
            CommandUtils.sendBilingual(acceptor, "リクエストに関係するプレイヤーが見つかりません。", "A player involved in the request could not be found.");
        } else {
            source.teleport(destination.getWorld(), destination.getX(), destination.getY(), destination.getZ(),
                    Collections.emptySet(), source.getYaw(), source.getPitch(), false);
            CommandUtils.sendBilingual(source, destination.getName().getString() + " にテレポートしました！", "Teleported to " + destination.getName().getString() + "!");
            CommandUtils.sendBilingual(destination, source.getName().getString() + " がテレポートしてきました！", source.getName().getString() + " has teleported to you!");
        }

        TpaState.clearRequest(acceptor.getUuid(), request.originalRequester());
        return 1;
    }

    private static int denyRequest(ServerPlayerEntity denier, TpaState.TpaRequest request) {
        ServerPlayerEntity originalRequester = Objects.requireNonNull(denier.getServer()).getPlayerManager().getPlayer(request.originalRequester());
        if (originalRequester != null) {
            CommandUtils.sendBilingual(originalRequester, denier.getName().getString() + " がリクエストを拒否しました。", denier.getName().getString() + " denied your request.");
        }
        CommandUtils.sendBilingual(denier, "リクエストを拒否しました。", "Request denied.");

        TpaState.clearRequest(denier.getUuid(), request.originalRequester());
        return 1;
    }
}