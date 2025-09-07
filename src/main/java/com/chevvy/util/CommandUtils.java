package com.chevvy.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CommandUtils {
    public static void sendBilingual(ServerPlayerEntity player, String jp, String en) {
        if (player != null) {
            player.sendMessage(Text.literal(jp), false);
            player.sendMessage(Text.literal(en), false);
        }
    }
}
