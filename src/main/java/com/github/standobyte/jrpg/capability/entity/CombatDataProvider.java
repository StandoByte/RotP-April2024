package com.github.standobyte.jrpg.capability.entity;

import com.github.standobyte.jrpg.combat.EntityCombat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

public class CombatDataProvider implements ICapabilityProvider {
    @CapabilityInject(EntityCombat.class)
    public static Capability<EntityCombat> CAPABILITY = null;
    private LazyOptional<EntityCombat> instance;
    
    public CombatDataProvider(LivingEntity entity) {
        this.instance = LazyOptional.of(() -> new EntityCombat(entity));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return CAPABILITY.orEmpty(cap, instance);
    }
}
