package com.github.standobyte.jrpg.ai;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.github.standobyte.jojo.init.ModItems;
import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.entity.BowmanEntity;
import com.github.standobyte.jrpg.init.ActionType;
import com.github.standobyte.jrpg.init.ModCombatActions;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;

public class UsingAIAction extends CombatAction {
    public MobEntity asMob;

    private LivingEntity mobTarget;
    
    private int chooseGoalTimer;
    public List<PrioritizedGoal> aiGoals;

    public UsingAIAction(ActionType<?> type, LivingEntity user, Battle battle) {
        super(type, user, battle);
    }
    
    @Override
    public void tickPerform() {
        if (timer > 100) {
            setActionEnded(0);
            return;
        }
        
        if (aiGoals == null && asMob != null) {
            if (chooseGoalTimer >= 60) {
                battle.setEntityAction(entity, ModCombatActions.SKIP.createAction(entity, battle), null);
                return;
            }
            
            if (mobTarget == null) {
                asMob.targetSelector.tick();
                mobTarget = asMob.getTarget();
                
                if (mobTarget != null) ModMain.tmpLog("{} targets {}", asMob, mobTarget);
            }
            
            if (mobTarget != null) {
                if (aiGoals == null) {
                    asMob.goalSelector.tick();
                    List<PrioritizedGoal> pickedGoal = asMob.goalSelector.getRunningGoals()
                            .collect(Collectors.toList());
                    if (!pickedGoal.isEmpty()) {
                        aiGoals = pickedGoal;
                    }
                    
                    if (aiGoals != null) {
                        for (PrioritizedGoal goal : aiGoals) {
                            ModMain.tmpLog("{}'s AI uses {}", entity, goal.getGoal().getClass().getSimpleName());
                            goal.start();
                            
                            
                            if (entity instanceof BowmanEntity) {
                                BowmanEntity bowman = (BowmanEntity) entity;
                                if (goal.getGoal() == bowman.bowGoal && mobTarget != null && mobTarget.getType() == EntityType.PLAYER) {
                                    ItemStack arrow = new ItemStack(ModItems.STAND_ARROW.get());
                                    entity.setItemSlot(EquipmentSlotType.OFFHAND, arrow);
                                    battle.sendMessage(new TranslationTextComponent("jrpg.bowman_arrow", bowman.getDisplayName()));
                                }
                                else {
                                    entity.setItemSlot(EquipmentSlotType.OFFHAND, ItemStack.EMPTY);
                                }
                            }
                        }
                    }
                }
            }
            
            if (mobTarget == null || aiGoals == null) {
                ++chooseGoalTimer;
            }
        }
        
        if (aiGoals != null) {
            if (aiGoals.isEmpty()) {
                setActionEnded();
                return;
            }
            
            Iterator<PrioritizedGoal> iter = aiGoals.iterator();
            while (iter.hasNext()) {
                PrioritizedGoal goal = iter.next();
                goal.tick();
                if (!goal.canContinueToUse() || !goal.isRunning()) {
                    iter.remove();
                }
            }
        }
    }

}
