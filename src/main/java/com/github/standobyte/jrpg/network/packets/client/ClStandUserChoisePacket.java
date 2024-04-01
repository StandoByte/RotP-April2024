package com.github.standobyte.jrpg.network.packets.client;

import java.util.function.Supplier;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.EntityCombat;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class ClStandUserChoisePacket {
    private final boolean spare;
    
    public ClStandUserChoisePacket(boolean spare) {
        this.spare = spare;
    }
    
    
    public static class Handler implements IModPacketHandler<ClStandUserChoisePacket> {

        @Override
        public void encode(ClStandUserChoisePacket msg, PacketBuffer buf) {
            buf.writeBoolean(msg.spare);
        }

        @Override
        public ClStandUserChoisePacket decode(PacketBuffer buf) {
            ClStandUserChoisePacket packet = new ClStandUserChoisePacket(buf.readBoolean());
            return packet;
        }

        @Override
        public void handle(ClStandUserChoisePacket msg, Supplier<NetworkEvent.Context> ctx) {
            LivingEntity user = ctx.get().getSender();
            EntityCombat.get(user).ifPresent(data -> {
                Battle battle = data.getBattle();
                battle.spareOrKill = msg.spare ? Battle.Choise.SPARE : Battle.Choise.KILL;
            });
        }

        @Override
        public Class<ClStandUserChoisePacket> getPacketClass() {
            return ClStandUserChoisePacket.class;
        }
    }
}
