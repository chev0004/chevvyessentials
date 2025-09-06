package com.example;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public record Home(Vec3d pos, RegistryKey<World> dimension) {
}