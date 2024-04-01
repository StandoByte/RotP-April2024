package com.github.standobyte.jrpg.potion;

import javax.annotation.Nullable;

import com.github.standobyte.jrpg.stats.EntityRPGData;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.InstantEffect;

public class InstantSPEffect extends InstantEffect {

    public InstantSPEffect(EffectType pCategory, int pColor) {
        super(pCategory, pColor);
    }
    
    @Override
    public void applyInstantenousEffect(@Nullable Entity pSource, @Nullable Entity pIndirectSource, 
            LivingEntity pLivingEntity, int pAmplifier, double pHealth) {
        EntityRPGData.get(pLivingEntity).ifPresent(data -> {
            data.setStaminaPoints(data.getStaminaPoints() + 25 * (1 << pAmplifier));
        });
    }
}
