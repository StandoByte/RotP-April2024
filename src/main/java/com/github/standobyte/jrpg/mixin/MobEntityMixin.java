package com.github.standobyte.jrpg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.standobyte.jrpg.combat.EntityCombat;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.world.World;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity {
    
    protected MobEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }
    
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    public void turnBasedBattleMobAi(CallbackInfo ci) {
        EntityCombat.get(this).ifPresent(mob -> {
            if (mob.isInBattle()) {
                ci.cancel();
                mob.getBattle().battleMobAiTick(this);
            }
        });
    }
}
