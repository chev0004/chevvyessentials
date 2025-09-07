package com.chevvy.commands.homes;

import com.chevvy.Home;
import com.chevvy.state.HomeState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.*;

public class HomeCommand {
    private static final List<BlockPos> SPAWN_OFFSETS = List.of(
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(1, 0, 1), new BlockPos(1, 0, -1),
            new BlockPos(-1, 0, 1), new BlockPos(-1, 0, -1)
    );

    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        SuggestionProvider<ServerCommandSource> homeSuggestions = (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            Map<String, Home> homes = HomeState.getHomes(player.getUuid());
            return CommandSource.suggestMatching(homes.keySet(), builder);
        };

        dispatcher.register(CommandManager.literal("home")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) return 0;
                    if (player.getRespawn() == null) {
                        CommandUtils.sendBilingual(player,
                                Text.literal("ホームベッドが設定されていません。ベッドを使ってスポーンポイントを設定してください。").formatted(Formatting.GRAY),
                                Text.literal("You have no home bed set. Use a bed to set your spawn point.").formatted(Formatting.GRAY));
                        return 0;
                    }
                    BlockPos bedPos = player.getRespawn().pos();
                    ServerWorld world = Objects.requireNonNull(player.getServer()).getWorld(player.getRespawn().dimension());
                    if (bedPos == null || world == null) {
                        CommandUtils.sendBilingual(player,
                                Text.literal("ホームベッドが設定されていません。ベッドを使ってスポーンポイントを設定してください。").formatted(Formatting.GRAY),
                                Text.literal("You have no home bed set. Use a bed to set your spawn point.").formatted(Formatting.GRAY));
                        return 0;
                    }
                    BlockState stateAtSpawn = world.getBlockState(bedPos);
                    if (!stateAtSpawn.isIn(BlockTags.BEDS) && !(stateAtSpawn.getBlock() instanceof RespawnAnchorBlock)) {
                        CommandUtils.sendBilingual(player,
                                Text.literal("ホームベッドが破壊されたか、見つかりません。").formatted(Formatting.GRAY),
                                Text.literal("Your home bed was destroyed or is missing.").formatted(Formatting.GRAY));
                        player.setSpawnPoint(null, false);
                        return 0;
                    }
                    Optional<Vec3d> safePosition;
                    if (stateAtSpawn.getBlock() instanceof BedBlock) {
                        safePosition = BedBlock.findWakeUpPosition(player.getType(), world, bedPos, stateAtSpawn.get(BedBlock.FACING), player.getRespawn().angle());
                    } else {
                        safePosition = findSafeSpawnAround(bedPos, world);
                    }

                    if (safePosition.isPresent()) {
                        Vec3d pos = safePosition.get();
                        player.teleport(world, pos.getX(), pos.getY(), pos.getZ(), Collections.emptySet(), player.getYaw(), player.getPitch(), false);
                        CommandUtils.sendBilingual(player,
                                Text.literal("ホームベッドにテレポートしました！").formatted(Formatting.GRAY),
                                Text.literal("Teleported to your home bed!").formatted(Formatting.GRAY));
                        return 1;
                    } else {
                        CommandUtils.sendBilingual(player,
                                Text.literal("ホームベッドがふさがれているか、見つかりません。").formatted(Formatting.GRAY),
                                Text.literal("Your home bed is obstructed or missing.").formatted(Formatting.GRAY));
                        return 0;
                    }
                })
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(homeSuggestions)
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player == null) return 0;

                            String homeName = StringArgumentType.getString(context, "name");
                            Home home = HomeState.getHome(player.getUuid(), homeName);

                            if (home != null) {
                                ServerWorld world = Objects.requireNonNull(player.getServer()).getWorld(home.dimension());
                                Vec3d pos = home.pos();
                                if (world != null) {
                                    player.teleport(world, pos.getX(), pos.getY(), pos.getZ(),
                                            Collections.emptySet(), player.getYaw(), player.getPitch(), false);
                                    CommandUtils.sendBilingual(player,
                                            Text.empty().append(Text.literal("ホーム「").formatted(Formatting.GRAY))
                                                    .append(Text.literal(homeName).formatted(Formatting.GREEN))
                                                    .append(Text.literal("」にテレポートしました！").formatted(Formatting.GRAY)),
                                            Text.empty().append(Text.literal("Teleported to home '").formatted(Formatting.GRAY))
                                                    .append(Text.literal(homeName).formatted(Formatting.GREEN))
                                                    .append(Text.literal("'!").formatted(Formatting.GRAY))
                                    );
                                } else {
                                    CommandUtils.sendBilingual(player,
                                            Text.literal("ホームが存在するワールドが見つかりません。").formatted(Formatting.GRAY),
                                            Text.literal("The world for this home could not be found.").formatted(Formatting.GRAY));
                                }
                            } else {
                                CommandUtils.sendBilingual(player,
                                        Text.empty().append(Text.literal("ホーム「").formatted(Formatting.GRAY))
                                                .append(Text.literal(homeName).formatted(Formatting.GREEN))
                                                .append(Text.literal("」が見つかりませんでした！").formatted(Formatting.GRAY)),
                                        Text.empty().append(Text.literal("Home '").formatted(Formatting.GRAY))
                                                .append(Text.literal(homeName).formatted(Formatting.GREEN))
                                                .append(Text.literal("' not found!").formatted(Formatting.GRAY))
                                );
                            }
                            return 1;
                        })
                )
        );
    }

    private static Optional<Vec3d> findSafeSpawnAround(BlockPos centerPos, ServerWorld world) {
        for (BlockPos offset : SPAWN_OFFSETS) {
            BlockPos candidatePos = centerPos.add(offset);
            BlockPos floorPos = candidatePos.down();
            if (world.getBlockState(floorPos).isSideSolidFullSquare(world, floorPos, Direction.UP)) {
                VoxelShape bodyShape = world.getBlockState(candidatePos).getCollisionShape(world, candidatePos);
                VoxelShape headShape = world.getBlockState(candidatePos.up()).getCollisionShape(world, candidatePos.up());
                if (bodyShape.isEmpty() && headShape.isEmpty()) {
                    return Optional.of(Vec3d.ofBottomCenter(candidatePos));
                }
            }
        }
        return Optional.empty();
    }
}