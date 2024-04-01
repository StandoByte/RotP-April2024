package com.github.standobyte.jrpg.combat.action.ai;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.init.ActionType;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;

public class MeleeAttackAction extends AiGoalWrappingAction<MeleeAttackGoal> {

    public MeleeAttackAction(ActionType<?> type, MobEntity entity, Battle battle) {
        super(type, entity, battle);
        targeting = TargetingType.ENEMIES;
        isMelee = true;
    }

    @Override
    public Optional<Pair<PrioritizedGoal, MeleeAttackGoal>> findAiGoal(MobEntity mob) {
        return goalMatching(mob, pg -> pg.getGoal() instanceof MeleeAttackGoal);
    }
    
}
