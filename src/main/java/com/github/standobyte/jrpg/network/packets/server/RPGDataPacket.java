package com.github.standobyte.jrpg.network.packets.server;

import java.util.function.Supplier;

import com.github.standobyte.jrpg.client.ClientUtil;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;
import com.github.standobyte.jrpg.stats.EntityRPGData;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class RPGDataPacket {
    private final int entityId;
    private EntityRPGData srvData;
    private PacketBuffer clBuf = null;
    
    public RPGDataPacket(EntityRPGData srvData, int entityId) {
        this.srvData = srvData;
        this.entityId = entityId;
    }
    
    public RPGDataPacket(int entityId) {
        this.entityId = entityId;
    }
    
    
    public static class Handler implements IModPacketHandler<RPGDataPacket> {

        @Override
        public void encode(RPGDataPacket msg, PacketBuffer buf) {
            buf.writeInt(msg.entityId);
            msg.srvData.toBuf(buf);
        }

        @Override
        public RPGDataPacket decode(PacketBuffer buf) {
            int entityId = buf.readInt();
            RPGDataPacket packet = new RPGDataPacket(entityId);
            packet.clBuf = buf;
            return packet;
        }

        @Override
        public void handle(RPGDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
            Entity entity = ClientUtil.getEntityById(msg.entityId);
            if (entity instanceof LivingEntity) {
                EntityRPGData.get((LivingEntity) entity).ifPresent(data -> data.fromBuf(msg.clBuf));
            }
        }

        @Override
        public Class<RPGDataPacket> getPacketClass() {
            return RPGDataPacket.class;
        }
    }
}
