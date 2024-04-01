package com.github.standobyte.jrpg.combat.action;

import com.github.standobyte.jrpg.client.ClientUtil;
import com.github.standobyte.jrpg.client.battleui.BattleScreen;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.init.ActionType;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;

public class UseItemAction extends CombatAction {
    
    public UseItemAction(ActionType<?> type, LivingEntity entity, Battle battle) {
        super(type, entity, battle);
        targeting = TargetingType.ALLIES;
    }
    
    @Override
    public void clExtraInputData(PacketBuffer dataToSend) {
        dataToSend.writeInt(ClientUtil.getClientPlayer().getId());
        dataToSend.writeVarInt(BattleScreen.slotClicked);
    }
    
    public static boolean canUseItem(ItemStack item) {
        return !item.isEmpty() && (
                item.getItem() == Items.MILK_BUCKET || item.getItem() == Items.HONEY_BOTTLE || item.getItem() instanceof PotionItem);
    }
    
    @Override
    public void onActionUsed(PacketBuffer extraInput) {
        if (!entity.level.isClientSide() && target != null) {
            int playerId = extraInput.readInt();
            int slot = extraInput.readVarInt();
            
            Entity e = entity.level.getEntity(playerId);
            if (e instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) e;
                PlayerInventory inventory = player.inventory;
                if (slot >= 0 && slot < inventory.items.size()) {
                    ItemStack item = inventory.items.get(slot);
                    if (canUseItem(item)) {
                        if (item.getItem() == Items.MILK_BUCKET) {
                            entity.level.playSound(null, target.getX(), target.getEyeY(), target.getZ(), 
                                    item.getDrinkingSound(), entity.getSoundSource(), 0.5F, entity.level.random.nextFloat() * 0.1F + 0.9F);
                            target.curePotionEffects(item);
                        }
                        else if (item.getItem() == Items.HONEY_BOTTLE) {
                            entity.level.playSound(null, target.getX(), target.getEyeY(), target.getZ(), 
                                    item.getDrinkingSound(), entity.getSoundSource(), 0.5F, entity.level.random.nextFloat() * 0.1F + 0.9F);
                            target.removeEffect(Effects.POISON);
                        }
                        else if (item.getItem() instanceof ThrowablePotionItem) {
                            PotionEntity potionEntity = new PotionEntity(entity.level, player);
                            potionEntity.setItem(item);
                            potionEntity.moveTo(target.getX(), target.getY() + 3, target.getZ());
                            entity.level.addFreshEntity(potionEntity);
                        }
                        else if (item.getItem() instanceof PotionItem) {
                            entity.level.playSound(null, target.getX(), target.getEyeY(), target.getZ(), 
                                    item.getDrinkingSound(), entity.getSoundSource(), 0.5F, entity.level.random.nextFloat() * 0.1F + 0.9F);
                            
                            for (EffectInstance potionEffect : PotionUtils.getMobEffects(item)) {
                                if (potionEffect.getEffect().isInstantenous()) {
                                    potionEffect.getEffect().applyInstantenousEffect(target, target, target, potionEffect.getAmplifier(), 1.0D);
                                } else {
                                    target.addEffect(new EffectInstance(potionEffect));
                                }
                            }
                        }
                        
                        if (!player.abilities.instabuild) {
                            item.shrink(1);
                        }
                    }
                }
            }
            
            setActionEnded();
        }
    }
}
