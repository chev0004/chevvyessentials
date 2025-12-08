package com.chevvy.map;

import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SharedMapExportCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sharedmap")
                .then(CommandManager.literal("export")
                        .then(CommandManager.argument("scale", IntegerArgumentType.integer(1, 32))
                                .executes(ctx -> {
                                    int scale = IntegerArgumentType.getInteger(ctx, "scale");
                                    SharedMapManager.instance().exportAsImage(scale);
                                    ctx.getSource().sendFeedback(() -> Text.literal("Exported shared-map.png (scale=" + scale + ")"), false);
                                    return 1;
                                }))
                        .executes(ctx -> {
                            SharedMapManager.instance().exportAsImage(4);
                            ctx.getSource().sendFeedback(() -> Text.literal("Exported shared-map.png (scale=4)"), false);
                            return 1;
                        })
                )
        );
    }
}
