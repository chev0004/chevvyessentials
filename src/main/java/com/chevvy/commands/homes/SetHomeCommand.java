package com.chevvy.commands.homes;

import com.chevvy.Home;
import com.chevvy.config.ModConfig;
import com.chevvy.state.HomeState;
import com.chevvy.util.CommandUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;

public class SetHomeCommand {
    public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sethome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player == null) return 0;

                            String homeName = StringArgumentType.getString(context, "name");
                            Map<String, Home> playerHomes = HomeState.getHomes(player.getUuid());

                            int maxHomes = ModConfig.get().maxHomes;
                            if (!playerHomes.containsKey(homeName) && playerHomes.size() >= maxHomes) {
                                CommandUtils.sendBilingual(player,
                                        Text.empty().append(Text.literal("ホームの最大数（").formatted(Formatting.GRAY))
                                                .append(Text.literal(String.valueOf(maxHomes)).formatted(Formatting.GREEN))
                                                .append(Text.literal("）に達しました。").formatted(Formatting.GRAY)),
                                        Text.empty().append(Text.literal("You have reached the maximum number of homes (").formatted(Formatting.GRAY))
                                                .append(Text.literal(String.valueOf(maxHomes)).formatted(Formatting.GREEN))
                                                .append(Text.literal(").").formatted(Formatting.GRAY))
                                );
                                return 0;
                            }

                            Vec3d pos = player.getPos();
                            ServerWorld world = player.getWorld();
                            RegistryKey<World> dimension = world.getRegistryKey();

                            Home newHome = new Home(pos, dimension);
                            HomeState.setHome(player.getUuid(), homeName, newHome);

                            CommandUtils.sendBilingual(player,
                                    Text.empty().append(Text.literal("ホーム「").formatted(Formatting.GRAY))
                                            .append(Text.literal(homeName).formatted(Formatting.GREEN))
                                            .append(Text.literal("」が設定されました！").formatted(Formatting.GRAY)),
                                    Text.empty().append(Text.literal("Home '").formatted(Formatting.GRAY))
                                            .append(Text.literal(homeName).formatted(Formatting.GREEN))
                                            .append(Text.literal("' set!").formatted(Formatting.GRAY))
                            );
                            return 1;
                        })
                )
        );
    }
}