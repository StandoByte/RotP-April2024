package com.github.standobyte.jrpg.mixin;

import java.util.Collections;
import java.util.Iterator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.github.standobyte.jrpg.combat.EntityCombat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.world.World;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    
    protected LivingEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }
    
    @ModifyVariable(method = "tickEffects", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    public Iterator<Effect> turnBasedDisableEffectsTick(Iterator<Effect> iterator) {
        boolean isInBattle = EntityCombat.get((LivingEntity) (Entity) this).map(EntityCombat::isInBattle).orElse(false);
        return isInBattle ? Collections.emptyIterator() : iterator;
    }
}
