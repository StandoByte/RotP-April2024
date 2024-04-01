package com.github.standobyte.jrpg.combat.action.stand;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.init.ActionType;

import net.minecraft.entity.LivingEntity;

public class ResolveAction extends StandTurnBasedAction {

    public ResolveAction(ActionType<?> type, LivingEntity user, Battle battle) {
        super(type, user, battle);
    }

}
