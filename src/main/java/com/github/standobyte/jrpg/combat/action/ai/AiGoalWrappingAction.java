package com.github.standobyte.jrpg.combat.action.ai;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.init.ActionType;
import com.github.standobyte.jrpg.init.ActionType.ActionFactory;
import com.github.standobyte.jrpg.util.TriFunction;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;

public abstract class AiGoalWrappingAction<T extends Goal> extends CombatAction {
    protected final MobEntity mob;
    protected PrioritizedGoal goalWrapper;
    protected T aiGoal;
    
    public AiGoalWrappingAction(ActionType<?> type, MobEntity entity, Battle battle) {
        super(type, entity, battle);
        this.mob = entity;
        findAiGoal(entity).ifPresent(goal -> {
            this.goalWrapper = goal.getLeft();
            this.aiGoal = goal.getRight();
        });
    }
    
    public abstract Optional<Pair<PrioritizedGoal, T>> findAiGoal(MobEntity mob);
    
    public static <G extends Goal> Optional<Pair<PrioritizedGoal, G>> goalMatching(MobEntity mob, Predicate<PrioritizedGoal> aiGoal) {
        Set<PrioritizedGoal> goals = mob.goalSelector.availableGoals;
        return goals.stream()
                .filter(aiGoal)
                .sorted(Comparator.comparingInt(PrioritizedGoal::getPriority))
                .findFirst()
                .map(goal -> Pair.of(goal, (G) goal.getGoal()));
    }
    
    @Override
    public void onActionUsed(PacketBuffer extraInput) {
        super.onActionUsed(extraInput);
        if (!entity.level.isClientSide() && target != null) {
            mob.setTarget(target);
            goalWrapper.start();
            battle.sendMessage(new TranslationTextComponent("action.jrpg.mob_melee.msg", entity.getDisplayName()));
        }
    }
    
    @Override
    public void tickPerform() {
        if (!entity.level.isClientSide()) {
            try {
                goalWrapper.tick();
                if (timer >= 150) {
                    setActionEnded();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                setActionEnded(0);
            }
        }
    }
    
    @Override
    public void setActionEnded(int delay) {
        if (this.afterPerformDelay < 0) {
            goalWrapper.stop();
        }
        super.setActionEnded(delay);
    }
    
    public T getGoal() {
        return aiGoal;
    }
    
    
    
    public static <A extends AiGoalWrappingAction<T>, T extends Goal> ActionFactory<A> constructorFor(
            TriFunction<ActionType<?>, MobEntity, Battle, A> constructor) {
        return (type, entity, battle) -> {
            if (entity instanceof MobEntity) {
                A action = constructor.apply(type, (MobEntity) entity, battle);
                return action;
            }

            return null;
        };
    }
}
