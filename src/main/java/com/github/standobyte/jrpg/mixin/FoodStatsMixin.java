package com.github.standobyte.jrpg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.standobyte.jrpg.combat.EntityCombat;
import com.github.standobyte.jrpg.stats.EntityRPGData;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.FoodStats;

@Mixin(FoodStats.class)
public abstract class FoodStatsMixin {
    
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    protected void turnBasedNoFoodTick(PlayerEntity player, CallbackInfo ci) {
        if (EntityCombat.get(player).map(EntityCombat::isInBattle).orElse(false)) {
            ci.cancel();
        }
    }
    
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;heal(F)V"))
    protected void turnBasedSPTick(PlayerEntity player, CallbackInfo ci) {
        EntityRPGData.get(player).ifPresent(data -> data.setStaminaPoints(data.getStaminaPoints() + 4));
    }
    
}
