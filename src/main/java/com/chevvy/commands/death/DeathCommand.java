package com.chevvy.commands.death;

import com.chevvy.DeathLocation;
import com.chevvy.state.DeathState;
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

public class DeathCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("death")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) return 0;

                    DeathLocation deathLocation = DeathState.getDeathLocation(player.getUuid());

                    if (deathLocation != null) {
                        ServerWorld world = Objects.requireNonNull(context.getSource().getServer()).getWorld(deathLocation.dimension());
                        Vec3d pos = deathLocation.pos();

                        if (world != null) {
                            player.teleport(world, pos.getX(), pos.getY(), pos.getZ(),
                                    Collections.emptySet(), player.getYaw(), player.getPitch(), false);

                            CommandUtils.sendBilingual(player,
                                    Text.literal("最後の死亡場所に戻りました。").formatted(Formatting.GRAY),
                                    Text.literal("Teleported to your last death location.").formatted(Formatting.GRAY)
                            );
                        } else {
                            CommandUtils.sendBilingual(player,
                                    Text.literal("死亡したワールドが見つかりませんでした。").formatted(Formatting.GRAY),
                                    Text.literal("The world you died in could not be found.").formatted(Formatting.GRAY)
                            );
                        }
                    } else {
                        CommandUtils.sendBilingual(player,
                                Text.literal("保存されている死亡場所はありません。").formatted(Formatting.GRAY),
                                Text.literal("You have no saved death location.").formatted(Formatting.GRAY)
                        );
                    }
                    return 1;
                })
        );
    }
}