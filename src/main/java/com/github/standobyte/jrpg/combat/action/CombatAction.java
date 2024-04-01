package com.github.standobyte.jrpg.combat.action;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.client.ClientStuff;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.init.ActionType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class CombatAction {
    public final ResourceLocation actionTypeId;
    public final ActionType<?> type;
    public final LivingEntity entity;
    public final Battle battle;
    public LivingEntity target;
    
    public double staminaCost = 0;
    
    protected int timer = 0;
    protected int afterPerformDelay = -1;
    public boolean _actionOverFlag = false;
    
    public TargetingType targeting = null;
    public boolean isMelee = false;
    
    private int cooldown;
    protected int cooldownStarting = 0;
    protected int spCost = 0;
    
    public CombatAction(ActionType<?> type, LivingEntity user, Battle battle) {
        this.actionTypeId = type.getId();
        this.entity = user;
        this.target = null;
        this.battle = battle;
        this.type = type;
    }
    
    public void setTarget(LivingEntity target) {
        this.target = target;
    }
    
    public void afterInit() {
        this.cooldown = cooldownStarting;
    }
    
    
    public final ResourceLocation getTypeId() {
        return actionTypeId;
    }
    
    public void clExtraInputData(PacketBuffer dataToSend) {}
    
    public boolean isFreeAim() {
        return targeting == TargetingType.FREE_AIM;
    }
    
    @Nonnull
    public List<LivingEntity> targetSelection() {
        List<LivingEntity> targets = new ArrayList<>();
        if (targeting != null) {
            switch (targeting) {
            case ENEMIES:
                targets.addAll(filterAlive(battle.getEnemies(entity).stream()));
                break;
            case ALLIES:
                targets.addAll(filterAlive(battle.getTeammates(entity).stream()));
            case ALL:
                targets.addAll(filterAlive(battle.getAllEntities()));
            default:
                break;
            }
        }
        
        if (isMelee && ClientStuff.movementRange != null) {
            targets = targets.stream().filter(entity -> ClientStuff.movementRange.isInRange(entity.position()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return targets;
    }
    
    protected static List<LivingEntity> filterAlive(Stream<LivingEntity> entities) {
        return entities.filter(LivingEntity::isAlive).collect(Collectors.toList());
    }
    
    public static enum TargetingType {
        ENEMIES,
        ALLIES,
        ALL,
        FREE_AIM
    }
    
    public void onActionUsed(PacketBuffer extraInput) {
        if (!entity.level.isClientSide()) {
            ModMain.tmpLog("{} used {}.", entity, getName().getString());
        }
    }
    
    public final void tickTimer() {
        ++timer;
        if (afterPerformDelay > -1 && --afterPerformDelay == -1) {
            _actionOverFlag = true;
        }
    }
    
    public boolean isAfterPerformDelay() {
        return afterPerformDelay > -1;
    }
    
    public void tickPerform() {
        setActionEnded();
    }
    
    public void setActionEnded() {
        setActionEnded(30);
    }
    
    public void setActionEnded(int delay) {
        if (this.afterPerformDelay < 0) {
            this.afterPerformDelay = delay;
        }
    }
    
    public boolean isActionOver() {
        return isAfterPerformDelay() || _actionOverFlag;
    }
    
    public ITextComponent getName() {
        return type.getName();
    }
}
