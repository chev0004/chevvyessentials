package com.chevvy.commands.teleport;

import com.chevvy.BackLocation;
import com.chevvy.state.BackState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collections;

public class SpawnCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("spawn")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) return 0;

                    MinecraftServer server = context.getSource().getServer();
                    ServerWorld overworld = server.getWorld(World.OVERWORLD);
                    
                    if (overworld == null) {
                        CommandUtils.sendBilingual(player,
                                Text.literal("オーバーワールドが見つかりませんでした。").formatted(Formatting.GRAY),
                                Text.literal("Overworld could not be found.").formatted(Formatting.GRAY));
                        return 0;
                    }

                    // Save the current position before teleporting
                    Vec3d currentPos = new Vec3d(player.getX(), player.getY(), player.getZ());
                    BackLocation backLocation = new BackLocation(currentPos, player.getEntityWorld().getRegistryKey());
                    BackState.setBackLocation(player.getUuid(), backLocation);

                    // Get spawn position from world spawn point
                    var spawnPoint = overworld.getSpawnPoint();
                    BlockPos spawnBlockPos = spawnPoint.getPos();
                    Vec3d spawnPos = Vec3d.ofBottomCenter(spawnBlockPos);

                    // Teleport to spawn
                    player.teleport(overworld, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(),
                            Collections.emptySet(), player.getYaw(), player.getPitch(), false);

                    CommandUtils.sendBilingual(player,
                            Text.literal("ワールドスポーンにテレポートしました。").formatted(Formatting.GRAY),
                            Text.literal("Teleported to world spawn.").formatted(Formatting.GRAY));

                    return 1;
                })
        );
    }
}




