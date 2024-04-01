package com.github.standobyte.jrpg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.world.World;

@Mixin(SlimeEntity.class)
public abstract class SlimeEntityMixin extends MobEntity {
    
    protected SlimeEntityMixin(EntityType<? extends MobEntity> type, World world) {
        super(type, world);
    }
    
    @Inject(method = "isDealsDamage", at = @At("HEAD"), cancellable = true)
    public void turnBasedSmallSlimeAttack(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(isEffectiveAi());
    }
}
