package com.github.standobyte.jrpg.network.packets.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.EntityCombat;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

public class ClUseActionPacket {
    private final int entityId;
    private final int targetId;
    private final ResourceLocation actionId;
    private Consumer<PacketBuffer> writeExtraData = null;
    private PacketBuffer extraData = null;
    
    public ClUseActionPacket(int entityId, int targetId, CombatAction action) {
        this(entityId, targetId, action.actionTypeId);
        this.writeExtraData = action::clExtraInputData;
    }
    
    private ClUseActionPacket(int entityId, int targetId, ResourceLocation actionId) {
        this.entityId = entityId;
        this.targetId = targetId;
        this.actionId = actionId;
    }
    
    
    public static class Handler implements IModPacketHandler<ClUseActionPacket> {

        @Override
        public void encode(ClUseActionPacket msg, PacketBuffer buf) {
            buf.writeInt(msg.entityId);
            buf.writeInt(msg.targetId);
            buf.writeResourceLocation(msg.actionId);
            if (msg.writeExtraData != null) {
                msg.writeExtraData.accept(buf);
            }
        }

        @Override
        public ClUseActionPacket decode(PacketBuffer buf) {
            int entityId = buf.readInt();
            int targetId = buf.readInt();
            ResourceLocation actionId = buf.readResourceLocation();
            
            ClUseActionPacket packet = new ClUseActionPacket(entityId, targetId, actionId);
            packet.extraData = buf;
            return packet;
        }

        @Override
        public void handle(ClUseActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            if (msg.actionId == null) return;
            
            ServerPlayerEntity playerSender = ctx.get().getSender();
            Entity entity = playerSender.level.getEntity(msg.entityId);
            if (!(entity instanceof LivingEntity)) return;
            
            LivingEntity user = (LivingEntity) entity;
            EntityCombat.get(user).ifPresent(data -> {
                CombatAction action = data.getMoveset().getActionFromId(msg.actionId);
                if (action == null) return;
                
                Battle battle = data.getBattle();
                if (battle.getControllingPlayer(user).map(player -> player == playerSender).orElse(false)) {
                    Entity target = playerSender.level.getEntity(msg.targetId);
                    if (target instanceof LivingEntity) {
                        LivingEntity targetLiving = (LivingEntity) target;
                        if (battle.isInBattle(targetLiving)) {
                            action.setTarget(targetLiving);
                        }
                    }
                    
                    data.getBattle().setEntityAction(user, action, msg.extraData);
                }
            });
        }

        @Override
        public Class<ClUseActionPacket> getPacketClass() {
            return ClUseActionPacket.class;
        }
    }
}
