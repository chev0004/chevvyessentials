package com.chevvy.commands.homes;

import com.chevvy.state.HomeState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class DelHomeCommand {
    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        SuggestionProvider<ServerCommandSource> homeSuggestions = (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            return CommandSource.suggestMatching(HomeState.getHomes(player.getUuid()).keySet(), builder);
        };

        dispatcher.register(CommandManager.literal("delhome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(homeSuggestions)
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player == null) return 0;

                            String homeName = StringArgumentType.getString(context, "name");
                            if (HomeState.removeHome(player.getUuid(), homeName)) {
                                CommandUtils.sendBilingual(player,
                                        "ホーム「" + homeName + "」を削除しました。",
                                        "Home '" + homeName + "' removed.");
                            } else {
                                CommandUtils.sendBilingual(player,
                                        "ホーム「" + homeName + "」が見つかりませんでした。",
                                        "Home '" + homeName + "' not found.");
                            }
                            return 1;
                        })
                )
        );
    }
}
