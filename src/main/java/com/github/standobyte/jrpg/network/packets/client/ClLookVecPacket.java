package com.github.standobyte.jrpg.network.packets.client;

import java.util.function.Supplier;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.EntityCombat;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;

import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;

public class ClLookVecPacket {
    private final int entityId;
    private final double x;
    private final double y;
    private final double z;
    
    public ClLookVecPacket(int entityId, double x, double y, double z) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    
    public static class Handler implements IModPacketHandler<ClLookVecPacket> {

        @Override
        public void encode(ClLookVecPacket msg, PacketBuffer buf) {
            buf.writeInt(msg.entityId);
            buf.writeDouble(msg.x);
            buf.writeDouble(msg.y);
            buf.writeDouble(msg.z);
        }

        @Override
        public ClLookVecPacket decode(PacketBuffer buf) {
            int entityId = buf.readInt();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            return new ClLookVecPacket(entityId, x, y, z);
        }

        @Override
        public void handle(ClLookVecPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayerEntity playerSender = ctx.get().getSender();
            Entity entity = playerSender.level.getEntity(msg.entityId);
            if (!(entity instanceof LivingEntity)) return;
            
            LivingEntity living = (LivingEntity) entity;
            EntityCombat.get(living).ifPresent(data -> {
                Battle battle = data.getBattle();
                if (battle.getControllingPlayer(living).map(player -> player == playerSender).orElse(false)) {
                    living.lookAt(EntityAnchorArgument.Type.EYES, 
                            new Vector3d(msg.x, msg.y, msg.z).add(EntityAnchorArgument.Type.EYES.apply(living)));
                }
            });
        }

        @Override
        public Class<ClLookVecPacket> getPacketClass() {
            return ClLookVecPacket.class;
        }
    }
}
