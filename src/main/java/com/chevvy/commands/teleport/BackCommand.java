package com.chevvy.commands.teleport;

import com.chevvy.BackLocation;
import com.chevvy.state.BackState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.Objects;

public class BackCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("back")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) return 0;

                    BackLocation backLocation = BackState.getBackLocation(player.getUuid());

                    if (backLocation != null) {
                        ServerWorld world = Objects.requireNonNull(context.getSource().getServer()).getWorld(backLocation.dimension());
                        Vec3d pos = backLocation.pos();

                        if (world != null) {
                            // Save the current position before teleporting (so /back can be chained)
                            saveBackLocation(player, world, pos);

                            CommandUtils.sendBilingual(player,
                                    Text.literal("前の場所に戻りました。").formatted(Formatting.GRAY),
                                    Text.literal("Teleported back to your previous location.").formatted(Formatting.GRAY));
                        } else {
                            CommandUtils.sendBilingual(player,
                                    Text.literal("前の場所が存在するワールドが見つかりませんでした。").formatted(Formatting.GRAY),
                                    Text.literal("The world for your previous location could not be found.").formatted(Formatting.GRAY));
                        }
                    } else {
                        CommandUtils.sendBilingual(player,
                                Text.literal("戻る場所がありません。").formatted(Formatting.GRAY),
                                Text.literal("You have no previous location to return to.").formatted(Formatting.GRAY));
                    }
                    return 1;
                })
        );
    }

    public static void saveBackLocation(ServerPlayerEntity player, ServerWorld world, Vec3d pos) {
        Vec3d currentPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        BackLocation newBackLocation = new BackLocation(currentPos, player.getEntityWorld().getRegistryKey());
        BackState.setBackLocation(player.getUuid(), newBackLocation);

        player.teleport(world, pos.getX(), pos.getY(), pos.getZ(),
                Collections.emptySet(), player.getYaw(), player.getPitch(), false);
    }
}




