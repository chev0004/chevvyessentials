package com.chevvy.commands.teleport;

import com.chevvy.state.TpaState;
import com.chevvy.util.CommandUtils;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collections;
import java.util.Objects;

public class TpaResponseCommand {
    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpa")
                .then(CommandManager.literal("accept")
                        .executes(context -> {
                            ServerPlayerEntity acceptor = context.getSource().getPlayer();
                            if (acceptor == null) return 0;

                            TpaState.TpaRequest request = TpaState.getRequest(acceptor.getUuid());
                            if (request == null) {
                                CommandUtils.sendBilingual(acceptor,
                                        "保留中のTPAリクエストはありません。",
                                        "You have no pending TPA requests.");
                                return 0;
                            }

                            ServerPlayerEntity source = Objects.requireNonNull(acceptor.getServer()).getPlayerManager().getPlayer(request.sourcePlayer());
                            ServerPlayerEntity destination = Objects.requireNonNull(acceptor.getServer()).getPlayerManager().getPlayer(request.destinationPlayer());

                            if (source == null || destination == null) {
                                CommandUtils.sendBilingual(acceptor,
                                        "リクエストに関係するプレイヤーが見つかりません。",
                                        "A player involved in the request could not be found.");
                                TpaState.clearRequest(acceptor.getUuid());
                                return 0;
                            }

                            source.teleport(
                                    destination.getWorld(),
                                    destination.getX(), destination.getY(), destination.getZ(),
                                    Collections.emptySet(),
                                    source.getYaw(), source.getPitch(),
                                    false
                            );

                            CommandUtils.sendBilingual(source,
                                    destination.getName().getString() + " にテレポートしました！",
                                    "Teleported to " + destination.getName().getString() + "!");
                            CommandUtils.sendBilingual(destination,
                                    source.getName().getString() + " がテレポートしてきました！",
                                    source.getName().getString() + " has teleported to you!");

                            TpaState.clearRequest(acceptor.getUuid());
                            return 1;
                        })
                )
                .then(CommandManager.literal("deny")
                        .executes(context -> {
                            ServerPlayerEntity denier = context.getSource().getPlayer();
                            if (denier == null) return 0;

                            TpaState.TpaRequest request = TpaState.getRequest(denier.getUuid());
                            if (request == null) {
                                CommandUtils.sendBilingual(denier,
                                        "保留中のTPAリクエストはありません。",
                                        "You have no pending TPA requests.");
                                return 0;
                            }

                            ServerPlayerEntity originalRequester = Objects.requireNonNull(denier.getServer()).getPlayerManager().getPlayer(request.originalRequester());
                            if (originalRequester != null) {
                                CommandUtils.sendBilingual(originalRequester,
                                        denier.getName().getString() + " がリクエストを拒否しました。",
                                        denier.getName().getString() + " denied your request.");
                            }

                            CommandUtils.sendBilingual(denier,
                                    "リクエストを拒否しました。",
                                    "Request denied.");

                            TpaState.clearRequest(denier.getUuid());
                            return 1;
                        })
                )
        );
    }
}