package com.chevvy.events;

import com.chevvy.DeathLocation;
import com.chevvy.state.DeathState;
import com.chevvy.util.CommandUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PlayerDeathHandler {
    public static void register() {
        // Register an event listener for when any living entity dies on the server.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // Check if the entity that died is a player.
            if (entity instanceof ServerPlayerEntity player) {
                // If it is, save their location.
                DeathLocation location = new DeathLocation(player.getPos(), player.getWorld().getRegistryKey());
                DeathState.setDeathLocation(player.getUuid(), location);

                // Notify the player that their death location has been saved.
                // This message will appear in their chat upon respawning.
                CommandUtils.sendBilingual(player,
                        Text.literal("死亡した場所が保存されました。").formatted(Formatting.GRAY)
                                .append(Text.literal("/death").formatted(Formatting.YELLOW))
                                .append(Text.literal(" で戻ることができます。").formatted(Formatting.GRAY)),
                        Text.literal("Your death location has been saved. Use ").formatted(Formatting.GRAY)
                                .append(Text.literal("/death").formatted(Formatting.YELLOW))
                                .append(Text.literal(" to return.").formatted(Formatting.GRAY))
                );
            }
        });
    }
}