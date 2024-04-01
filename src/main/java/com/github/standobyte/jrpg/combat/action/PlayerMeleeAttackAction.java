package com.github.standobyte.jrpg.combat.action;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.init.ActionType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.ForgeMod;

public class PlayerMeleeAttackAction extends CombatAction {
    
    public PlayerMeleeAttackAction(ActionType<?> type, LivingEntity entity, Battle battle) {
        super(type, entity, battle);
        targeting = TargetingType.ENEMIES;
        isMelee = true;
    }
    
    @Override
    public void onActionUsed(PacketBuffer extraInput) {
        if (target != null && entity instanceof PlayerEntity) {
            double reachDistance = entity.getAttributeValue(ForgeMod.REACH_DISTANCE.get());
            Vector3d vec = entity.position().subtract(target.position());
            if (vec.lengthSqr() > reachDistance * reachDistance) {
                Vector3d pos = target.position().add(vec.normalize().scale(target.getBbWidth() + 1.0F));
                entity.teleportTo(pos.x, pos.y, pos.z);
            }
            PlayerEntity player = (PlayerEntity) entity;
            player.attack(target);
            if (!entity.level.isClientSide()) {
                setActionEnded(15);
            }
        }
    }

}
