package com.github.standobyte.jrpg.network.packets.server;

import java.util.function.Supplier;

import com.github.standobyte.jrpg.client.ClientUtil;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;
import com.github.standobyte.jrpg.stats.EntityRPGData;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class StaminaPointsPacket {
    private final int entityId;
    private final double points;
    
    public StaminaPointsPacket(int entityId, double points) {
        this.entityId = entityId;
        this.points = points;
    }
    
    
    public static class Handler implements IModPacketHandler<StaminaPointsPacket> {

        @Override
        public void encode(StaminaPointsPacket msg, PacketBuffer buf) {
            buf.writeInt(msg.entityId);
            buf.writeDouble(msg.points);
        }

        @Override
        public StaminaPointsPacket decode(PacketBuffer buf) {
            int entityId = buf.readInt();
            double points = buf.readDouble();
            return new StaminaPointsPacket(entityId, points);
        }

        @Override
        public void handle(StaminaPointsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            Entity entity = ClientUtil.getEntityById(msg.entityId);
            if (entity instanceof LivingEntity) {
                EntityRPGData.get((LivingEntity) entity).ifPresent(data -> data.setStaminaPoints(msg.points));
            }
        }

        @Override
        public Class<StaminaPointsPacket> getPacketClass() {
            return StaminaPointsPacket.class;
        }
    }
}
