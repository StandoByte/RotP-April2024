package com.github.standobyte.jrpg.combat.action;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.init.ActionType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TranslationTextComponent;

public class BlockWithShieldAction extends CombatAction {
    private Hand shieldHand;
    private ItemStack shieldItem;
    
    public BlockWithShieldAction(ActionType<?> type, LivingEntity entity, Battle battle) {
        super(type, entity, battle);
    }
    
    @Override
    public void onActionUsed(PacketBuffer extraInput) {
        if (!entity.level.isClientSide()) {
            shieldHand = Hand.OFF_HAND;
            shieldItem = entity.getItemInHand(Hand.OFF_HAND);
            if (!(!shieldItem.isEmpty() && shieldItem.isShield(entity))) {
                shieldHand = Hand.MAIN_HAND;
                shieldItem = entity.getItemInHand(Hand.MAIN_HAND);
            }
            if (!shieldItem.isEmpty() && shieldItem.isShield(entity)) {
                entity.startUsingItem(shieldHand);
                battle.sendMessage(new TranslationTextComponent("action.jrpg.shield_block.msg", entity.getDisplayName()));
            }
            else {
                shieldHand = null;
                shieldItem = null;
            }
            setActionEnded(5);
        }
    }

}
