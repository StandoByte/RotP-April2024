package com.github.standobyte.jrpg.network.packets.server;

import java.util.function.Supplier;

import com.github.standobyte.jrpg.client.battleui.BattleScreen;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class GiveChoisePacket {
    
    public GiveChoisePacket() {
    }
    
    
    
    public static class Handler implements IModPacketHandler<GiveChoisePacket> {

        @Override
        public void encode(GiveChoisePacket msg, PacketBuffer buf) {
        }

        @Override
        public GiveChoisePacket decode(PacketBuffer buf) {
            GiveChoisePacket packet = new GiveChoisePacket();
            return packet;
        }

        @Override
        public void handle(GiveChoisePacket msg, Supplier<NetworkEvent.Context> ctx) {
            if (BattleScreen.instance != null) {
                BattleScreen.instance.giveChoise();
            }
        }

        @Override
        public Class<GiveChoisePacket> getPacketClass() {
            return GiveChoisePacket.class;
        }
    }
}
