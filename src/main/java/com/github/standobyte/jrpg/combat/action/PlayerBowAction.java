package com.github.standobyte.jrpg.combat.action;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.init.ActionType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;

public class PlayerBowAction extends CombatAction {
    
    public PlayerBowAction(ActionType<?> type, LivingEntity entity, Battle battle) {
        super(type, entity, battle);
    }
    
    @Override
    public void onActionUsed(PacketBuffer extraInput) {
    }
    
    @Override
    public void tickPerform() {
    }

}
