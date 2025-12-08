package com.chevvy;

import com.chevvy.commands.homes.*;
import com.chevvy.commands.death.DeathCommand;
import com.chevvy.commands.teleport.TpaCommand;
import com.chevvy.commands.teleport.TpaHereCommand;
import com.chevvy.commands.vote.CvCommand;
import com.chevvy.config.ModConfig;
import com.chevvy.events.PlayerDeathHandler;
import com.chevvy.map.SharedMapExportCommand;
import com.chevvy.map.SharedMapManager;
import com.chevvy.map.SharedMapTicker;
import com.chevvy.state.DeathState;
import com.chevvy.state.HomeState;
import com.chevvy.util.TpaManager;
import com.chevvy.util.VoteManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChevvyEssentials implements ModInitializer {
    public static final String MOD_ID = "chevvyessentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.initialize();
        HomeState.initialize();
        TpaManager.initialize();
        DeathState.initialize();
        PlayerDeathHandler.register();
        VoteManager.initialize();
        SharedMapManager.initialize();
        SharedMapTicker.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LOGGER.info("Registering commands for " + MOD_ID);

            // Home Commands
            SetHomeCommand.register(dispatcher);
            HomeCommand.register(dispatcher);
            DelHomeCommand.register(dispatcher);
            HomesCommand.register(dispatcher);

            // Teleport Commands
            TpaCommand.register(dispatcher);
            TpaHereCommand.register(dispatcher);

            // Vote Commands
            CvCommand.register(dispatcher);

            // Misc Commands
            DeathCommand.register(dispatcher);

            // Map Commands
            SharedMapExportCommand.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> SharedMapManager.instance().save());

        LOGGER.info(MOD_ID + " has been initialized!");
    }
}