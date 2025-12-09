package com.chevvy.commands.teleport;

import com.chevvy.state.TpaState;
import com.chevvy.util.CommandUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
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
            MinecraftServer server = context.getSource().getServer();
            for (UUID requesterUuid : requests.keySet()) {
                ServerPlayerEntity onlinePlayer = server.getPlayerManager().getPlayer(requesterUuid);
                if (onlinePlayer != null) {
                    requesters.add(onlinePlayer.getName().getString());
                }
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
                            ServerPlayerEntity target = context.getSource().getServer().getPlayerManager().getPlayer(targetName);
                            if (target == null) {
                                CommandUtils.sendBilingual(requester,
                                        Text.empty().append(Text.literal("プレイヤーが見つかりません: ").formatted(Formatting.GRAY)).append(Text.literal(targetName).formatted(Formatting.GREEN)),
                                        Text.empty().append(Text.literal("Player not found: ").formatted(Formatting.GRAY)).append(Text.literal(targetName).formatted(Formatting.GREEN)));
                                return 0;
                            }

                            if (target.getUuid().equals(requester.getUuid())) {
                                CommandUtils.sendBilingual(requester,
                                        Text.literal("自分自身にはテレポートできません。").formatted(Formatting.GRAY),
                                        Text.literal("You can’t send a TPA request to yourself.").formatted(Formatting.GRAY));
                                return 0;
                            }

                            TpaState.createTpaRequest(requester, target);
                            CommandUtils.sendBilingual(requester,
                                    Text.empty().append(Text.literal(targetName).formatted(Formatting.GREEN)).append(Text.literal("さんにTPAリクエストを送りました。").formatted(Formatting.GRAY)),
                                    Text.empty().append(Text.literal("Sent TPA request to ").formatted(Formatting.GRAY)).append(Text.literal(targetName).formatted(Formatting.GREEN)));

                            target.sendMessage(Text.empty().append(Text.literal(requester.getName().getString()).formatted(Formatting.GREEN))
                                    .append(Text.literal("さんがあなたにテレポートしようとしています。").formatted(Formatting.GRAY))
                                    .append(Text.literal("/tpa accept").formatted(Formatting.AQUA))
                                    .append(Text.literal(" または ").formatted(Formatting.GRAY))
                                    .append(Text.literal("/tpa deny").formatted(Formatting.AQUA))
                                    .append(Text.literal(" を入力してください。").formatted(Formatting.GRAY)));
                            target.sendMessage(Text.empty().append(Text.literal(requester.getName().getString()).formatted(Formatting.GREEN))
                                    .append(Text.literal(" wants to teleport to you. Type ").formatted(Formatting.GRAY))
                                    .append(Text.literal("/tpa accept").formatted(Formatting.AQUA))
                                    .append(Text.literal(" or ").formatted(Formatting.GRAY))
                                    .append(Text.literal("/tpa deny").formatted(Formatting.AQUA))
                                    .append(Text.literal(".").formatted(Formatting.GRAY)));

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
        player.sendMessage(Text.literal("  プレイヤーにテレポートリクエストを送信する。").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("  Sends a teleport request to a player.").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("/tpahere <player>").formatted(Formatting.AQUA));
        player.sendMessage(Text.literal("  プレイヤーにあなたの場所へのテレポートをリクエストする。").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("  Requests a player to teleport to your location.").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("/tpa accept [player]").formatted(Formatting.AQUA));
        player.sendMessage(Text.literal("  保留中のテレポートリクエストを承認する。").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("  Accepts a pending teleport request.").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("/tpa deny [player]").formatted(Formatting.AQUA));
        player.sendMessage(Text.literal("  保留中のテレポートリクエストを拒否する。").formatted(Formatting.GRAY));
        player.sendMessage(Text.literal("  Denies a pending teleport request.").formatted(Formatting.GRAY));
    }

    private static int executeResponse(ServerCommandSource source, String requesterName, boolean accept) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        MinecraftServer server = source.getServer();

        Map<UUID, TpaState.TpaRequest> requests = TpaState.getRequestsForPlayer(player.getUuid());

        if (requests.isEmpty()) {
            CommandUtils.sendBilingual(player, Text.literal("保留中のTPAリクエストはありません。").formatted(Formatting.GRAY), Text.literal("You have no pending TPA requests.").formatted(Formatting.GRAY));
            return 0;
        }

        TpaState.TpaRequest requestToProcess = null;

        if (requesterName != null) {
            ServerPlayerEntity onlinePlayer = server.getPlayerManager().getPlayer(requesterName);
            if (onlinePlayer != null) {
                UUID requesterUuid = onlinePlayer.getUuid();
                requestToProcess = requests.get(requesterUuid);
            }
            if (requestToProcess == null) {
                CommandUtils.sendBilingual(player,
                        Text.empty().append(Text.literal(requesterName).formatted(Formatting.GREEN)).append(Text.literal("さんからの保留中のリクエストはありません。").formatted(Formatting.GRAY)),
                        Text.empty().append(Text.literal("You have no pending request from ").formatted(Formatting.GRAY)).append(Text.literal(requesterName).formatted(Formatting.GREEN)).append(Text.literal(".").formatted(Formatting.GRAY))
                );
                return 0;
            }
        } else {
            if (requests.size() > 1) {
                String pendingRequesters = requests.keySet().stream()
                        .map(uuid -> {
                            ServerPlayerEntity onlinePlayer = server.getPlayerManager().getPlayer(uuid);
                            return onlinePlayer != null ? onlinePlayer.getName().getString() : null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", "));
                String command = accept ? "/tpa accept <player>" : "/tpa deny <player>";
                CommandUtils.sendBilingual(player,
                        Text.empty().append(Text.literal("複数のTPAリクエストがあります: ").formatted(Formatting.GRAY))
                                .append(Text.literal(pendingRequesters).formatted(Formatting.GREEN))
                                .append(Text.literal("。 ").formatted(Formatting.GRAY))
                                .append(Text.literal(command).formatted(Formatting.AQUA))
                                .append(Text.literal(" を使用して選択してください。").formatted(Formatting.GRAY)),
                        Text.empty().append(Text.literal("You have multiple TPA requests from: ").formatted(Formatting.GRAY))
                                .append(Text.literal(pendingRequesters).formatted(Formatting.GREEN))
                                .append(Text.literal(". Please use ").formatted(Formatting.GRAY))
                                .append(Text.literal(command).formatted(Formatting.AQUA))
                                .append(Text.literal(" to choose.").formatted(Formatting.GRAY))
                );
                return 0;
            }
            requestToProcess = requests.values().iterator().next();
        }

        if (accept) {
            return acceptRequest(server, player, requestToProcess);
        } else {
            return denyRequest(server, player, requestToProcess);
        }
    }

    private static int acceptRequest(MinecraftServer server, ServerPlayerEntity acceptor, TpaState.TpaRequest request) {
        ServerPlayerEntity source = server.getPlayerManager().getPlayer(request.sourcePlayer());
        ServerPlayerEntity destination = server.getPlayerManager().getPlayer(request.destinationPlayer());

        if (source == null || destination == null) {
            CommandUtils.sendBilingual(acceptor,
                    Text.literal("リクエストに関係するプレイヤーが見つかりません。").formatted(Formatting.GRAY),
                    Text.literal("A player involved in the request could not be found.").formatted(Formatting.GRAY));
        } else {
            source.teleport(destination.getEntityWorld(), destination.getX(), destination.getY(), destination.getZ(),
                    Collections.emptySet(), source.getYaw(), source.getPitch(), false);

            CommandUtils.sendBilingual(source,
                    Text.empty().append(Text.literal(destination.getName().getString()).formatted(Formatting.GREEN)).append(Text.literal("さんにテレポートしました！").formatted(Formatting.GRAY)),
                    Text.empty().append(Text.literal("Teleported to ").formatted(Formatting.GRAY)).append(Text.literal(destination.getName().getString()).formatted(Formatting.GREEN)).append(Text.literal("!").formatted(Formatting.GRAY)));
            CommandUtils.sendBilingual(destination,
                    Text.empty().append(Text.literal(source.getName().getString()).formatted(Formatting.GREEN)).append(Text.literal("さんがテレポートしてきました！").formatted(Formatting.GRAY)),
                    Text.empty().append(Text.literal(source.getName().getString()).formatted(Formatting.GREEN)).append(Text.literal(" has teleported to you!").formatted(Formatting.GRAY)));
        }

        TpaState.clearRequest(acceptor.getUuid(), request.originalRequester());
        return 1;
    }

    private static int denyRequest(MinecraftServer server, ServerPlayerEntity denier, TpaState.TpaRequest request) {
        ServerPlayerEntity originalRequester = server.getPlayerManager().getPlayer(request.originalRequester());
        String requesterName = "Someone";
        if (originalRequester != null) {
            requesterName = originalRequester.getName().getString();
        }

        if (originalRequester != null) {
            CommandUtils.sendBilingual(originalRequester,
                    Text.empty().append(Text.literal(denier.getName().getString()).formatted(Formatting.GREEN)).append(Text.literal("さんがリクエストを拒否しました。").formatted(Formatting.GRAY)),
                    Text.empty().append(Text.literal(denier.getName().getString()).formatted(Formatting.GREEN)).append(Text.literal(" denied your request.").formatted(Formatting.GRAY)));
        }
        CommandUtils.sendBilingual(denier,
                Text.empty().append(Text.literal(requesterName).formatted(Formatting.GREEN)).append(Text.literal("さんのリクエストを拒否しました。").formatted(Formatting.GRAY)),
                Text.empty().append(Text.literal("Request from ").formatted(Formatting.GRAY)).append(Text.literal(requesterName).formatted(Formatting.GREEN)).append(Text.literal(" denied.").formatted(Formatting.GRAY)));

        TpaState.clearRequest(denier.getUuid(), request.originalRequester());
        return 1;
    }
}