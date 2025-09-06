package com.example;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.BedBlock;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final List<BlockPos> SPAWN_OFFSETS = List.of(
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(1, 0, 1), new BlockPos(1, 0, -1),
            new BlockPos(-1, 0, 1), new BlockPos(-1, 0, -1)
    );

    @Override
    public void onInitialize() {
        ModState.initialize();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("sethome")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            sendBilingualMessage(player, "エラー：ホームの名前を指定する必要があります。", "Error: You must provide a name for your home.");
                        }
                        return 0;
                    })
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                Map<String, Home> playerHomes = ModState.getHomes(player.getUuid());
                                int maxHomes = ModConfig.get().maxHomes;
                                if (!playerHomes.containsKey(StringArgumentType.getString(context, "name")) && playerHomes.size() >= maxHomes) {
                                    sendBilingualMessage(player, "ホームの最大数（" + maxHomes + "）に達しました。", "You have reached the maximum number of homes (" + maxHomes + ").");
                                    return 0;
                                }
                                final String homeName = StringArgumentType.getString(context, "name");

                                Vec3d currentPos = player.getPos();
                                ServerWorld currentWorld = player.getWorld();
                                RegistryKey<World> currentDimension = currentWorld.getRegistryKey();

                                Home newHome = new Home(currentPos, currentDimension);
                                ModState.setHome(player.getUuid(), homeName, newHome);

                                sendBilingualMessage(player, "ホーム「" + homeName + "」が設定されました！", "Home '" + homeName + "' set!");
                                return 1;
                            }))
            );

            dispatcher.register(CommandManager.literal("home")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                final String homeName = StringArgumentType.getString(context, "name");
                                Home home = ModState.getHome(player.getUuid(), homeName);

                                if (home != null) {
                                    ServerWorld targetWorld = Objects.requireNonNull(player.getServer()).getWorld(home.dimension());
                                    Vec3d targetPos = home.pos();

                                    if (targetWorld == null) {
                                        sendBilingualMessage(player, "ホームが存在するワールドが見つかりません。", "The world for this home could not be found.");
                                        return 0;
                                    }

                                    player.teleport(targetWorld, targetPos.getX(), targetPos.getY(), targetPos.getZ(), Collections.emptySet(), player.getYaw(), player.getPitch(), false);
                                    sendBilingualMessage(player, "ホーム「" + homeName + "」にテレポートしました！", "Teleported to home '" + homeName + "'!");
                                } else {
                                    sendBilingualMessage(player, "ホーム「" + homeName + "」が見つかりませんでした！", "Home '" + homeName + "' not found!");
                                }
                                return 1;
                            }))
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;
                        if (player.getRespawn() == null) {
                            sendBilingualMessage(player, "ホームベッドが設定されていません。ベッドを使ってスポーンポイントを設定してください。", "You have no home bed set. Use a bed to set your spawn point.");
                            return 0;
                        }
                        BlockPos bedPos = player.getRespawn().pos();
                        ServerWorld world = Objects.requireNonNull(player.getServer()).getWorld(player.getRespawn().dimension());
                        if (bedPos == null || world == null) {
                            sendBilingualMessage(player, "ホームベッドが設定されていません。ベッドを使ってスポーンポイントを設定してください。", "You have no home bed set. Use a bed to set your spawn point.");
                            return 0;
                        }
                        BlockState stateAtSpawn = world.getBlockState(bedPos);
                        if (!stateAtSpawn.isIn(BlockTags.BEDS) && !(stateAtSpawn.getBlock() instanceof RespawnAnchorBlock)) {
                            sendBilingualMessage(player, "ホームベッドが破壊されたか、見つかりません。", "Your home bed was destroyed or is missing.");
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
                            sendBilingualMessage(player, "ホームベッドにテレポートしました！", "Teleported to your home bed!");
                            return 1;
                        } else {
                            sendBilingualMessage(player, "ホームベッドがふさがれているか、見つかりません。", "Your home bed is obstructed or missing.");
                            return 0;
                        }
                    })
            );

            dispatcher.register(CommandManager.literal("delhome")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;

                                final String homeName = StringArgumentType.getString(context, "name");
                                if (ModState.removeHome(player.getUuid(), homeName)) {
                                    sendBilingualMessage(player,
                                            "ホーム「" + homeName + "」を削除しました。",
                                            "Home '" + homeName + "' removed."
                                    );
                                } else {
                                    sendBilingualMessage(player,
                                            "ホーム「" + homeName + "」が見つかりませんでした。",
                                            "Home '" + homeName + "' not found."
                                    );
                                }
                                return 1;
                            }))
            );

            dispatcher.register(CommandManager.literal("homes")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;

                        Map<String, Home> playerHomes = ModState.getHomes(player.getUuid());
                        if (playerHomes.isEmpty()) {
                            sendBilingualMessage(player,
                                    "ホームが設定されていません。/sethome <name> を使用してください。",
                                    "You have no homes set. Use /sethome <name>."
                            );
                        } else {
                            String homeList = String.join(", ", playerHomes.keySet());
                            sendBilingualMessage(player,
                                    "ホーム一覧: " + homeList,
                                    "Your homes: " + homeList
                            );
                        }
                        return 1;
                    })
            );
        });

        LOGGER.info("Hello Fabric world! Homes are now persistent and cross-dimensional.");
    }

    private Optional<Vec3d> findSafeSpawnAround(BlockPos centerPos, ServerWorld world) {
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

    private void sendBilingualMessage(ServerPlayerEntity player, String japaneseMessage, String englishMessage) {
        if (player != null) {
            player.sendMessage(Text.literal(japaneseMessage), false);
            player.sendMessage(Text.literal(englishMessage), false);
        }
    }
}