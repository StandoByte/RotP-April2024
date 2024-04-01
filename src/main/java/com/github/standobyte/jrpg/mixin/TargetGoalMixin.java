package com.github.standobyte.jrpg.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.EntityCombat;

import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;

@Mixin(TargetGoal.class)
public abstract class TargetGoalMixin extends Goal {
    @Shadow
    @Final
    protected MobEntity mob;
    
    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    protected void turnBasedAttackCheck(@Nullable LivingEntity potentialTarget, EntityPredicate targetPredicate, CallbackInfoReturnable<Boolean> ci) {
        if (potentialTarget != null) {
            Battle mobBattle = EntityCombat.get(mob).map(EntityCombat::getBattle).orElse(null);
            Battle targetBattle = EntityCombat.get(potentialTarget).map(EntityCombat::getBattle).orElse(null);
            if (mobBattle != targetBattle) {
                ci.setReturnValue(false);
            }
        }
    }
    
}
