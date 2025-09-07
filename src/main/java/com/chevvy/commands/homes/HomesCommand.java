package com.chevvy.commands.homes;

import com.chevvy.state.ModState;
import com.chevvy.Home;
import com.chevvy.util.CommandUtils;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

public class HomesCommand {
    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("homes")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) return 0;

                    Map<String, Home> homes = ModState.getHomes(player.getUuid());
                    if (homes.isEmpty()) {
                        CommandUtils.sendBilingual(player,
                                "ホームが設定されていません。/sethome <name> を使用してください。",
                                "You have no homes set. Use /sethome <name>.");
                    } else {
                        String list = String.join(", ", homes.keySet());
                        CommandUtils.sendBilingual(player,
                                "ホーム一覧: " + list,
                                "Your homes: " + list);
                    }
                    return 1;
                })
        );
    }
}
