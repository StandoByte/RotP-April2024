package com.github.standobyte.jrpg.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.world.World;

@Mixin(TameableEntity.class)
public abstract class TameableEntityMixin extends AnimalEntity {
    
    protected TameableEntityMixin(EntityType<? extends AnimalEntity> type, World world) {
        super(type, world);
    }
    
    @Shadow
    public abstract UUID getOwnerUUID();
    @Shadow
    public abstract LivingEntity getOwner();
    
    @Inject(method = "setOrderedToSit", at = @At("TAIL"))
    public void turnBasedOnTamedSit(boolean orderedToSit, CallbackInfo ci) {
    }
}
