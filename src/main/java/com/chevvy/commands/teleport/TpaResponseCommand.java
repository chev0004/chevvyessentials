package com.chevvy.commands.teleport;

import com.chevvy.state.TpaState;
import com.chevvy.util.CommandUtils;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

public class TpaResponseCommand {
    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpa")
                .then(CommandManager.literal("accept")
                        .executes(context -> {
                            ServerPlayerEntity target = context.getSource().getPlayer();
                            if (target == null) return 0;

                            UUID requesterId = TpaState.getRequester(target.getUuid());
                            if (requesterId == null) {
                                CommandUtils.sendBilingual(target,
                                        "保留中のTPAリクエストはありません。",
                                        "You have no pending TPA requests.");
                                return 0;
                            }

                            ServerPlayerEntity requester = Objects.requireNonNull(target.getServer()).getPlayerManager().getPlayer(requesterId);
                            if (requester == null) {
                                CommandUtils.sendBilingual(target,
                                        "リクエストしたプレイヤーが見つかりません。",
                                        "Requester not found.");
                                return 0;
                            }

                            requester.teleport(
                                    target.getWorld(),
                                    target.getX(), target.getY(), target.getZ(),
                                    Collections.emptySet(),
                                    requester.getYaw(), requester.getPitch(),
                                    false
                            );

                            CommandUtils.sendBilingual(requester,
                                    target.getName().getString() + " にテレポートしました！",
                                    "Teleported to " + target.getName().getString() + "!");
                            CommandUtils.sendBilingual(target,
                                    requester.getName().getString() + " がテレポートしてきました！",
                                    requester.getName().getString() + " has teleported to you!");

                            TpaState.clearRequest(target.getUuid());
                            return 1;
                        })
                )
                .then(CommandManager.literal("deny")
                        .executes(context -> {
                            ServerPlayerEntity target = context.getSource().getPlayer();
                            if (target == null) return 0;

                            UUID requesterId = TpaState.getRequester(target.getUuid());
                            if (requesterId == null) {
                                CommandUtils.sendBilingual(target,
                                        "保留中のTPAリクエストはありません。",
                                        "You have no pending TPA requests.");
                                return 0;
                            }

                            ServerPlayerEntity requester = Objects.requireNonNull(target.getServer()).getPlayerManager().getPlayer(requesterId);
                            if (requester != null) {
                                CommandUtils.sendBilingual(requester,
                                        target.getName().getString() + " がリクエストを拒否しました。",
                                        target.getName().getString() + " denied your request.");
                            }

                            CommandUtils.sendBilingual(target,
                                    "リクエストを拒否しました。",
                                    "Request denied.");

                            TpaState.clearRequest(target.getUuid());
                            return 1;
                        })
                )
        );
    }
}
