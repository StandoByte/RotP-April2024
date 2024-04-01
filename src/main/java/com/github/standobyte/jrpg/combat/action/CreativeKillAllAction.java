package com.github.standobyte.jrpg.combat.action;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.init.ActionType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;

public class CreativeKillAllAction extends CombatAction {
    
    public CreativeKillAllAction(ActionType<?> type, LivingEntity entity, Battle battle) {
        super(type, entity, battle);
    }
    
    @Override
    public void onActionUsed(PacketBuffer extraInput) {
        if (!entity.level.isClientSide()) {
            battle.getEnemies(entity).forEach(enemy -> {
                if (enemy.isAlive()) {
                    enemy.kill();
                }
            });
            ModMain.tmpLog("\"/kill\"'ed all.");
            setActionEnded(15);
        }
    }

}
