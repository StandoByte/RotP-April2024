package com.github.standobyte.jrpg.combat.action.stand;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.init.ActionType;

import net.minecraft.entity.LivingEntity;

public class StandTurnBasedAction extends CombatAction {

    public StandTurnBasedAction(ActionType<?> type, LivingEntity user, Battle battle) {
        super(type, user, battle);
    }

}
