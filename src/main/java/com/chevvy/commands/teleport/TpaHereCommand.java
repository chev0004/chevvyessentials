package com.chevvy.commands.teleport;

import com.chevvy.state.TpaState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TpaHereCommand {
    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        SuggestionProvider<ServerCommandSource> playerSuggestions = (context, builder) ->
                CommandSource.suggestMatching(context.getSource().getServer().getPlayerNames(), builder);

        dispatcher.register(CommandManager.literal("tpahere")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    TpaCommand.sendTpaHelp(player);
                    return 1;
                })
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(playerSuggestions)
                        .executes(TpaHereCommand::run)
                )
        );
    }

    private static int run(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity requester = context.getSource().getPlayer();
        if (requester == null) return 0;

        String targetName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity target = context.getSource().getServer().getPlayerManager().getPlayer(targetName);

        if (TpaCommand.handleTargetNotFound(requester, targetName, target)) return 0;

        assert target != null;
        if (target.getUuid().equals(requester.getUuid())) {
            CommandUtils.sendBilingual(requester,
                    Text.literal("自分自身をここに呼ぶことはできません。").formatted(Formatting.GRAY),
                    Text.literal("You can’t send a TPAHERE request to yourself.").formatted(Formatting.GRAY));
            return 0;
        }

        TpaState.createTpaHereRequest(requester, target);

        CommandUtils.sendBilingual(requester,
                Text.empty().append(Text.literal(targetName).formatted(Formatting.GREEN)).append(Text.literal("さんにTPAHEREリクエストを送りました。").formatted(Formatting.GRAY)),
                Text.empty().append(Text.literal("Sent TPAHERE request to ").formatted(Formatting.GRAY)).append(Text.literal(targetName).formatted(Formatting.GREEN)));

        target.sendMessage(Text.empty().append(Text.literal(requester.getName().getString()).formatted(Formatting.GREEN))
                .append(Text.literal("さんがあなたを呼んでいます。").formatted(Formatting.GRAY))
                .append(Text.literal("/tpa accept").formatted(Formatting.AQUA))
                .append(Text.literal(" または ").formatted(Formatting.GRAY))
                .append(Text.literal("/tpa deny").formatted(Formatting.AQUA))
                .append(Text.literal(" を入力してください。").formatted(Formatting.GRAY)));
        target.sendMessage(Text.empty().append(Text.literal(requester.getName().getString()).formatted(Formatting.GREEN))
                .append(Text.literal(" wants you to teleport to them. Type ").formatted(Formatting.GRAY))
                .append(Text.literal("/tpa accept").formatted(Formatting.AQUA))
                .append(Text.literal(" or ").formatted(Formatting.GRAY))
                .append(Text.literal("/tpa deny").formatted(Formatting.AQUA))
                .append(Text.literal(".").formatted(Formatting.GRAY)));

        return 1;
    }
}