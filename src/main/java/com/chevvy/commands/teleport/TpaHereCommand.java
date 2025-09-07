package com.chevvy.commands.teleport;

import com.chevvy.state.TpaState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Objects;

public class TpaHereCommand {
    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpa")
                .then(CommandManager.literal("here")
                        .then(CommandManager.argument("player", StringArgumentType.word())
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
                                                "自分自身をここに呼ぶことはできません。",
                                                "You can’t send a TPAHERE request to yourself.");
                                        return 0;
                                    }

                                    // reversed: requester wants target to come
                                    TpaState.createRequest(target, requester);

                                    CommandUtils.sendBilingual(requester,
                                            targetName + " にTPAHEREリクエストを送りました。",
                                            "Sent TPAHERE request to " + targetName);

                                    target.sendMessage(
                                            Text.literal(requester.getName().getString()
                                            + " があなたを呼んでいます。/tpa accept または /tpa deny を入力してください。"), false);

                                    return 1;
                                })
                        )
                )
        );
    }
}
