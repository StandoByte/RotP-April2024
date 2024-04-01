package com.github.standobyte.jrpg.combat.action;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.init.ActionType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;

public class CreativeKillAction extends CombatAction {
    
    public CreativeKillAction(ActionType<?> type, LivingEntity entity, Battle battle) {
        super(type, entity, battle);
        targeting = TargetingType.ENEMIES;
    }
    
    @Override
    public void onActionUsed(PacketBuffer extraInput) {
        if (!entity.level.isClientSide() && target != null) {
            target.kill();
            ModMain.tmpLog("\"/kill\"'ed {}.", target);
            setActionEnded(15);
        }
    }

}
