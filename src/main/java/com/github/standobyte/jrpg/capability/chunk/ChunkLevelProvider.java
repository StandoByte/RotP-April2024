package com.github.standobyte.jrpg.capability.chunk;

import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class ChunkLevelProvider implements ICapabilitySerializable<INBT>{
    @CapabilityInject(ChunkLevel.class)
    public static Capability<ChunkLevel> CAPABILITY = null;
    private LazyOptional<ChunkLevel> instance;
    
    public ChunkLevelProvider(Chunk chunk) {
        this.instance = LazyOptional.of(() -> new ChunkLevel(chunk));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return CAPABILITY.orEmpty(cap, instance);
    }

    @Override
    public INBT serializeNBT() {
        return CAPABILITY.getStorage().writeNBT(CAPABILITY, instance.orElseThrow(
                () -> new IllegalArgumentException("Capability is not attached.")), null);
    }

    @Override
    public void deserializeNBT(INBT nbt) {
        CAPABILITY.getStorage().readNBT(CAPABILITY, instance.orElseThrow(
                () -> new IllegalArgumentException("Capability is not attached.")), null, nbt);
    }

}
