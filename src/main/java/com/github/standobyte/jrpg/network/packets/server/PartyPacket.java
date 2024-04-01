package com.github.standobyte.jrpg.network.packets.server;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.standobyte.jrpg.client.ClientStuff;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;
import com.github.standobyte.jrpg.party.Party;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class PartyPacket {
    private Party party;
    private PacketBuffer clBuf = null;
    
    public PartyPacket(Party party) {
        this.party = party;
    }
    
    private PartyPacket() {}
    
    
    
    public static class Handler implements IModPacketHandler<PartyPacket> {

        @Override
        public void encode(PartyPacket msg, PacketBuffer buf) {
            msg.party.toBuf(buf);
        }

        @Override
        public PartyPacket decode(PacketBuffer buf) {
            PartyPacket packet = new PartyPacket();
            packet.clBuf = buf;
            return packet;
        }

        @Override
        public void handle(PartyPacket msg, Supplier<NetworkEvent.Context> ctx) {
            Party party = Party.fromBuf(msg.clBuf);
            ClientStuff.currentParty = Optional.ofNullable(party);
        }

        @Override
        public Class<PartyPacket> getPacketClass() {
            return PartyPacket.class;
        }
    }
}
