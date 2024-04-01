package com.github.standobyte.jrpg.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.entity.ai.attributes.Attributes;

public class BattleTurnQueue {
    private final Battle battle;
    List<LivingEntity> allEntities = new ArrayList<>();
    
    private Object2DoubleMap<LivingEntity> counter = new Object2DoubleArrayMap<>();
    private Object2IntMap<LivingEntity> entitiesHadTurn = new Object2IntArrayMap<>();
    private double avgSpeed;
    
    private LivingEntity lastTurnEntity;
    private int turnsInARow = 0;
    
    public int cyclesCount = 0;
    
    public BattleTurnQueue(Battle battle) {
        this.battle = battle;
    }
    
    public void addEntity(LivingEntity entity) {
        allEntities.add(entity);
    }
    
    public void giveInitiative(List<LivingEntity> attackingTeam) {
        double maxAttackSpeed = allEntities.stream()
                .mapToDouble(BattleTurnQueue::getAttackSpeed)
                .max().orElse(0);
        for (LivingEntity entity : attackingTeam) {
            counter.put(entity, maxAttackSpeed);
        }
    }
    
    public void onBattleStart() {
        avgSpeed = allEntities.stream()
                .mapToDouble(BattleTurnQueue::getAttackSpeed)
                .average()
                .orElse(0);
    }
    
    private void resetCycle() {
        counter.clear();
        entitiesHadTurn.clear();
        lastTurnEntity = null;
        avgSpeed = allEntities.stream()
                .mapToDouble(BattleTurnQueue::getAttackSpeed)
                .average()
                .orElse(0);
        turnsInARow = 0;
        ++cyclesCount;
    }
    
    public LivingEntity calcNewTurnEntity() {
        if (allEntities.stream().allMatch(entity -> !entity.isAlive() || entitiesHadTurn.getInt(entity) > 0)) {
            resetCycle();
        }
        
        for (LivingEntity entity : allEntities) {
            if (!entity.isAlive()) {
                counter.put(entity, 0);
            }
            else if (entity != lastTurnEntity) {
                counter.put(entity, counter.getDouble(entity) + getAttackSpeed(entity));
            }
        }
        
        List<LivingEntity> fastestToSlowest = counter.object2DoubleEntrySet().stream()
                .filter(entry -> entry.getKey().isAlive())
                .sorted(Comparator.comparingDouble(entry -> entry.getDoubleValue()))
                .map(Object2DoubleMap.Entry::getKey)
                .collect(Collectors.toCollection(LinkedList::new));
        Collections.reverse(fastestToSlowest);
        
        LivingEntity legal = null;
        for (LivingEntity entity : fastestToSlowest) {
            if (!entity.isAlive()) {
                continue;
            }
            
            int hadTurnsThisCycle = entitiesHadTurn.getInt(entity);
            if (hadTurnsThisCycle <= 0 || 
                    hadTurnsThisCycle < (int) (getAttackSpeed(entity) / avgSpeed)
                    && !(lastTurnEntity == entity && turnsInARow >= 2)) {
                legal = entity;
                break;
            }
        }
        
        if (lastTurnEntity == legal) {
            ++turnsInARow;
        }
        lastTurnEntity = legal;
        if (lastTurnEntity != null) {
            entitiesHadTurn.put(lastTurnEntity, entitiesHadTurn.getInt(lastTurnEntity) + 1);
        }
        return lastTurnEntity;
    }
    
    private static double getAttackSpeed(LivingEntity entity) {
        AttributeModifierManager attributes = entity.getAttributes();
        if (!attributes.hasAttribute(Attributes.ATTACK_SPEED)) {
            return 0;
        }
        return attributes.getValue(Attributes.ATTACK_SPEED);
    }
    
    
    
    void removeEntity(LivingEntity entity) {
        allEntities.remove(entity);
        counter.removeDouble(entity);
        entitiesHadTurn.removeInt(entity);
    }
    
}
