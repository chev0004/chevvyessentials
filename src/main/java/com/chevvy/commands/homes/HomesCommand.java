package com.chevvy.commands.homes;

import com.chevvy.state.HomeState;
import com.chevvy.Home;
import com.chevvy.util.CommandUtils;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Iterator;
import java.util.Map;

public class HomesCommand {
    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("homes")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) return 0;

                    Map<String, Home> homes = HomeState.getHomes(player.getUuid());
                    if (homes.isEmpty()) {
                        CommandUtils.sendBilingual(player,
                                Text.empty().append(Text.literal("ホームが設定されていません。").formatted(Formatting.GRAY))
                                        .append(Text.literal("/sethome <name>").formatted(Formatting.YELLOW))
                                        .append(Text.literal(" を使用してください。").formatted(Formatting.GRAY)),
                                Text.empty().append(Text.literal("You have no homes set. Use ").formatted(Formatting.GRAY))
                                        .append(Text.literal("/sethome <name>").formatted(Formatting.YELLOW))
                                        .append(Text.literal(".").formatted(Formatting.GRAY))
                        );
                    } else {
                        MutableText jpList = Text.literal("ホーム一覧: ").formatted(Formatting.GRAY);
                        MutableText enList = Text.literal("Your homes: ").formatted(Formatting.GRAY);

                        Iterator<String> iterator = homes.keySet().iterator();
                        while (iterator.hasNext()) {
                            String homeName = iterator.next();
                            jpList.append(Text.literal(homeName).formatted(Formatting.GREEN));
                            enList.append(Text.literal(homeName).formatted(Formatting.GREEN));
                            if (iterator.hasNext()) {
                                jpList.append(Text.literal(", ").formatted(Formatting.GRAY));
                                enList.append(Text.literal(", ").formatted(Formatting.GRAY));
                            }
                        }
                        CommandUtils.sendBilingual(player, jpList, enList);
                    }
                    return 1;
                })
        );
    }
}