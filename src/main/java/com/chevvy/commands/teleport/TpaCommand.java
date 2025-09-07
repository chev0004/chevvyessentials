package com.chevvy.commands.teleport;

import com.chevvy.state.TpaState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Objects;

public class TpaCommand {
    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        SuggestionProvider<ServerCommandSource> playerSuggestions = (context, builder) ->
                CommandSource.suggestMatching(context.getSource().getServer().getPlayerNames(), builder);

        dispatcher.register(CommandManager.literal("tpa")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(playerSuggestions)
                        .executes(context -> {
                            ServerPlayerEntity requester = context.getSource().getPlayer();
                            if (requester == null) return 0;

                            String targetName = StringArgumentType.getString(context, "player");
                            ServerPlayerEntity target = Objects.requireNonNull(requester.getServer()).getPlayerManager().getPlayer(targetName);
                            if (target == null) {
                                CommandUtils.sendBilingual(requester,
                                        "プレイヤーが見つかりません: " + targetName,
                                        "Player not found: " + targetName);
                                return 0;
                            }

                            if (target.getUuid().equals(requester.getUuid())) {
                                CommandUtils.sendBilingual(requester,
                                        "自分自身にはテレポートできません。",
                                        "You can’t send a TPA request to yourself.");
                                return 0;
                            }

                            TpaState.createTpaRequest(requester, target);
                            CommandUtils.sendBilingual(requester,
                                    targetName + " にTPAリクエストを送りました。",
                                    "Sent TPA request to " + targetName);

                            target.sendMessage(Text.literal(requester.getName().getString()
                                    + " があなたにテレポートしようとしています。/tpa accept または /tpa deny を入力してください。"), false);
                            target.sendMessage(Text.literal(requester.getName().getString()
                                    + " wants to teleport to you. Type /tpa accept or /tpa deny."), false);

                            return 1;
                        })
                )
        );
    }
}