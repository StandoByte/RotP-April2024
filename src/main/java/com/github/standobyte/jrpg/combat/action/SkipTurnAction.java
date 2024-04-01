package com.github.standobyte.jrpg.combat.action;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.init.ActionType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;

public class SkipTurnAction extends CombatAction {
    
    public SkipTurnAction(ActionType<?> type, LivingEntity entity, Battle battle) {
        super(type, entity, battle);
    }
    
    @Override
    public void onActionUsed(PacketBuffer extraInput) {
        if (!entity.level.isClientSide()) {
            ModMain.tmpLog("{} skips the turn.", entity);
            battle.sendMessage(new TranslationTextComponent("action.jrpg.skip.msg", entity.getDisplayName()));
            _actionOverFlag = true;
        }
    }
}
