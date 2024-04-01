package com.github.standobyte.jrpg.capability.world;

import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class DimensionFirstChunkProvider implements ICapabilitySerializable<INBT>{
    @CapabilityInject(DimensionFirstChunk.class)
    public static Capability<DimensionFirstChunk> CAPABILITY = null;
    private LazyOptional<DimensionFirstChunk> instance;
    
    public DimensionFirstChunkProvider(World world) {
        this.instance = LazyOptional.of(() -> new DimensionFirstChunk(world));
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
