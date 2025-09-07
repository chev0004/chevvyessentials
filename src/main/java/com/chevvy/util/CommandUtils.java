package com.chevvy.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CommandUtils {
    public static void sendBilingual(ServerPlayerEntity player, Text jp, Text en) {
        if (player != null) {
            player.sendMessage(jp, false);
            player.sendMessage(en, false);
        }
    }
}