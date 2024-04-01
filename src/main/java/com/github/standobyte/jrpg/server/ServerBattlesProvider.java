package com.github.standobyte.jrpg.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

public class ServerBattlesProvider implements ICapabilityProvider {
    @CapabilityInject(ServerStuff.class)
    public static Capability<ServerStuff> CAPABILITY = null;
    private LazyOptional<ServerStuff> instance;

    public ServerBattlesProvider(ServerWorld overworld) {
        this.instance = LazyOptional.of(() -> new ServerStuff(overworld));
    }
    
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return CAPABILITY.orEmpty(cap, instance);
    }
    
    public static ServerStuff getServerBattlesData(MinecraftServer server) {
        return server.overworld().getCapability(ServerBattlesProvider.CAPABILITY).orElseThrow(
                () -> new IllegalArgumentException("Capability is not attached."));
    }

}
