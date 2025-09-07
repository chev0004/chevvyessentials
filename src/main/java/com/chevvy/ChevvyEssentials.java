package com.chevvy;

import com.chevvy.commands.homes.*;
import com.chevvy.state.HomeState;
import com.chevvy.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChevvyEssentials implements ModInitializer {
    public static final String MOD_ID = "chevvyessentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.initialize();
        HomeState.initialize();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SetHomeCommand.register(dispatcher);
            HomeCommand.register(dispatcher);
            DelHomeCommand.register(dispatcher);
            HomesCommand.register(dispatcher);
        });

        LOGGER.info("ChevvyEssentials initialized with Home commands.");
    }
}
