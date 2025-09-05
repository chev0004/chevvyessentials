package com.example;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "modid";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Map<UUID, BlockPos> homes = new HashMap<>();

	@Override
	public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("sethome")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) {
                            context.getSource().sendError(Text.literal("This command can only be run by a player."));
                            return 0;
                        }
                        homes.put(player.getUuid(), player.getBlockPos());
                        player.sendMessage(Text.literal("Home set!"), false);
                        return 1;
                    })
            );

            dispatcher.register(CommandManager.literal("home")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) {
                            context.getSource().sendError(Text.literal("This command can only be run by a player."));
                            return 0;
                        }

                        BlockPos home = homes.get(player.getUuid());
                        if (home != null) {
                            ServerWorld world = (ServerWorld) player.getWorld();

                            // Get the block state and its collision shape at the home position
                            BlockState homeBlockState = world.getBlockState(home);
                            VoxelShape collisionShape = homeBlockState.getCollisionShape(world, home);

                            // Calculate the precise Y coordinate for the top surface of the block.
                            // home.getY() is the bottom of the block space.
                            // collisionShape.getMax(Direction.Axis.Y) is the height of the collision box (e.g., 1.0 for a full block, 0.5 for a slab).
                            double topY = home.getY() + collisionShape.getMax(Direction.Axis.Y);

                            // Teleport the player to the calculated position
                            player.teleport(world, home.getX() + 0.5, topY, home.getZ() + 0.5, Collections.emptySet(), player.getYaw(), player.getPitch(), false);
                            player.sendMessage(Text.literal("Teleported home!"), false);

                        } else {
                            player.sendMessage(Text.literal("No home set! Use /sethome first."), false);
                        }
                        return 1;
                    })
            );

        });

        LOGGER.info("Hello Fabric world!");
	}
}