package com.example;

import com.mojang.brigadier.arguments.StringArgumentType;
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
import java.util.Map;
import java.util.Objects;


public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.initialize();
        ModState.initialize();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("sethome")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            sendBilingualMessage(player, "エラー：ホームの名前を指定する必要があります。", "Error: You must provide a name for your home.");
                        }
                        return 0; // Return 0 to indicate failure
                    })
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;

                                Map<String, BlockPos> playerHomes = ModState.getHomes(player.getUuid());
                                int maxHomes = ModConfig.get().maxHomes;

                                // Check if player is at their home limit, but allow overwriting existing homes
                                if (!playerHomes.containsKey(StringArgumentType.getString(context, "name")) && playerHomes.size() >= maxHomes) {
                                    sendBilingualMessage(player,
                                            "ホームの最大数（" + maxHomes + "）に達しました。",
                                            "You have reached the maximum number of homes (" + maxHomes + ")."
                                    );
                                    return 0;
                                }

                                final String homeName = StringArgumentType.getString(context, "name");
                                ModState.setHome(player.getUuid(), homeName, player.getBlockPos());
                                sendBilingualMessage(player,
                                        "ホーム「" + homeName + "」が設定されました！",
                                        "Home '" + homeName + "' set!"
                                );
                                return 1;
                            }))
            );

            dispatcher.register(CommandManager.literal("home")
                    // This runs when a player types `/home <name>`
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;

                                final String homeName = StringArgumentType.getString(context, "name");
                                BlockPos home = ModState.getHome(player.getUuid(), homeName);

                                if (home != null) {
                                    ServerWorld world = Objects.requireNonNull(player.getServer()).getWorld(player.getWorld().getRegistryKey());
                                    if(world == null) return 0;

                                    BlockState homeBlockState = world.getBlockState(home);
                                    VoxelShape collisionShape = homeBlockState.getCollisionShape(world, home);
                                    double topY = home.getY();
                                    if (!collisionShape.isEmpty()) {
                                        double maxY = collisionShape.getMax(Direction.Axis.Y);
                                        if (!Double.isInfinite(maxY) && !Double.isNaN(maxY)) {
                                            topY += maxY;
                                        }
                                    }

                                    player.teleport(world, home.getX() + 0.5, topY, home.getZ() + 0.5,
                                            Collections.emptySet(), player.getYaw(), player.getPitch(), false);
                                    player.fallDistance = 0;
                                    player.sendAbilitiesUpdate();
                                    sendBilingualMessage(player,
                                            "ホーム「" + homeName + "」にテレポートしました！",
                                            "Teleported to home '" + homeName + "'!"
                                    );
                                } else {
                                    sendBilingualMessage(player,
                                            "ホーム「" + homeName + "」が見つかりませんでした！",
                                            "Home '" + homeName + "' not found!"
                                    );
                                }
                                return 1;
                            }))
                    // This runs when a player just types `/home`
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player == null) return 0;

                        // Get player's respawn info
                        ServerPlayerEntity.Respawn respawn = player.getRespawn();
                        if (respawn == null) {
                            sendBilingualMessage(player,
                                    "ホームベッドが設定されていません。ベッドを使ってスポーンポイントを設定してください。",
                                    "You have no home bed set. Use a bed to set your spawn point."
                            );
                            System.out.println(player.getRespawn());
                            return 0;
                        }

                        // Get dimension/world
                        ServerWorld world = Objects.requireNonNull(player.getServer()).getWorld(respawn.dimension());
                        if (world == null) {
                            sendBilingualMessage(player,
                                    "ホームベッドが無効なディメンションにあります。",
                                    "Your home bed is in an invalid dimension."
                            );
                            return 0;
                        }

                        // Bed position & safety check
                        BlockPos bedPos = respawn.pos();
                        if (bedPos == null) {
                            sendBilingualMessage(player,
                                    "ホームベッドが見つかりません。",
                                    "Your home bed is missing."
                            );
                            return 0;
                        }

//                        BlockPos below = bedPos.down();
//                        if (!world.getBlockState(below).isSolidBlock(world, below)
//                                || !world.isAir(bedPos)
//                                || !world.isAir(bedPos.up())) {
//                            player.sendMessage(Text.literal("Your home bed is obstructed or missing."), false);
//                            return 0;
//                        }

                        // Teleport
                        BlockState blockState = world.getBlockState(bedPos);
                        VoxelShape collisionShape = blockState.getCollisionShape(world, bedPos);

                        double safeY = bedPos.getY();
                        if (!collisionShape.isEmpty()) {
                            safeY += collisionShape.getMax(Direction.Axis.Y);
                        }

                        player.teleport(
                                world,
                                bedPos.getX() + 1,
                                safeY,
                                bedPos.getZ() + 0.5,
                                Collections.emptySet(),
                                player.getYaw(),
                                player.getPitch(),
                                false
                        );

                        player.fallDistance = 0;
                        player.sendAbilitiesUpdate();
                        sendBilingualMessage(player, "ホームベッドにテレポートしました！", "Teleported to your home bed!");
                        return 1;
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

                        Map<String, BlockPos> playerHomes = ModState.getHomes(player.getUuid());
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

        LOGGER.info("Hello Fabric world! Homes are now persistent.");
    }

    /**
     * Sends a two-line message to the player, with the first line in Japanese
     * and the second in English.
     *
     * @param player The player to send the message to.
     * @param japaneseMessage The message content in Japanese.
     * @param englishMessage The message content in English.
     */
    private void sendBilingualMessage(ServerPlayerEntity player, String japaneseMessage, String englishMessage) {
        if (player != null) {
            player.sendMessage(Text.literal(japaneseMessage), false);
            player.sendMessage(Text.literal(englishMessage), false);
        }
    }
}