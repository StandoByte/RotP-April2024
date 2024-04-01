package com.github.standobyte.jrpg.capability.entity;

import com.github.standobyte.jrpg.stats.EntityRPGData;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class RPGDataProvider implements ICapabilitySerializable<INBT>{
    @CapabilityInject(EntityRPGData.class)
    public static Capability<EntityRPGData> CAPABILITY = null;
    private LazyOptional<EntityRPGData> instance;
    
    public RPGDataProvider(LivingEntity entity) {
        this.instance = LazyOptional.of(() -> new EntityRPGData(entity));
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
