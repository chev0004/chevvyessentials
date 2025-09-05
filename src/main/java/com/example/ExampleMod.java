package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModState.initialize();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("sethome")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) {
                            context.getSource().sendError(Text.literal("This command can only be run by a player."));
                            return 0;
                        }
                        ModState.setHome(player.getUuid(), player.getBlockPos());
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

                        BlockPos home = ModState.getHome(player.getUuid());
                        if (home != null) {
                            ServerWorld world = player.getWorld();

                            BlockState homeBlockState = world.getBlockState(home);
                            VoxelShape collisionShape = homeBlockState.getCollisionShape(world, home);
                            double topY = home.getY() + collisionShape.getMax(Direction.Axis.Y);

                            player.teleport(world, home.getX() + 0.5, topY, home.getZ() + 0.5, Collections.emptySet(), player.getYaw(), player.getPitch(), false);
                            player.sendMessage(Text.literal("Teleported home!"), false);

                        } else {
                            player.sendMessage(Text.literal("No home set! Use /sethome first."), false);
                        }
                        return 1;
                    })
            );
        });

        LOGGER.info("Hello Fabric world! Homes are now persistent.");
    }
}