package com.github.standobyte.jrpg.init;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.ai.UsingAIAction;
import com.github.standobyte.jrpg.combat.action.BlockWithShieldAction;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.combat.action.CreativeKillAction;
import com.github.standobyte.jrpg.combat.action.CreativeKillAllAction;
import com.github.standobyte.jrpg.combat.action.PlayerBowAction;
import com.github.standobyte.jrpg.combat.action.PlayerMeleeAttackAction;
import com.github.standobyte.jrpg.combat.action.SkipTurnAction;
import com.github.standobyte.jrpg.combat.action.UseItemAction;
import com.github.standobyte.jrpg.combat.action.ai.AiGoalWrappingAction;
import com.github.standobyte.jrpg.combat.action.ai.MeleeAttackAction;
import com.github.standobyte.jrpg.combat.action.stand.ResolveAction;

import net.minecraft.util.ResourceLocation;

public class ModCombatActions {
    
    public static void load() {}
    
    public static final ActionType<? extends CombatAction> SKIP = ActionType.registry().register(
            ActionType.fromConstructor(SkipTurnAction::new),
            new ResourceLocation(ModMain.RES_NAMESPACE, "skip"));
    
    public static final ActionType<? extends UsingAIAction> MOB_AI = ActionType.registry().register(
            ActionType.fromConstructor(UsingAIAction::new),
            new ResourceLocation(ModMain.RES_NAMESPACE, "mob_ai"));
    
    public static final ActionType<? extends CombatAction> CREATIVE_KILL = ActionType.registry().register(
            ActionType.fromConstructor(CreativeKillAction::new),
            new ResourceLocation(ModMain.RES_NAMESPACE, "slash_kill"));
    
    public static final ActionType<? extends CombatAction> CREATIVE_KILL_ALL = ActionType.registry().register(
            ActionType.fromConstructor(CreativeKillAllAction::new),
            new ResourceLocation(ModMain.RES_NAMESPACE, "slash_kill_all"));
    
    
    public static final ActionType<? extends CombatAction> PLAYER_MELEE_ATTACK = ActionType.registry().register(
            ActionType.fromConstructor(PlayerMeleeAttackAction::new),
            new ResourceLocation(ModMain.RES_NAMESPACE, "melee_attack"));
    
    public static final ActionType<? extends CombatAction> PLAYER_RANGED_ATTACK = ActionType.registry().register(
            ActionType.fromConstructor(PlayerBowAction::new),
            new ResourceLocation(ModMain.RES_NAMESPACE, "bow_attack"));
    
    public static final ActionType<? extends CombatAction> PLAYER_SHIELD_BLOCK = ActionType.registry().register(
            ActionType.fromConstructor(BlockWithShieldAction::new),
            new ResourceLocation(ModMain.RES_NAMESPACE, "shield_block"));
    
    
    public static final ActionType<MeleeAttackAction> MOB_MELEE_ATTACK = ActionType.registry().register(
            ActionType.fromConstructor(AiGoalWrappingAction.constructorFor(MeleeAttackAction::new)), 
            new ResourceLocation(ModMain.RES_NAMESPACE, "mob_melee"));
    
    public static final ActionType<? extends CombatAction> USE_ITEM = ActionType.registry().register(
            ActionType.fromConstructor(UseItemAction::new),
            new ResourceLocation(ModMain.RES_NAMESPACE, "item"));
    
    
    
    public static final ActionType<CombatAction> RESOLVE = ActionType.registry().register(
            ActionType.fromConstructor(ResolveAction::new), 
            new ResourceLocation(ModMain.RES_NAMESPACE, "stand_resolve"));
}
