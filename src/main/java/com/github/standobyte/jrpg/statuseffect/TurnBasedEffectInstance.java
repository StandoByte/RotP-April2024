package com.github.standobyte.jrpg.statuseffect;

import com.github.standobyte.jrpg.util.reflection.CommonReflection;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;

public class TurnBasedEffectInstance {
    public final EffectInstance vanillaEffect;
    
    public TurnBasedEffectInstance(EffectInstance vanillaEffect) {
        this.vanillaEffect = vanillaEffect;
    }
    
    public static final int TICKS_IN_TURN = 100;
    public int getTurnsDuration() {
        return getDurationInTurns(vanillaEffect.getDuration());
    }
    
    public static int getDurationInTurns(int durationInTicks) {
        if (durationInTicks > 0) {
            return (durationInTicks - 1) / TICKS_IN_TURN + 1;
        }
        return 0;
    }
    
    public boolean decTurn(LivingEntity entity) {
        if (vanillaEffect.getDuration() > 0) {
//            if (this.effect.isDurationEffectTick(this.duration, this.amplifier)) {
//                this.applyEffect(pEntity);
//            }
            decTurnDuration(vanillaEffect);
            
            if (vanillaEffect.getDuration() <= 0 && vanillaEffect.hiddenEffect != null) {
                vanillaEffect.setDetailsFrom(vanillaEffect.hiddenEffect);
                vanillaEffect.hiddenEffect = vanillaEffect.hiddenEffect.hiddenEffect;
                CommonReflection.onEffectUpdated(entity, vanillaEffect, true);
             }
        }
        
        return vanillaEffect.getDuration() > 0;
    }

    private int decTurnDuration(EffectInstance effect) {
        if (effect.hiddenEffect != null) {
            decTurnDuration(effect.hiddenEffect);
        }
        
        effect.duration = Math.max(effect.getDuration() - TICKS_IN_TURN, 0);
        // FIXME (jrpg) decrease the duration on client
        return effect.getDuration();
    }
    
    
    public Effect getEffect() {
        return vanillaEffect.getEffect();
    }
    
}
