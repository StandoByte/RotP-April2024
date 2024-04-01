package com.github.standobyte.jrpg.network.packets.server;

import java.util.function.Supplier;

import com.github.standobyte.jrpg.client.ClientStuff;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class BattleEndPacket {
    private final int battleId;
    
    public BattleEndPacket(int battleId) {
        this.battleId = battleId;
    }
    
    
    public static class Handler implements IModPacketHandler<BattleEndPacket> {

        @Override
        public void encode(BattleEndPacket msg, PacketBuffer buf) {
            buf.writeInt(msg.battleId);
        }

        @Override
        public BattleEndPacket decode(PacketBuffer buf) {
            return new BattleEndPacket(buf.readInt());
        }

        @Override
        public void handle(BattleEndPacket msg, Supplier<NetworkEvent.Context> ctx) {
            Battle clBattle = ClientStuff.getCurrentBattle();
            if (clBattle != null && clBattle.battleId == msg.battleId) {
                clBattle.endBattle();
                ClientStuff.setBattle(null);
            }
        }

        @Override
        public Class<BattleEndPacket> getPacketClass() {
            return BattleEndPacket.class;
        }
    }
}
