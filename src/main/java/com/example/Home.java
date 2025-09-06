package com.example;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record Home(BlockPos pos, RegistryKey<World> dimension) {
}