package com.github.standobyte.jrpg.combat;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.github.standobyte.jrpg.ai.MobCombatAI;
import com.github.standobyte.jrpg.capability.entity.CombatDataProvider;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.network.ModNetworkManager;
import com.github.standobyte.jrpg.network.packets.server.TurnBasedEffectsPacket;
import com.github.standobyte.jrpg.statuseffect.TurnBasedEffectInstance;
import com.google.common.collect.Maps;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.PotionEvent;

public class EntityCombat {
    private final LivingEntity entity;
    private Battle battle;
    private Moveset moveset;
    private MobCombatAI mobAI;
    
    public EntityCombat(LivingEntity entity) {
        this.entity = entity;
    }
    
    public static Optional<EntityCombat> get(LivingEntity entity) {
        return entity.getCapability(CombatDataProvider.CAPABILITY).resolve();
    }
    
    public void tick() {
        if (battle != null && !(entity instanceof SlimeEntity)) {
            entity.deathTime = Math.min(entity.deathTime, 18);
        }
    }
    
    
    @Nullable
    public final Battle getBattle() {
        return battle;
    }
    
    public void setBattle(Battle battle) {
        if (battle != null) {
            if (!entity.level.isClientSide()) {
                if (entity instanceof MobEntity) {
                    MobEntity mob = (MobEntity) entity;
                    mobAI = new MobCombatAI(mob, battle, this);
                    mobAI.initCombatAi();
                    mob.xxa = 0;
                    mob.zza = 0;
                    mob.yya = 0;
                    
                    if (mob instanceof CreeperEntity) {
                        CreeperEntity creeper = (CreeperEntity) mob;
                        creeper.setSwellDir(-1);
                    }
                }
                setupTurnBasedEffects();
            }
        }
        else {
            clearAllEffects();
            this.moveset = null;
        }
        this.battle = battle;
    }
    
    public final boolean isInBattle() {
        return battle != null;
    }
    
    public Moveset getMoveset() {
        return moveset;
    }
    
    
    
    public void mobEntityAiTick() {
        if (mobAI != null) {
            mobAI.mobAiTick(entity == battle.getCurrentTurnEntity());
        }
    }
    
    public void onPerformAttack() {
        if (mobAI != null) {
            CombatAction action = battle.getCurrentAction(entity);
            if (action != null) {
                action.setActionEnded();
            }
        }
    }
    
    
    
    public void onTurnStarted(boolean controlledByPlayer) {
        entity.stopUsingItem();
        if (entity instanceof MobEntity) {
            mobAI.onTurnStarted(controlledByPlayer);
        }
        this.moveset = Moveset.createNewTurnMoveset(entity, battle);
    }
    
    public void onTurnEnded() {
        statusEffectsTurnEnd();
        
        entity.xxa = 0;
        entity.yya = 0;
        entity.zza = 0;
    }
    
    
    
    private final Map<Effect, TurnBasedEffectInstance> turnBasedEffects = Maps.newHashMap();
    
    private void setupTurnBasedEffects() {
        turnBasedEffects.clear();
        entity.getActiveEffectsMap().forEach((effect, instance) -> {
            turnBasedEffects.put(effect, new TurnBasedEffectInstance(instance));
        });
        if (!entity.level.isClientSide()) {
            ModNetworkManager.sendToClientsTrackingAndSelf(TurnBasedEffectsPacket.addMupltile(entity.getId(), entity.getActiveEffects()), entity);
        }
    }
    
    public void addEffect(EffectInstance vanillaEffect) {
        TurnBasedEffectInstance turnBased = new TurnBasedEffectInstance(vanillaEffect);

        turnBasedEffects.put(vanillaEffect.getEffect().getEffect(), turnBased);
        if (!entity.level.isClientSide()) {
            ModNetworkManager.sendToClientsTrackingAndSelf(TurnBasedEffectsPacket.add(entity.getId(), vanillaEffect), entity);
        }
    }
    
    public void removeEffect(Effect effect) {
        turnBasedEffects.remove(effect);
        if (!entity.level.isClientSide()) {
            ModNetworkManager.sendToClientsTrackingAndSelf(TurnBasedEffectsPacket.remove(entity.getId(), effect), entity);
        }
    }
    
    private void clearAllEffects() {
        turnBasedEffects.clear();
    }
    
    private void statusEffectsTurnEnd() {
        Iterator<Effect> iterator = turnBasedEffects.keySet().iterator();

        try {
            while (iterator.hasNext()) {
                Effect effect = iterator.next();
                TurnBasedEffectInstance effectInstance = turnBasedEffects.get(effect);
                if (!effectInstance.decTurn(entity)) {
                    if (!entity.level.isClientSide && !MinecraftForge.EVENT_BUS.post(new PotionEvent.PotionExpiryEvent(entity, effectInstance.vanillaEffect))) {
                        iterator.remove();
                        entity.removeEffect(effectInstance.getEffect());
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
        }
    }
    
    public Collection<TurnBasedEffectInstance> getTurnBasedEffects() {
        return turnBasedEffects.values();
    }
    
    
}
