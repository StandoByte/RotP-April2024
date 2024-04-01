package com.github.standobyte.jrpg.ai;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.EntityCombat;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.entity.BowmanEntity;
import com.github.standobyte.jrpg.init.ModCombatActions;
import com.github.standobyte.jrpg.util.reflection.CommonReflection;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.server.ServerWorld;

public class MobCombatAI {
    private final MobEntity entity;
    private final Battle battle;
    private final EntityCombat entityCombatHandler;
    private boolean aiInitialized;
    private GoalSelector lookOnlyGoals;
    private boolean controlledByPlayer;
    
    public MobCombatAI(MobEntity entity, Battle battle, EntityCombat entityCombatHandler) {
        this.entity = entity;
        this.battle = battle;
        this.entityCombatHandler = entityCombatHandler;
    }
    
    public void initCombatAi() {
        if (aiInitialized) return;
        
        entity.xxa = 0;
        entity.yya = 0;
        entity.zza = 0;
        
        Set<PrioritizedGoal> goals = entity.goalSelector.availableGoals;
        Map<Boolean, List<PrioritizedGoal>> goalsByLookOnly = goals.stream().collect(Collectors.partitioningBy(goalPrioritized -> {
            Goal goal = goalPrioritized.getGoal();
            return goal instanceof LookAtGoal || goal instanceof LookRandomlyGoal;
        }));
        
        this.lookOnlyGoals = new GoalSelector(entity.level.getProfilerSupplier());
        Set<PrioritizedGoal> lookOnlyGoalsSet = this.lookOnlyGoals.availableGoals;
        for (PrioritizedGoal goal : goalsByLookOnly.get(true)) {
            lookOnlyGoalsSet.add(goal);
        }
        
        aiInitialized = true;
    }
    
    public void onTurnStarted(boolean controlledByPlayer) {
        this.controlledByPlayer = controlledByPlayer;
        
        if (entity instanceof CreeperEntity) {
            CreeperEntity creeper = (CreeperEntity) entity;
            if (creeper.getSwellDir() > 0) {
                if (!entity.level.isClientSide()) {
                    CommonReflection.explodeCreeper(creeper);
                }
                return;
            }
        }
        else if (entity instanceof BowmanEntity) {
            BowmanEntity bowman = (BowmanEntity) entity;
            if (bowman.disappearNextTurn) {
                if (!bowman.level.isClientSide()) {
                    ((ServerWorld) bowman.level).sendParticles(ParticleTypes.LARGE_SMOKE,
                            bowman.getX(), bowman.getY(), bowman.getZ(), 80, 
                            bowman.getBbWidth() / 8, bowman.getBbHeight() / 4, bowman.getBbWidth() / 8, 0);
                    bowman.remove();
                }
                return;
            }
        }
        
//        entity.goalSelector.getRunningGoals().forEach(Goal::stop);
        
        entity.playAmbientSound();
        
        this.controlledByPlayer = controlledByPlayer;
        if (!controlledByPlayer) {
            UsingAIAction aiAction = ModCombatActions.MOB_AI.createAction(entity, battle);
            aiAction.asMob = entity;
            battle.setEntityAction(entity, aiAction, null);
        }
    }

    public void mobAiTick(boolean thisEntityTurn) {
        if (!aiInitialized) return;
        
        if (thisEntityTurn) {
            entity.getSensing().tick();
            
            CombatAction curAction = battle.getCurrentAction(entity);
            if (curAction instanceof UsingAIAction && curAction.isActionOver()) {
                return;
            }
            
            this.lookOnlyGoals.tick();
            
            if (!controlledByPlayer || curAction != null) {
                entity.getNavigation().tick();
                entity.getMoveControl().tick();
                entity.getLookControl().tick();
                entity.getJumpControl().tick();
            }
        }
        else {
            entity.getSensing().tick();
            entity.targetSelector.tick();
            this.lookOnlyGoals.tick();
            entity.getLookControl().tick();
        }
    }
}
